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

    // ==================== 原有登录方式（保留兼容） ====================

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
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
        // 短信验证码校验（选填：传了 phone + code 则必须校验）
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            if (!dto.getPhone().matches(RedisKeys.PHONE_REGEX)) {
                throw new BusinessException(
                        AuthErrorCode.PHONE_FORMAT_INVALID.getCode(),
                        AuthErrorCode.PHONE_FORMAT_INVALID.getMessage());
            }
            if (dto.getCode() == null || dto.getCode().isBlank()) {
                throw new BusinessException(400, "验证码不能为空");
            }
            String codeKey = RedisKeys.smsCodeKey(dto.getPhone());
            String storedCode = redisTemplate.opsForValue().get(codeKey);
            if (storedCode == null || !storedCode.equals(dto.getCode())) {
                throw new BusinessException(
                        AuthErrorCode.SMS_CODE_INVALID.getCode(),
                        AuthErrorCode.SMS_CODE_INVALID.getMessage());
            }
            redisTemplate.delete(codeKey);

            // 手机号唯一性检查
            if (userRepository.existsByPhone(dto.getPhone())) {
                throw new BusinessException(400, "该手机号已被注册");
            }
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        User user = User.builder()
                .uid(generateUid())
                .username(dto.getUsername())
                .password(PasswordUtil.encode(dto.getPassword()))
                .realName(dto.getRealName())
                .roleType(dto.getRoleType())
                .status(1)
                .build();

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.setPhone(dto.getPhone());
        }

        userRepository.save(user);

        // 新用户注册奖励 25 智学点
        pointService.charge(user.getId(), 25, "gift", "新用户注册奖励 25 点");

        if (dto.getRoleType() == 3 && dto.getSubjectIds() != null && !dto.getSubjectIds().isBlank()) {
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

        // 5. 发送短信
        smsService.sendVerifyCode(phone, code);
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

        // 2. 验证码校验（一次性使用）
        String codeKey = RedisKeys.smsCodeKey(req.getPhone());
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            throw new BusinessException(
                    AuthErrorCode.SMS_CODE_INVALID.getCode(),
                    AuthErrorCode.SMS_CODE_INVALID.getMessage());
        }
        redisTemplate.delete(codeKey); // 验证成功立即删除

        // 3. 查找用户
        User user = userRepository.findByPhone(req.getPhone()).orElse(null);

        if (user != null) {
            // 老用户 → 直接登录
            if (user.getStatus() != 1) {
                throw new BusinessException("账号已被禁用");
            }
            doLogin(user);
            return LoginVO.builder()
                    .user(toUserVO(user))
                    .token(StpUtil.getTokenValue())
                    .build();
        }

        // 4. 新用户 → 必须携带角色信息完成注册
        if (req.getRole() == null || req.getRole().isBlank()) {
            throw new BusinessException(
                    AuthErrorCode.ROLE_REQUIRED.getCode(),
                    AuthErrorCode.ROLE_REQUIRED.getMessage());
        }

        user = createUserByRole(req.getPhone(), req.getRole(),
                req.getTeacherInfo(), req.getStudentInfo());
        doLogin(user);
        return LoginVO.builder()
                .user(toUserVO(user))
                .token(StpUtil.getTokenValue())
                .build();
    }

    // ==================== 微信登录 ====================

    @Override
    public LoginVO wechatLogin(WechatLoginRequest req) {
        if (req.getUnionid() == null || req.getUnionid().isBlank()) {
            throw new BusinessException(400, "微信 unionid 不能为空");
        }

        // 查找 unionid 绑定
        Optional<User> boundUser = wechatBindingService.findUserByUnionid(req.getUnionid());

        if (boundUser.isPresent()) {
            // 情况A：已绑定 → 直接登录
            User user = boundUser.get();
            if (user.getStatus() != 1) {
                throw new BusinessException("账号已被禁用");
            }
            doLogin(user);
            return LoginVO.builder()
                    .user(toUserVO(user))
                    .token(StpUtil.getTokenValue())
                    .build();
        }

        // 情况B：未绑定 → 返回需要绑定手机号标记
        return LoginVO.builder()
                .needBindPhone(true)
                .unionid(req.getUnionid())
                .build();
    }

    @Override
    @Transactional
    public LoginVO bindPhone(BindPhoneRequest req) {
        // 1. 验证码校验
        String codeKey = RedisKeys.smsCodeKey(req.getPhone());
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(req.getCode())) {
            throw new BusinessException(
                    AuthErrorCode.SMS_CODE_INVALID.getCode(),
                    AuthErrorCode.SMS_CODE_INVALID.getMessage());
        }
        redisTemplate.delete(codeKey);

        // 2. 绑定唯一性校验（由 WechatBindingService 执行）
        User user = userRepository.findByPhone(req.getPhone()).orElse(null);

        if (user != null) {
            // 手机号已有账号 → 校验绑定冲突后绑定微信
            wechatBindingService.bindWechatToPhone(
                    req.getPhone(), req.getUnionid(), req.getOpenid(), null, null);

            if (user.getStatus() != 1) {
                throw new BusinessException("账号已被禁用");
            }
            doLogin(user);
            return LoginVO.builder()
                    .user(toUserVO(user))
                    .token(StpUtil.getTokenValue())
                    .build();
        }

        // 新手机号 → 创建用户 + 绑定微信（需先校验唯一性）
        if (req.getRole() == null || req.getRole().isBlank()) {
            throw new BusinessException(
                    AuthErrorCode.ROLE_REQUIRED.getCode(),
                    AuthErrorCode.ROLE_REQUIRED.getMessage());
        }

        // 唯一性校验：新手机号也不会被其他微信绑定（双重保险）
        if (wechatRepository.existsByUnionid(req.getUnionid())) {
            throw new BusinessException(
                    AuthErrorCode.WECHAT_ALREADY_BOUND.getCode(),
                    AuthErrorCode.WECHAT_ALREADY_BOUND.getMessage());
        }

        user = createUserByRole(req.getPhone(), req.getRole(),
                req.getTeacherInfo(), req.getStudentInfo());

        // 创建微信绑定
        UserWechat binding = UserWechat.builder()
                .userUid(user.getUid())
                .openid(req.getOpenid())
                .unionid(req.getUnionid())
                .build();
        wechatRepository.save(binding);

        doLogin(user);
        return LoginVO.builder()
                .user(toUserVO(user))
                .token(StpUtil.getTokenValue())
                .build();
    }

    // ==================== 已登录用户微信管理 ====================

    @Override
    @Transactional
    public void bindWechat(Long userId, BindWechatRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.USER_NOT_FOUND.getCode(),
                        AuthErrorCode.USER_NOT_FOUND.getMessage()));
        wechatBindingService.bindWechatToCurrentUser(
                user.getUid(), req.getOpenid(), req.getUnionid(),
                req.getNickname(), req.getAvatar());
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

    /** 根据角色创建用户 */
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

        // 新用户注册奖励 25 智学点
        pointService.charge(user.getId(), 25, "gift", "新用户注册奖励 25 点");

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
