# Gotchas

## FXML 注意事项
- **不要在 FXML 标签名中写泛型参数** — `<ListView<CardImage>>` 会导致 XML 解析错误（`XMLStreamException: ParseError`）。FXML 标签名只用 `ListView`，Java 泛型由 Controller 字段声明即可。
- **不要在 FXML 的 `onAction` 中引用 `private` 方法** — FXML 反射调用需要方法可访问。建议在 Controller 的 `initialize()` 中用 `setOnAction()` 绑定事件。

## JavaFX + Maven
- 启动 GUI 必须用 `mvn javafx:run`，不能用 `java -jar`（除非单独配置了 JavaFX 模块路径）。
