package com.boardgamedesigner.card.model;

import javafx.scene.image.Image;

import java.nio.file.Path;

/**
 * 单张卡牌图片的领域模型. 支持复制以创建同一源图的多份实例.
 */
public class CardImage {

    private final Path sourcePath;
    private final String fileName;
    private final int widthPx;
    private final int heightPx;

    public CardImage(Path sourcePath, int widthPx, int heightPx) {
        this.sourcePath = sourcePath;
        this.fileName = sourcePath.getFileName().toString();
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }

    /** 从已有实例复制一份新的列表项（共享同一源文件路径）. */
    public CardImage copy() {
        return new CardImage(this.sourcePath, this.widthPx, this.heightPx);
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public String getFileName() {
        return fileName;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public double getAspectRatio() {
        return (double) widthPx / heightPx;
    }

    public Image createThumbnail(double thumbnailWidth) {
        double ratio = thumbnailWidth / widthPx;
        return new Image(
                sourcePath.toUri().toString(),
                thumbnailWidth,
                heightPx * ratio,
                true,
                true
        );
    }

    @Override
    public String toString() {
        return fileName;
    }
}
