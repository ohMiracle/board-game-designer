package com.boardgamedesigner.card.service;

import com.boardgamedesigner.card.config.LayoutConfig;
import com.boardgamedesigner.card.model.CardBack;
import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.card.model.CardSheet;
import com.boardgamedesigner.card.model.CardSlot;
import com.boardgamedesigner.util.ImageScaleUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * A4 PDF 导出服务。将排版好的 CardSheet 列表导出为 PDF 文件.
 */
public class PdfExportService {

    private final ImageLoadService imageLoadService;

    public PdfExportService() {
        this.imageLoadService = new ImageLoadService();
    }

    /**
     * 导出 CardSheet 列表为 PDF 文件.
     *
     * @param sheets     排版好的正面页面
     * @param outputPath 输出 PDF 文件路径
     * @param cardBacks  卡背图片列表（每个卡背生成一整页背面）
     * @return 输出文件路径
     */
    public Path export(java.util.List<CardSheet> sheets, Path outputPath,
                       java.util.List<CardBack> cardBacks) {
        try (PDDocument document = new PDDocument()) {
            for (CardSheet sheet : sheets) {
                addPage(document, sheet);
            }

            for (CardBack cardBack : cardBacks) {
                int pageCount = cardBack.getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    addBackPage(document, cardBack.getImage());
                }
            }

            document.save(outputPath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 导出失败: " + outputPath, e);
        }
        return outputPath;
    }

    /**
     * 无卡背的导出（向后兼容）.
     */
    public Path export(java.util.List<CardSheet> sheets, Path outputPath) {
        return export(sheets, outputPath, java.util.List.of());
    }

    private void addPage(PDDocument document, CardSheet sheet) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            for (CardSlot slot : sheet.getSlots()) {
                drawCard(document, cs, slot);
            }
        }
    }

    private void addBackPage(PDDocument document, CardImage cardBack) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        BufferedImage original = imageLoadService.loadFullImage(cardBack.getSourcePath());
        if (original == null) {
            return;
        }

        BufferedImage processed = ImageScaleUtil.scaleAndCrop(
                original,
                LayoutConfig.cardWidthPx(),
                LayoutConfig.cardHeightPx()
        );

        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, processed);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            for (int col = 0; col < LayoutConfig.COLS; col++) {
                for (int row = 0; row < LayoutConfig.ROWS; row++) {
                    cs.drawImage(
                            pdfImage,
                            (float) LayoutConfig.cardXPoints(col),
                            (float) LayoutConfig.cardYPoints(row),
                            (float) LayoutConfig.mmToPoints(LayoutConfig.CARD_WIDTH_MM),
                            (float) LayoutConfig.mmToPoints(LayoutConfig.CARD_HEIGHT_MM)
                    );
                }
            }
        }
    }

    private void drawCard(PDDocument document, PDPageContentStream cs, CardSlot slot) throws IOException {
        BufferedImage original = imageLoadService.loadFullImage(slot.getCardImage().getSourcePath());
        if (original == null) {
            return;
        }

        BufferedImage processed = ImageScaleUtil.scaleAndCrop(
                original,
                LayoutConfig.cardWidthPx(),
                LayoutConfig.cardHeightPx()
        );

        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, processed);

        cs.drawImage(
                pdfImage,
                (float) slot.getXPoints(),
                (float) slot.getYPoints(),
                (float) slot.getWidthPoints(),
                (float) slot.getHeightPoints()
        );
    }
}
