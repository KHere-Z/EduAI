package com.eduai.system.service.impl;

import com.eduai.common.BusinessException;
import com.eduai.system.dto.PreviewFile;
import com.eduai.system.entity.ResourceFile;
import com.eduai.system.service.ResourcePreviewService;
import com.eduai.system.vo.ArchiveEntryVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 学习资源「30% 试看预览」实现。
 * <p>
 * 预览能力实时判定：pdf / 图片 / Office（LibreOffice 转 pdf）/ zip（按 preview_path 抽内部文件）。
 * 预览文件落盘 uploads/previews/{id}.{pdf|png}，与原始文件隔离，首次生成后缓存复用。
 */
@Slf4j
@Service
public class ResourcePreviewServiceImpl implements ResourcePreviewService {

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    /** 可预览图片扩展名 */
    private static final Set<String> IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "gif");
    /** Office 扩展名（LibreOffice 转 pdf） */
    private static final Set<String> OFFICE_EXTS = Set.of("doc", "docx", "ppt", "pptx", "xls", "xlsx");
    /** zip 解压总大小上限（防 zip 炸弹） */
    private static final long MAX_UNZIP_BYTES = 50L * 1024 * 1024;

    /** 中文字体候选路径（优先级：classpath 捆绑 → Windows → macOS → Linux） */
    private static final String[] CJK_FONT_PATHS = {
            "fonts/NotoSansSC-Regular.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "/System/Library/Fonts/PingFang.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"
    };

    /** 按资源 ID 的生成锁，防并发首次预览重复生成 */
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    @Override
    public List<ArchiveEntryVO> inspectArchive(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (!"zip".equals(extOf(file.getOriginalFilename()))) {
            throw new BusinessException(400, "仅支持 zip 格式");
        }

        List<ArchiveEntryVO> result = new ArrayList<>();
        Path tmp = null;
        try {
            tmp = Files.createTempFile("inspect-", ".zip");
            Files.copy(file.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);
            try (ZipFile zip = ZipFile.builder().setFile(tmp.toFile()).get()) {
                Enumeration<ZipArchiveEntry> entries = zip.getEntries();
                while (entries.hasMoreElements()) {
                    ZipArchiveEntry e = entries.nextElement();
                    if (e.isDirectory()) continue;
                    String path = e.getName().replace("\\", "/");
                    // 跳过非法路径（绝对路径 / 穿越）
                    if (path.startsWith("/") || path.contains("../")) continue;
                    String fileName = path.contains("/")
                            ? path.substring(path.lastIndexOf('/') + 1) : path;
                    String innerExt = extOf(fileName);
                    boolean previewable = "pdf".equals(innerExt)
                            || IMAGE_EXTS.contains(innerExt) || OFFICE_EXTS.contains(innerExt);
                    result.add(new ArchiveEntryVO(path, fileName, e.getSize(), previewable));
                }
            }
        } catch (IOException e) {
            throw new BusinessException(400, "zip 解析失败: " + e.getMessage());
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
        return result;
    }

    @Override
    public String resolvePreviewType(ResourceFile resource) {
        if (resource == null || resource.getFileName() == null) return "none";
        String ext = extOf(resource.getFileName());
        if (ext == null) return "none";
        if ("pdf".equals(ext)) return "pdf";
        if (IMAGE_EXTS.contains(ext)) return "image";
        if (OFFICE_EXTS.contains(ext)) return "pdf";
        if ("zip".equals(ext)) {
            String previewPath = resource.getPreviewPath();
            if (previewPath == null || previewPath.isBlank()) return "none";
            String innerExt = extOf(previewPath);
            if (innerExt == null) return "none";
            if ("pdf".equals(innerExt)) return "pdf";
            if (IMAGE_EXTS.contains(innerExt)) return "image";
            if (OFFICE_EXTS.contains(innerExt)) return "pdf";
            return "none";
        }
        return "none";
    }

    @Override
    public PreviewFile getPreviewFile(ResourceFile resource) {
        String type = resolvePreviewType(resource);
        if ("none".equals(type)) {
            throw new BusinessException(404, "该资源无预览内容");
        }
        if (resource.getFilePath() == null || resource.getFilePath().isBlank()) {
            throw new BusinessException(404, "文件路径缺失");
        }
        Path src = Path.of(resource.getFilePath());
        if (!Files.exists(src)) {
            throw new BusinessException(404, "文件已被删除");
        }

        Object lock = locks.computeIfAbsent(resource.getId(), k -> new Object());
        synchronized (lock) {
            Path previewFile = previewFilePath(resource.getId(), type);
            if (!Files.exists(previewFile)) {
                try {
                    generatePreview(resource, src, type, previewFile);
                } catch (BusinessException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("生成预览失败 id={} fileName={}", resource.getId(), resource.getFileName(), e);
                    throw new BusinessException(500, "预览生成失败");
                }
            }
            return new PreviewFile(resource.getFileName(),
                    "pdf".equals(type) ? "application/pdf" : "image/png",
                    new FileSystemResource(previewFile));
        }
    }

    // ==================== 内部方法 ====================

    /** 预览文件落盘路径：uploads/previews/{id}.pdf / {id}.png */
    private Path previewFilePath(Long id, String type) {
        return Path.of(uploadDir, "previews", id + ("pdf".equals(type) ? ".pdf" : ".png"));
    }

    /** 按源文件类型分派预览生成 */
    private void generatePreview(ResourceFile resource, Path src, String type, Path target) throws Exception {
        String ext = extOf(resource.getFileName());
        if ("zip".equals(ext)) {
            generateFromZip(resource, src, target);
        } else if ("pdf".equals(type) && OFFICE_EXTS.contains(ext)) {
            Path converted = convertOfficeToPdf(src);
            try {
                generatePdfPreview(converted, target);
            } finally {
                deleteRecursively(converted.getParent());
            }
        } else if ("pdf".equals(type)) {
            generatePdfPreview(src, target);
        } else {
            generateImagePreview(src, target);
        }
    }

    /** zip：解压 → 按 preview_path 定位内部文件 → 按内部类型递归生成 */
    private void generateFromZip(ResourceFile resource, Path zipPath, Path target) throws Exception {
        String previewPath = resource.getPreviewPath();
        Path tmpDir = Files.createTempDirectory("zip-preview-");
        try {
            Path inner = extractEntry(zipPath, previewPath, tmpDir);
            if (inner == null) {
                throw new BusinessException(404, "压缩包内未找到预览入口文件");
            }
            String innerExt = extOf(previewPath);
            if ("pdf".equals(innerExt)) {
                generatePdfPreview(inner, target);
            } else if (IMAGE_EXTS.contains(innerExt)) {
                generateImagePreview(inner, target);
            } else if (OFFICE_EXTS.contains(innerExt)) {
                Path converted = convertOfficeToPdf(inner);
                try {
                    generatePdfPreview(converted, target);
                } finally {
                    deleteRecursively(converted.getParent());
                }
            } else {
                throw new BusinessException(404, "预览入口文件不可预览");
            }
        } finally {
            deleteRecursively(tmpDir);
        }
    }

    /** 从 zip 提取指定条目（含大小上限 + 路径穿越校验），未命中返回 null */
    private Path extractEntry(Path zipPath, String targetName, Path outDir) throws Exception {
        String wanted = targetName.replace("\\", "/");
        try (ZipFile zip = ZipFile.builder().setFile(zipPath.toFile()).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            long total = 0;
            ZipArchiveEntry target = null;
            while (entries.hasMoreElements()) {
                ZipArchiveEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName().replace("\\", "/");
                if (name.startsWith("/") || name.contains("../")) {
                    throw new BusinessException(400, "压缩包含非法路径");
                }
                total += e.getSize();
                if (total > MAX_UNZIP_BYTES) {
                    throw new BusinessException(400, "压缩包解压大小超限");
                }
                if (name.equals(wanted)) {
                    target = e;
                }
            }
            if (target == null) return null;
            String safeName = Path.of(wanted).getFileName().toString();
            Path dest = outDir.resolve(safeName);
            try (InputStream is = zip.getInputStream(target)) {
                Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        }
    }

    /** PDF：截前 ceil(总页×30%) 页 + 每页水印 */
    private void generatePdfPreview(Path src, Path target) throws Exception {
        try (PDDocument doc = Loader.loadPDF(src.toFile())) {
            int total = doc.getNumberOfPages();
            // 预览页数：≤25 页取前 ceil(总页×20%) 页(最少 1 页)；>25 页固定前 5 页
            int n = total <= 25 ? Math.max(1, (int) Math.ceil(total * 0.2)) : 5;
            for (int i = total - 1; i >= n; i--) {
                doc.removePage(i);
            }
            PDFont font = loadChineseFont(doc);
            String mark = (font instanceof PDType1Font) ? "ZhiXue AI" : "智学AI";
            for (int i = 0; i < n; i++) {
                addWatermark(doc, doc.getPage(i), font, mark);
            }
            Files.createDirectories(target.getParent());
            doc.save(target.toFile());
        }
    }

    /** 单页水印：半透明灰色斜向「预览」平铺 */
    private void addWatermark(PDDocument doc, PDPage page, PDFont font, String mark) throws IOException {
        float w = page.getMediaBox().getWidth();
        float h = page.getMediaBox().getHeight();
        try (PDPageContentStream cs = new PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(0.3f);
            cs.setGraphicsStateParameters(gs);
            cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
            float fontSize = Math.max(40, Math.min(w, h) / 6);
            cs.setFont(font, fontSize);
            float stepX = fontSize * 4;
            float stepY = fontSize * 3;
            for (float y = -h; y < h * 1.5f; y += stepY) {
                for (float x = -w; x < w * 1.5f; x += stepX) {
                    cs.beginText();
                    cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(30), x, y));
                    cs.showText(mark);
                    cs.endText();
                }
            }
        }
    }

    /** 图片：只加水印、不截断、不改尺寸，统一输出 PNG */
    private void generateImagePreview(Path src, Path target) throws Exception {
        BufferedImage img = ImageIO.read(src.toFile());
        if (img == null) {
            throw new BusinessException(400, "无法读取图片");
        }
        int w = img.getWidth();
        int h = img.getHeight();
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(120, 120, 120, 80));
            int fontSize = Math.max(30, Math.min(w, h) / 8);
            g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            String mark = "智学AI";
            g.translate(w / 2.0, h / 2.0);
            g.rotate(Math.toRadians(-30));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(mark);
            int stepX = fontSize * 4;
            int stepY = fontSize * 3;
            for (int y = -h; y < h * 2; y += stepY) {
                for (int x = -w; x < w * 2; x += stepX) {
                    g.drawString(mark, x - tw / 2f, y);
                }
            }
        } finally {
            g.dispose();
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(img, "png", target.toFile());
    }

    /** Office → LibreOffice headless 转 PDF，返回临时 PDF 路径（调用方负责清理父目录） */
    private Path convertOfficeToPdf(Path src) throws Exception {
        Path tmpDir = Files.createTempDirectory("soffice-");
        // 独立 user profile：不依赖 HOME（systemd 下 HOME 常不可写，LibreOffice 初始化会报
        // "User installation could not be completed"），且每进程独立 profile 避免并发转换锁冲突。
        Path profileDir = Files.createTempDirectory("lo-profile-");
        Path logFile = tmpDir.resolve("soffice.log");
        try {
            ProcessBuilder pb = new ProcessBuilder("soffice", "--headless",
                    "-env:UserInstallation=file://" + profileDir.toAbsolutePath(),
                    "--convert-to", "pdf", "--outdir", tmpDir.toString(), src.toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile.toFile());
            // 关键：HOME 也指向可写临时目录，绕开 /home/eduai 权限导致的 dconf/User installation 失败。
            pb.environment().put("HOME", profileDir.toString());
            Process p = pb.start();
            boolean finished = p.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                deleteRecursively(tmpDir);
                throw new BusinessException(500, "Office 转 PDF 超时");
            }
            if (p.exitValue() != 0) {
                String output = readLog(logFile);
                log.error("Office 转 PDF 失败 src={} exit={} output={}", src, p.exitValue(), output);
                deleteRecursively(tmpDir);
                throw new BusinessException(500, "Office 转 PDF 失败");
            }
            String baseName = src.getFileName().toString();
            int dot = baseName.lastIndexOf('.');
            String pdfName = (dot > 0 ? baseName.substring(0, dot) : baseName) + ".pdf";
            Path pdf = tmpDir.resolve(pdfName);
            if (!Files.exists(pdf)) {
                String output = readLog(logFile);
                log.error("Office 转 PDF 未生成输出文件 src={} output={}", src, output);
                deleteRecursively(tmpDir);
                throw new BusinessException(500, "Office 转 PDF 未生成输出文件");
            }
            return pdf;
        } finally {
            deleteRecursively(profileDir);
        }
    }

    /** 读取 soffice 输出日志（无/过长则截断），用于失败排障 */
    private String readLog(Path logFile) {
        try {
            if (Files.exists(logFile)) {
                String s = Files.readString(logFile);
                return s.length() > 500 ? s.substring(0, 500) : s;
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    /** 加载中文字体（classpath → 系统路径），全失败回退标准字体（英文水印） */
    private PDFont loadChineseFont(PDDocument doc) {
        for (String p : CJK_FONT_PATHS) {
            try {
                if (p.startsWith("fonts/")) {
                    try (InputStream is = getClass().getClassLoader().getResourceAsStream(p)) {
                        if (is != null) return PDType0Font.load(doc, is);
                    }
                } else {
                    Path f = Path.of(p);
                    if (Files.exists(f)) return PDType0Font.load(doc, f.toFile());
                }
            } catch (Exception ignored) {
                // 尝试下一个候选
            }
        }
        log.warn("未找到中文字体文件，水印回退英文 ZhiXue AI");
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    /** 小写扩展名（无点返回 null） */
    private String extOf(String name) {
        if (name == null) return null;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        return name.substring(dot + 1).toLowerCase();
    }

    /** 递归删除目录（失败静默） */
    private void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {
        }
    }
}
