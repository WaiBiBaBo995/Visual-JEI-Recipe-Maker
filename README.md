# Visual JEI Recipe Maker 1.0.1 -English-

Minecraft 1.20.1 Forge edition, version **1.0.1**. Requires Java 17 and Forge 47.1.1+. On the client, install JEI 15.74.0.225 or EMI 1.1.24+1.20.1, or both. This branch is an independent project; NeoForge 1.21.1 is on `main`.

## Installation and Usage

Place the matching JAR in your Forge 1.20.1 instance's `mods` directory. The server also needs VJRM for original recipe reading and server-side export; dedicated servers do not need JEI/EMI.

Press **F8** in a world. Select a recipe type, then drag items or fluids from JEI/EMI or the player inventory into recipe slots; no items are consumed. Click a slot to edit its ID, tag, or count. The EMI copy book button is shown by default; the JEI button is hidden on first install. Bind **Show/Hide JEI/EMI Recipe Copy Button** in Controls to toggle it.

- The editor scales with the window and GUI scale and remains centered.
- Right-clicking a required ingredient shows the rejection reason at the cursor, plays the vanilla rejection sound, and preserves the draft.
- **Values** edits existing time, mana, energy, chance, count, and nested fields while preserving large integer precision and numeric string types.
- **Edit JSON** provides highlighting, line numbers, errors, bracket matching/completion, indentation, undo/redo, formatting, and copying. Ctrl+Shift+F formats; Ctrl+S applies.
- A yellow border means the slot lacks a unique JSON field binding. Select a field and click **Bind Slot**. Bound field tooltips use gold text.

Client exports go to `vjrm_exports/vjrm_generated/data/<namespace>/recipes/<path>.json`; server exports go to `<world>/datapacks/vjrm_generated/data/<namespace>/recipes/<path>.json`. The datapack format is **15**. Server export requires permission level 2 by default, configurable with `exportPermissionLevel` in the world's `serverconfig/vjrm-server.toml`. Reading originals does not require OP. Overwriting must be enabled explicitly. Enable the datapack and run `/reload` after export.

Recipe sources and type search show both IDs and names. Slot tooltips retain ingredient names, IDs, mod names, and browser descriptions. **Copy to Clipboard** copies the draft without applying it.

Optional Mekanism support covers gases, infusions, pigments, slurries, paths such as `chemicalInput`, `gasInput`, and `input_chemical`, tags, and alternatives. Bind, drag, edit IDs/tags, and change long counts. Mekanism is not bundled. See the [official source](https://github.com/mekanism/Mekanism/tree/1.20.x).

## Manual JSON Fields

When automatic detection misses a field, select a slot and click **JSON Fields** above the recipe diagram. Search the current JSON or enter a JSON Pointer, choose the type and input/output role, then **Add / Update**. Selecting a slot first binds the new field immediately.

Item, fluid, and optional chemical fields support dragging and ID/tag operations. Use **Raw JSON** for private structures and enter valid JSON in the value box. Changes use the actual serializer and roll back on failure. Array indices start at zero; escape `/` as `~1` and `~` as `~0` in keys.

Manual definitions belong to the draft, survive resizing and same-type JSON edits, and reset on template/type changes. They are not exported; removing a definition does not delete JSON data. Astral Sorcery runtime checks in the supplied source apply to its NeoForge 1.21.1 edition.

## EMI and JEMI

The EMI copy button works without development recipe decorators. `showEmiCopyButton` defaults to `true` in `config/vjrm-client.toml`; the keybind toggles and saves it. JEI uses `showJeiCopyButton`.

EMI renders complete original `EmiRecipe.addWidgets` components, preserving backgrounds, arrows, flames, text, and fluid slots. JEMI recipes keep their original widgets; compatible JEI categories can be rebuilt through JEMI's JEI runtime.

**Third-party pages that only integrate with JEI require JEI alongside EMI.** With only EMI, vanilla and native EMI pages work. If a wrapper category cannot be rebuilt from edited data, its slots update while other dynamic text retains the original display values; the editor shows a notice.

References: [EMI 1.20](https://github.com/emilyploszaj/emi/tree/1.20), [JEI 1.20.1](https://github.com/mezz/JustEnoughItems/tree/1.20.1).

## Recipe Format

Validation uses Forge 1.20.1's `RecipeSerializer.fromJson`. Items use `item` / SNBT `nbt`, fluids use Forge `FluidStack`, and original conditions and extension fields are preserved. Vanilla cooking outputs are item ID strings with count fixed at one; stonecutting count is the root `count` field.

There is no general recipe JSON encoder in 1.20.1. A mod's serializer determines valid numeric ranges and machine behavior; vanilla `cookingtime` also checks integer truncation. Private formats, chemicals, and program-generated recipes can require manual JSON editing.

## Build and Verification

Use Java 17 in this branch:

```powershell
.\gradlew.bat build
.\gradlew.bat verifyRecipes
.\gradlew.bat -Pviewer=none verifyRecipes
```

Release JARs are in `build/libs/`, processed by `reobfJar`, and exclude test recipes, plugins, JEI, and EMI. For client checks, first run `verifyRecipes`, then copy `run/gameTestServer/world` to `run/client/saves/Vjrm UI Verification`. Use a 1.20.1 world, not a 1.21 world.

```powershell
.\gradlew.bat -PverifyClient verifyClient
.\gradlew.bat -PverifyClient -Pviewer=emi verifyClient
.\gradlew.bat -PverifyClient -Pviewer=both verifyClient
```

Development fixtures in `src/test` cover serialization, numeric precision, original JSON, fluids, datapack paths/formats, permissions, rate limiting, networking, and real JEI/EMI/JEMI layouts. Results and screenshots go to `build/verification/`.

Optional Mekanism checks use `.\tools\prepare-mekanism-tests.ps1` to prepare verified local test dependencies, followed by Gradle with `-PverifyMekanism`. EMI client checks disable recipe decorators and click the book button on a real recipe page. Historical compatibility checks do not guarantee every modpack's private recipe formats.

## Versions and branches

Each branch is an independent project. Install the JAR matching both Minecraft and the mod loader.

| Minecraft | Loader | Branch | Game Java |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | [main](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/main) | 21 |
| 1.20.1 | Forge | [forge-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/forge-1.20.1) | 17 |
| 1.20.1 | Fabric | [fabric-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.20.1) | 17 |
| 1.21.1 | Fabric | [fabric-1.21.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.21.1) | 21 |
| 26.1.2 | Fabric | [fabric-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-26.1.2) | 25 |
| 26.1.2 | NeoForge | [neoforge-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/neoforge-26.1.2) | 25 |

Version **1.0.1** includes the updated editor and gold binding tooltips. The player inventory label no longer includes the parenthetical explanation. Loader metadata contains the exact mod version `1.0.1`; artifact filenames may also identify Minecraft and the loader. See [CHANGELOG.md](CHANGELOG.md) for history.

# Visual JEI Recipe Maker 1.0.1 -中文-

1.0.1 的 Minecraft 1.20.1 Forge 移植版。Java 17，Forge 47.1.1+；客户端安装 JEI 15.74.0.225 或 EMI 1.1.24+1.20.1，任选一个，也可以同时安装。此分支为独立工程，1.21.1 NeoForge 工程位于 `main` 分支。

## 安装与使用

把构建后的 JAR 放入 1.20.1 Forge 实例的 `mods`。联机读取原始配方与服务端导出需要服务端也安装本模组；服务端不需要 JEI/EMI。

进入世界按 **F8**。在「配方类型」中选模板，从 JEI/EMI 侧栏或下方玩家物品栏拖入物品；可以选中槽位输入物品/流体 ID、标签和数量。操作不消耗物品。EMI 复制配方的书本按钮默认显示；JEI 保持原有配置、首次安装默认隐藏。在控制设置中为「显示/隐藏 JEI/EMI 配方复制按钮」绑定快捷键即可切换。

- 编辑器按整个游戏视口居中，随分辨率和 GUI 比例自动缩放。
- 右键不能清除槽位时，在鼠标位置显示原因，并播放原版村民拒绝音效。失败时回滚草稿。
- 「数值」编辑已有的耗时、魔力、能量、概率、数量和嵌套数值；保留大整数精度与数字字符串类型。
- 「编辑 JSON」提供语法颜色、行号、错误定位、括号配对、括号/引号补全、自动缩进、撤销重做和格式化。Ctrl+Shift+F 格式化，Ctrl+S 应用。
- 黄色边框表示槽位还没有唯一对应的 JSON 字段，可先选择字段再点击「绑定槽位」。

客户端导出到 `vjrm_exports/vjrm_generated/data/<命名空间>/recipes/<路径>.json`；服务端导出到 `<世界>/datapacks/vjrm_generated/data/.../recipes/...`。数据包格式为 **15**。服务端默认要求权限等级 2，可在世界的 `serverconfig/vjrm-server.toml` 修改。读取原始配方不要求 OP。覆盖必须显式启用，导出后通过 `/reload` 加载。

历史更新见 [CHANGELOG.md](CHANGELOG.md)。

配方来源与类型检索均显示 ID 和名称。悬停配方槽位可查看原料名称、ID、所属模组及浏览器原有文字说明，已绑定槽位追加 JSON 字段路径。「编辑 JSON」中的「复制到剪贴板」会原样复制草稿，不触发应用。

可选兼容 Mekanism 气体、灌注、颜料、浆液，识别 `chemicalInput` / `gasInput` / `input_chemical` 等表达式、标签和替代原料。支持绑定、拖拽替换、ID/标签和 long 数量编辑。字段与绑定按钮位于配方图上方。Mekanism 不随 JAR 打包；参考 [官方源码](https://github.com/mekanism/Mekanism/tree/1.20.x)。

## 手动 JSON 字段

自动识别中没有对应字段时，先点击槽位，再点击配方图上方的「JSON字段」。搜索并选择当前 JSON 的节点，或输入 `/...` 路径，指定类型和输入/输出用途后「添加／更新」。字段会加入可选列表；先选择了槽位时会直接绑定。

物品、流体和可选 Mekanism 化学品可使用原有拖拽和 ID/标签操作。任意私有结构可选「原始 JSON」，在编辑器值栏输入有效 JSON，再按「应用修改」；失败时回滚。数组下标从 0 开始，键名中的 `/` 和 `~` 分别写作 `~1`、`~0`。

手动定义属于当前草稿，缩放和同类型 JSON 编辑后保留；更换模板或配方类型后重置，不写入导出的配方。「移除手动字段」只移除定义，不删除 JSON 数据。更多示例见 [主工程说明](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/blob/main/README.md)。Astral Sorcery 本体的运行验证针对官方 1.21.1 NeoForge 版本。

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

本版 35 项基础检查、Mekanism 真实化学品检查及客户端配置已通过。第三方界面通过测试专用机器类别验证；Mekanism 已使用正式模组验证；未逐一实测所有整合包模组。

Mekanism 额外测试可先运行 `.\tools\prepare-mekanism-tests.ps1`，准备校验过的本地 Maven 测试依赖，再为 Gradle 添加 `-PverifyMekanism`。

EMI 客户端验证会关闭配方装饰器，从真实配方页点击书本按钮打开编辑器，检查原始 JSON、来源名称与槽位提示。

## 版本与分支

每个分支都是独立工程。请安装同时匹配 Minecraft 版本与加载器的 JAR。

| Minecraft | 加载器 | 分支 | 游戏 Java |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | [main](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/main) | 21 |
| 1.20.1 | Forge | [forge-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/forge-1.20.1) | 17 |
| 1.20.1 | Fabric | [fabric-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.20.1) | 17 |
| 1.21.1 | Fabric | [fabric-1.21.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.21.1) | 21 |
| 26.1.2 | Fabric | [fabric-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-26.1.2) | 25 |
| 26.1.2 | NeoForge | [neoforge-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/neoforge-26.1.2) | 25 |

当前版本 **1.0.1** 包含新版编辑器及金色绑定字段提示。玩家物品栏标题已移除括号说明；模组元数据中的版本为精确的 `1.0.1`，JAR 文件名可附带游戏版本和加载器标识。历史更新见 [CHANGELOG.md](CHANGELOG.md)。
