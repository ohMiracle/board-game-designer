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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A4 PDF 导出服务。将排版好的 CardSheet 列表导出为 PDF 文件.
 */
public class PdfExportService {

    private final ImageLoadService imageLoadService;

    public PdfExportService() {
        this.imageLoadService = new ImageLoadService();
    }

    /**
     * 导出已分配卡背的页面为 PDF。正面背面交替排列，背面镜像排版支持双面打印.
     *
     * @param sheets     所有排版好的正面页面（含未分配）
     * @param outputPath 输出 PDF 文件路径
     * @param cardBacks  卡背列表
     * @return 输出文件路径
     */
    public Path export(List<CardSheet> sheets, Path outputPath, List<CardBack> cardBacks) {
        // 按卡背分组正面页，未分配的跳过
        Map<CardBack, List<CardSheet>> backSheets = new LinkedHashMap<>();
        for (CardSheet sheet : sheets) {
            CardBack owner = findBackForSheet(sheet, cardBacks);
            if (owner != null) {
                backSheets.computeIfAbsent(owner, k -> new ArrayList<>()).add(sheet);
            }
        }

        try (PDDocument document = new PDDocument()) {
            for (Map.Entry<CardBack, List<CardSheet>> entry : backSheets.entrySet()) {
                CardBack back = entry.getKey();
                List<CardSheet> frontSheets = entry.getValue();
                int backCount = back.getPageCount();
                int maxLen = Math.max(frontSheets.size(), backCount);

                for (int i = 0; i < maxLen; i++) {
                    if (i < frontSheets.size()) {
                        addFrontPage(document, frontSheets.get(i));
                    }
                    if (i < backCount) {
                        CardSheet frontSheet = i < frontSheets.size() ? frontSheets.get(i) : null;
                        addBackPage(document, back.getImage(), frontSheet);
                    }
                }
            }

            document.save(outputPath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 导出失败: " + outputPath, e);
        }
        return outputPath;
    }

    private CardBack findBackForSheet(CardSheet sheet, List<CardBack> cardBacks) {
        List<CardImage> cards = sheet.getCards();
        if (cards.isEmpty()) return null;
        for (CardBack cb : cardBacks) {
            if (cb.getLinkedCards().contains(cards.get(0))) return cb;
        }
        return null;
    }

    private void addFrontPage(PDDocument document, CardSheet sheet) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            for (CardSlot slot : sheet.getSlots()) {
                drawCard(document, cs, slot);
            }
        }
    }

    private void addBackPage(PDDocument document, CardImage cardBack, CardSheet frontSheet) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        BufferedImage original = imageLoadService.loadFullImage(cardBack.getSourcePath());
        if (original == null) {
            return;
        }

        double bleedMm = LayoutConfig.BLEED_MM;
        int bleedPx = (int) Math.round(bleedMm / LayoutConfig.MM_PER_INCH * LayoutConfig.DPI);
        BufferedImage processed = ImageScaleUtil.scaleAndCrop(
                original,
                LayoutConfig.cardWidthPx() + bleedPx * 2,
                LayoutConfig.cardHeightPx() + bleedPx * 2
        );

        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, processed);

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            List<CardSlot> frontSlots = frontSheet != null ? frontSheet.getSlots() : List.of();
            int count = frontSlots.isEmpty() ? LayoutConfig.CARDS_PER_PAGE : frontSlots.size();

            float bleedPt = (float) LayoutConfig.mmToPoints(bleedMm);
            float cardWPt = (float) LayoutConfig.mmToPoints(LayoutConfig.CARD_WIDTH_MM);
            float cardHPt = (float) LayoutConfig.mmToPoints(LayoutConfig.CARD_HEIGHT_MM);

            for (int i = 0; i < count; i++) {
                int col = i < frontSlots.size() ? frontSlots.get(i).getCol() : i % LayoutConfig.COLS;
                int row = i < frontSlots.size() ? frontSlots.get(i).getRow() : i / LayoutConfig.COLS;
                int mirroredCol = LayoutConfig.COLS - 1 - col;

                cs.drawImage(
                        pdfImage,
                        (float) LayoutConfig.cardXPoints(mirroredCol) - bleedPt,
                        (float) LayoutConfig.cardYPoints(row) - bleedPt,
                        cardWPt + bleedPt * 2,
                        cardHPt + bleedPt * 2
                );
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
