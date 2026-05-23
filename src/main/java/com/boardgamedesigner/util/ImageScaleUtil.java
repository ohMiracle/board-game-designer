package com.boardgamedesigner.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * 图片缩放裁剪工具。使用 Fill + Center-Crop 策略将任意比例图片适配到目标尺寸.
 */
public final class ImageScaleUtil {

    private ImageScaleUtil() {
    }

    /**
     * 对图片执行 center-crop 缩放。
     * 先等比缩放至铺满目标区域，再居中对齐裁剪多余部分.
     *
     * @param original    原始图片
     * @param targetWidth  目标宽度像素
     * @param targetHeight 目标高度像素
     * @return 缩放裁剪后的图片
     */
    public static BufferedImage scaleAndCrop(BufferedImage original, int targetWidth, int targetHeight) {
        double srcRatio = (double) original.getWidth() / original.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;

        int scaledW, scaledH;
        if (srcRatio > targetRatio) {
            // 图片更宽 — 按目标高度缩放，左右裁剪
            scaledH = targetHeight;
            scaledW = (int) Math.round(targetHeight * srcRatio);
        } else {
            // 图片更高（或相等）— 按目标宽度缩放，上下裁剪
            scaledW = targetWidth;
            scaledH = (int) Math.round(targetWidth / srcRatio);
        }

        // 先缩放到中间尺寸
        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, scaledW, scaledH, null);
        g.dispose();

        // 从中间裁剪
        int cropX = (scaledW - targetWidth) / 2;
        int cropY = (scaledH - targetHeight) / 2;

        return scaled.getSubimage(cropX, cropY, targetWidth, targetHeight);
    }

    /**
     * 仅等比缩放，不裁剪.
     */
    public static BufferedImage scaleToFit(BufferedImage original, int maxWidth, int maxHeight) {
        double scale = Math.min(
                (double) maxWidth / original.getWidth(),
                (double) maxHeight / original.getHeight()
        );
        int w = (int) Math.round(original.getWidth() * scale);
        int h = (int) Math.round(original.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }
}
