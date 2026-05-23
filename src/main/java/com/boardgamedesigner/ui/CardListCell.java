package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.model.CardBack;
import com.boardgamedesigner.card.model.CardImage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 卡牌列表自定义渲染单元：序号 + 缩略图 + 卡背名称标签 + 删除按钮.
 * 支持右键菜单（复制/分配卡背/设为卡背/删除）和拖拽排序.
 */
public class CardListCell extends ListCell<CardImage> {

    private final HBox root;
    private final ImageView thumbnailView;
    private final Label backNameLabel;
    private final Button deleteBtn;
    private final Label indexLabel;

    private final Consumer<CardImage> onCopy;
    private final Consumer<CardImage> onDelete;
    private final Consumer<CardImage> onSetCardBack;
    private final Consumer<CardImage> onUnsetCardBack;
    private final Consumer<CardBack> onAssignBack;
    private final Runnable onUnassignBack;
    private final Supplier<List<CardBack>> availableBacksSupplier;
    private final Function<CardImage, String> backNameLookup;
    private final Runnable onChanged;

    public CardListCell(Consumer<CardImage> onCopy,
                        Consumer<CardImage> onDelete,
                        Consumer<CardImage> onSetCardBack,
                        Consumer<CardImage> onUnsetCardBack,
                        Consumer<CardBack> onAssignBack,
                        Runnable onUnassignBack,
                        Supplier<List<CardBack>> availableBacksSupplier,
                        Function<CardImage, String> backNameLookup,
                        Supplier<Integer> indexSupplier,
                        Runnable onChanged) {

        this.onCopy = onCopy;
        this.onDelete = onDelete;
        this.onSetCardBack = onSetCardBack;
        this.onUnsetCardBack = onUnsetCardBack;
        this.onAssignBack = onAssignBack;
        this.onUnassignBack = onUnassignBack;
        this.availableBacksSupplier = availableBacksSupplier;
        this.backNameLookup = backNameLookup;
        this.onChanged = onChanged;

        indexLabel = new Label();
        indexLabel.setPrefWidth(28);
        indexLabel.setAlignment(Pos.CENTER_RIGHT);
        indexLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(52);
        thumbnailView.setFitHeight(52);
        thumbnailView.setPreserveRatio(true);

        backNameLabel = new Label();
        backNameLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 10px; -fx-font-weight: bold;");

        deleteBtn = new Button("×");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c00; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 0 4 0 4;");
        deleteBtn.setOnAction(e -> {
            CardImage item = getItem();
            if (item != null) {
                onDelete.accept(item);
                onChanged.run();
            }
        });

        VBox centerBox = new VBox(2, new Text(), backNameLabel);
        centerBox.setAlignment(Pos.CENTER_LEFT);
        // invisible spacer text to align with thumbnail height
        centerBox.getChildren().get(0).setVisible(false);

        root = new HBox(6, indexLabel, thumbnailView, centerBox, deleteBtn);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(2, 4, 2, 4));

        // 动态构建右键上下文菜单
        root.setOnContextMenuRequested(e -> {
            CardImage item = getItem();
            if (item == null) return;
            ContextMenu menu = buildContextMenu();
            menu.show(root, e.getScreenX(), e.getScreenY());
        });

        // 拖拽源
        setOnDragDetected(e -> {
            int idx = getIndex();
            if (idx < 0) return;
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("card:" + idx);
            db.setContent(cc);
            e.consume();
        });

        // 拖拽目标
        setOnDragOver(e -> {
            if (e.getGestureSource() != this && e.getDragboard().hasString()
                    && e.getDragboard().getString().startsWith("card:")) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString() && db.getString().startsWith("card:")) {
                int fromIdx = Integer.parseInt(db.getString().substring(5));
                int toIdx = getIndex();
                if (fromIdx != toIdx && toIdx >= 0) {
                    ListView<CardImage> lv = getListView();
                    if (lv != null) {
                        CardImage item = lv.getItems().remove(fromIdx);
                        lv.getItems().add(toIdx, item);
                        lv.getSelectionModel().select(toIdx);
                        e.setDropCompleted(true);
                        onChanged.run();
                    }
                }
            }
            e.consume();
        });
    }

    private ContextMenu buildContextMenu() {
        CardImage item = getItem();
        ContextMenu menu = new ContextMenu();

        if (onCopy != null) {
            MenuItem copyItem = new MenuItem("复制此卡牌");
            copyItem.setOnAction(e -> {
                if (getItem() != null) {
                    onCopy.accept(getItem());
                    onChanged.run();
                }
            });
            menu.getItems().add(copyItem);
        }

        // "分配卡背" 子菜单
        if (availableBacksSupplier != null && onAssignBack != null) {
            List<CardBack> backs = availableBacksSupplier.get();
            Menu assignMenu = new Menu("分配卡背");

            for (CardBack back : backs) {
                MenuItem backItem = new MenuItem(back.getName()
                        + " (已关联 " + back.getLinkedCount() + " 张)");
                backItem.setOnAction(e -> {
                    onAssignBack.accept(back);
                    onChanged.run();
                });
                assignMenu.getItems().add(backItem);
            }

            if (!backs.isEmpty() && onUnassignBack != null) {
                assignMenu.getItems().add(new SeparatorMenuItem());
            }
            if (onUnassignBack != null) {
                MenuItem noneItem = new MenuItem("取消分配");
                noneItem.setOnAction(e -> {
                    onUnassignBack.run();
                    onChanged.run();
                });
                assignMenu.getItems().add(noneItem);
            }

            menu.getItems().add(assignMenu);
        }

        if (onSetCardBack != null) {
            MenuItem backItem = new MenuItem("设为卡背");
            backItem.setOnAction(e -> {
                if (getItem() != null) {
                    onSetCardBack.accept(getItem());
                }
            });
            menu.getItems().add(backItem);
        }

        if (onUnsetCardBack != null) {
            MenuItem unsetBackItem = new MenuItem("取消卡背");
            unsetBackItem.setOnAction(e -> {
                if (getItem() != null) {
                    onUnsetCardBack.accept(getItem());
                    onChanged.run();
                }
            });
            menu.getItems().add(unsetBackItem);
        }

        if (!menu.getItems().isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem deleteItem = new MenuItem("删除此卡牌");
        deleteItem.setOnAction(e -> {
            if (getItem() != null) {
                onDelete.accept(getItem());
                onChanged.run();
            }
        });
        menu.getItems().add(deleteItem);

        return menu;
    }

    @Override
    protected void updateItem(CardImage item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        int idx = getIndex();
        indexLabel.setText(String.valueOf(idx + 1));
        try {
            thumbnailView.setImage(item.createThumbnail(52));
        } catch (Exception ignored) {
        }

        if (backNameLookup != null) {
            String name = backNameLookup.apply(item);
            if (name != null && !name.isEmpty()) {
                backNameLabel.setText(name);
                backNameLabel.setVisible(true);
            } else {
                backNameLabel.setVisible(false);
            }
        }

        setGraphic(root);
    }
}
