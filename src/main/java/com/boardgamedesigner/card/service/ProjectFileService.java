package com.boardgamedesigner.card.service;

import com.boardgamedesigner.card.model.CardBack;
import com.boardgamedesigner.card.model.CardImage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目文件保存/加载服务。将当前卡牌和卡背状态序列化为 JSON 文件.
 */
public class ProjectFileService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 保存项目到 JSON 文件.
     *
     * @param path         保存路径
     * @param cardFolder   卡牌文件夹
     * @param cards        正面卡牌列表
     * @param cardBacks    卡背列表
     */
    public void save(Path path, Path cardFolder, List<CardImage> cards, List<CardBack> cardBacks) {
        ProjectData data = new ProjectData();
        data.version = 1;
        data.cardFolder = cardFolder != null ? cardFolder.toString() : null;

        // 保存卡牌（文件名，可能有重复表示复制）
        for (CardImage card : cards) {
            data.cards.add(card.getFileName());
        }

        // 保存卡背
        for (CardBack back : cardBacks) {
            BackData bd = new BackData();
            bd.file = back.getImage().getFileName();
            bd.name = back.getName();
            bd.overridePageCount = back.getOverridePageCount();
            // 关联的卡牌用索引表示
            for (CardImage linked : back.getLinkedCards()) {
                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i) == linked) {
                        bd.linkedIndices.add(i);
                        break;
                    }
                }
            }
            data.cardBacks.add(bd);
        }

        try {
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException e) {
            throw new UncheckedIOException("保存项目失败: " + path, e);
        }
    }

    /**
     * 从 JSON 文件加载项目.
     *
     * @param path         项目文件路径
     * @param imageLoadService 用于重新加载图片
     * @return 加载结果（cardFolder, cards, cardBacks）
     */
    public LoadResult load(Path path, ImageLoadService imageLoadService) {
        String json;
        try {
            json = Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("读取项目文件失败: " + path, e);
        }

        ProjectData data = GSON.fromJson(json, ProjectData.class);
        if (data == null || data.cards == null) {
            throw new IllegalArgumentException("无效的项目文件格式");
        }

        Path cardFolder = data.cardFolder != null ? Path.of(data.cardFolder) : null;

        // 加载所有卡牌
        List<CardImage> cards = new ArrayList<>();
        if (cardFolder != null && Files.isDirectory(cardFolder)) {
            List<CardImage> allImages = imageLoadService.loadFromFolder(cardFolder);
            for (String fileName : data.cards) {
                CardImage found = allImages.stream()
                        .filter(c -> c.getFileName().equals(fileName))
                        .findFirst().orElse(null);
                if (found != null) {
                    cards.add(found);
                }
            }
        }

        // 加载卡背
        List<CardBack> cardBacks = new ArrayList<>();
        if (data.cardBacks != null) {
            for (BackData bd : data.cardBacks) {
                // 在 cards 中找对应的图片（卡背原图已被移除，需从文件夹加载）
                CardImage backImage = null;
                if (cardFolder != null && Files.isDirectory(cardFolder)) {
                    List<CardImage> allImages = imageLoadService.loadFromFolder(cardFolder);
                    backImage = allImages.stream()
                            .filter(c -> c.getFileName().equals(bd.file))
                            .findFirst().orElse(null);
                }
                if (backImage == null) continue;

                CardBack back = new CardBack(backImage);
                back.setName(bd.name != null ? bd.name : back.getName());
                back.setOverridePageCount(bd.overridePageCount);

                // 恢复关联
                if (bd.linkedIndices != null) {
                    for (int idx : bd.linkedIndices) {
                        if (idx >= 0 && idx < cards.size()) {
                            back.linkCard(cards.get(idx));
                        }
                    }
                }
                cardBacks.add(back);
            }
        }

        return new LoadResult(cardFolder, cards, cardBacks);
    }

    public static class LoadResult {
        public final Path cardFolder;
        public final List<CardImage> cards;
        public final List<CardBack> cardBacks;

        LoadResult(Path cardFolder, List<CardImage> cards, List<CardBack> cardBacks) {
            this.cardFolder = cardFolder;
            this.cards = cards;
            this.cardBacks = cardBacks;
        }
    }

    @SuppressWarnings("unused")
    private static class ProjectData {
        int version;
        String cardFolder;
        List<String> cards = new ArrayList<>();
        List<BackData> cardBacks = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private static class BackData {
        String file;
        String name;
        int overridePageCount;
        List<Integer> linkedIndices = new ArrayList<>();
    }
}
