package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.model.CardBack;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * 卡背列表自定义渲染单元格：缩略图 + 关联数 + 页数 + 删除按钮.
 */
public class CardBackListCell extends ListCell<CardBack> {

    private final HBox root;
    private final ImageView thumbnailView;
    private final Label nameLabel;
    private final Label infoLabel;
    private final Button deleteBtn;
    private final Label indexLabel;

    public CardBackListCell(Consumer<CardBack> onUnsetBack,
                            Consumer<CardBack> onDelete,
                            Consumer<CardBack> onChangePageCount,
                            Consumer<CardBack> onRename,
                            Runnable onChanged) {

        indexLabel = new Label();
        indexLabel.setPrefWidth(22);
        indexLabel.setAlignment(Pos.CENTER_RIGHT);
        indexLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(48);
        thumbnailView.setFitHeight(48);
        thumbnailView.setPreserveRatio(true);

        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        deleteBtn = new Button("×");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c00; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 0 4 0 4;");
        deleteBtn.setOnAction(e -> {
            CardBack item = getItem();
            if (item != null) {
                onDelete.accept(item);
                onChanged.run();
            }
        });

        VBox textBox = new VBox(2, nameLabel, infoLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(6, indexLabel, thumbnailView, textBox, deleteBtn);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(2, 4, 2, 4));

        // 右键上下文菜单
        setOnContextMenuRequested(e -> {
            CardBack item = getItem();
            if (item == null) return;

            ContextMenu menu = new ContextMenu();

            MenuItem renameItem = new MenuItem("重命名...");
            renameItem.setOnAction(ev -> onRename.accept(item));
            menu.getItems().add(renameItem);

            MenuItem changePageItem = new MenuItem("修改页数...");
            changePageItem.setOnAction(ev -> onChangePageCount.accept(item));
            menu.getItems().add(changePageItem);

            menu.getItems().add(new SeparatorMenuItem());

            MenuItem unsetItem = new MenuItem("取消卡背");
            unsetItem.setOnAction(ev -> {
                onUnsetBack.accept(item);
                onChanged.run();
            });
            menu.getItems().add(unsetItem);
            menu.getItems().add(new SeparatorMenuItem());

            MenuItem deleteItem = new MenuItem("删除此卡背");
            deleteItem.setOnAction(ev -> {
                onDelete.accept(item);
                onChanged.run();
            });
            menu.getItems().add(deleteItem);

            menu.show(this, e.getScreenX(), e.getScreenY());
        });

        // 拖拽支持（卡背列表内部排序）
        setOnDragDetected(e -> {
            int idx = getIndex();
            if (idx < 0) return;
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("back:" + idx);
            db.setContent(cc);
            e.consume();
        });

        setOnDragOver(e -> {
            if (e.getGestureSource() != this && e.getDragboard().hasString()) {
                String s = e.getDragboard().getString();
                if (s.startsWith("back:")) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString() && db.getString().startsWith("back:")) {
                int fromIdx = Integer.parseInt(db.getString().substring(5));
                int toIdx = getIndex();
                if (fromIdx != toIdx && toIdx >= 0) {
                    ListView<CardBack> lv = getListView();
                    if (lv != null) {
                        CardBack item = lv.getItems().remove(fromIdx);
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

    @Override
    protected void updateItem(CardBack item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        int idx = getIndex();
        indexLabel.setText(String.valueOf(idx + 1));

        try {
            thumbnailView.setImage(item.getImage().createThumbnail(48));
        } catch (Exception ignored) {
        }

        nameLabel.setText(item.getName());
        String pageInfo;
        if (item.isAutoPageCount()) {
            pageInfo = "自动(" + item.getPageCount() + ")页";
        } else {
            pageInfo = item.getPageCount() + "页";
        }
        infoLabel.setText("关联 " + item.getLinkedCount() + " 张 | " + pageInfo);

        setGraphic(root);
    }
}
