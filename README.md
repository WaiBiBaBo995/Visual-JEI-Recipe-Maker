# Visual JEI Recipe Maker — 1.20.1 Forge

0.5.3 的 Minecraft 1.20.1 Forge 移植版。Java 17，Forge 47.1.1+；客户端安装 JEI 15.74.0.225 或 EMI 1.1.24+1.20.1，任选一个，也可以同时安装。此分支为独立工程，1.21.1 NeoForge 工程位于 `main` 分支。

## 安装与使用

把构建后的 JAR 放入 1.20.1 Forge 实例的 `mods`。联机读取原始配方与服务端导出需要服务端也安装本模组；服务端不需要 JEI/EMI。

进入世界按 **F8**。在「配方类型」中选模板，从 JEI/EMI 侧栏或下方玩家物品栏拖入物品；可以选中槽位输入物品/流体 ID、标签和数量。操作不消耗物品。EMI 复制配方的书本按钮默认显示；JEI 保持原有配置、首次安装默认隐藏。在控制设置中为「显示/隐藏 JEI/EMI 配方复制按钮」绑定快捷键即可切换。

- 编辑器按整个游戏视口居中，随分辨率和 GUI 比例自动缩放。
- 右键不能清除槽位时，在鼠标位置显示原因，并播放原版村民拒绝音效。失败时回滚草稿。
- 「数值」编辑已有的耗时、魔力、能量、概率、数量和嵌套数值；保留大整数精度与数字字符串类型。
- 「编辑 JSON」提供语法颜色、行号、错误定位、括号配对、括号/引号补全、自动缩进、撤销重做和格式化。Ctrl+Shift+F 格式化，Ctrl+S 应用。
- 黄色边框表示槽位还没有唯一对应的 JSON 字段，可先选择字段再点击「绑定槽位」。

客户端导出到 `vjrm_exports/vjrm_generated/data/<命名空间>/recipes/<路径>.json`；服务端导出到 `<世界>/datapacks/vjrm_generated/data/.../recipes/...`。数据包格式为 **15**。服务端默认要求权限等级 2，可在世界的 `serverconfig/vjrm-server.toml` 修改。读取原始配方不要求 OP。覆盖必须显式启用，导出后通过 `/reload` 加载。

历史更新记录见 [CHANGELOG.md](CHANGELOG.md)。

配方来源与类型检索均显示 ID 和名称。悬停配方槽位可查看原料名称、ID、所属模组及浏览器原有文字说明，已绑定槽位追加 JSON 字段路径。「编辑 JSON」中的「复制到剪贴板」会原样复制草稿，不触发应用。

可选兼容 Mekanism 气体、灌注、颜料、浆液，识别 `chemicalInput` / `gasInput` / `input_chemical` 等表达式、标签和替代原料。支持绑定、拖拽替换、ID/标签和 long 数量编辑。字段与绑定按钮位于配方图上方。Mekanism 不随 JAR 打包；参考 [官方源码](https://github.com/mekanism/Mekanism/tree/1.20.x)。

## EMI 与 JEMI

EMI 复制按钮不依赖开发用配方装饰器，默认使用 `config/vjrm-client.toml` 的 `showEmiCopyButton = true`，快捷键可显隐并保存。JEI 使用原有 `showJeiCopyButton`。

EMI 页面使用原配方的完整 `EmiRecipe.addWidgets`，保留背景、箭头、火焰、文字和流体槽；不只绘制槽位。来自 JEMI 的配方继续使用 JEMI 的原组件；可以重建的 JEI 类别通过 JEMI 提供的 JEI 运行时重建。

**只有 JEI 适配的第三方模组界面，需要同时安装 JEI。** EMI 1.20 分支的 JEMI 使用 JEI API 和运行时；仅装 EMI 时可用原版及模组原生 EMI 界面。包装类别不能从编辑后的配方重建时，槽位会更新，其他动态文字仍来自原始展示对象，编辑器会显示提示。

参考：[EMI 1.20 官方源码](https://github.com/emilyploszaj/emi/tree/1.20)、[JEI 1.20.1 官方源码](https://github.com/mezz/JustEnoughItems/tree/1.20.1)。

## 1.20.1 配方格式

使用 Forge 1.20.1 的 `RecipeSerializer.fromJson` 校验；物品使用 `item` / SNBT `nbt`，流体使用 Forge `FluidStack`。原版烧炼结果是物品 ID 字符串，数量固定为 1；切石的数量在根字段 `count`。原始数据包条件及其他扩展字段保留。

1.20.1 没有通用的配方 JSON 编码器。数值是否被机器使用、合法范围等由对应模组的 serializer 决定；额外检查原版 `cookingtime` 的整数截断。不会承诺所有私有格式、化学品或程序生成配方都能自动对应槽位，复杂字段仍可用 JSON 编辑。

## 构建与验证

使用 Java 17，在此目录执行：

```powershell
.\gradlew.bat build
.\gradlew.bat verifyRecipes
.\gradlew.bat -Pviewer=none verifyRecipes
```

发布 JAR 位于 `build/libs/`，经过 `reobfJar`，不包含测试配方、测试插件、JEI 或 EMI。

客户端验证使用独立的 1.20.1 世界。在 `verifyRecipes` 成功后，将其生成的 `run/gameTestServer/world` 复制为 `run/client/saves/Vjrm UI Verification`（不要使用 1.21 世界），然后运行：

```powershell
.\gradlew.bat -PverifyClient verifyClient
.\gradlew.bat -PverifyClient -Pviewer=emi verifyClient
.\gradlew.bat -PverifyClient -Pviewer=both verifyClient
```

开发运行包含 `src/test` 的自定义机器配方和验证插件。验证覆盖配方序列化、数值精度、原始 JSON、流体、数据包路径与格式、权限和限频、网络读取与导出，以及实际客户端编辑器和完整 JEI/EMI/JEMI 布局。结果及截图在 `build/verification/`。

本版 35 项基础检查、Mekanism 真实化学品检查及客户端配置已通过，详见 [验证记录](VALIDATION.md)。第三方界面通过测试专用机器类别验证；Mekanism 已使用正式模组验证；未逐一实测所有整合包模组。

Mekanism 额外测试可先运行 `.\tools\prepare-mekanism-tests.ps1`，准备校验过的本地 Maven 测试依赖，再为 Gradle 添加 `-PverifyMekanism`。

EMI 客户端验证会关闭配方装饰器，从真实配方页点击书本按钮打开编辑器，检查原始 JSON、来源名称与槽位提示。
