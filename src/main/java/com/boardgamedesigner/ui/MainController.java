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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;

import java.awt.print.PrinterJob;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainController {

    @FXML private Button chooseFolderBtn;
    @FXML private Button splitImageBtn;
    @FXML private Button selectAllBtn;
    @FXML private Button deselectAllBtn;
    @FXML private Button exportBtn;
    @FXML private Button printBtn;
    @FXML private Label cardCountLabel;
    @FXML private Label cardBackCountLabel;
    @FXML private Label statusLabel;
    @FXML private TreeView<Object> cardTreeView;
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
    private final TreeItem<Object> treeRoot = new TreeItem<>();
    private List<CardSheet> sheets = List.of();
    private volatile boolean isExporting = false;
    private Path cardFolderPath;

    // 右键菜单前的选中组快照
    private final List<TreeItem<Object>> preClickSelectedGroups = new ArrayList<>();

    @FXML
    private void initialize() {
        cardTreeView.setRoot(treeRoot);
        cardTreeView.setShowRoot(false);
        cardBackListView.setItems(cardBackList);

        // 右键时快照当前已选分组，用于多组批量分配卡背
        cardTreeView.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!e.isSecondaryButtonDown()) return;
            preClickSelectedGroups.clear();
            for (TreeItem<Object> item : cardTreeView.getSelectionModel().getSelectedItems()) {
                if (item.getValue() instanceof String) {
                    preClickSelectedGroups.add(item);
                }
            }
        });

        cardTreeView.setCellFactory(tv -> new CardTreeCell(
                this::copyCard,
                this::removeCard,
                this::setCardBack,
                this::assignCardBack,
                this::unassignCardBackForSelected,
                this::assignBackToCards,
                this::unassignBackFromCards,
                () -> new ArrayList<>(cardBackList),
                this::onCardListChanged,
                () -> new ArrayList<>(preClickSelectedGroups)
        ));

        cardBackListView.setCellFactory(lv -> new CardBackListCell(
                this::unsetCardBack,
                this::removeCardBack,
                this::changeBackPageCount,
                this::renameCardBack,
                this::onCardListChanged
        ));

        cardTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cardBackListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        chooseFolderBtn.setOnAction(e -> onChooseFolder());
        splitImageBtn.setOnAction(e -> onSplitImage());
        selectAllBtn.setOnAction(e -> onCopySelected());
        deselectAllBtn.setOnAction(e -> onRemoveSelected());
        exportBtn.setOnAction(e -> onExportPdf());
        printBtn.setOnAction(e -> onPrint());

        selectAllBtn.setText("复制选中");
        deselectAllBtn.setText("删除选中");
    }

    // ---- 选区辅助 ----

    private CardImage getSelectedCard() {
        TreeItem<Object> sel = cardTreeView.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getValue() instanceof CardImage c) return c;
        return null;
    }

    private List<CardImage> getSelectedCards() {
        List<CardImage> result = new ArrayList<>();
        for (TreeItem<Object> item : cardTreeView.getSelectionModel().getSelectedItems()) {
            if (item.getValue() instanceof CardImage c) result.add(c);
        }
        return result;
    }

    // ---- 文件夹选择 ----

    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择卡牌图片文件夹");
        File dir = chooser.showDialog(cardTreeView.getScene().getWindow());
        if (dir == null) return;

        cardFolderPath = dir.toPath();

        Path projectFile = cardFolderPath.resolve("default.json");
        if (projectFile.toFile().exists()) {
            try {
                ProjectFileService.LoadResult result = projectFileService.load(projectFile, imageLoadService);
                cardList.setAll(result.cards);
                cardBackList.setAll(result.cardBacks);
                statusLabel.setText("已自动加载 default.json");
            } catch (Exception e) {
                loadFolderFresh();
            }
        } else {
            loadFolderFresh();
        }

        refreshSheets();
        buildCardTree();
        updateStatus();
        updateCountLabels();
        boolean noCards = cardList.isEmpty();
        exportBtn.setDisable(noCards);
        printBtn.setDisable(noCards);
        renderAllPreviews();
    }

    private void loadFolderFresh() {
        List<CardImage> loaded = imageLoadService.loadFromFolder(cardFolderPath);
        cardList.setAll(loaded);
        cardBackList.clear();
        autoDetectCardBacks();
    }

    // ---- 图片分切 ----

    private void onSplitImage() {
        CardImage selected = getSelectedCard();
        if (selected == null) {
            showError("提示", "请先在卡牌列表中选择一张要分切的图片");
            return;
        }
        if (cardFolderPath == null) {
            showError("提示", "请先选择卡牌文件夹");
            return;
        }

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
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
            cardList.remove(card);
            cardBackList.add(new CardBack(card));
        }
    }

    // ---- 卡背操作 ----

    private void setCardBack(CardImage card) {
        for (CardBack cb : cardBackList) {
            cb.unlinkCard(card);
        }
        cardList.remove(card);
        cardBackList.add(new CardBack(card));
        onCardListChanged();
    }

    private void unsetCardBack(CardBack cardBack) {
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
        List<CardImage> selected = getSelectedCards();
        if (selected.isEmpty()) return;

        for (CardImage card : selected) {
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
            back.linkCard(card);
        }
    }

    private void unassignCardBackForSelected() {
        List<CardImage> selected = getSelectedCards();
        for (CardImage card : selected) {
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
        }
    }

    private void assignBackToCards(List<CardImage> cards, CardBack back) {
        for (CardImage card : cards) {
            for (CardBack cb : cardBackList) {
                cb.unlinkCard(card);
            }
            back.linkCard(card);
        }
    }

    private void unassignBackFromCards(List<CardImage> cards) {
        for (CardImage card : cards) {
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
        dialog.setContentText("页数（0=自动，当前=" + back.getPageCount() + " | 关联=" + back.getLinkedCount() + "张）:");

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
        for (CardBack cb : cardBackList) {
            cb.unlinkCard(target);
        }
        cardList.remove(target);
    }

    private void onCopySelected() {
        List<CardImage> selected = getSelectedCards();
        if (selected.isEmpty()) return;
        for (int i = selected.size() - 1; i >= 0; i--) {
            copyCard(selected.get(i));
        }
        onCardListChanged();
    }

    private void onRemoveSelected() {
        List<CardImage> selected = getSelectedCards();
        if (selected.isEmpty()) return;
        for (CardImage card : selected) {
            removeCard(card);
        }
        onCardListChanged();
    }

    private void onCardListChanged() {
        refreshSheets();
        buildCardTree();
        updateStatus();
        updateCountLabels();
        boolean noCards = cardList.isEmpty();
        exportBtn.setDisable(noCards);
        printBtn.setDisable(noCards);
        cardBackListView.refresh();
        renderAllPreviews();
        autoSave();
    }

    // ---- 树形视图构建 ----

    private void buildCardTree() {
        List<TreeItem<Object>> groups = new ArrayList<>();

        Map<CardBack, Integer> backPageCounter = new LinkedHashMap<>();
        for (CardBack cb : cardBackList) {
            backPageCounter.put(cb, 0);
        }
        int unassignedPage = 0;

        for (CardSheet sheet : sheets) {
            List<CardImage> cards = sheet.getCards();
            if (cards.isEmpty()) continue;

            // 判断该 sheet 属于哪个卡背
            CardBack owner = null;
            for (CardBack cb : cardBackList) {
                if (cb.getLinkedCards().contains(cards.get(0))) {
                    owner = cb;
                    break;
                }
            }

            String groupName;
            if (owner != null) {
                int page = backPageCounter.get(owner) + 1;
                backPageCounter.put(owner, page);
                groupName = owner.getName() + " - 第" + page + "页 (" + cards.size() + "张)";
            } else {
                unassignedPage++;
                groupName = "未分配 - 第" + unassignedPage + "页 (" + cards.size() + "张)";
            }

            TreeItem<Object> group = new TreeItem<>(groupName);
            group.setExpanded(false);
            for (CardImage card : cards) {
                group.getChildren().add(new TreeItem<>(card));
            }
            groups.add(group);
        }

        treeRoot.getChildren().setAll(groups);
    }

    private void autoSave() {
        if (cardFolderPath == null) return;
        try {
            projectFileService.save(cardFolderPath.resolve("default.json"), cardFolderPath,
                    new ArrayList<>(cardList), new ArrayList<>(cardBackList));
        } catch (Exception ignored) {
        }
    }

    private void updateCountLabels() {
        cardCountLabel.setText("卡牌 (" + cardList.size() + ")");
        int totalLinked = cardBackList.stream().mapToInt(CardBack::getLinkedCount).sum();
        cardBackCountLabel.setText("卡背 (" + cardBackList.size() + " | 关联 " + totalLinked + " 张)");
    }

    // ---- 预览渲染 ----

    private void refreshSheets() {
        List<CardImage> remaining = new ArrayList<>(cardList);
        List<CardSheet> allSheets = new ArrayList<>();

        // 去重：每张卡牌最多关联一个卡背
        List<CardImage> seen = new ArrayList<>();
        for (CardBack cb : cardBackList) {
            for (CardImage card : new ArrayList<>(cb.getLinkedCards())) {
                if (seen.contains(card)) {
                    cb.unlinkCard(card);
                } else {
                    seen.add(card);
                }
            }
        }

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

        if (!remaining.isEmpty()) {
            allSheets.addAll(layoutCalculator.calculateSheets(remaining));
        }

        sheets = allSheets;
    }

    private void renderAllPreviews() {
        previewContainer.getChildren().clear();

        // 仅统计已分配卡背的页面
        int assignedSheetCount = 0;
        for (CardSheet sheet : sheets) {
            if (findBackForSheet(sheet) != null) assignedSheetCount++;
        }
        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = assignedSheetCount + backPages;

        if (sheets.isEmpty() && cardBackList.isEmpty()) {
            Label emptyLabel = new Label("（空页面）");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            previewContainer.getChildren().add(emptyLabel);
            return;
        }

        // 按卡背分组 sheet（未分配的跳过）
        Map<CardBack, List<CardSheet>> backSheets = new LinkedHashMap<>();
        for (CardSheet sheet : sheets) {
            CardBack owner = findBackForSheet(sheet);
            if (owner != null) {
                backSheets.computeIfAbsent(owner, k -> new ArrayList<>()).add(sheet);
            }
        }

        int pageNum = 1;
        for (Map.Entry<CardBack, List<CardSheet>> entry : backSheets.entrySet()) {
            CardBack back = entry.getKey();
            int backCount = back.getPageCount();
            List<CardSheet> frontSheets = entry.getValue();

            int maxLen = Math.max(frontSheets.size(), backCount);
            for (int i = 0; i < maxLen; i++) {
                if (i < frontSheets.size()) {
                    PreviewPage page = new PreviewPage();
                    page.drawFront(frontSheets.get(i), pageNum++, totalPages);
                    previewContainer.getChildren().add(page);
                }
                if (i < backCount) {
                    PreviewPage backPage = new PreviewPage();
                    CardSheet frontSheet = i < frontSheets.size() ? frontSheets.get(i) : null;
                    backPage.drawBack(back.getImage(), frontSheet, pageNum++, totalPages);
                    previewContainer.getChildren().add(backPage);
                }
            }
        }
    }

    private CardBack findBackForSheet(CardSheet sheet) {
        List<CardImage> cards = sheet.getCards();
        if (cards.isEmpty()) return null;
        for (CardBack cb : cardBackList) {
            if (cb.getLinkedCards().contains(cards.get(0))) return cb;
        }
        return null;
    }

    private void updateStatus() {
        int assignedSheets = 0;
        for (CardSheet sheet : sheets) {
            if (findBackForSheet(sheet) != null) assignedSheets++;
        }
        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = assignedSheets + backPages;
        int unassigned = cardList.size() - cardBackList.stream().mapToInt(CardBack::getLinkedCount).sum();
        String info = !cardBackList.isEmpty()
                ? " | 背面 " + backPages + " 页"
                : "";
        String unassignedInfo = unassigned > 0 ? " | 未分配 " + unassigned + " 张（不打印）" : "";
        statusLabel.setText("共 " + cardList.size() + " 张卡牌 | 预计 " + totalPages + " 页" + info + unassignedInfo);
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
        // 只计算已分配卡背的页数（未分配的不导出）
        int assignedSheets = 0;
        for (CardSheet sheet : sheets) {
            if (findBackForSheet(sheet) != null) assignedSheets++;
        }
        int backPages = cardBackList.stream().mapToInt(CardBack::getPageCount).sum();
        int totalPages = assignedSheets + backPages;
        if (totalPages == 0) {
            showError("提示", "没有已分配卡背的卡牌可导出");
            return;
        }
        String fileName = "卡牌打印_" + totalPages + "页_" + ts + ".pdf";
        Path outputPath = cardFolderPath.resolve(fileName);

        isExporting = true;
        exportBtn.setDisable(true);
        printBtn.setDisable(true);
        statusLabel.setText("正在导出 PDF → " + fileName + " ...");

        Stage progressStage = showProgressStage("正在导出 PDF...");

        List<CardBack> backs = new ArrayList<>(this.cardBackList);

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() {
                return pdfExportService.export(sheets, outputPath, backs);
            }

            @Override
            protected void succeeded() {
                progressStage.close();
                isExporting = false;
                exportBtn.setDisable(false);
                printBtn.setDisable(false);
                statusLabel.setText("导出完成: " + outputPath.getFileName());
                showInfo("导出成功", "PDF 已保存到:\n" + outputPath.toAbsolutePath());
            }

            @Override
            protected void failed() {
                progressStage.close();
                isExporting = false;
                exportBtn.setDisable(false);
                printBtn.setDisable(false);
                statusLabel.setText("导出失败");
                showError("导出失败", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    // ---- 打印 ----

    private void onPrint() {
        if (sheets.isEmpty()) {
            showError("提示", "没有可打印的卡牌");
            return;
        }
        if (cardFolderPath == null) {
            showError("错误", "未选择卡牌文件夹");
            return;
        }

        int assignedSheets = 0;
        for (CardSheet sheet : sheets) {
            if (findBackForSheet(sheet) != null) assignedSheets++;
        }
        if (assignedSheets == 0) {
            showError("提示", "没有已分配卡背的卡牌可打印");
            return;
        }

        List<CardBack> backs = new ArrayList<>(this.cardBackList);
        statusLabel.setText("正在准备打印...");
        printBtn.setDisable(true);
        exportBtn.setDisable(true);

        Stage progressStage = showProgressStage("正在准备打印...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path tempFile = Files.createTempFile("card_print_", ".pdf");
                try {
                    pdfExportService.export(sheets, tempFile, backs);

                    PrinterJob job = PrinterJob.getPrinterJob();
                    PDDocument doc = PDDocument.load(tempFile.toFile());
                    job.setPrintable(new PDFPrintable(doc));

                    if (job.printDialog()) {
                        job.print();
                    }
                    doc.close();
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressStage.close();
                printBtn.setDisable(false);
                exportBtn.setDisable(false);
                statusLabel.setText("打印完成");
            }

            @Override
            protected void failed() {
                progressStage.close();
                printBtn.setDisable(false);
                exportBtn.setDisable(false);
                statusLabel.setText("打印失败");
                showError("打印失败", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    private Stage showProgressStage(String message) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(48, 48);
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 13px;");

        VBox box = new VBox(16, pi, label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-radius: 8;");

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
        return stage;
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
