package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.model.CardSheet;
import com.boardgamedesigner.card.model.CardSlot;
import com.boardgamedesigner.card.config.LayoutConfig;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A4 纸面预览自定义控件。在 Canvas 上绘制 3×3 卡牌网格.
 */
public class PreviewPane extends Pane {

    private Canvas canvas;
    private CardSheet currentSheet;

    // 图片缓存，避免每次重绘重新加载
    private final Map<String, Image> imageCache = new HashMap<>();

    // A4 宽高比 210:297 ≈ 0.707
    private static final double A4_RATIO = LayoutConfig.PAPER_WIDTH_MM / LayoutConfig.PAPER_HEIGHT_MM;

    public PreviewPane() {
        this.canvas = new Canvas();
        getChildren().add(canvas);

        // 监听尺寸变化，自动重绘
        widthProperty().addListener((obs, old, val) -> resizeCanvas());
        heightProperty().addListener((obs, old, val) -> resizeCanvas());
    }

    private void resizeCanvas() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) return;

        // 保持 A4 比例，居中显示
        double canvasW, canvasH;
        if (w / h > A4_RATIO) {
            canvasH = h - 20;
            canvasW = canvasH * A4_RATIO;
        } else {
            canvasW = w - 20;
            canvasH = canvasW / A4_RATIO;
        }

        canvas.setWidth(canvasW);
        canvas.setHeight(canvasH);
        canvas.setLayoutX((w - canvasW) / 2);
        canvas.setLayoutY((h - canvasH) / 2);

        drawSheet();
    }

    /**
     * 设置当前要显示的 CardSheet.
     */
    public void setSheet(CardSheet sheet) {
        this.currentSheet = sheet;
        imageCache.clear();
        drawSheet();
    }

    private void drawSheet() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cw = canvas.getWidth();
        double ch = canvas.getHeight();

        // 背景白色
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cw, ch);

        // A4 边框
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, cw, ch);

        if (currentSheet == null || currentSheet.getSlots().isEmpty()) {
            // 空页面提示
            gc.setFill(Color.GRAY);
            gc.fillText("（空页面）", cw / 2 - 20, ch / 2);
            return;
        }

        // 绘制每个卡牌槽位
        for (CardSlot slot : currentSheet.getSlots()) {
            double x = slot.getXMm() / LayoutConfig.PAPER_WIDTH_MM * cw;
            double y = slot.getYMm() / LayoutConfig.PAPER_HEIGHT_MM * ch;
            double w = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * cw;
            double h = LayoutConfig.CARD_HEIGHT_MM / LayoutConfig.PAPER_HEIGHT_MM * ch;

            // 卡牌占位区域
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, w, h);

            // 卡牌图片
            Image img = loadPreviewImage(slot);
            if (img != null) {
                gc.drawImage(img, x, y, w, h);
            } else {
                // 加载中 / 加载失败占位
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(x, y, w, h);
                gc.setFill(Color.GRAY);
                gc.fillText(slot.getCardImage().getFileName(), x + 4, y + h / 2);
            }
        }
    }

    private Image loadPreviewImage(CardSlot slot) {
        String path = slot.getCardImage().getSourcePath().toString();
        Image cached = imageCache.get(path);
        if (cached != null) {
            return cached;
        }

        try {
            // 加载缩略图用于预览
            double previewCardW = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * canvas.getWidth();
            Image img = slot.getCardImage().createThumbnail(previewCardW * 2); // 多加些分辨率
            imageCache.put(path, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 切换为卡背预览，显示当前页面对应背面（3×3 满铺卡背）.
     */
    public void showCardBack(Path cardBackPath) {
        if (cardBackPath == null || currentSheet == null) return;
        // 使用特殊标记让 drawSheet 绘制卡背
        drawBackSheet(cardBackPath);
    }

    private void drawBackSheet(Path cardBackPath) {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cw = canvas.getWidth();
        double ch = canvas.getHeight();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cw, ch);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, cw, ch);

        // 3×3 都画卡背图
        Image backImg = loadBackPreview(cardBackPath);
        double cardW = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * cw;
        double cardH = LayoutConfig.CARD_HEIGHT_MM / LayoutConfig.PAPER_HEIGHT_MM * ch;

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                double x = (LayoutConfig.HORIZONTAL_MARGIN_MM
                        + col * (LayoutConfig.CARD_WIDTH_MM + LayoutConfig.HORIZONTAL_MARGIN_MM))
                        / LayoutConfig.PAPER_WIDTH_MM * cw;
                double y = (LayoutConfig.VERTICAL_MARGIN_MM
                        + row * (LayoutConfig.CARD_HEIGHT_MM + LayoutConfig.VERTICAL_MARGIN_MM))
                        / LayoutConfig.PAPER_HEIGHT_MM * ch;

                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(0.5);
                gc.strokeRect(x, y, cardW, cardH);

                if (backImg != null) {
                    gc.drawImage(backImg, x, y, cardW, cardH);
                } else {
                    gc.setFill(Color.LIGHTGRAY);
                    gc.fillRect(x, y, cardW, cardH);
                }
            }
        }
    }

    private Image loadBackPreview(Path cardBackPath) {
        String key = cardBackPath.toString();
        Image cached = imageCache.get(key);
        if (cached != null) return cached;

        try {
            double previewCardW = LayoutConfig.CARD_WIDTH_MM / LayoutConfig.PAPER_WIDTH_MM * canvas.getWidth();
            Image img = new Image(
                    cardBackPath.toUri().toString(),
                    previewCardW * 2,
                    0,
                    true,
                    true
            );
            imageCache.put(key, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        this.currentSheet = null;
        this.imageCache.clear();
        drawSheet();
    }
}
