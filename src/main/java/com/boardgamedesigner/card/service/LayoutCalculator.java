package com.boardgamedesigner.card.service;

import com.boardgamedesigner.card.config.LayoutConfig;
import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.card.model.CardSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * 3×3 网格排版计算器。将卡牌列表按每页 9 张分组生成 CardSheet.
 */
public class LayoutCalculator {

    /**
     * 计算卡牌布局，按每页最多 CARDS_PER_PAGE 张分组.
     *
     * @param cards 需要排版的卡牌列表
     * @return 按页分组的 CardSheet 列表
     */
    public List<CardSheet> calculateSheets(List<CardImage> cards) {
        List<CardSheet> sheets = new ArrayList<>();

        for (int i = 0; i < cards.size(); i += LayoutConfig.CARDS_PER_PAGE) {
            int end = Math.min(i + LayoutConfig.CARDS_PER_PAGE, cards.size());
            List<CardImage> pageCards = cards.subList(i, end);
            sheets.add(new CardSheet(i / LayoutConfig.CARDS_PER_PAGE, pageCards));
        }

        return sheets;
    }

    /**
     * 计算总页数.
     */
    public int calculatePageCount(int cardCount) {
        return (int) Math.ceil((double) cardCount / LayoutConfig.CARDS_PER_PAGE);
    }
}
