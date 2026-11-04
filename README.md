# Visual JEI Recipe Maker 1.0.5 -English-

## New in 1.0.5

- Manage local/world packs, disable original JSON recipes, and double-click recipe entries to edit saved JSON.
- Use pack-name completion, rename/move/restore dialogs, ZIP exports and configurable backup retention.

- Preserve enchantments and custom item data when dragging ingredients, adding empty crafting slots, previewing, and exporting recipes.
- Minecraft 1.20.1 uses native strict whole-NBT ingredients. Minecraft 1.21.1/26.1.2 matches each recorded component value exactly and allows additional component types; custom data is compared as a complete value.
- Quantity-only edits preserve the original predicate. Replacing an item or switching to a tag rebuilds its conditions.
- NeoForge 1.21.1 rejects ingredients with explicitly removed components and retains the previous recipe because its native format cannot express absence. Other component-era editions preserve removals. Exported recipes use the corresponding loader/API's native formats.

See [CHANGELOG.md](CHANGELOG.md) for the full history and [COMPATIBILITY.md](COMPATIBILITY.md)
for platform-specific integrations.

This branch targets **Minecraft 26.1.2 / NeoForge**, version **1.0.5**.

## Thaumaturge

Available on **NeoForge 26.1.2** with JEI 29.x. Install Thaumaturge and its dependencies separately. Each `{ "aspect": "thaumaturge:ignis", "amount": 10 }` in `aspects` becomes an independent aspect field resolved through public codecs and the active datapack registry, including additional aspects. Drag JEI aspects, edit IDs and positive counts, or choose **Aspect** in manual fields. Thaumaturge still renders native icons, names, and research rules; tooltips append IDs, mod names, and gold bindings.

`components` arrays map to independent input slots. Duplicate materials bind by array position; alternatives within one entry remain one slot. Data component ingredient conditions are retained as whole ingredients, preventing inner aspect components from becoming extra slots. Replacements update the selected ingredient condition while retaining other recipe data. JSON editing/highlighting/copying and datapack export remain available. See [COMPATIBILITY.md](COMPATIBILITY.md).

Independent **Minecraft 26.1.2 NeoForge** edition, version **1.0.5**, using **Java 25**. Other Minecraft/loader editions are maintained on separate branches.

Manual fields and slot bindings save automatically to `config/vjrm/field_profiles.json` in the
client instance and restore across restarts, worlds, and servers. Fields match the recipe type;
bindings also match the viewer, category, and ordered slot layout. Missing paths or incompatible
layouts are skipped without erasing saved settings. **Reset Type** in **JSON Fields** clears the
current type's saved metadata. Removing/resetting definitions leaves recipe JSON unchanged;
these settings are not included in exports.

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

Version **1.0.5** preserves ingredient NBT/components and quantity-only edits. See [CHANGELOG.md](CHANGELOG.md) for history.

# Visual JEI Recipe Maker 1.0.5 -中文-

## 1.0.5 更新

- 同步本地／世界数据包管理、原配方禁用与双击配方编辑。
- 支持数据包名称补全、重命名／移动／恢复弹窗、ZIP 导出和备份保留配置。

- 修复拖入带附魔、自定义数据的原料后数据丢失，覆盖空合成槽添加、预览和配方导出。
- Minecraft 1.20.1 使用原生严格 NBT 材料，整份 NBT 必须相同。1.21.1／26.1.2 要求记录的各组件值相同，允许额外组件类型；自定义数据按完整值比较。
- 仅修改数量保留原条件，替换物品或改用标签时重建条件。
- NeoForge 1.21.1 的原生格式不能表达组件缺失，遇到显式删除组件的原料时拒绝修改并保留原配方；其他组件版本保留删除条件。导出配方使用对应加载器／API 的原生材料格式。

完整历史见 [CHANGELOG.md](CHANGELOG.md)，平台限定的兼容功能见 [COMPATIBILITY.md](COMPATIBILITY.md)。

当前分支适用于 **Minecraft 26.1.2 / NeoForge 1.0.5**。

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

未识别的字段可通过「JSON字段」搜索节点或填写 JSON Pointer（例如 `/custom/input/0`）添加；指定输入/输出及物品、流体或原始 JSON 类型。先选择槽位再添加，会直接绑定。手动字段和槽位绑定会自动保存到客户端实例的 `config/vjrm/field_profiles.json`，重启后、跨存档和服务器均可复用。
字段按配方类型恢复；槽位绑定还会核对 JEI／EMI、类别及槽位布局。当前配方缺少的路径或不匹配的布局会跳过，保留配置供其他配方使用。
在「JSON字段」中点击「重置此类型」可清除此类型保存的字段和绑定。移除或重置定义不会修改配方 JSON，也不会将这些配置写入导出配方。第三方私有结构可选内部 ID 节点或使用原始 JSON 编辑；修改仍由该配方实际 serializer 校验。

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

1.0.5：修复原料 NBT／组件丢失，仅修改数量时保留原条件。完整更新记录见 [CHANGELOG.md](CHANGELOG.md)。

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

## Managing recipes and datapacks

Choose **Manage packs** in the editor to switch between **Current world** and **Local exports**. Click a pack to select it for rename, deletion or export; double-click its name to open recipe entries. Double-click a recipe entry to edit the saved JSON, retaining its ID and pack; disabled entries must be restored first. Changing the ID saves a new recipe and keeps the original. The manager no longer creates packs or sets the export target. Exporting from the editor creates a named pack when needed. Rename, move and restore use dedicated name dialogs that preserve input on validation errors; moving to a missing pack confirms its creation. Back and Esc return from entries/backups to the pack list, then to the editor. Enter the target name beside the editor's overwrite toggle. It initially contains `vjrm_generated`; an empty field shows a name hint and must be filled before export/disable. Prefix suggestions merge local and current-world packs with source labels; use the mouse, arrow keys, Tab to accept and Esc to dismiss. Existing `vjrm_generated` packs remain supported.

JEI/EMI copies retain their original recipe IDs. The editor's **Disable recipe** button opens confirmation directly, showing the source recipe ID and entered target pack, without searching a manager catalog. Cancel or completion returns to the editor. Disable rules are written to that pack in the current world. Removing an override or disable entry restores the upstream recipe after `/reload`; upstream files stay intact and original JSON is not backed up. Cross-pack ID conflicts still require an explicit choice to replace the existing entry or move it into the target pack.

World management is server-authoritative and uses `exportPermissionLevel` (OP2 by default). Players must run `/reload` themselves after changes. VJRM attempts to select its packs at higher priority and reports if this cannot be ensured; check `/datapack list` when prompted. NeoForge conditional overrides disable JSON recipes only when above the source pack. Recipes injected later by scripts or mod code are outside this feature; GTCEu KubeJS exports keep their existing workflow.

**Export ZIP** packages the complete pack and downloads it to the client's `vjrm_exports/exports/<name>.zip`, including recipes, disable rules and management metadata. It does not install the ZIP into a world.

Deleting a pack confirms its name and counts before moving it to **Deleted packs**. Restore under an available name; restoration refuses name and recipe-ID conflicts. Permanent backup deletion requires another confirmation. `backupRetentionDays` controls retention separately for local exports and world packs:

- `0` (default): clean on client/world-server shutdown, with startup cleanup after an abnormal exit.
- Positive: retain that many days; clean expired backups at startup and shutdown.
- `-1`: retain until manually purged.

Local settings live in `config/vjrm-client.toml`, with backups in `config/vjrm/backups`. World backups live in `<world>/data/vjrm/backups`; retention uses NeoForge's loaded `vjrm-server.toml` (normally under the world's `serverconfig`; dedicated-server locations follow NeoForge). A remote player disconnect does not clean server backups; the server must stop. Cleanup never removes active packs.

## 配方与数据包管理

编辑器内点击「管理数据包」，在「当前世界」与「本地导出」之间切换。单击选中数据包可重命名、删除、导出；双击包名打开配方列表，编辑、移除或移动配方。双击配方条目可直接编辑保存的 JSON，保留原 ID 和包名；禁用条目需先恢复。修改 ID 导出会另存新配方并保留原条目。管理器不再提供新建包或设置导出目标；新包在编辑器导出时按填写的名称自动创建。重命名、移动和备份恢复均在独立弹窗中输入名称，校验失败保留输入；移动目标不存在时会提示确认创建。返回键与 Esc 从包内或备份页面返回包列表，再返回编辑器。编辑器覆盖开关右侧可填写目标包名，首次预填 `vjrm_generated`，清空后提示「数据包名称」并要求填写。输入时按前缀补全本地和当前世界已有包名，标注来源，支持鼠标、↑↓选择、Tab 接受和 Esc 收起。原有的 `vjrm_generated` 包继续可用。

JEI/EMI 复制的配方保留原始 ID，点击编辑器的「禁用配方」会直接弹出确认页，显示原始配方 ID 与当前填写的目标包，不再进入管理器搜索。取消或完成后返回编辑器。禁用规则写入当前世界的目标包；移除覆盖／禁用条目并执行 `/reload` 后恢复上游配方，不修改上游文件，也不另备份原始 JSON。跨包同 ID 冲突仍需明确选择替换原包条目或移动到目标包。

世界管理沿用 `exportPermissionLevel`（默认 OP2），所有文件操作由服务器校验。保存后仍需玩家自行执行 `/reload`。VJRM 会尝试将管理包选入较高优先级；如界面提示优先级未能保证，请检查 `/datapack list`。禁用通过 NeoForge 的同 ID 条件覆盖生效，要求高于配方来源包；不处理脚本或模组代码在运行时注入的配方。GTCEu KubeJS 脚本继续走原有入口。

「重新导出」会将整个包打包为 ZIP，并下载到客户端的 `vjrm_exports/exports/<包名>.zip`，包含配方、禁用规则和管理标记。该操作不会自动将 ZIP 安装进世界。

删除包需确认包名、配方数与禁用规则数，然后移入「已删除包」。恢复时填写目标包名，不覆盖同名包或制造跨包配方冲突；「永久删除」需再次确认。备份保留由 `backupRetentionDays` 控制：

- `0`（默认）：关闭客户端／对应世界服务器时清理，异常退出遗留备份在下次启动补清理。
- 正数：保留相应天数，在启动与关闭时清理过期备份。
- `-1`：保留到手动永久删除。

本地配置位于 `config/vjrm-client.toml`，本地备份在 `config/vjrm/backups`；世界备份在 `<世界>/data/vjrm/backups`，保留配置沿用 NeoForge 的 `vjrm-server.toml`（通常为世界的 `serverconfig`，专服以实际加载位置为准）。远程玩家退出不会清理服务器备份，独立服务器停止时才处理。备份清理不删除现存数据包。
