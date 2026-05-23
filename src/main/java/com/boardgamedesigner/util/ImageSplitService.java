package com.boardgamedesigner.util;

import com.boardgamedesigner.card.model.CardImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片网格分切服务：将一张大图按指定行列数均匀分切成多张卡牌.
 */
public class ImageSplitService {

    /**
     * 将图片按 cols × rows 均匀网格分切.
     *
     * @param sourcePath 源图片路径
     * @param outputDir  输出目录
     * @param cols       列数
     * @param rows       行数
     * @return 切分出的 CardImage 列表
     */
    public List<CardImage> split(Path sourcePath, Path outputDir, int cols, int rows) {
        BufferedImage original;
        try {
            original = ImageIO.read(sourcePath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("无法读取图片: " + sourcePath, e);
        }
        if (original == null) {
            throw new IllegalArgumentException("不支持的图片格式: " + sourcePath);
        }

        int imgW = original.getWidth();
        int imgH = original.getHeight();
        int cardW = imgW / cols;
        int cardH = imgH / rows;

        String baseName = stripExtension(sourcePath.getFileName().toString());
        List<CardImage> result = new ArrayList<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * cardW;
                int y = row * cardH;

                BufferedImage cardImg = original.getSubimage(x, y, cardW, cardH);
                String outName = baseName + "_" + (row + 1) + "x" + (col + 1) + ".png";
                Path outPath = outputDir.resolve(outName);

                try {
                    ImageIO.write(cardImg, "png", outPath.toFile());
                } catch (IOException e) {
                    throw new UncheckedIOException("保存切分图片失败: " + outPath, e);
                }

                result.add(new CardImage(outPath, cardW, cardH));
            }
        }

        return result;
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
