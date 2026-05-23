# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

桌游卡牌 A4 排版打印工具。输入卡牌图片文件夹，通过 JavaFX GUI 预览 3×3 网格排版效果，导出为可直接 A4 打印的 PDF。

- **语言**: Java 17
- **构建工具**: Maven（`E:\software\apache-maven-3.6.3\bin\mvn`）
- **GUI**: JavaFX (OpenJFX 17.0.9)
- **PDF**: Apache PDFBox 2.0.30
- **测试**: JUnit 5 + AssertJ

## 常用命令

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=LayoutCalculatorTest

# 启动 GUI
mvn javafx:run

# 打包 JAR
mvn package
```

## 架构

分层架构：UI → Application → Domain → Infrastructure

```
com.boardgamedesigner
├── BoardGameDesignerApp.java          # JavaFX Application 入口
├── ui/
│   ├── MainController.java            # 主界面控制器，串联所有操作
│   ├── PreviewPane.java               # A4 预览 Canvas 控件
│   └── CardListCell.java              # 列表单元渲染（缩略图+选择框）
├── card/
│   ├── model/
│   │   ├── CardImage.java             # 单张卡牌（路径/尺寸/宽高比）
│   │   ├── CardSheet.java             # 单页 A4 的卡牌集合（≤9张）
│   │   └── CardSlot.java              # 卡牌在 A4 上的精确槽位（含坐标）
│   ├── service/
│   │   ├── ImageLoadService.java      # 图片加载（快速读取尺寸 + 完整加载）
│   │   ├── LayoutCalculator.java      # 按每页 9 张分组生成 CardSheet
│   │   └── PdfExportService.java      # A4 PDF 导出（300 DPI, center-crop）
│   └── config/
│       └── LayoutConfig.java          # 排版常量（纸张/卡牌尺寸/边距/DPI）
└── util/
    ├── ImageScaleUtil.java            # Fill + Center-Crop 缩放裁剪
    └── FileUtil.java                  # 文件扫描 + 中文自然排序
```

### 关键计算

- **A4**: 210×297mm，卡牌: 63×88mm，3×3 网格
- **边距**: 水平 5.25mm，垂直 8.25mm（等距分布）
- **PDF 坐标**: mm × (72/25.4) pt，原点在左下角
- **图片策略**: Center-Crop — 等比缩放至铺满目标区域后居中裁剪
- **导出 DPI**: 300（卡牌约 744×1039 像素）
