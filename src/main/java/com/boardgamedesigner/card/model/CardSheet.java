package com.boardgamedesigner.card.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一张 A4 纸面上的卡牌排列（最多 9 张）.
 */
public class CardSheet {

    private final int pageIndex;
    private final List<CardSlot> slots;
    private final List<CardImage> cards;

    public CardSheet(int pageIndex, List<CardImage> cards) {
        this.pageIndex = pageIndex;
        this.cards = List.copyOf(cards);
        this.slots = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            int col = i % 3;
            int row = i / 3;
            slots.add(new CardSlot(cards.get(i), col, row));
        }
    }

    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * 人类可读的页码（从 1 开始）.
     */
    public int getPageNumber() {
        return pageIndex + 1;
    }

    public List<CardSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /**
     * 该页上的实际卡牌数量（≤9）.
     */
    public List<CardImage> getCards() {
        return cards;
    }

    public int getCardCount() {
        return slots.size();
    }
}
