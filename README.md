# Visual JEI Recipe Maker 1.0.4 -English-

## New in 1.0.4

- Preserve enchantments and custom item data when dragging ingredients, adding empty crafting slots, previewing, and exporting recipes.
- Minecraft 1.20.1 uses native strict whole-NBT ingredients. Minecraft 1.21.1/26.1.2 matches each recorded component value exactly and allows additional component types; custom data is compared as a complete value.
- Quantity-only edits preserve the original predicate. Replacing an item or switching to a tag rebuilds its conditions.
- NeoForge 1.21.1 rejects ingredients with explicitly removed components and retains the previous recipe because its native format cannot express absence. Other component-era editions preserve removals. Exported recipes use the corresponding loader/API's native formats.

See [CHANGELOG.md](CHANGELOG.md) for the full history and [COMPATIBILITY.md](COMPATIBILITY.md)
for platform-specific integrations.

This branch targets **Minecraft 1.21.1 / Fabric**, version **1.0.4**.

## Installation

| Minecraft | Game Java | Fabric Loader | Fabric API | Recipe browser |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17 | 0.19.5 | 0.92.12+1.20.1 | JEI 15.74.0.225 or EMI 1.1.24+1.20.1 |
| 1.21.1 | 21 | 0.19.5 | 0.116.6+1.21.1 | JEI 19.27.0.350 or EMI 1.1.24+1.21.1 |
| 26.1.2 | 25 | 0.19.5 | 0.155.3+26.1.2 | JEI 29.47.0.111 |

Install the VJRM JAR for this branch's Minecraft version alongside the matching Fabric API and browser. Fabric 1.20.1/1.21.1 support JEI alone, EMI alone, or both; when both are present, the editor uses EMI and JEMI for JEI categories. Fabric 26.1.2 integrates with JEI only.

For multiplayer, install the same Minecraft/loader edition of VJRM and Fabric API on the server to read active recipe JSON, list source templates, and export to world datapacks. The server does not need JEI/EMI. On 26.1.2, Fabric recipe synchronization handles changed vanilla recipes; third-party categories use their own synchronization and browser integration.

## Usage

Exported component ingredients require Fabric API on the server.

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

Vanilla 26.1.2 ingredients use compact strings and `#tags`. Manual fields and slot bindings save automatically to `config/vjrm/field_profiles.json` in the
client instance and restore across restarts, worlds, and servers. Fields match the recipe type;
bindings also match the viewer, category, and ordered slot layout. Missing paths or incompatible
layouts are skipped without erasing saved settings. **Reset Type** in **JSON Fields** clears the
current type's saved metadata. Removing/resetting definitions leaves recipe JSON unchanged;
these settings are not included in exports. Explicitly enable overwriting when replacing an existing recipe.

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

Version **1.0.4** preserves ingredient NBT/components and quantity-only edits. See [CHANGELOG.md](CHANGELOG.md) for history.


# Visual JEI Recipe Maker 1.0.4 -中文-

## 1.0.4 更新

- 修复拖入带附魔、自定义数据的原料后数据丢失，覆盖空合成槽添加、预览和配方导出。
- Minecraft 1.20.1 使用原生严格 NBT 材料，整份 NBT 必须相同。1.21.1／26.1.2 要求记录的各组件值相同，允许额外组件类型；自定义数据按完整值比较。
- 仅修改数量保留原条件，替换物品或改用标签时重建条件。
- NeoForge 1.21.1 的原生格式不能表达组件缺失，遇到显式删除组件的原料时拒绝修改并保留原配方；其他组件版本保留删除条件。导出配方使用对应加载器／API 的原生材料格式。

完整历史见 [CHANGELOG.md](CHANGELOG.md)，平台限定的兼容功能见 [COMPATIBILITY.md](COMPATIBILITY.md)。

当前分支适用于 **Minecraft 1.21.1 / Fabric 1.0.4**。

## 安装组合

| Minecraft | 游戏 Java | Fabric Loader | Fabric API | 配方浏览器 |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17 | 0.19.5 | 0.92.12+1.20.1 | JEI 15.74.0.225 或 EMI 1.1.24+1.20.1 |
| 1.21.1 | 21 | 0.19.5 | 0.116.6+1.21.1 | JEI 19.27.0.350 或 EMI 1.1.24+1.21.1 |
| 26.1.2 | 25 | 0.19.5 | 0.155.3+26.1.2 | JEI 29.47.0.111 |

将对应游戏版本、加载器的 VJRM JAR 放入 `mods/`，同时安装 Fabric API 和表中的浏览器。1.20.1、1.21.1 可以单独安装 JEI、单独安装 EMI 或共存；共存时编辑器使用 EMI，并可通过 JEMI 保留 JEI 类别的绘制。26.1.2 不接入 EMI。

客户端需要浏览器。多人服务器安装相同游戏版本、加载器的 VJRM 和 Fabric API，用于读取当前实际生效的配方 JSON、列出来源模板并向世界数据包导出；VJRM 的服务端功能无需 JEI/EMI。26.1.2 为原版配方启用 Fabric 的配方同步，浏览器可以收到服务器原版配方的改动；第三方类别继续使用其自身的同步和浏览器集成。

## 使用

Fabric 1.21.1 将带附魔、名称或其他已修改组件的物品拖入原料槽时，会通过 Fabric API 原生组件材料保留这些条件。已记录组件的值须完全相同，允许额外的组件类型；仅改数量会保留条件，重新拖入物品或切换标签会重建条件。导出的组件配方需要服务器安装 Fabric API。

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

26.1.2 模板和材料编辑使用新版的字符串及 `#标签` 格式。手动字段和槽位绑定会自动保存到客户端实例的 `config/vjrm/field_profiles.json`，重启后、跨存档和服务器均可复用。
字段按配方类型恢复；槽位绑定还会核对 JEI／EMI、类别及槽位布局。当前配方缺少的路径或不匹配的布局会跳过，保留配置供其他配方使用。
在「JSON字段」中点击「重置此类型」可清除此类型保存的字段和绑定。移除或重置定义不会修改配方 JSON，也不会将这些配置写入导出配方。

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
