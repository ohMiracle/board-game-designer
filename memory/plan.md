# Plan

## 已完成

### 核心功能
- [x] LayoutConfig, CardImage (含 copy), CardSheet, CardSlot
- [x] ImageScaleUtil (center-crop), FileUtil (文件扫描排序)
- [x] ImageLoadService, LayoutCalculator, PdfExportService
- [x] 单元测试: LayoutCalculatorTest (7), ImageScaleUtilTest (5)

### GUI
- [x] JavaFX FXML 布局、MainController
- [x] PreviewPane (A4 Canvas 预览)
- [x] CardListCell (序号 + 缩略图 + ×按钮 + 右键菜单 + 拖拽排序)
- [x] 卡牌列表自由复制/移动/删除
- [x] 卡背图片选择 + 背面页生成（每正面页后跟一张 3×3 满铺卡背页）
- [x] 卡背预览切换 (CheckBox "卡背")

### 后续
- [ ] 裁剪标记 (crop marks)
- [ ] 自定义卡牌尺寸
- [ ] 国际化
