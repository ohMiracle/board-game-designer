package com.boardgamedesigner.util;

import com.boardgamedesigner.card.config.LayoutConfig;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageScaleUtilTest {

    @Test
    void scaleAndCropWideImage() {
        // 800x400 = 宽高比 2.0，目标 200x200 = 1.0
        // 图片更宽，应按高度缩放：scaledH=200, scaledW=200*2.0=400
        // 裁剪: cropX=(400-200)/2=100, cropY=0
        BufferedImage original = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = ImageScaleUtil.scaleAndCrop(original, 200, 200);

        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(200);
    }

    @Test
    void scaleAndCropTallImage() {
        // 400x800 = 宽高比 0.5，目标 200x200 = 1.0
        // 图片更高，应按宽度缩放：scaledW=200, scaledH=200/0.5=400
        // 裁剪: cropX=0, cropY=(400-200)/2=100
        BufferedImage original = new BufferedImage(400, 800, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = ImageScaleUtil.scaleAndCrop(original, 200, 200);

        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(200);
    }

    @Test
    void scaleAndCropExactMatch() {
        // 比例正好匹配
        BufferedImage original = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = ImageScaleUtil.scaleAndCrop(original, 200, 200);

        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(200);
    }

    @Test
    void scaleAndCropOutputMatchesCardDimensions() {
        // 验证目标卡牌尺寸输出
        int cardW = LayoutConfig.cardWidthPx();
        int cardH = LayoutConfig.cardHeightPx();

        // 使用不同比例的图片测试
        BufferedImage wide = new BufferedImage(1920, 800, BufferedImage.TYPE_INT_RGB);
        BufferedImage tall = new BufferedImage(800, 1920, BufferedImage.TYPE_INT_RGB);

        BufferedImage resultWide = ImageScaleUtil.scaleAndCrop(wide, cardW, cardH);
        BufferedImage resultTall = ImageScaleUtil.scaleAndCrop(tall, cardW, cardH);

        assertThat(resultWide.getWidth()).isEqualTo(cardW);
        assertThat(resultWide.getHeight()).isEqualTo(cardH);
        assertThat(resultTall.getWidth()).isEqualTo(cardW);
        assertThat(resultTall.getHeight()).isEqualTo(cardH);
    }

    @Test
    void scaleToFitPreservesAspectRatio() {
        BufferedImage original = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = ImageScaleUtil.scaleToFit(original, 200, 200);

        // 800x400 适配 200x200 框：按宽度 200 缩放，高度 = 400*(200/800) = 100
        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(100);
    }
}
