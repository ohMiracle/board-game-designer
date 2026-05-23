# Board Game Designer

桌游卡牌 A4 排版打印工具。输入卡牌图片文件夹，通过 GUI 预览 3×3 网格排版效果，导出为可直接 A4 打印的 PDF。

## 功能

- 卡牌与卡背分栏管理，支持拖拽排序
- 多张卡牌关联同一卡背，卡背自动按关联数量计算打印页数（可手动覆盖）
- 不同卡背的卡牌不会混排在同一页
- 图片网格分切：选中一张大图，按指定行列数均匀分割为多张卡牌
- 项目保存/加载：自动在卡牌文件夹下读写 `default.json`，保留所有编辑状态
- A4 排版实时预览
- 导出 300 DPI 的 A4 PDF

## 卡牌规格

| 项目 | 尺寸 |
|------|------|
| 纸张 | A4 (210×297mm) |
| 卡牌 | 63×88mm |
| 排列 | 3×3 网格 |

## 环境要求

- Java 17+
- Maven 3.6+

## 快速开始

```bash
# 编译
mvn compile

# 运行
mvn javafx:run

# 测试
mvn test

# 打包
mvn package
```

## 项目结构

```
com.boardgamedesigner
├── BoardGameDesignerApp.java          # JavaFX 入口
├── ui/
│   ├── MainController.java            # 主控制器
│   ├── PreviewPage.java               # A4 预览 Canvas
│   ├── CardListCell.java              # 卡牌列表单元格
│   └── CardBackListCell.java          # 卡背列表单元格
├── card/
│   ├── model/
│   │   ├── CardImage.java             # 卡牌模型
│   │   ├── CardBack.java              # 卡背模型
│   │   ├── CardSheet.java             # 单页 A4 排版
│   │   └── CardSlot.java              # 槽位坐标
│   ├── service/
│   │   ├── ImageLoadService.java      # 图片加载
│   │   ├── LayoutCalculator.java      # 排版计算
│   │   ├── PdfExportService.java      # PDF 导出
│   │   └── ProjectFileService.java    # 项目 JSON 序列化
│   └── config/
│       └── LayoutConfig.java          # 排版常量
└── util/
    ├── ImageScaleUtil.java            # Center-Crop 缩放
    ├── ImageSplitService.java         # 图片网格分切
    └── FileUtil.java                  # 文件扫描排序
```
