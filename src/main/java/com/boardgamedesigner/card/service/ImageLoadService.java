package com.boardgamedesigner.card.service;

import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.util.FileUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 从文件夹加载卡牌图片的服务.
 */
public class ImageLoadService {

    /**
     * 加载文件夹中的所有图片，跳过无法读取的文件.
     *
     * @param folderPath 图片文件夹路径
     * @return 成功加载的 CardImage 列表
     */
    public List<CardImage> loadFromFolder(Path folderPath) {
        List<Path> imagePaths = FileUtil.scanImageFiles(folderPath);
        List<CardImage> cards = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (Path path : imagePaths) {
            try {
                int[] dimensions = readDimensions(path);
                if (dimensions != null) {
                    cards.add(new CardImage(path, dimensions[0], dimensions[1]));
                } else {
                    failures.add(path.getFileName().toString());
                }
            } catch (IOException e) {
                failures.add(path.getFileName().toString());
            }
        }

        if (!failures.isEmpty() && cards.isEmpty()) {
            throw new RuntimeException("未找到可读取的 PNG/JPG 图片文件。文件夹: " + folderPath);
        }

        return cards;
    }

    /**
     * 快速读取图片尺寸，不将完整图片加载到内存.
     */
    private int[] readDimensions(Path path) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new int[]{
                            reader.getWidth(0),
                            reader.getHeight(0)
                    };
                } finally {
                    reader.dispose();
                }
            }
        }
        return null;
    }

    /**
     * 完整加载一张图片到 BufferedImage.
     */
    public BufferedImage loadFullImage(Path path) throws IOException {
        return ImageIO.read(path.toFile());
    }
}
