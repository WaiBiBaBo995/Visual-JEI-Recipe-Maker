# Visual JEI Recipe Maker 1.0.1 -English-

Independent **Minecraft 26.1.2 NeoForge** edition, version **1.0.1**, using **Java 25**. Other Minecraft/loader editions are maintained on separate branches.

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

Version **1.0.1** includes the updated editor and gold binding tooltips. The player inventory label no longer includes the parenthetical explanation. Loader metadata contains the exact mod version `1.0.1`; artifact filenames may also identify Minecraft and the loader. See [CHANGELOG.md](CHANGELOG.md) for history.

# Visual JEI Recipe Maker 1.0.1 -中文-

独立的 **NeoForge 26.1.2** 移植工程，使用 **Java 25**。其他版本分别位于对应分支；1.21.1 NeoForge 位于 `main`。

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
