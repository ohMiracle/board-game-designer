package com.boardgamedesigner.card.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡背模型：包含卡背图片、关联的正面卡牌列表、以及可选的页数覆盖.
 */
public class CardBack {

    private final CardImage image;
    private final List<CardImage> linkedCards = new ArrayList<>();
    private String name;
    private int overridePageCount; // 0 = 自动计算

    public CardBack(CardImage image) {
        this.image = image;
        this.name = stripExtension(image.getFileName());
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public CardImage getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null && !name.isBlank() ? name.trim() : this.name;
    }

    public List<CardImage> getLinkedCards() {
        return linkedCards;
    }

    public int getLinkedCount() {
        return linkedCards.size();
    }

    public int getOverridePageCount() {
        return overridePageCount;
    }

    public void setOverridePageCount(int overridePageCount) {
        this.overridePageCount = Math.max(0, overridePageCount);
    }

    public int getPageCount() {
        if (overridePageCount > 0) return overridePageCount;
        return Math.max(1, (int) Math.ceil(linkedCards.size() / 9.0));
    }

    public boolean isAutoPageCount() {
        return overridePageCount <= 0;
    }

    public void linkCard(CardImage card) {
        if (!linkedCards.contains(card)) {
            linkedCards.add(card);
        }
    }

    public void unlinkCard(CardImage card) {
        linkedCards.remove(card);
    }
}
