package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.model.CardBack;
import com.boardgamedesigner.card.model.CardImage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * TreeView 单元格渲染：分组标题（String）与卡牌行（CardImage）.
 */
public class CardTreeCell extends TreeCell<Object> {

    private final HBox cardRow;
    private final HBox groupRow;
    private final Label groupLabel;
    private final ImageView thumbnailView;
    private final Tooltip imageTooltip;

    private final Consumer<CardImage> onCopy;
    private final Consumer<CardImage> onDelete;
    private final Consumer<CardImage> onSetCardBack;
    private final Consumer<CardBack> onAssignBack;
    private final Runnable onUnassignBack;
    private final BiConsumer<List<CardImage>, CardBack> onGroupAssignBack;
    private final Consumer<List<CardImage>> onGroupUnassignBack;
    private final Supplier<List<CardBack>> availableBacksSupplier;
    private final Runnable onChanged;
    private final Supplier<List<TreeItem<Object>>> preClickGroupsSupplier;

    public CardTreeCell(Consumer<CardImage> onCopy,
                        Consumer<CardImage> onDelete,
                        Consumer<CardImage> onSetCardBack,
                        Consumer<CardBack> onAssignBack,
                        Runnable onUnassignBack,
                        BiConsumer<List<CardImage>, CardBack> onGroupAssignBack,
                        Consumer<List<CardImage>> onGroupUnassignBack,
                        Supplier<List<CardBack>> availableBacksSupplier,
                        Runnable onChanged,
                        Supplier<List<TreeItem<Object>>> preClickGroupsSupplier) {

        this.onCopy = onCopy;
        this.onDelete = onDelete;
        this.onSetCardBack = onSetCardBack;
        this.onAssignBack = onAssignBack;
        this.onUnassignBack = onUnassignBack;
        this.onGroupAssignBack = onGroupAssignBack;
        this.onGroupUnassignBack = onGroupUnassignBack;
        this.availableBacksSupplier = availableBacksSupplier;
        this.onChanged = onChanged;
        this.preClickGroupsSupplier = preClickGroupsSupplier;

        // 分组标题行
        groupLabel = new Label();
        groupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
        groupRow = new HBox(groupLabel);
        groupRow.setAlignment(Pos.CENTER_LEFT);
        groupRow.setPadding(new Insets(4, 8, 4, 8));
        groupRow.setStyle("-fx-background-color: #e8e8e8;");

        // 整组右键菜单
        groupRow.setOnContextMenuRequested(e -> {
            TreeItem<Object> groupItem = getTreeItem();
            if (groupItem == null) return;
            ContextMenu menu = buildGroupContextMenu(groupItem);
            menu.show(groupRow, e.getScreenX(), e.getScreenY());
        });

        // 卡牌行
        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(40);
        thumbnailView.setFitHeight(40);
        thumbnailView.setPreserveRatio(true);

        imageTooltip = new Tooltip();
        imageTooltip.setStyle("-fx-background-color: white; -fx-padding: 4;");
        Tooltip.install(thumbnailView, imageTooltip);

        cardRow = new HBox(6, thumbnailView);
        cardRow.setAlignment(Pos.CENTER_LEFT);
        cardRow.setPadding(new Insets(2, 4, 2, 8));

        cardRow.setOnContextMenuRequested(e -> {
            if (getItem() instanceof CardImage card) {
                ContextMenu menu = buildCardContextMenu(card);
                menu.show(cardRow, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private ContextMenu buildGroupContextMenu(TreeItem<Object> groupItem) {
        ContextMenu menu = new ContextMenu();

        List<CardBack> backs = availableBacksSupplier.get();
        if (!backs.isEmpty()) {
            Menu assignMenu = new Menu("整组分配卡背");
            for (CardBack back : backs) {
                MenuItem backItem = new MenuItem(back.getName() + " (已关联 " + back.getLinkedCount() + " 张)");
                backItem.setOnAction(e -> {
                    onGroupAssignBack.accept(collectGroupCards(groupItem), back);
                    onChanged.run();
                });
                assignMenu.getItems().add(backItem);
            }
            assignMenu.getItems().add(new SeparatorMenuItem());
            MenuItem noneItem = new MenuItem("整组取消分配");
            noneItem.setOnAction(e -> {
                onGroupUnassignBack.accept(collectGroupCards(groupItem));
                onChanged.run();
            });
            assignMenu.getItems().add(noneItem);
            menu.getItems().add(assignMenu);
        }

        return menu;
    }

    private List<CardImage> collectGroupCards(TreeItem<Object> groupItem) {
        List<CardImage> cards = new ArrayList<>();
        collectCardsFromGroup(cards, groupItem);
        for (TreeItem<Object> sel : preClickGroupsSupplier.get()) {
            if (sel != groupItem) {
                collectCardsFromGroup(cards, sel);
            }
        }
        return cards;
    }

    private void collectCardsFromGroup(List<CardImage> collector, TreeItem<Object> groupItem) {
        for (TreeItem<Object> child : groupItem.getChildren()) {
            if (child.getValue() instanceof CardImage card) {
                collector.add(card);
            }
        }
    }

    private ContextMenu buildCardContextMenu(CardImage card) {
        ContextMenu menu = new ContextMenu();

        MenuItem copyItem = new MenuItem("复制此卡牌");
        copyItem.setOnAction(e -> {
            onCopy.accept(card);
            onChanged.run();
        });
        menu.getItems().add(copyItem);

        List<CardBack> backs = availableBacksSupplier.get();
        if (!backs.isEmpty()) {
            Menu assignMenu = new Menu("分配卡背");
            for (CardBack back : backs) {
                MenuItem backItem = new MenuItem(back.getName() + " (已关联 " + back.getLinkedCount() + " 张)");
                backItem.setOnAction(e -> {
                    onAssignBack.accept(back);
                    onChanged.run();
                });
                assignMenu.getItems().add(backItem);
            }
            assignMenu.getItems().add(new SeparatorMenuItem());
            MenuItem noneItem = new MenuItem("取消分配");
            noneItem.setOnAction(e -> {
                onUnassignBack.run();
                onChanged.run();
            });
            assignMenu.getItems().add(noneItem);
            menu.getItems().add(assignMenu);
        }

        MenuItem backItem = new MenuItem("设为卡背");
        backItem.setOnAction(e -> onSetCardBack.accept(card));
        menu.getItems().add(backItem);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem deleteItem = new MenuItem("删除此卡牌");
        deleteItem.setOnAction(e -> {
            onDelete.accept(card);
            onChanged.run();
        });
        menu.getItems().add(deleteItem);

        return menu;
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        if (item instanceof String groupName) {
            groupLabel.setText(groupName);
            setGraphic(groupRow);
        } else if (item instanceof CardImage card) {
            Image thumb = null;
            try {
                thumb = card.createThumbnail(40);
            } catch (Exception ignored) {
            }
            thumbnailView.setImage(thumb);

            // 悬停放大预览
            if (thumb != null && !thumb.isError()) {
                Image preview = null;
                try {
                    preview = card.createThumbnail(400);
                } catch (Exception ignored) {
                }
                if (preview != null && !preview.isError()) {
                    ImageView previewView = new ImageView(preview);
                    previewView.setFitWidth(400);
                    previewView.setFitHeight(400);
                    previewView.setPreserveRatio(true);
                    imageTooltip.setGraphic(previewView);
                }
            }
            setGraphic(cardRow);
        } else {
            setGraphic(null);
        }
    }
}
