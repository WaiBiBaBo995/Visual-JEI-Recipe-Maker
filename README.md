# Visual JEI Recipe Maker 1.0.2 -English-

## New in 1.0.2

JSON and supported GTCEu JS pages display native browser icons after recognized item/fluid IDs, including manually added fields. Recipe type search also shows category icons. Icons belong to the display layer: copying, formatting, and exports keep the original text unchanged. Wrapping, selection, mouse positioning, and cursor movement account for icon space; invalid drafts clear stale icons. Fluid samples fill their icons, and 26.1.2 separates ingredient/text rendering layers for correct highlighting and clipping.

All editions use the exact mod metadata version `1.0.2`. The inventory label retains its local-language name without the parenthetical explanation. The 26.1.2 editions support JEI only. See [CHANGELOG.md](CHANGELOG.md) for full history and [COMPATIBILITY.md](COMPATIBILITY.md) for platform-specific integrations.

This independent branch contains **Minecraft 26.1.2 Fabric**, version **1.0.2**. Use the matching row below for installation. Other editions are maintained on separate branches.

## Installation

| Minecraft | Game Java | Fabric Loader | Fabric API | Recipe browser |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17 | 0.19.5 | 0.92.12+1.20.1 | JEI 15.74.0.225 or EMI 1.1.24+1.20.1 |
| 1.21.1 | 21 | 0.19.5 | 0.116.6+1.21.1 | JEI 19.27.0.350 or EMI 1.1.24+1.21.1 |
| 26.1.2 | 25 | 0.19.5 | 0.155.3+26.1.2 | JEI 29.47.0.111 |

Install the VJRM JAR for this branch's Minecraft version alongside the matching Fabric API and browser. Fabric 1.20.1/1.21.1 support JEI alone, EMI alone, or both; when both are present, the editor uses EMI and JEMI for JEI categories. Fabric 26.1.2 integrates with JEI only.

For multiplayer, install the same Minecraft/loader edition of VJRM and Fabric API on the server to read active recipe JSON, list source templates, and export to world datapacks. The server does not need JEI/EMI. On 26.1.2, Fabric recipe synchronization handles changed vanilla recipes; third-party categories use their own synchronization and browser integration.

## Usage

1. Press **F8** in a world. The editor scales and stays centered, with field selection, **JSON Fields**, and binding controls above the original recipe diagram.
2. Select a recipe type and drag ingredients from the browser or player inventory. No items are consumed. Click a slot to edit its ID/tag/count; right-click removes optional inputs or shows a rejection reason at the cursor with a vanilla sound.
3. Tooltips preserve ingredient names, IDs, and browser descriptions; appended binding paths use **gold** text. The player inventory label has no parenthetical suffix.
4. JEI copy book buttons are hidden by default. Bind the copy-button toggle in Controls, or change the client configuration. EMI copy buttons are shown by default.
5. **Values** edits existing time, energy, chance, and other numeric fields. **Edit JSON** supports highlighting, completion, indentation, undo/redo, formatting, validation, and copying the current draft without applying it.
6. For missing automatic bindings, select a slot and use **JSON Fields**. Search existing JSON nodes or enter a JSON Pointer, choose item/fluid/raw JSON and input/output, then add the field. Select inner nodes for private structures. The actual serializer validates changes and failures roll back. Definitions are not included in exports.

Fabric uses JSON configuration: `showJeiCopyButton` and `showEmiCopyButton` in `config/vjrm-client.json`; `exportPermissionLevel` in `config/vjrm-server.json`, default 2, range 0–4. Restart the game/server after editing files.

Fluid bridging follows Fabric Transfer units: one bucket is **81000 droplets**, rather than Forge mB. Drag replacement preserves the original JSON amount. Vanilla cooking output counts in 1.20.1/1.21.1 are fixed at one; unsupported changes are rejected. Other formats follow the relevant serializer.

## Export

Client exports are in `vjrm_exports/vjrm_generated/`; server exports are in the world's `datapacks/vjrm_generated/`. Enable the generated pack and run `/reload`.

| Minecraft | Recipe directory | Datapack format |
| --- | --- | --- |
| 1.20.1 | `data/<namespace>/recipes/<path>.json` | 15 |
| 1.21.1 | `data/<namespace>/recipe/<path>.json` | 48 |
| 26.1.2 | `data/<namespace>/recipe/<path>.json` | 101.1 |

Vanilla 26.1.2 ingredients use compact strings and `#tags`. Manual field definitions stay in the current draft and are never exported as VJRM metadata. Explicitly enable overwriting when replacing an existing recipe.

## Build and Verification

Use **Java 25 to run Gradle**, with the target Java 17/21/25 compiler toolchain installed. Each branch is an independent project; run from its root:

```powershell
.\gradlew.bat build
.\gradlew.bat verifyServer -PverifyRuntime -PwithoutJei
.\gradlew.bat verifyClient -PverifyRuntime
# Only for 1.20.1 and 1.21.1:
.\gradlew.bat verifyClient -PverifyRuntime -Pviewer=emi
.\gradlew.bat verifyClient -PverifyRuntime -Pviewer=both
```

Release JARs are in `build/libs/`. Verification uses isolated worlds under `run/`; run server checks before client checks. Fixtures and recipes belong to a separate test mod and are excluded from release JARs, as is third-party browser code. History is in [CHANGELOG.md](CHANGELOG.md). JEI 26.1 reference: [official source](https://github.com/mezz/JustEnoughItems/tree/26.1).

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

Version **1.0.2** includes native item/fluid text icons, recipe category icons, the updated editor, and gold binding tooltips. The player inventory label no longer includes the parenthetical explanation. Loader metadata contains the exact mod version `1.0.2`; artifact filenames may also identify Minecraft and the loader. See [CHANGELOG.md](CHANGELOG.md) for history.


# Visual JEI Recipe Maker 1.0.2 -中文-

## 1.0.2 更新

JSON 与适用版本的 GTCEu JS 编辑页在已识别的物品/流体 ID 后显示原生图标，手动字段也参与识别；配方类型检索显示类别图标。图标只属于显示层，不改变草稿、复制、格式化或导出的文字。换行、鼠标定位、选择与光标移动按图标占位计算，无效草稿会清理过期图标。流体图标使用满幅示例；26.1.2 分离材料与文字渲染层，保留高亮和裁切。

模组元数据统一为精确版本 `1.0.2`，玩家物品栏标题保留各语言名称并删除括号说明。26.1.2 仅支持 JEI。完整历史见 [CHANGELOG.md](CHANGELOG.md)，平台限定的兼容功能见 [COMPATIBILITY.md](COMPATIBILITY.md)。

此独立分支包含 **Minecraft 26.1.2 Fabric 1.0.2**，其他版本位于各自分支。

## 安装组合

| Minecraft | 游戏 Java | Fabric Loader | Fabric API | 配方浏览器 |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17 | 0.19.5 | 0.92.12+1.20.1 | JEI 15.74.0.225 或 EMI 1.1.24+1.20.1 |
| 1.21.1 | 21 | 0.19.5 | 0.116.6+1.21.1 | JEI 19.27.0.350 或 EMI 1.1.24+1.21.1 |
| 26.1.2 | 25 | 0.19.5 | 0.155.3+26.1.2 | JEI 29.47.0.111 |

将对应游戏版本、加载器的 VJRM JAR 放入 `mods/`，同时安装 Fabric API 和表中的浏览器。1.20.1、1.21.1 可以单独安装 JEI、单独安装 EMI 或共存；共存时编辑器使用 EMI，并可通过 JEMI 保留 JEI 类别的绘制。26.1.2 不接入 EMI。

客户端需要浏览器。多人服务器安装相同游戏版本、加载器的 VJRM 和 Fabric API，用于读取当前实际生效的配方 JSON、列出来源模板并向世界数据包导出；VJRM 的服务端功能无需 JEI/EMI。26.1.2 为原版配方启用 Fabric 的配方同步，浏览器可以收到服务器原版配方的改动；第三方类别继续使用其自身的同步和浏览器集成。

## 使用

- 进入世界按 **F8**。编辑器随分辨率缩放并居中，字段选择、JSON 字段和绑定按钮位于原生配方图上方。
- 从浏览器或玩家物品栏拖拽填入，操作不消耗物品。点击槽位可编辑 ID、标签和数量；右键不可移除时，在鼠标处显示原因并播放原版提示音。
- 槽位提示保留名称、ID 和浏览器说明，追加的「已绑定到 字段路径」以 **金色** 显示。
- JEI 的复制书本按钮默认隐藏，在控制设置中为「显示/隐藏 JEI/EMI 配方复制按钮」绑定按键并切换，或修改客户端配置。EMI 的复制按钮默认显示。
- 「数值」编辑已有运行时间、能量等参数。「编辑 JSON」保留高亮、补全、缩进、撤销/重做、格式化、校验和复制到剪贴板。
- 自动字段不足时，选择槽位后打开「JSON字段」，从当前 JSON 搜索节点，或填写 JSON Pointer，指定类型与输入/输出后添加。也可直接编辑原始 JSON。修改仍由实际 serializer 校验，失败回滚。

Fabric 配置使用 JSON：`config/vjrm-client.json` 的 `showJeiCopyButton`、`showEmiCopyButton` 控制复制按钮；`config/vjrm-server.json` 的 `exportPermissionLevel` 默认 2，范围 0–4。配置作用于该游戏/服务器实例。修改文件后重启生效。

流体桥接使用 Fabric transfer 单位，1 桶为 **81000 droplets**，不会把浏览器里的流体数量当成 Forge 的 mB。拖拽替换保留原 JSON 槽位的数量；第三方私有格式可选择内部节点或用原始 JSON 编辑。

1.20.1、1.21.1 的原版烹饪产物数量固定为 1，超出范围的槽位修改会被拒绝。其他配方按各自 serializer 支持的格式编辑；不会引入只存在于 Forge/NeoForge 的配方扩展。

## 导出

客户端导出在 `vjrm_exports/vjrm_generated/`，服务端导出在世界的 `datapacks/vjrm_generated/`。首次导出后启用该数据包并执行 `/reload`。

| Minecraft | 配方目录 | 数据包格式 |
| --- | --- | --- |
| 1.20.1 | `data/<namespace>/recipes/<path>.json` | 15 |
| 1.21.1 | `data/<namespace>/recipe/<path>.json` | 48 |
| 26.1.2 | `data/<namespace>/recipe/<path>.json` | 101.1 |

26.1.2 模板和材料编辑使用新版的字符串及 `#标签` 格式。手动字段定义只存在于当前草稿，不写进配方 JSON。

## 开发与验证

用 **Java 25 启动 Gradle**，并安装目标项目所需的 Java 17/21/25 工具链。在当前分支的工程根目录运行：

```powershell
.\gradlew.bat build
.\gradlew.bat verifyServer -PverifyRuntime -PwithoutJei
.\gradlew.bat verifyClient -PverifyRuntime
# 仅适用于 1.20.1、1.21.1：
.\gradlew.bat verifyClient -PverifyRuntime -Pviewer=emi
.\gradlew.bat verifyClient -PverifyRuntime -Pviewer=both
```

验证使用工程 `run/` 下的独立世界；先运行服务端检查，再运行客户端。测试类与测试配方属于独立测试模组，发布 JAR 不包含这些内容和第三方浏览器代码。

更新记录统一在 [CHANGELOG.md](CHANGELOG.md)。JEI 26.1 源码参考：[官方分支](https://github.com/mezz/JustEnoughItems/tree/26.1)。


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

各分支均为独立可构建工程，安装时匹配游戏版本与加载器。
