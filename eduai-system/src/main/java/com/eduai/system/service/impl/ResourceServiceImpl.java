package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.entity.UserRelation;
import com.eduai.security.repository.UserRelationRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.PointService;
import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.ResourceChapterDTO;
import com.eduai.system.dto.ResourceSectionDTO;
import com.eduai.system.dto.ResourceTextbookDTO;
import com.eduai.system.entity.ResourceChapter;
import com.eduai.system.entity.ResourceDownload;
import com.eduai.system.entity.ResourceFile;
import com.eduai.system.entity.ResourceSection;
import com.eduai.system.entity.ResourceTextbook;
import com.eduai.system.repository.ResourceChapterRepository;
import com.eduai.system.repository.ResourceDownloadRepository;
import com.eduai.system.repository.ResourceFileRepository;
import com.eduai.system.repository.ResourceSectionRepository;
import com.eduai.system.repository.ResourceTextbookRepository;
import com.eduai.system.service.ResourceService;
import com.eduai.system.vo.ResourceFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 学习资源 Service 实现（教材 → 章节 → 小节 → 资源文件，四级目录独立于知识点资源）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceTextbookRepository textbookRepository;
    private final ResourceChapterRepository chapterRepository;
    private final ResourceSectionRepository sectionRepository;
    private final ResourceFileRepository fileRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository relationRepository;
    private final ResourceDownloadRepository resourceDownloadRepository;
    private final PointService pointService;

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    // ==================== 权限校验 ====================

    /** 校验当前用户是否为教师或管理员（roleType=3 或 1） */
    private User checkTeacherOrAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3 && user.getRoleType() != 1) {
            throw new BusinessException(403, "仅教师或管理员可访问");
        }
        return user;
    }

    /** 校验已登录（任意角色） */
    private void checkAuthenticated() {
        StpUtil.checkLogin();
    }

    /** 当前登录用户上下文 */
    private record CurrentUser(Long userId, Long uid, int roleType) {}

    private CurrentUser currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        int roleType = user.getRoleType() != null ? user.getRoleType() : 0;
        return new CurrentUser(userId, user.getUid(), roleType);
    }

    /** 学生已接受师生关系的老师 uid 集合（含 User.teacherUid 兜底） */
    private Set<Long> teacherUidsOfStudent(CurrentUser ctx) {
        Set<Long> uids = new HashSet<>();
        if (ctx.uid == null) return uids;
        List<UserRelation> rels = new ArrayList<>();
        rels.addAll(relationRepository.findByFromUid(ctx.uid));
        rels.addAll(relationRepository.findByToUid(ctx.uid));
        for (UserRelation r : rels) {
            if (!"accepted".equals(r.getStatus())) continue;
            String type = r.getType() == null ? "teacher_student" : r.getType();
            if (!"teacher_student".equals(type)) continue;
            Long otherUid = r.getFromUid().equals(ctx.uid) ? r.getToUid() : r.getFromUid();
            User other = userRepository.findByUid(otherUid).orElse(null);
            if (other != null && other.getRoleType() != null && other.getRoleType() == 3) {
                uids.add(otherUid);
            }
        }
        User me = userRepository.findByUid(ctx.uid).orElse(null);
        if (me != null && me.getTeacherUid() != null) {
            uids.add(me.getTeacherUid());
        }
        return uids;
    }

    /** 当前用户是否可查看该资源（共享 + 私有可见性过滤） */
    private boolean canReadResource(ResourceFile f, CurrentUser ctx) {
        if (ctx.roleType == 1) return true;                       // 管理员看全部
        boolean shared = f.getShared() == null || f.getShared();  // 旧数据缺省共享
        if (shared) return true;                                  // 共享：所有登录用户可见
        if (f.getUploaderId() == null) return true;               // 无 owner 的旧数据视为共享
        if (f.getUploaderId().equals(ctx.userId)) return true;    // 上传者本人
        if (ctx.roleType == 4) {                                  // 学生：owner 为其已接受老师
            Long ownerUid = userRepository.findById(f.getUploaderId())
                    .map(User::getUid).orElse(null);
            return ownerUid != null && teacherUidsOfStudent(ctx).contains(ownerUid);
        }
        return false;
    }

    // ==================== 教材 ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resourceTextbooks", key = "'subject:' + #subject", unless = "#result == null")
    public List<ResourceTextbook> listTextbooks(String subject) {
        checkAuthenticated();
        return textbookRepository.findBySubjectOrderBySortOrderAsc(subject);
    }

    @Override
    @Transactional
    @CacheEvict(value = "resourceTextbooks", allEntries = true)
    public ResourceTextbook createTextbook(ResourceTextbookDTO dto) {
        checkTeacherOrAdmin();
        int sortOrder = textbookRepository.findTopBySubjectOrderBySortOrderDesc(dto.getSubject())
                .map(t -> t.getSortOrder() + 1)
                .orElse(1);
        ResourceTextbook textbook = ResourceTextbook.builder()
                .subject(dto.getSubject())
                .stage(dto.getStage())
                .version(dto.getVersion())
                .name(dto.getName())
                .sortOrder(sortOrder)
                .build();
        return textbookRepository.save(textbook);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "resourceTextbooks", allEntries = true),
            @CacheEvict(value = "resourceChapters", allEntries = true),
            @CacheEvict(value = "resourceSections", allEntries = true)
    })
    public void deleteTextbook(Long id) {
        checkTeacherOrAdmin();
        ResourceTextbook textbook = textbookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "教材不存在"));

        List<ResourceChapter> chapters = chapterRepository.findByTextbookIdOrderBySortOrderAsc(id);
        for (ResourceChapter chapter : chapters) {
            deleteSectionsByChapterId(chapter.getId());
            chapterRepository.delete(chapter);
        }
        textbookRepository.delete(textbook);
        log.info("删除教材 id={} name={}（级联 {} 个章节）", id, textbook.getName(), chapters.size());
    }

    // ==================== 章节 ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resourceChapters", key = "'textbookId:' + #textbookId", unless = "#result == null")
    public List<ResourceChapter> listChapters(Long textbookId) {
        checkAuthenticated();
        return chapterRepository.findByTextbookIdOrderBySortOrderAsc(textbookId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "resourceChapters", allEntries = true)
    public ResourceChapter createChapter(Long textbookId, ResourceChapterDTO dto) {
        checkTeacherOrAdmin();
        textbookRepository.findById(textbookId)
                .orElseThrow(() -> new BusinessException(404, "教材不存在"));

        int sortOrder = chapterRepository.findTopByTextbookIdOrderBySortOrderDesc(textbookId)
                .map(c -> c.getSortOrder() + 1)
                .orElse(1);
        ResourceChapter chapter = ResourceChapter.builder()
                .textbookId(textbookId)
                .name(dto.getName())
                .sortOrder(sortOrder)
                .build();
        return chapterRepository.save(chapter);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "resourceChapters", allEntries = true),
            @CacheEvict(value = "resourceSections", allEntries = true)
    })
    public void deleteChapter(Long id) {
        checkTeacherOrAdmin();
        ResourceChapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));

        deleteSectionsByChapterId(id);
        chapterRepository.delete(chapter);
        log.info("删除章节 id={} name={}", id, chapter.getName());
    }

    // ==================== 小节 ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "resourceSections", key = "'chapterId:' + #chapterId", unless = "#result == null")
    public List<ResourceSection> listSections(Long chapterId) {
        checkAuthenticated();
        return sectionRepository.findByChapterIdOrderBySortOrderAsc(chapterId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "resourceSections", allEntries = true)
    public ResourceSection createSection(Long chapterId, ResourceSectionDTO dto) {
        checkTeacherOrAdmin();
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));

        int sortOrder = sectionRepository.findTopByChapterIdOrderBySortOrderDesc(chapterId)
                .map(s -> s.getSortOrder() + 1)
                .orElse(1);
        ResourceSection section = ResourceSection.builder()
                .chapterId(chapterId)
                .name(dto.getName())
                .sortOrder(sortOrder)
                .build();
        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    @CacheEvict(value = "resourceSections", allEntries = true)
    public void deleteSection(Long id) {
        checkTeacherOrAdmin();
        ResourceSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "小节不存在"));

        deleteFilesBySectionId(id);
        sectionRepository.delete(section);
        log.info("删除小节 id={} name={}", id, section.getName());
    }

    // ==================== 资源文件 ====================

    @Override
    @Transactional(readOnly = true)
    public List<ResourceFileVO> listResources(Long sectionId, String subject) {
        checkAuthenticated();
        CurrentUser ctx = currentUser();
        return fileRepository.findBySectionIdOrderByCreatedAtDesc(sectionId)
                .stream()
                .filter(f -> canReadResource(f, ctx))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ResourceFileVO> uploadResources(Long sectionId, String subject, String tag,
                                                String year, Integer price, Boolean shared,
                                                List<MultipartFile> files) {
        User uploader = checkTeacherOrAdmin();
        sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "小节不存在"));

        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        boolean isShared = shared == null || shared;

        String author = uploader.getNickname() != null ? uploader.getNickname()
                : (uploader.getRealName() != null ? uploader.getRealName() : uploader.getUsername());

        List<ResourceFileVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            ResourceFile saved = saveFile(sectionId, subject, tag, year, price, isShared, author, uploader.getId(), file);
            result.add(toVO(saved));
        }

        if (result.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        log.info("{} 上传学习资源: sectionId={}, tag={}, shared={}, 数量={}", author, sectionId, tag, isShared, result.size());
        return result;
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        checkTeacherOrAdmin();
        ResourceFile resource = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "资源不存在"));

        deletePhysicalFile(resource.getFilePath());
        fileRepository.delete(resource);
        log.info("删除学习资源 id={} fileName={}", id, resource.getFileName());
    }

    @Override
    @Transactional
    public DownloadFile downloadResource(Long id) {
        checkAuthenticated();
        Long userId = StpUtil.getLoginIdAsLong();
        ResourceFile resource = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "资源不存在"));

        if (!canReadResource(resource, currentUser())) {
            throw new BusinessException(403, "无权访问该资源");
        }

        if (resource.getFilePath() == null || resource.getFilePath().isBlank()) {
            throw new BusinessException(404, "文件路径缺失");
        }
        Path filePath = Path.of(resource.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "文件已被删除");
        }

        // 上传者本人下载不扣费、不计分成；同一学生重复下载不重复扣费
        boolean isUploader = resource.getUploaderId() != null
                && resource.getUploaderId().equals(userId);
        if (!isUploader && !resourceDownloadRepository.existsByResourceIdAndUserId(id, userId)) {
            int price = resource.getPrice() == null ? 0 : resource.getPrice();
            // 首次下载：扣费 + 记录下载 + 下载量+1 + 老师分成
            if (price > 0) {
                pointService.consume(userId, price, "下载学习资源《" + resource.getFileName() + "》");
            }
            resourceDownloadRepository.save(ResourceDownload.builder()
                    .resourceId(id)
                    .userId(userId)
                    .build());
            resource.setDownloadCount((resource.getDownloadCount() == null ? 0 : resource.getDownloadCount()) + 1);
            // 50% 分成：转为智学点发放给上传者（1智学点=1分，分成=price/2 点）
            if (price > 0 && resource.getUploaderId() != null) {
                int commission = price / 2;
                if (commission > 0) {
                    pointService.charge(resource.getUploaderId(), commission, "gift",
                            "资源下载分成《" + resource.getFileName() + "》");
                }
            }
        }

        // 流式返回：FileSystemResource 延迟读取磁盘文件，由 ResponseEntity<Resource>
        // 按块写出，避免 Files.readAllBytes 把整个文件载入堆内存。
        return new DownloadFile(resource.getFileName(), new FileSystemResource(filePath));
    }

    // ==================== 内部方法 ====================

    /** 落盘并入库单个文件 */
    private ResourceFile saveFile(Long sectionId, String subject, String tag, String year,
                                  Integer price, boolean shared, String author, Long uploaderId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }

        // 存储路径（相对 uploads/）：uploads/resource/yyyy-MM/uuid.ext
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String storedName = UUID.randomUUID().toString() + ext;
        Path dir = Path.of(uploadDir, "resource", dateDir);
        Path filePath = dir.resolve(storedName);

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }

        ResourceFile resource = ResourceFile.builder()
                .sectionId(sectionId)
                .subject(subject == null || subject.isBlank() ? "math" : subject)
                .type(tag)
                .year(year)
                .price(price == null ? 0 : price)
                .fileName(originalName != null ? originalName : storedName)
                .filePath(filePath.toString().replace("\\", "/"))
                .fileSize(file.getSize())
                .author(author)
                .uploaderId(uploaderId)
                .shared(shared)
                .build();

        return fileRepository.save(resource);
    }

    /** 级联删除某章节下的所有小节及其资源文件 */
    private void deleteSectionsByChapterId(Long chapterId) {
        List<ResourceSection> sections = sectionRepository.findByChapterIdOrderBySortOrderAsc(chapterId);
        for (ResourceSection section : sections) {
            deleteFilesBySectionId(section.getId());
            sectionRepository.delete(section);
        }
    }

    /** 删除某小节下的所有资源文件（含物理文件） */
    private void deleteFilesBySectionId(Long sectionId) {
        List<ResourceFile> files = fileRepository.findBySectionIdOrderByCreatedAtDesc(sectionId);
        for (ResourceFile file : files) {
            deletePhysicalFile(file.getFilePath());
            fileRepository.delete(file);
        }
    }

    /** 删除物理文件（失败仅告警，不阻断） */
    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.warn("删除资源文件失败: path={}, err={}", filePath, e.getMessage());
        }
    }

    /** ResourceFile Entity → VO */
    private ResourceFileVO toVO(ResourceFile r) {
        return ResourceFileVO.builder()
                .id(r.getId())
                .sectionId(r.getSectionId())
                .subject(r.getSubject())
                .type(r.getType())
                .tag(r.getType())
                .year(r.getYear())
                .price(r.getPrice())
                .title(r.getTitle())
                .fileName(r.getFileName())
                .fileSize(r.getFileSize())
                .filePath(r.getFilePath())
                .author(r.getAuthor())
                .uploaderId(r.getUploaderId())
                .shared(r.getShared())
                .downloadCount(r.getDownloadCount())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
