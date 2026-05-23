package com.boardgamedesigner.card.config;

/**
 * 排版参数配置。所有尺寸单位为毫米。
 */
public final class LayoutConfig {

    // A4 纸张 (mm)
    public static final double PAPER_WIDTH_MM = 210.0;
    public static final double PAPER_HEIGHT_MM = 297.0;

    // 标准卡牌 (mm)
    public static final double CARD_WIDTH_MM = 63.0;
    public static final double CARD_HEIGHT_MM = 88.0;

    // 网格
    public static final int COLS = 3;
    public static final int ROWS = 3;
    public static final int CARDS_PER_PAGE = COLS * ROWS; // 9

    // PDF 导出 DPI
    public static final int DPI = 300;

    // mm 与 point 转换常量 (1 inch = 25.4 mm, 1 point = 1/72 inch)
    public static final double MM_PER_INCH = 25.4;
    public static final double POINTS_PER_INCH = 72.0;
    public static final double POINTS_PER_MM = POINTS_PER_INCH / MM_PER_INCH;

    // 等距边距计算
    public static final double HORIZONTAL_MARGIN_MM =
            (PAPER_WIDTH_MM - COLS * CARD_WIDTH_MM) / (COLS + 1);

    public static final double VERTICAL_MARGIN_MM =
            (PAPER_HEIGHT_MM - ROWS * CARD_HEIGHT_MM) / (ROWS + 1);

    /**
     * 将毫米转换为 PDF point (PDF 坐标系，72 DPI).
     */
    public static double mmToPoints(double mm) {
        return mm * POINTS_PER_MM;
    }

    /**
     * 计算卡牌在 PDF 页面上的 X 坐标（point，PDF 原点在左下角）.
     */
    public static double cardXPoints(int col) {
        double xMm = HORIZONTAL_MARGIN_MM + col * (CARD_WIDTH_MM + HORIZONTAL_MARGIN_MM);
        return mmToPoints(xMm);
    }

    /**
     * 计算卡牌在 PDF 页面上的 Y 坐标（point，PDF 原点在左下角 — 因此从页面顶部向下计算）.
     */
    public static double cardYPoints(int row) {
        double yFromTopMm = VERTICAL_MARGIN_MM + row * (CARD_HEIGHT_MM + VERTICAL_MARGIN_MM);
        double yFromBottomMm = PAPER_HEIGHT_MM - yFromTopMm - CARD_HEIGHT_MM;
        return mmToPoints(yFromBottomMm);
    }

    /**
     * 卡牌在 300 DPI 下的目标像素尺寸.
     */
    public static int cardWidthPx() {
        return (int) Math.round(CARD_WIDTH_MM / MM_PER_INCH * DPI);
    }

    public static int cardHeightPx() {
        return (int) Math.round(CARD_HEIGHT_MM / MM_PER_INCH * DPI);
    }

    private LayoutConfig() {
    }
}
