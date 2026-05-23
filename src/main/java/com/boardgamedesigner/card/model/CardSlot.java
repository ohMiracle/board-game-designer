package com.boardgamedesigner.card.model;

import com.boardgamedesigner.card.config.LayoutConfig;

/**
 * 卡牌在 A4 纸面上的一个槽位，包含该位置精确坐标.
 */
public class CardSlot {

    private final CardImage cardImage;
    private final int col;
    private final int row;
    private final double xMm;
    private final double yMm;
    private final double scaledWidthMm;
    private final double scaledHeightMm;

    public CardSlot(CardImage cardImage, int col, int row) {
        this.cardImage = cardImage;
        this.col = col;
        this.row = row;
        this.xMm = LayoutConfig.HORIZONTAL_MARGIN_MM
                + col * (LayoutConfig.CARD_WIDTH_MM + LayoutConfig.HORIZONTAL_MARGIN_MM);
        this.yMm = LayoutConfig.VERTICAL_MARGIN_MM
                + row * (LayoutConfig.CARD_HEIGHT_MM + LayoutConfig.VERTICAL_MARGIN_MM);
        this.scaledWidthMm = LayoutConfig.CARD_WIDTH_MM;
        this.scaledHeightMm = LayoutConfig.CARD_HEIGHT_MM;
    }

    public CardImage getCardImage() {
        return cardImage;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    /**
     * 槽位在 A4 上的 X 坐标（mm，原点左上角）.
     */
    public double getXMm() {
        return xMm;
    }

    /**
     * 槽位在 A4 上的 Y 坐标（mm，原点左上角）.
     */
    public double getYMm() {
        return yMm;
    }

    public double getScaledWidthMm() {
        return scaledWidthMm;
    }

    public double getScaledHeightMm() {
        return scaledHeightMm;
    }

    /**
     * PDF 坐标系下的 X（point，原点左下角）.
     */
    public double getXPoints() {
        return LayoutConfig.cardXPoints(col);
    }

    /**
     * PDF 坐标系下的 Y（point，原点左下角）.
     */
    public double getYPoints() {
        return LayoutConfig.cardYPoints(row);
    }

    /**
     * PDF 坐标系下的宽度（point）.
     */
    public double getWidthPoints() {
        return LayoutConfig.mmToPoints(LayoutConfig.CARD_WIDTH_MM);
    }

    /**
     * PDF 坐标系下的高度（point）.
     */
    public double getHeightPoints() {
        return LayoutConfig.mmToPoints(LayoutConfig.CARD_HEIGHT_MM);
    }

    @Override
    public String toString() {
        return String.format("Slot(%d,%d): %s", col, row, cardImage.getFileName());
    }
}
