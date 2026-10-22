# Visual JEI Recipe Maker 1.0.2 -English-

## New in 1.0.2

JSON and supported GTCEu JS pages display native browser icons after recognized item/fluid IDs, including manually added fields. Recipe type search also shows category icons. Icons belong to the display layer: copying, formatting, and exports keep the original text unchanged. Wrapping, selection, mouse positioning, and cursor movement account for icon space; invalid drafts clear stale icons. Fluid samples fill their icons, and 26.1.2 separates ingredient/text rendering layers for correct highlighting and clipping.

All editions use the exact mod metadata version `1.0.2`. The inventory label retains its local-language name without the parenthetical explanation. The 26.1.2 editions support JEI only. See [CHANGELOG.md](CHANGELOG.md) for full history and [COMPATIBILITY.md](COMPATIBILITY.md) for platform-specific integrations.

## Thaumaturge

Available on **NeoForge 26.1.2** with JEI 29.x. Install Thaumaturge and its dependencies separately. Each `{ "aspect": "thaumaturge:ignis", "amount": 10 }` in `aspects` becomes an independent aspect field resolved through public codecs and the active datapack registry, including additional aspects. Drag JEI aspects, edit IDs and positive counts, or choose **Aspect** in manual fields. Thaumaturge still renders native icons, names, and research rules; tooltips append IDs, mod names, and gold bindings.

`components` arrays map to independent input slots. Duplicate materials bind by array position; alternatives within one entry remain one slot. Data component ingredient conditions are retained as whole ingredients, preventing inner aspect components from becoming extra slots. Replacements update the selected ingredient condition while retaining other recipe data. JSON editing/highlighting/copying and datapack export remain available. See [COMPATIBILITY.md](COMPATIBILITY.md).

Independent **Minecraft 26.1.2 NeoForge** edition, version **1.0.2**, using **Java 25**. Other Minecraft/loader editions are maintained on separate branches.

## Installation

- Minecraft **26.1.2**, NeoForge **26.1.2.122+**, Java **25**.
- Install **JEI 29.47.0.111+ for NeoForge 26.1.2** and VJRM on the client.
- Multiplayer servers need the matching VJRM version for original recipe JSON reading, third-party template lookup, and server-side datapack export. Dedicated servers do not need JEI.
- This edition integrates with JEI. Do not install older Minecraft editions of EMI, JEI, or VJRM with it.

## Usage

Press **F8** in a world. Select a recipe type, then drag ingredients from JEI or the player inventory; no items are consumed. The recipe diagram stays centered with field selection, **JSON Fields**, and binding controls above it. Slot tooltips show names, IDs, and gold binding paths. Rejected right-click removal shows the reason at the cursor and plays a vanilla notification sound.

The **Copy to VJRM Editor** book button is hidden by default. Bind **Show/Hide JEI Recipe Copy Button** under Visual JEI Recipe Maker in Controls to toggle it, or set `showJeiCopyButton = true` in `config/vjrm-client.toml` while the game is closed. Copying reads real source JSON and retains extension fields. JEI's `getSourceRecipeIds` takes priority over display IDs; NeoForge's synchronized recipe index also maps original Recipe objects to their real IDs.

**Values** edits existing time, energy, and other numeric fields or common numeric strings. **Edit JSON** provides highlighting, bracket completion, indentation, undo/redo, formatting, validation, and **Copy to Clipboard**. Copying preserves the current draft without applying it.

For undetected fields, use **JSON Fields** to search a node or enter a JSON Pointer such as `/custom/input/0`, then select input/output and item, fluid, or raw JSON type. Selecting a slot first binds the field immediately. Definitions belong only to the draft and never enter exported JSON. Select inner ID nodes for private wrappers or edit raw JSON; the actual serializer validates changes.

Vanilla 26.1.2 ingredients use compact forms such as `"minecraft:stone"` and `"#minecraft:logs"`. Templates, replacements, tags, alternative arrays, and empty crafting slot insertion follow that format. Exports use `data/<namespace>/recipe/<path>.json` and datapack format **101.1**.

Server export defaults to permission level 2, configurable with `exportPermissionLevel` in the world's `serverconfig/vjrm-server.toml`. Client exports are under `vjrm_exports/`. Put the generated pack in the world's `datapacks/`, enable it, and run `/reload`. Loader/browser integration does not imply runtime verification of every third-party mod; older Astral Sorcery and Mekanism checks do not establish 26.1.2 compatibility.

## Build and Verification

Set `JAVA_HOME` to Java 25 and run:

```powershell
.\gradlew.bat build
.\gradlew.bat verifyServer -PverifyRuntime -PwithoutJei
.\gradlew.bat verifyClient -PverifyRuntime
```

The first server check creates an isolated world under this project's `run/`; client checks reuse it. Results are written to `build/verification/server/result.txt` and `build/verification/client/result.txt`, with screenshots in the client directory. Test classes, test recipes, and third-party dependencies are excluded from release JARs. History is in [CHANGELOG.md](CHANGELOG.md).

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

## Thaumaturge

**26.1.2 NeoForge** 支持 Thaumaturge `AspectInstance` 源质槽、源质 ID、数量、拖拽、提示与手动绑定。通过公开 codec 和当前数据包注册表解析，包括附加源质；原生图标与研究解锁规则由 Thaumaturge 负责。

`components` 数组中的多个输入按位置分别绑定，重复材料互不影响，数据组件材料条件作为完整材料处理。继续使用 JSON 编辑与数据包导出。完整用法见 [COMPATIBILITY.md](COMPATIBILITY.md)。

独立的 **NeoForge 26.1.2** 移植工程，使用 **Java 25**。其他版本位于各自分支；1.21.1 NeoForge 位于 `main`。

## 安装

- Minecraft **26.1.2**，NeoForge **26.1.2.122+**，Java **25**。
- 客户端安装 **JEI 29.47.0.111+（26.1.2 NeoForge 版）** 和 VJRM。
- 多人服务器也安装相同版本 VJRM，才能读取服务器当前生效的配方 JSON、检索第三方模板和向世界数据包导出。专用服务器无需安装 JEI。
- EMI 官方目前没有 26.1.2 构建，此移植版使用 JEI。不要混装旧版 EMI、JEI 或其他 Minecraft 版本的 VJRM。

## 使用

进入世界按 **F8** 打开编辑器。选择配方类型后，从 JEI 或玩家物品栏拖入原料；操作不消耗物品。配方图在编辑器展示区内居中，字段、JSON 字段和槽位绑定按钮位于图上方。槽位悬停显示名称、ID 和金色绑定路径；右键无法移除时，在鼠标位置显示原因并播放原版提示音。

JEI「复制到 VJRM 编辑器」书本按钮沿用默认隐藏设置。在游戏控制设置的 Visual JEI Recipe Maker 分类中，为「显示/隐藏 JEI 配方复制按钮」绑定按键，按一次开启；也可在退出游戏后将 `config/vjrm-client.toml` 的 `showJeiCopyButton` 设为 `true`。复制读取实际来源 JSON，保留扩展字段。新版 JEI 的 `getSourceRecipeIds` 优先于展示 ID；原始 Recipe 对象还可通过 NeoForge 同步配方索引查找真实 ID。

「数值」可修改运行时间、能量等已存在的数值和常见数字字符串。「编辑 JSON」保留语法高亮、括号补全、缩进、撤销/重做、格式化、校验和「复制到剪贴板」。复制按钮原样复制当前草稿，不应用草稿。

未识别的字段可通过「JSON字段」搜索节点或填写 JSON Pointer（例如 `/custom/input/0`）添加；指定输入/输出及物品、流体或原始 JSON 类型。先选择槽位再添加，会直接绑定。手动定义只属于当前草稿，不混入导出 JSON。第三方私有结构可选内部 ID 节点或使用原始 JSON 编辑；修改仍由该配方实际 serializer 校验。

26.1.2 的原版材料使用 `"minecraft:stone"`、`"#minecraft:logs"` 等紧凑格式。模板、材料替换、标签、可选材料数组和空工作台槽位插入已适配；不会把原版材料写回旧版的 `{"item":"..."}` 格式。导出目录为 `data/<namespace>/recipe/<path>.json`，数据包格式为 **101.1**。

服务端导出默认需要权限等级 2，可在世界的 `serverconfig/vjrm-server.toml` 修改 `exportPermissionLevel`。客户端导出位于游戏目录的 `vjrm_exports/`。把数据包放进世界 `datapacks/` 并启用后执行 `/reload`。加载器和浏览器适配不等于所有第三方模组均已在 26.1.2 实测；旧版 Astral Sorcery、Mekanism 的运行验证不直接沿用到这个版本。

## 构建与验证

设置 `JAVA_HOME` 为 Java 25，在本目录运行：

```powershell
.\gradlew.bat build
.\gradlew.bat verifyServer -PverifyRuntime -PwithoutJei
.\gradlew.bat verifyClient -PverifyRuntime
```

首次验证会在本工程 `run/` 下创建隔离的测试世界；客户端检查使用上一步生成的世界。检查结果在 `build/verification/server/result.txt` 和 `build/verification/client/result.txt`，截图在后一个目录。测试类、测试配方和第三方依赖不进入发布 JAR。

更新记录见 [CHANGELOG.md](CHANGELOG.md)。。

本次 GTCEu / Thaumaturge 兼容的具体用法见 [COMPATIBILITY.md](COMPATIBILITY.md)。

1.0.2：JSON 编辑页显示已识别物品/流体字符串的原生图标；配方类型检索显示类别图标。图标不改变草稿或剪贴板文本。完整更新记录见 [CHANGELOG.md](CHANGELOG.md)。

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
