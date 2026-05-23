package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.config.LayoutConfig;
import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.card.model.CardSheet;
import com.boardgamedesigner.card.model.CardSlot;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单页 A4 预览 Canvas，固定宽度，高度按 A4 比例计算.
 */
public class PreviewPage extends Canvas {

    private static final double PREVIEW_WIDTH = 340;
    private static final double PREVIEW_HEIGHT = PREVIEW_WIDTH / LayoutConfig.PAPER_WIDTH_MM * LayoutConfig.PAPER_HEIGHT_MM;

    private final Map<String, Image> imageCache = new HashMap<>();

    public PreviewPage() {
        super(PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    /**
     * 绘制该页的正面卡牌.
     */
    public void drawFront(CardSheet sheet, int pageNum, int totalPages) {
        GraphicsContext gc = getGraphicsContext2D();
        double cw = getWidth();
        double ch = getHeight();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cw, ch);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, cw, ch);

        // 页码标签
        gc.setFill(Color.GRAY);
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("第 " + pageNum + " 页 (正面)  / 共 " + totalPages + " 页", cw / 2, ch - 6);

        if (sheet == null || sheet.getSlots().isEmpty()) return;

        for (CardSlot slot : sheet.getSlots()) {
            double x = slot.getXMm() / LayoutConfig.PAPER_WIDTH_MM * cw;
            double y = slot.getYMm() / LayoutConfig.PAPER_HEIGHT_MM * ch;
            double w = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * cw;
            double h = LayoutConfig.CARD_HEIGHT_MM / LayoutConfig.PAPER_HEIGHT_MM * ch;

            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, w, h);

            Image img = loadPreviewImage(slot.getCardImage().getSourcePath());
            if (img != null) {
                gc.drawImage(img, x, y, w, h);
            } else {
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(x, y, w, h);
            }
        }
    }

    /**
     * 绘制该页的背面——与对应正面页卡牌数量一致，列位置左右镜像，方便双面打印.
     */
    public void drawBack(CardImage cardBack, CardSheet frontSheet, int pageNum, int totalPages) {
        GraphicsContext gc = getGraphicsContext2D();
        double cw = getWidth();
        double ch = getHeight();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cw, ch);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, cw, ch);

        gc.setFill(Color.GRAY);
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("第 " + pageNum + " 页 (背面)  / 共 " + totalPages + " 页", cw / 2, ch - 6);

        if (cardBack == null) return;

        Image backImg = loadPreviewImage(cardBack.getSourcePath());
        double bleedMm = LayoutConfig.BLEED_MM;
        double cardW = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * cw;
        double cardH = LayoutConfig.CARD_HEIGHT_MM / LayoutConfig.PAPER_HEIGHT_MM * ch;
        double bleedW = bleedMm / LayoutConfig.PAPER_WIDTH_MM * cw;
        double bleedH = bleedMm / LayoutConfig.PAPER_HEIGHT_MM * ch;

        List<CardSlot> frontSlots = frontSheet != null ? frontSheet.getSlots() : List.of();
        int count = frontSlots.isEmpty() ? LayoutConfig.CARDS_PER_PAGE : frontSlots.size();

        for (int i = 0; i < count; i++) {
            int col = i < frontSlots.size() ? frontSlots.get(i).getCol() : i % LayoutConfig.COLS;
            int row = i < frontSlots.size() ? frontSlots.get(i).getRow() : i / LayoutConfig.COLS;
            int mirroredCol = LayoutConfig.COLS - 1 - col;

            double x = (LayoutConfig.HORIZONTAL_MARGIN_MM
                    + mirroredCol * (LayoutConfig.CARD_WIDTH_MM + LayoutConfig.HORIZONTAL_MARGIN_MM))
                    / LayoutConfig.PAPER_WIDTH_MM * cw;
            double y = (LayoutConfig.VERTICAL_MARGIN_MM
                    + row * (LayoutConfig.CARD_HEIGHT_MM + LayoutConfig.VERTICAL_MARGIN_MM))
                    / LayoutConfig.PAPER_HEIGHT_MM * ch;

            // 卡背外扩 bleedMm，居中放大绘制
            double bleedX = x - bleedW;
            double bleedY = y - bleedH;
            double bleedCardW = cardW + bleedW * 2;
            double bleedCardH = cardH + bleedH * 2;

            if (backImg != null) {
                gc.drawImage(backImg, bleedX, bleedY, bleedCardW, bleedCardH);
            } else {
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(bleedX, bleedY, bleedCardW, bleedCardH);
            }
        }
    }

    private Image loadPreviewImage(Path imagePath) {
        String key = imagePath.toString();
        Image cached = imageCache.get(key);
        if (cached != null) return cached;

        try {
            Image img = new Image(imagePath.toUri().toString(), PREVIEW_WIDTH / 3 * 2, 0, true, true);
            imageCache.put(key, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }
}
