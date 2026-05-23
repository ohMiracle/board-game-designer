package com.boardgamedesigner.card.service;

import com.boardgamedesigner.card.config.LayoutConfig;
import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.card.model.CardSheet;
import com.boardgamedesigner.card.model.CardSlot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

class LayoutCalculatorTest {

    private final LayoutCalculator calculator = new LayoutCalculator();

    @Test
    void emptyListReturnsNoSheets() {
        List<CardSheet> sheets = calculator.calculateSheets(List.of());
        assertThat(sheets).isEmpty();
    }

    @Test
    void singleCardReturnsOneSheet() {
        List<CardImage> cards = createCards(1);
        List<CardSheet> sheets = calculator.calculateSheets(cards);

        assertThat(sheets).hasSize(1);
        assertThat(sheets.get(0).getCardCount()).isEqualTo(1);
        assertThat(sheets.get(0).getPageNumber()).isEqualTo(1);
    }

    @Test
    void nineCardsReturnOneFullSheet() {
        List<CardImage> cards = createCards(9);
        List<CardSheet> sheets = calculator.calculateSheets(cards);

        assertThat(sheets).hasSize(1);
        assertThat(sheets.get(0).getCardCount()).isEqualTo(9);
    }

    @Test
    void tenCardsReturnTwoSheets() {
        List<CardImage> cards = createCards(10);
        List<CardSheet> sheets = calculator.calculateSheets(cards);

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).getCardCount()).isEqualTo(9);
        assertThat(sheets.get(1).getCardCount()).isEqualTo(1);
        assertThat(sheets.get(1).getPageNumber()).isEqualTo(2);
    }

    @Test
    void cardSlotPositionsAreCorrect() {
        List<CardImage> cards = createCards(9);
        List<CardSheet> sheets = calculator.calculateSheets(cards);
        CardSheet sheet = sheets.get(0);

        // 左上角卡牌 (0,0)
        CardSlot topLeft = sheet.getSlots().get(0);
        assertThat(topLeft.getCol()).isEqualTo(0);
        assertThat(topLeft.getRow()).isEqualTo(0);
        assertThat(topLeft.getXMm()).isCloseTo(5.25, byLessThan(0.01));
        assertThat(topLeft.getYMm()).isCloseTo(8.25, byLessThan(0.01));

        // 右上角卡牌 (2,0)
        CardSlot topRight = sheet.getSlots().get(2);
        assertThat(topRight.getCol()).isEqualTo(2);
        assertThat(topRight.getRow()).isEqualTo(0);
        assertThat(topRight.getXMm()).isCloseTo(141.75, byLessThan(0.01));

        // 正中间卡牌 (1,1) — 在 slots 中是 index 4
        CardSlot center = sheet.getSlots().get(4);
        assertThat(center.getCol()).isEqualTo(1);
        assertThat(center.getRow()).isEqualTo(1);
        assertThat(center.getXMm()).isCloseTo(73.50, byLessThan(0.01));
        assertThat(center.getYMm()).isCloseTo(104.50, byLessThan(0.01));

        // 右下角卡牌 (2,2) — index 8
        CardSlot bottomRight = sheet.getSlots().get(8);
        assertThat(bottomRight.getCol()).isEqualTo(2);
        assertThat(bottomRight.getRow()).isEqualTo(2);
        assertThat(bottomRight.getXMm()).isCloseTo(141.75, byLessThan(0.01));
        assertThat(bottomRight.getYMm()).isCloseTo(200.75, byLessThan(0.01));
    }

    @Test
    void calculatePageCountReturnsCorrectValue() {
        assertThat(calculator.calculatePageCount(0)).isEqualTo(0);
        assertThat(calculator.calculatePageCount(1)).isEqualTo(1);
        assertThat(calculator.calculatePageCount(9)).isEqualTo(1);
        assertThat(calculator.calculatePageCount(10)).isEqualTo(2);
        assertThat(calculator.calculatePageCount(18)).isEqualTo(2);
        assertThat(calculator.calculatePageCount(19)).isEqualTo(3);
    }

    @Test
    void pdfCoordinatesAreWithinPageBounds() {
        List<CardImage> cards = createCards(9);
        List<CardSheet> sheets = calculator.calculateSheets(cards);

        for (CardSheet sheet : sheets) {
            for (CardSlot slot : sheet.getSlots()) {
                // PDF 坐标原点在左下角，A4 页宽约 595pt，高约 842pt
                assertThat(slot.getXPoints()).isGreaterThanOrEqualTo(0);
                assertThat(slot.getYPoints()).isGreaterThanOrEqualTo(0);
                assertThat(slot.getWidthPoints()).isGreaterThan(0);
                assertThat(slot.getHeightPoints()).isGreaterThan(0);

                // 卡牌不应超出页面右边界和上边界
                assertThat(slot.getXPoints() + slot.getWidthPoints())
                        .isLessThanOrEqualTo(LayoutConfig.mmToPoints(LayoutConfig.PAPER_WIDTH_MM));
                assertThat(slot.getYPoints() + slot.getHeightPoints())
                        .isLessThanOrEqualTo(LayoutConfig.mmToPoints(LayoutConfig.PAPER_HEIGHT_MM));
            }
        }
    }

    private List<CardImage> createCards(int count) {
        List<CardImage> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(new CardImage(Path.of("card_" + (i + 1) + ".png"), 744, 1039));
        }
        return cards;
    }
}
