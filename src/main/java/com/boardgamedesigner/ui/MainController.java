package com.boardgamedesigner.ui;

import com.boardgamedesigner.card.model.CardBack;
import com.boardgamedesigner.card.model.CardImage;
import com.boardgamedesigner.card.model.CardSheet;
import com.boardgamedesigner.card.service.ImageLoadService;
import com.boardgamedesigner.card.service.LayoutCalculator;
import com.boardgamedesigner.card.service.PdfExportService;
import com.boardgamedesigner.card.service.ProjectFileService;
import com.boardgamedesigner.util.ImageSplitService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MainController {

    @FXML private Button chooseFolderBtn;
    @FXML private Button splitImageBtn;
    @FXML private Button selectAllBtn;
    @FXML private Button deselectAllBtn;
    @FXML private Button exportBtn;
    @FXML private Label cardCountLabel;
    @FXML private Label cardBackCountLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<CardImage> cardListView;
    @FXML private ListView<CardBack> cardBackListView;
    @FXML private ScrollPane previewScrollPane;
    @FXML private VBox previewContainer;

    private final ImageLoadService imageLoadService = new ImageLoadService();
    private final LayoutCalculator layoutCalculator = new LayoutCalculator();
    private final PdfExportService pdfExportService = new PdfExportService();
    private final ProjectFileService projectFileService = new ProjectFileService();
    private final ImageSplitService imageSplitService = new ImageSplitService();

    private final ObservableList<CardImage> cardList = FXCollections.observableArrayList();
    private final ObservableList<CardBack> cardBackList = FXCollections.observableArrayList();
    private List<CardSheet> sheets = List.of();
    private volatile boolean isExporting = false;
    private Path cardFolderPath;

    @FXML
    private void initialize() {
        cardListView.setItems(cardList);
        cardBackListView.setItems(cardBackList);

        // 正面卡牌列表的单元格工厂
        Function<CardImage, String> backNameLookup = card -> {
            for (CardBack cb : cardBackList) {
                if (cb.getLinkedCards().contains(card)) return cb.getName();
            }
            return null;
        };

        cardListView.setCellFactory(lv -> new CardListCell(
                this::copyCard,
                this::removeCard,
                this::setCardBack,
                null,
                this::assignCardBack,
                this::unassignCardBackForSelected,
                () -> new ArrayList<>(cardBackList),
                backNameLookup,
                () -> cardList.size(),
                this::onCardListChanged
        ));

        // 卡背列表的单元格工厂
        cardBackListView.setCellFactory(lv -> new CardBackListCell(
                this::unsetCardBack,
                this::removeCardBack,
                this::changeBackPageCount,
                this::renameCardBack,
                this::onCardListChanged
        ));

        cardListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cardBackListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        chooseFolderBtn.setOnAction(e -> onChooseFolder());
        splitImageBtn.setOnAction(e -> onSplitImage());
        selectAllBtn.setOnAction(e -> onCopySelected());
        deselectAllBtn.setOnAction(e -> onRemoveSelected());
        exportBtn.setOnAction(e -> onExportPdf());

        selectAllBtn.setText("复制选中");
        deselectAllBtn.setText("删除选中");
    }

    // ---- 文件夹选择 ----

    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择卡牌图片文件夹");
        File dir = chooser.showDialog(cardListView.getScene().getWindow());
        if (dir == null) return;

        cardFolderPath = dir.toPath();

        // 优先加载已有的项目文件
        Path projectFile = cardFolderPath.resolve("default.json");
        if (projectFile.toFile().exists()) {
            try {
                ProjectFileService.LoadResult result = projectFileService.load(projectFile, imageLoadService);
                cardList.setAll(result.cards);
                cardBackList.setAll(result.cardBacks);
                statusLabel.setText("已自动加载 default.json");
            } catch (Exception e) {
                // 加载失败则回退到正常扫描
                loadFolderFresh();
            }
        } else {
            loadFolderFresh();
        }

        refreshSheets();
        updateStatus();
        updateCountLabels();
        exportBtn.setDisable(cardList.isEmpty());
        renderAllPreviews();
    }

    private void loadFolderFresh() {
        List<CardImage> loaded = imageLoadService.loadFromFolder(cardFolderPath);
        cardList.setAll(loaded);
        cardBackList.clear();
        autoDetectCardBacks();
    }

    // ---- 项目保存/加载 ----


    // ---- 图片分切 ----

    private void onSplitImage() {
        CardImage selected = cardListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("提示", "请先在卡牌列表中选择一张要分切的图片");
            return;
        }
        if (cardFolderPath == null) {
            showError("提示", "请先选择卡牌文件夹");
            return;
        }

        // 行列数输入对话框
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("图片分切");
        dialog.setHeaderText("将 \"" + selected.getFileName() + "\" 均匀分切");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Spinner<Integer> colsSpinner = new Spinner<>(1, 20, 3);
        Spinner<Integer> rowsSpinner = new Spinner<>(1, 20, 3);
        colsSpinner.setEditable(true);
        rowsSpinner.setEditable(true);
        grid.add(new Label("列数:"), 0, 0);
        grid.add(colsSpinner, 1, 0);
        grid.add(new Label("行数:"), 0, 1);
        grid.add(rowsSpinner, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) return new int[]{colsSpinner.getValue(), rowsSpinner.getValue()};
            return null;
        });

        Optional<int[]> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        int cols = result.get()[0];
        int rows = result.get()[1];
        Path sourcePath = selected.getSourcePath();

        statusLabel.setText("正在分切图片...");

        Task<List<CardImage>> task = new Task<>() {
            @Override
            protected List<CardImage> call() {
                return imageSplitService.split(sourcePath, cardFolderPath, cols, rows);
            }

            @Override
            protected void succeeded() {
                List<CardImage> split = getValue();
                cardList.remove(selected);
                cardList.addAll(split);
                statusLabel.setText("分切完成，切出 " + split.size() + " 张卡牌");
                onCardListChanged();
            }

            @Override
            protected void failed() {
                statusLabel.setText("分切失败");
                showError("分切失败", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    private void autoDetectCardBacks() {
        List<CardImage> detected = new ArrayList<>();
        for (CardImage card : new ArrayList<>(cardList)) {
            String name = card.getFileName().toLowerCase();
            if (name.contains("卡背") || name.contains("back") || name.contains("背面")) {
                detected.add(card);
            }
        }
        for (CardImage card : detected) {
            cardList.remove(card);
            cardBackList.add(new CardBack(card));
        }
    }

    // ---- 卡背操作 ----

    private void setCardBack(CardImage card) {
        cardList.remove(card);
        cardBackList.add(new CardBack(card));
        onCardListChanged();
    }

    private void unsetCardBack(CardBack cardBack) {
        // 清理关联的卡牌
        for (CardImage linked : new ArrayList<>(cardBack.getLinkedCards())) {
            cardBack.unlinkCard(linked);
        }
        cardBackList.remove(cardBack);
        cardList.add(cardBack.getImage());
        onCardListChanged();
    }

    private void removeCardBack(CardBack cardBack) {
        cardBackList.remove(cardBack);
        onCardListChanged();
    }

    // ---- 卡牌-卡背关联 ----

    private void assignCardBack(CardBack back) {
        List<CardImage> selected = new ArrayList<>(cardListView.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        for (CardImage card : selected) {
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
            back.linkCard(card);
        }
    }

    private void unassignCardBackForSelected() {
        List<CardImage> selected = new ArrayList<>(cardListView.getSelectionModel().getSelectedItems());
        for (CardImage card : selected) {
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
        }
    }

    private void renameCardBack(CardBack back) {
        TextInputDialog dialog = new TextInputDialog(back.getName());
        dialog.setTitle("重命名卡背");
        dialog.setHeaderText("卡背图片: " + back.getImage().getFileName());
        dialog.setContentText("名称:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            back.setName(val);
            onCardListChanged();
        });
    }

    private void changeBackPageCount(CardBack back) {
        TextInputDialog dialog = new TextInputDialog(
                back.isAutoPageCount() ? "0" : String.valueOf(back.getOverridePageCount()));
        dialog.setTitle("修改页数");
        dialog.setHeaderText("卡背: " + back.getName());
        dialog.setContentText("页数（0=自动计算，当前自动值=" + Math.max(1, (int) Math.ceil(back.getLinkedCount() / 9.0)) + "）:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                int n = Integer.parseInt(val.trim());
                back.setOverridePageCount(n);
                onCardListChanged();
            } catch (NumberFormatException ignored) {
            }
        });
    }

    // ---- 卡牌操作 ----

    private void copyCard(CardImage source) {
        int idx = cardList.indexOf(source);
        if (idx >= 0) {
            cardList.add(idx + 1, source.copy());
        }
    }

    private void removeCard(CardImage target) {
        // 清理该卡牌在所有卡背中的关联
        for (CardBack cb : cardBackList) {
            cb.unlinkCard(target);
        }
        cardList.remove(target);
    }

    private void onCopySelected() {
        CardImage selected = cardListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            copyCard(selected);
            onCardListChanged();
        }
    }

    private void onRemoveSelected() {
        CardImage selected = cardListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            removeCard(selected);
            onCardListChanged();
        }
    }

    private void onCardListChanged() {
        refreshSheets();
        updateStatus();
        updateCountLabels();
        exportBtn.setDisable(cardList.isEmpty());
        cardListView.refresh();
        cardBackListView.refresh();
        renderAllPreviews();
        autoSave();
    }

    private void autoSave() {
        if (cardFolderPath == null) return;
        try {
            projectFileService.save(cardFolderPath.resolve("default.json"), cardFolderPath,
                    new ArrayList<>(cardList), new ArrayList<>(cardBackList));
        } catch (Exception ignored) {
            // 自动保存失败不打扰用户
        }
    }

    private void updateCountLabels() {
        cardCountLabel.setText("卡牌 (" + cardList.size() + ")");
        int totalLinked = cardBackList.stream().mapToInt(CardBack::getLinkedCount).sum();
        cardBackCountLabel.setText("卡背 (" + cardBackList.size() + " | 关联 " + totalLinked + " 张)");
    }

    // ---- 预览渲染 ----

    private void refreshSheets() {
        // 按卡背分组排版：不同卡背的卡牌不在同一页
        List<CardImage> remaining = new ArrayList<>(cardList);
        List<CardSheet> allSheets = new ArrayList<>();

        for (CardBack cb : cardBackList) {
            List<CardImage> group = new ArrayList<>();
            for (CardImage card : new ArrayList<>(remaining)) {
                if (cb.getLinkedCards().contains(card)) {
                    group.add(card);
                    remaining.remove(card);
                }
            }
            if (!group.isEmpty()) {
                allSheets.addAll(layoutCalculator.calculateSheets(group));
            }
        }

        // 未分配卡背的卡牌放在最后
        if (!remaining.isEmpty()) {
            allSheets.addAll(layoutCalculator.calculateSheets(remaining));
        }

        sheets = allSheets;
    }

    private void renderAllPreviews() {
        previewContainer.getChildren().clear();

        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = sheets.size() + backPages;

        if (sheets.isEmpty() && cardBackList.isEmpty()) {
            Label emptyLabel = new Label("（空页面）");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            previewContainer.getChildren().add(emptyLabel);
            return;
        }

        int pageNum = 1;
        for (int i = 0; i < sheets.size(); i++) {
            PreviewPage page = new PreviewPage();
            page.drawFront(sheets.get(i), pageNum++, totalPages);
            previewContainer.getChildren().add(page);
        }

        for (CardBack cardBack : cardBackList) {
            int count = cardBack.getPageCount();
            for (int i = 0; i < count; i++) {
                PreviewPage back = new PreviewPage();
                back.drawBack(cardBack.getImage(), pageNum++, totalPages);
                previewContainer.getChildren().add(back);
            }
        }
    }

    private void updateStatus() {
        int frontPages = sheets.size();
        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = frontPages + backPages;
        String info = !cardBackList.isEmpty()
                ? " | 背面 " + backPages + " 页"
                : "";
        statusLabel.setText("共 " + cardList.size() + " 张卡牌 | 预计 " + totalPages + " 页" + info);
    }

    // ---- PDF 导出 ----

    private void onExportPdf() {
        if (sheets.isEmpty()) {
            showError("提示", "没有可导出的卡牌");
            return;
        }
        if (cardFolderPath == null) {
            showError("错误", "未选择卡牌文件夹");
            return;
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd_HHmmss"));
        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = sheets.size() + backPages;
        String fileName = "卡牌打印_" + totalPages + "页_" + ts + ".pdf";
        Path outputPath = cardFolderPath.resolve(fileName);

        isExporting = true;
        exportBtn.setDisable(true);
        statusLabel.setText("正在导出 PDF → " + fileName + " ...");

        List<CardBack> backs = new ArrayList<>(this.cardBackList);

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() {
                return pdfExportService.export(sheets, outputPath, backs);
            }

            @Override
            protected void succeeded() {
                isExporting = false;
                exportBtn.setDisable(false);
                statusLabel.setText("导出完成: " + outputPath.getFileName());
                showInfo("导出成功", "PDF 已保存到:\n" + outputPath.toAbsolutePath());
            }

            @Override
            protected void failed() {
                isExporting = false;
                exportBtn.setDisable(false);
                statusLabel.setText("导出失败");
                showError("导出失败", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
}
