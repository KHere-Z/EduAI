package com.eduai.security.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.eduai.common.BusinessException;
import com.eduai.common.Result;
import com.eduai.common.storage.CosStorageService;
import com.eduai.common.util.PasswordUtil;
import com.eduai.security.constant.RedisKeys;
import com.eduai.security.dto.*;
import com.eduai.security.entity.Teacher;
import com.eduai.security.entity.User;
import com.eduai.security.entity.UserWechat;
import com.eduai.security.enums.AuthErrorCode;
import com.eduai.security.enums.RoleEnum;
import com.eduai.security.repository.OrganizationRepository;
import com.eduai.security.repository.TeacherRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.repository.UserWechatRepository;
import com.eduai.security.service.AuthService;
import com.eduai.security.service.PointService;
import com.eduai.security.service.SmsService;
import com.eduai.security.service.WechatBindingService;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 认证服务实现
 * <p>
 * 支持：用户名密码 / 手机号短信 / 微信开放平台 三种登录方式。
 * 微信绑定唯一性由 {@link WechatBindingService} 保证。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final OrganizationRepository organizationRepository;
    private final UserWechatRepository wechatRepository;
    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final WechatBindingService wechatBindingService;
    private final JdbcTemplate jdbcTemplate;
    private final CosStorageService cosStorageService;
    private final PointService pointService;

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 验证码「比对成功才删除」的原子脚本：匹配返回 1，否则返回 0（不删）。
     * <p>
     * 为什么必须原子：写成 {@code get()} 再 {@code delete()} 两步的话，两者之间没有原子性，
     * 同码并发的两个注册请求会**都**读到码、都通过校验 → 可能建出两个同手机号账号
     *（{@code uk_phone} 尚未上线，DB 层拦不住）。之后该号 {@code findByPhone} 返回多行，
     * 抛 {@code IncorrectResultSizeDataAccessException}，这个号就永久登不上了。
     * <p>
     * 用 Lua 而非 Redis 6.2+ 的 {@code GETDEL}：目标机是 Redis 6，6.2 以下没有该命令；
     * Lua 从 2.6 起就有。<b>仅注册流程用它</b> —— 短信登录刻意**不能**提前消费验证码，
     * 见 {@code loginBySms} 里的顺序约束。
     */
    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    // ==================== 原有登录方式（保留兼容） ====================

    @Override
    public LoginVO login(LoginDTO dto) {
        // 登录标识：先按用户名匹配，未命中再按手机号匹配。
        // 前端是同一个输入框（提示「用户名 / 手机号」，字段名仍为 username），
        // 所以后端只需把匹配条件从「只按用户名」放宽为「用户名或手机号」，请求体不变。
        //
        // 这里用两步 or() 而非 findByUsernameOrPhone 单条 OR 查询，是因为：
        // 若某用户的 username 恰好长得像手机号、而另一个用户的 phone 正是这个号，
        // OR 查询会命中两行，Spring Data 的 Optional 签名会抛
        // IncorrectResultSizeDataAccessException → 前端拿到 500。
        // 两步走天然有优先级（用户名优先），且任何情况下都只取一行。
        String identifier = dto.getUsername();
        User user = userRepository.findByUsername(identifier)
                // 手机号入库前经 PHONE_REGEX(^1[3-9]\d{9}$) 校验后原样存储，
                // 即 11 位纯数字、不带 +86；用户手输可能带空格，故这条路径做 trim
                .or(() -> userRepository.findByPhone(identifier.trim()))
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 存量明文密码命中 → 透明升级为 BCrypt 哈希
        if (!PasswordUtil.isBcrypt(user.getPassword())) {
            user.setPassword(PasswordUtil.encode(dto.getPassword()));
            userRepository.save(user);
            log.info("用户 {} 密码已透明升级为 BCrypt", user.getId());
        }

        doLogin(user);
        String token = StpUtil.getTokenValue();

        return LoginVO.builder()
                .user(toUserVO(user))
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // 手机号必填（@NotBlank 已兜一层，此处 null 归一为 "" 走同一个格式错误，不抛 NPE）。
        // 统一 trim 后使用：让「格式校验 / 取验证码 key / 唯一性检查 / 落库」四处拿到同一个值，
        // 否则带空格的号码可能存进库，之后 findByPhone 永远匹配不上。
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (!phone.matches(RedisKeys.PHONE_REGEX)) {
            throw new BusinessException(
                    AuthErrorCode.PHONE_FORMAT_INVALID.getCode(),
                    AuthErrorCode.PHONE_FORMAT_INVALID.getMessage());
        }

        // 角色白名单：自助注册只允许 老师(3) / 学生(4)。
        // RoleEnum 不含管理员(1)，故此处天然拒绝越权注册管理员。
        RoleEnum roleEnum;
        try {
            roleEnum = RoleEnum.fromDbValue(dto.getRoleType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色类型: " + dto.getRoleType());
        }

        // 用户名唯一性检查。刻意放在验证码校验**之前**：用户名被占用是最常见的失败，
        // 不该为此烧掉用户手里那条验证码 —— 否则用户得等 sendSms 的 60 秒频控
        //（SMS_LIMIT_TTL）才能重来。用户名不构成枚举风险，前端本来就要把这个提示展示给用户。
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 验证码校验 + 一次性消费（原子：比对成功才删，见 CONSUME_CODE_SCRIPT）
        //
        // ⚠️ 顺序约束，不要改动：验证码校验必须留在下面 existsByPhone **之前**。
        // 一旦把手机号查询提前，攻击者用任意垃圾验证码打本接口，就能靠
        // 40003（码错）与「该手机号已被注册」的差异免费枚举已注册手机号。
        String codeKey = RedisKeys.smsCodeKey(phone);
        String submittedCode = dto.getCode() == null ? "" : dto.getCode();
        Long consumed = redisTemplate.execute(
                CONSUME_CODE_SCRIPT, Collections.singletonList(codeKey), submittedCode);
        if (consumed == null || consumed == 0L) {
            throw new BusinessException(
                    AuthErrorCode.SMS_CODE_INVALID.getCode(),
                    AuthErrorCode.SMS_CODE_INVALID.getMessage());
        }

        // 手机号唯一性检查（放在消费之后，见上面的顺序约束）
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(400, "该手机号已被注册");
        }

        User user = User.builder()
                .uid(generateUid())
                .username(dto.getUsername())
                .password(PasswordUtil.encode(dto.getPassword()))
                .realName(dto.getRealName())
                .roleType(roleEnum.getDbValue())
                .status(1)
                .phone(phone)
                .build();

        userRepository.save(user);

        // 新用户注册欢迎礼包：49 智学点 + 7 天体验会员
        pointService.charge(user.getId(), 49, "gift", "新用户注册奖励 49 点");
        // 体验会员本身不赠点，点数由上面那笔单独发放（grantTrialMembership 内部刻意不 charge）
        pointService.grantTrialMembership(user.getId(), 7);

        if (roleEnum == RoleEnum.TEACHER && dto.getSubjectIds() != null && !dto.getSubjectIds().isBlank()) {
            Teacher teacher = Teacher.builder()
                    .userId(user.getId())
                    .subjectIds(dto.getSubjectIds())
                    .orgId(dto.getOrgId())
                    .title(dto.getTitle())
                    .build();
            teacherRepository.save(teacher);
        }

        return toUserVO(user);
    }

    @Override
    public UserVO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getUid() == null) {
            user.setUid(generateUid());
            userRepository.save(user);
            log.info("用户 {} 补全 uid={}", userId, user.getUid());
        }
        return toUserVO(user);
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getSubjects() != null) {
            user.setSubjects(toJson(req.getSubjects()));
            // 同步 teachers.subject_ids（管理员端读取）
            teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                teacher.setSubjectIds(String.join(",", req.getSubjects()));
                teacherRepository.save(teacher);
            });
            // 同步 students.subjects（学生端、管理员端读取）
            if (user.getRoleType() != null && user.getRoleType() == 4) {
                jdbcTemplate.update(
                        "UPDATE students SET subjects = ? WHERE user_id = ?",
                        toJson(req.getSubjects()), user.getId());
            }
        }

        // 年级 / 学校（学生个人中心可编辑，写 students 表）
        if (req.getGrade() != null || req.getSchool() != null) {
            saveStudentGradeSchool(user, req.getGrade(), req.getSchool());
        }

        // 手机号变更需验证唯一性
        if (req.getPhone() != null && !req.getPhone().isBlank()
                && !req.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new BusinessException(400, "该手机号已被其他账号绑定");
            }
            user.setPhone(req.getPhone());
        }

        userRepository.save(user);
        log.info("个人信息已更新: uid={}", user.getUid());
        return toUserVO(user);
    }

    /** 学生档案：写 grade/school 到 students 表（无记录则自动创建） */
    private void saveStudentGradeSchool(User user, String grade, String school) {
        if (user.getRoleType() == null || user.getRoleType() != 4) {
            return;
        }
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE user_id = ?", Integer.class, user.getId());
        if (cnt == null || cnt == 0) {
            String name = user.getRealName();
            if (name == null || name.isBlank()) name = user.getNickname();
            if (name == null || name.isBlank()) name = "新同学";
            jdbcTemplate.update(
                    "INSERT INTO students (name, user_id, grade, school, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                    name, user.getId(), grade, school);
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE students SET ");
        List<Object> args = new ArrayList<>();
        if (grade != null) {
            sql.append("grade = ?, ");
            args.add(grade);
        }
        if (school != null) {
            sql.append("school = ?, ");
            args.add(school);
        }
        sql.setLength(sql.length() - 2); // 去掉末尾 ", "
        sql.append(" WHERE user_id = ?");
        args.add(user.getId());
        jdbcTemplate.update(sql.toString(), args.toArray());
    }

    @Override
    @Transactional
    public Map<String, String> uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "头像文件不能为空");
        }
        // 仅允许图片类型，防止上传任意文件
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "头像仅支持图片文件");
        }
        // 头像 512×512，限制 5MB 足够
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(400, "头像文件不能超过 5MB");
        }

        String ext = extractExt(contentType, file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(400, "头像文件读取失败");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String name = UUID.randomUUID() + "." + ext;
        String key = "avatars/" + dateDir + "/" + name;

        // 优先 COS，回退本地磁盘（与题库图片一致）
        String avatarUrl = cosStorageService.upload(bytes, key);
        if (avatarUrl == null) {
            try {
                Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("avatars").resolve(dateDir);
                Files.createDirectories(dir);
                Files.write(dir.resolve(name), bytes);
                avatarUrl = "/uploads/avatars/" + dateDir + "/" + name;
            } catch (Exception e) {
                log.error("头像本地落盘失败: {}", e.getMessage(), e);
                throw new BusinessException(500, "头像上传失败，请稍后重试");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        log.info("头像已更新: uid={}, url={}", user.getUid(), avatarUrl);

        return Map.of("url", avatarUrl);
    }

    /** 从 Content-Type / 文件名提取扩展名，非法则回退 png */
    private String extractExt(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.contains("png")) return "png";
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
            if (contentType.contains("gif")) return "gif";
            if (contentType.contains("webp")) return "webp";
        }
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (Set.of("png", "jpg", "jpeg", "gif", "webp").contains(ext)) return "jpg".equals(ext) ? "jpg" : ext;
        }
        return "png";
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    // ==================== 手机号短信登录 ====================

    @Override
    public void sendSms(String phone) {
        // 1. 格式校验
        if (!phone.matches(RedisKeys.PHONE_REGEX)) {
            throw new BusinessException(
                    AuthErrorCode.PHONE_FORMAT_INVALID.getCode(),
                    AuthErrorCode.PHONE_FORMAT_INVALID.getMessage());
        }

        // 2. 发送限流检查
        String limitKey = RedisKeys.smsLimitKey(phone);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException(
                    AuthErrorCode.SMS_RATE_LIMITED.getCode(),
                    AuthErrorCode.SMS_RATE_LIMITED.getMessage());
        }

        // 3. 生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 存入 Redis（先存，确保短信发送后验证码可用）
        redisTemplate.opsForValue().set(
                RedisKeys.smsCodeKey(phone), code, Duration.ofSeconds(RedisKeys.SMS_CODE_TTL));
        redisTemplate.opsForValue().set(
                limitKey, "1", Duration.ofSeconds(RedisKeys.SMS_LIMIT_TTL));

        // 5. 发送短信（有效分钟数取自 Redis TTL，避免短信文案与实际有效期漂移）
        smsService.sendVerifyCode(phone, code, (int) (RedisKeys.SMS_CODE_TTL / 60));
    }

    @Override
    @Transactional
    public LoginVO loginBySms(LoginBySmsRequest req) {
        // 1. 格式校验
        if (!req.getPhone().matches(RedisKeys.PHONE_REGEX)) {
            throw new BusinessException(
                    AuthErrorCode.PHONE_FORMAT_INVALID.getCode(),
                    AuthErrorCode.PHONE_FORMAT_INVALID.getMessage());
        }

        // 2. 验证码校验 —— 此处**只校验、不消费**，删除挪到第 4 步手机号确认已注册之后。
        //    原因：未注册手机号会返回 40012，前端据此引导到 /auth/register；
        //    而 register 校验的是同一个 Redis key（sms:code:{phone}）。若在这里就删掉，
        //    用户到了注册页手里的码已失效，只能重新发码，而重发会撞上 sendSms 的
        //    60 秒频控（SMS_LIMIT_TTL）→ 用户被迫干等。保留后注册页可**直接复用**这条码
        //    （TTL 300s 内有效），既省一次等待，也省一条短信。
        //
        //    ⚠️ 顺序约束，不要改动：验证码校验必须留在 findByPhone **之前**。
        //    一旦把手机号查询提前，攻击者就能用任意垃圾验证码打本接口，靠
        //    40003（码错）与 40012（号未注册）的差异免费枚举已注册手机号。
        String codeKey = RedisKeys.smsCodeKey(req.getPhone());
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            throw new BusinessException(
                    AuthErrorCode.SMS_CODE_INVALID.getCode(),
                    AuthErrorCode.SMS_CODE_INVALID.getMessage());
        }

        // 3. 查找用户 —— 未注册直接拒绝，不再自动建号。
        //    口径：短信验证码只用于「已注册用户」登录；新用户必须先走 /auth/register，
        //    在那里完成角色选择、用户名/密码设置与手机号唯一性校验。
        //    移除自动建号的两个理由：
        //      1. 它绕过了 register 的完整校验，等于开了一条无密码建号通道
        //         （建出来的号 username / password 均为 null）；
        //      2. findByPhone + insert 是「先查后插」的 TOCTOU，并发下可造出同号多账号，
        //         之后该号 findByPhone 会直接抛 IncorrectResultSizeDataAccessException。
        //
        //    注意：本步失败（抛 40012）时**刻意不消费验证码**，留给 /auth/register 复用，
        //    这是上面「只校验不消费」的目的所在。
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.PHONE_NOT_REGISTERED.getCode(),
                        AuthErrorCode.PHONE_NOT_REGISTERED.getMessage()));

        // 4. 手机号已注册 → 验证码正式消费（一次性），校验状态后登录
        redisTemplate.delete(codeKey);

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        doLogin(user);
        return LoginVO.builder()
                .user(toUserVO(user))
                .token(StpUtil.getTokenValue())
                .build();
    }

    // ==================== 微信登录 ====================

    @Override
    public LoginVO wechatLogin(WechatLoginRequest req) {
        // ⚠️ 已停用（fail-closed）。原实现直接信任客户端传来的 unionid
        // （WechatLoginRequest.unionid），任何人构造一个 unionid 即可登录对应账号。
        // 当前 user_wechat 为空，暂无可利用面；但一旦微信登录真正上线，即为账号接管。
        //
        // 重新开放前必须完成（见 Phase 0 第 2 条）：
        //   1. 客户端只传微信授权 code，openid/unionid 由服务端调
        //      /sns/oauth2/access_token（网站应用）或 /sns/jscode2session（小程序）换取；
        //   2. 换取结果存 Redis 一次性 ticket（TTL 5-10min），LoginVO 只回 ticket 不回 unionid；
        //   3. bindPhone 凭 ticket 取身份，客户端全程不得接触 unionid。
        throw new BusinessException(
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getCode(),
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getMessage());
    }

    @Override
    @Transactional
    public LoginVO bindPhone(BindPhoneRequest req) {
        // ⚠️ 已停用（fail-closed）。原实现从入参取 unionid/openid 并直接落库绑定，
        // 而这两个值来自客户端、无法验证真伪 → 可被用来抢注他人的微信 unionid。
        //
        // 重新开放时必须重建为（见 Phase 0 第 2 条）：
        //   1. 入参不再含 unionid/openid，只收一次性 bindTicket；
        //   2. 校验手机号短信验证码（原逻辑，一次性消费 Redis key）；
        //   3. 凭 ticket 从 Redis 取回服务端换来的 openid/unionid（取后即删）；
        //   4. 手机号已有账号 → 校验双向唯一性后绑定到该账号并登录（不新建）；
        //      手机号无账号 → createUserByRole 建号 + 建绑定；
        //   5. 绑定唯一性仍复用 WechatBindingService.validateBindingUniqueness。
        throw new BusinessException(
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getCode(),
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getMessage());
    }

    // ==================== 已登录用户微信管理 ====================

    @Override
    @Transactional
    public void bindWechat(Long userId, BindWechatRequest req) {
        // ⚠️ 已停用（fail-closed）：同 bindPhone，入参 openid/unionid 不可信。
        // 重新开放时改为收一次性 bindTicket，凭 ticket 取回服务端换来的身份。
        throw new BusinessException(
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getCode(),
                AuthErrorCode.WECHAT_LOGIN_DISABLED.getMessage());
    }

    @Override
    @Transactional
    public void unbindWechat(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.USER_NOT_FOUND.getCode(),
                        AuthErrorCode.USER_NOT_FOUND.getMessage()));
        wechatBindingService.unbindWechat(user.getUid());
    }

    // ==================== 内部辅助方法 ====================

    /** 雪花算法实例（全局复用） */
    private static final cn.hutool.core.lang.Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    /** 生成雪花算法 UID（取后8位） */
    public static long generateUid() {
        return SNOWFLAKE.nextId() % 100_000_000;
    }

    /** 登录：更新 lastLogin，并补全 uid（老用户 uid 为空时惰性生成） */
    private void doLogin(User user) {
        StpUtil.login(user.getId());
        user.setLastLogin(LocalDateTime.now());
        if (user.getUid() == null) {
            user.setUid(generateUid());
            log.info("用户 {} 登录时补全 uid={}", user.getId(), user.getUid());
        }
        userRepository.save(user);
    }

    /**
     * 根据角色创建用户（手机号无账号时的建号逻辑）。
     * <p>
     * ⚠️ 当前**无任何调用点**：原先唯一的调用方 {@code loginBySms} 的自动建号分支已移除
     * （改为未注册直接返回 40012）。保留本方法是为微信绑定重建做准备 ——
     * 见 {@link #bindPhone} 注释第 4 条「手机号无账号 → createUserByRole 建号 + 建绑定」。
     * 若微信绑定最终不走这条路径，可连同 {@code TeacherRegisterInfo} /
     * {@code StudentRegisterInfo} 一并删除。
     */
    private User createUserByRole(String phone, String role,
                                   TeacherRegisterInfo teacherInfo,
                                   StudentRegisterInfo studentInfo) {
        RoleEnum roleEnum;
        try {
            roleEnum = RoleEnum.fromCode(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色类型: " + role);
        }

        User user = User.builder()
                .uid(generateUid())
                .phone(phone)
                .roleType(roleEnum.getDbValue())
                .status(1)
                .build();

        if (roleEnum == RoleEnum.TEACHER && teacherInfo != null) {
            user.setNickname(teacherInfo.getNickname());
            user.setSubjects(toJson(teacherInfo.getSubjects()));
            if (teacherInfo.getStudentUids() != null && !teacherInfo.getStudentUids().isEmpty()) {
                user.setStudentUids(toJson(teacherInfo.getStudentUids()));
            }
        } else if (roleEnum == RoleEnum.STUDENT && studentInfo != null) {
            user.setNickname(studentInfo.getNickname());
            user.setSubjects(toJson(studentInfo.getSubjects()));
            user.setTeacherUid(studentInfo.getTeacherUid());
        }

        user = userRepository.save(user);

        // 新用户注册欢迎礼包：49 智学点 + 7 天体验会员（与 register 保持同一口径）
        // 注：本方法当前无调用点，为微信绑定重建保留（见方法头注释）
        pointService.charge(user.getId(), 49, "gift", "新用户注册奖励 49 点");
        pointService.grantTrialMembership(user.getId(), 7);

        // 教师自注册：自动创建 teachers 记录
        if (roleEnum == RoleEnum.TEACHER) {
            List<String> subjects = teacherInfo != null ? teacherInfo.getSubjects() : Collections.emptyList();
            Teacher teacher = Teacher.builder()
                    .userId(user.getId())
                    .subjectIds(subjects != null && !subjects.isEmpty() ? String.join(",", subjects) : "")
                    .build();
            teacherRepository.save(teacher);
        }

        log.info("新用户注册: uid={}, phone={}, role={}", user.getUid(), maskPhone(phone), role);
        return user;
    }

    /** Object → JSON string */
    private String toJson(Object obj) {
        try {
            return JSON.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** UID → 8位字符串 */
    public static String formatUid(Long uid) {
        if (uid == null) return null;
        return String.format("%08d", uid);
    }

    /** 8位字符串 → UID */
    public static Long parseUid(String uidStr) {
        if (uidStr == null || uidStr.isBlank()) return null;
        return Long.parseLong(uidStr);
    }

    /** 手机号脱敏 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // ==================== Entity → VO ====================

    private UserVO toUserVO(User user) {
        RoleEnum roleEnum = null;
        try { roleEnum = RoleEnum.fromDbValue(user.getRoleType()); } catch (Exception ignored) {}

        // 解析 JSON 学科
        List<String> subjects = parseJsonArray(user.getSubjects());

        // 教师扩展字段
        List<Long> studentUids = parseJsonLongArray(user.getStudentUids());
        Long orgId = null, teacherUid = user.getTeacherUid();
        String orgName = null, title = null, avatar = user.getAvatar();

        if (user.getRoleType() != null && user.getRoleType() == 3) {
            teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                if (subjects.isEmpty()) {
                    subjects.addAll(parseSubjectIds(teacher.getSubjectIds()));
                }
            });
        }

        // 学生扩展字段（年级 / 学校，从 students 表读）
        String grade = null, school = null;
        if (user.getRoleType() != null && user.getRoleType() == 4) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT grade, school FROM students WHERE user_id = ?", user.getId());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                grade = (String) row.get("grade");
                school = (String) row.get("school");
            }
        }

        // 微信绑定状态
        boolean bindWechat = wechatRepository.findByUserUid(user.getUid()).isPresent();

        return UserVO.builder()
                .uid(formatUid(user.getUid()))
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .phone(user.getPhone())
                .phoneMasked(maskPhone(user.getPhone()))
                .email(user.getEmail())
                .roleType(user.getRoleType())
                .roleName(roleEnum != null ? roleEnum.getLabel() : null)
                .status(user.getStatus())
                .subjects(subjects)
                .studentUids(studentUids)
                .teacherUid(teacherUid)
                .grade(grade)
                .school(school)
                .orgId(orgId)
                .orgName(orgName)
                .title(title)
                .points(user.getPoints())
                .bindWechat(bindWechat)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    /** "math,physics" → ["math","physics"] */
    private List<String> parseSubjectIds(String subjectIds) {
        if (subjectIds == null || subjectIds.isBlank()) return Collections.emptyList();
        return Arrays.asList(subjectIds.split(","));
    }

    /** JSON string → List<String> */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<String> list = JSON.readValue(json, List.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** JSON string → List<Long> */
    private List<Long> parseJsonLongArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Number> list = JSON.readValue(json, List.class);
            if (list == null) return new ArrayList<>();
            return list.stream().map(Number::longValue).toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
