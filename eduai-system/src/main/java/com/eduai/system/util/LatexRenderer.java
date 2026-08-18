package com.eduai.system.util;

import lombok.extern.slf4j.Slf4j;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * LaTeX 数学公式渲染器
 * <p>
 * 使用 JLaTeXMath 将 LaTeX 公式渲染为 PNG 图像，返回 base64 data URL
 * 及基线偏移信息，用于在 Flying Saucer PDF 中与正文正确对齐。
 * </p>
 */
@Slf4j
public final class LatexRenderer {

    private LatexRenderer() {}

    /** 默认渲染字号（与试卷正文 12pt 匹配） */
    private static final float DEFAULT_SIZE = 12f;

    /** 前景色（#333 匹配试卷正文颜色） */
    private static final Color FG_COLOR = new Color(0x333333);

    /** 公式渲染结果：data URL + 基线偏移量 */
    public record FormulaResult(String dataUrl, int width, int height, int depth) {}

    /**
     * 将 LaTeX 公式渲染为带基线信息的 PNG
     *
     * @param latex    LaTeX 公式内容（不含 $ 分隔符）
     * @param fontSize 渲染字号（pt）
     * @return FormulaResult（dataUrl + width + height + depth）
     */
    public static FormulaResult render(String latex, float fontSize) {
        try {
            TeXFormula formula = new TeXFormula(latex);
            TeXIcon icon = formula.new TeXIconBuilder()
                    .setStyle(TeXConstants.STYLE_DISPLAY)
                    .setSize(fontSize)
                    .setWidth(TeXConstants.UNIT_PIXEL, 2048f, TeXConstants.ALIGN_LEFT)
                    .setIsMaxWidth(true)
                    .setInterLineSpacing(TeXConstants.UNIT_PIXEL, fontSize * 1.2f)
                    .build();

            int iconW = icon.getIconWidth();
            int iconH = icon.getIconHeight();
            int iconDepth = icon.getIconDepth();
            int pad = 2;

            if (iconW <= 0) iconW = 40;
            if (iconH <= 0) iconH = (int) (fontSize * 1.5);

            // 图片底部额外留出 depth 空间，让 vertical-align: baseline 时公式基线与文本基线对齐
            int imgW = iconW + pad * 2;
            int imgH = iconH + pad * 2;

            BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 透明背景
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, imgW, imgH);
            g.setComposite(AlphaComposite.SrcOver);

            // 在 (pad, pad) 绘制公式
            icon.paintIcon(null, g, pad, pad);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());

            // depth = 公式底部到基线的距离 + padding = iconDepth + pad
            int depth = iconDepth + pad;

            return new FormulaResult(dataUrl, imgW, imgH, depth);

        } catch (Exception e) {
            log.warn("LaTeX 渲染失败: latex={}, err={}", latex, e.getMessage());
            throw new RuntimeException("公式渲染失败: " + latex, e);
        }
    }

    /** 使用默认字号渲染 */
    public static FormulaResult render(String latex) {
        return render(latex, DEFAULT_SIZE);
    }
}
