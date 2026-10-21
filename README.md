# Visual JEI Recipe Maker 1.0.1 -English-

Minecraft 1.21.1, NeoForge 21.1.65+, Java 21. Mod ID: `vjrm`, package: `com.pureneko.vjrm`. On the client, install either JEI 19.27+ or EMI 1.1.24+; when both are installed, the editor uses EMI. The basic validation combination for this version: NeoForge 21.1.65 + JEI 19.27.0.350 / EMI 1.1.24+1.21.1.

## Usage

1. After entering a world, press **F8** to open the editor. You can change the key in the game controls settings.
2. Click **Recipe Type** and select a template from the searchable, scrollable list. The list includes vanilla templates and recognizable third-party datapack recipe types; third-party types use existing recipes as templates and preserve their original fields.
3. The **Copy to VJRM Editor** book button on EMI recipe pages is **shown by default**. The JEI button follows the existing configuration and is hidden by default on first install. Bind **Show/Hide JEI/EMI Recipe Copy Button** under the Visual JEI Recipe Maker category in controls settings (unbound by default) to toggle visibility. The toggle is saved in `config/vjrm-client.toml` and persists after restart; EMI uses `showEmiCopyButton`, JEI uses `showJeiCopyButton`.
4. Drag items or fluids from the JEI/EMI sidebar into bound recipe slots. The player's main inventory and hotbar are shown below; **you can only fill slots by dragging; clicking an inventory item does not fill the recipe, and no items are consumed**.
5. Drag-replacing preserves the slot's original count; merging ingredient slots replaces the entire duplicate ingredient group. After clicking a recipe slot, you can enter an item/fluid ID and count, then click **Apply Changes**. Entering tags such as `#minecraft:logs` can modify input tags.
6. Right-click to clear and remove crafting inputs; empty crafting slots can be filled again by dragging. Required fields and the last input are protected by serializer validation. Press Esc to return.
7. Slots that cannot automatically map to JSON fields show a yellow border. First click the slot, select the JSON field, then click **Bind Slot**. If the field is missing, use **JSON Fields** to add one from the current JSON. Fuel, catalyst, and display-only slots are not exportable ingredients.
8. Click **Values** at the top to search and modify existing numeric fields, including runtime, mana, energy, experience, probability, count, and nested parameters. Big integers, decimals, and scientific notation are supported; common numeric strings such as energy preserve the string type. The entire draft is validated together; Cancel discards changes.
9. **Copy to Clipboard** in **Edit JSON** copies the current draft as-is. The editor provides key, string, number, boolean/null, and error highlighting; line numbers, line/column position, matching bracket highlighting, bracket/quote completion, auto-indent on Enter, Tab indentation, Ctrl+Z/Y undo/redo, formatting, and live syntax error indications. Ctrl+Shift+F formats, Ctrl+S applies; Ctrl+A/C/V/X are supported. Before applying, the actual serializer validation is called.
10. Enter a new recipe ID, for example `vjrm:my_recipe`, and choose client-side or server-side export. Overwriting is refused by default and can be explicitly enabled.

The UI scales automatically, is centered horizontally and vertically across the whole screen, preserves the player inventory, and shows sidebars on both sides of the editor. The JEI sidebar itself must be enabled; toggle it with Ctrl+O.

## Source Information and Slot Tooltips

Recipe sources display the ID and category name; proxy recipes display the real source and display category. Hover over the source line to view complete information. Recipe type search also supports names and IDs.

Hover over a recipe slot to display the ingredient name, registry ID, mod, and the browser's original text description. Bound slots append the JSON field path in gold; unbound display slots can also show ingredient identity. Field selection and bind buttons are located above the recipe diagram, and the recipe diagram is centered in the display area.

## Recipe Browser and Chemical Support

EMI uses complete original recipe components, preserving backgrounds, animations, text, arrows, and fluid slots. JEMI-converted recipes use their original components. Rebuildable recipes are rebuilt from the current JSON; non-rebuildable third-party wrapper layouts only update slots, while other dynamic text still comes from the original recipe, and the UI displays a notice.

EMI 1.1.24's JEMI requires the JEI API and runtime. With only EMI installed, vanilla and mod-native EMI UIs can be displayed; mod UIs that only have JEI adaptation require JEI to be installed as well. The copy button requires no changes to EMI development settings. See the [EMI official implementation](https://github.com/emilyploszaj/emi).

Optional Mekanism adaptation supports gases, infusions, pigments, slurries, and the unified chemical representation in 1.21, including `chemicalInput`, `gasInput`, `input_chemical` and other paths, tags, and alternative ingredients. You can bind, drag, modify IDs/tags, and edit long counts; when Mekanism is absent, its API is not loaded. The tested version for 1.20.1 is 10.4.16.80, covering 730 chemical fields. See [Mekanism 1.20.x source](https://github.com/mekanism/Mekanism/tree/1.20.x).

The loader minimum applies to VJRM; other mods must still be installed according to their own requirements. JEI 19.64.0.455 requires NeoForge 21.1.238+.

## Original Recipe Reading and Third-Party Support

- Following the resource-reading approach of JEI Swift Copy, recipes are read by recipe ID from the **server-side currently active datapack JSON**, bypassing re-encoding of client-synchronized recipes and resolving the shaped crafting `Cannot encode unpacked recipe` issue.
- Supports singleplayer and multiplayer games where **the server has VJRM installed**. Reading recipes does not require OP; server-side write exports still check permissions. When a multiplayer server does not have VJRM installed, a clear notice is shown; vanilla templates and client-side export remain available.
- Preserves the original JSON conditions and extra fields; when modifying, only the selected ingredient, output, or count is updated. Recipes without original JSON or that are not loaded are skipped. Valid JSON can still be opened and edited through manual field definitions even when automatic field detection finds no fields. Program-generated recipes without datapack resources are not exported as datapack recipes.
- The type list is built from server-available datapacks and JEI categories, supporting third-party `Recipe`, `RecipeHolder`, and JEI wrapper objects that can provide real recipe IDs.
- Standard categories rebuild the JEI page from the edited recipe. Wrapper categories preserve an independent source layout and override item/fluid displays in bound slots; their dynamic text, time charts, and slot structure still come from the original display object, while actual parameters follow the JSON, and the UI displays a notice.
- Supports common items, item tags, item stacks, fluids, and NeoForge nested ingredient structures with counts. Ambiguous field matching requires manual binding, and unknown structures can be edited via JSON.
- Automatic mapping is not guaranteed for every mod's private format, arbitrary wrapper objects, or custom chemicals. Dedicated formats in actual modpacks may still require additional adaptation.

## Export

- Client: `<game directory>/vjrm_exports/vjrm_generated/data/<namespace>/recipe/<path>.json`.
- Server: `<world directory>/datapacks/vjrm_generated/data/<namespace>/recipe/<path>.json`.
- Both export methods create a complete datapack and `pack.mcmeta` (Minecraft 1.21.1 datapack version 48).
- After exporting, manually run `/reload`; client-exported directories must first be copied to the world's `datapacks`.
- Server export permission is controlled by `exportPermissionLevel` in `serverconfig/vjrm-server.toml`, defaulting to OP level 2 and configurable from 0–4.
- Original JSON reading is limited to 240000 characters; server export is limited to 24000 characters. If exceeded, use client-side export.

## Build and Verification

```powershell
.\gradlew.bat build
.\gradlew.bat verifyRecipes
.\gradlew.bat runClient
# Development runs for EMI only / both JEI and EMI installed
.\gradlew.bat -Pviewer=emi runClient
.\gradlew.bat -Pviewer=both runClient
```

`verifyRecipes` starts an isolated GameTest server and verifies vanilla serializers, unpacked recipes caused by client synchronization, original resource reading, custom serializers, items/fluids and tags with counts, metadata preservation, network payloads, skipping recipes without resources, and export permissions.

Optional `verifyClient` launches an isolated client using the development test world in `run/client-check/saves/Vjrm UI Verification`. You must first create that test save (you can copy `run/client/saves/New World`). The test uses screen input methods to check inventory dragging at two sizes, the JEI sidebar, button visibility and saving, third-party wrapper category catalogs and slot updates, and outputs check results and actual rendered screenshots to `build/verification/client`.

Test serializers, JEI wrapper categories, and test resources are used only for development runs and are not included in release JARs.

When Gradle cannot access the NeoForge configuration endpoint, you can use the existing `build/neoforge-moddev-config.json` and append `--init-script tools/local-launch-config.gradle`. When the dependency cache is complete, you can append `--offline`.

Create regression verification (development runs only; first place the corresponding release JAR at the specified path):

```powershell
# build/verification/deps/create-1.21.1-6.0.10.jar
.\gradlew.bat -PverifyCreate verifyRecipes verifyClient
```


## Manual JSON Fields

1. Select a recipe slot, then click **JSON Fields** above the recipe diagram. You can also add a field first and bind a slot later.
2. Search field names or JSON content and select a node, or enter a JSON Pointer such as `/required_lumen/0`. Array indices start at zero; escape `/` as `~1` and `~` as `~0` in object keys.
3. Choose the field type and input/output role, then click **Add / Update**. Item, fluid, and optional chemical fields support the existing dragging, ID/tag, and count operations. For private wrapper formats, select the inner ingredient or ID node to preserve the surrounding structure.
4. Choose **Raw JSON** for other structures. Enter valid JSON in the value box and apply; quote strings, and edit objects or arrays as a whole. For example, keep a Lumen node's `lumen` value while changing `amount`. Changes use the actual recipe serializer and roll back on failure. You can also edit complex structures in **Edit JSON**.
5. Removing a manual field removes its definition, not recipe data. Valid definitions survive refreshes, resizing, and JSON edits of the same recipe type; changing the template or type resets them. Definitions are not included in exported recipes.

Astral Sorcery recipes registered as raw Recipe objects can be mapped back to their real IDs. JEI and EMI/JEMI share this source resolution. Viewing Astral Sorcery pages that only integrate with JEI still requires JEI alongside EMI. The supplied source's Astral Sorcery 2.0.2.39 checks used NeoForge 21.1.252, JEI 19.64.0.455, ObserverLib 1.10.3.31, and Curios 9.5.1; these dependencies do not change VJRM's own minimum loader declaration.

## Create Compatibility

- Create processing, mechanical crafting, and sequenced assembly can be read from original datapacks, edited, and exported. `isSpecial()` means hidden from the vanilla recipe book and is not used to reject datapack recipes.
- Supports item/fluid inputs, fluid tags, chance outputs, and merged duplicate ingredients in mixing/packing slots. Drag replacement preserves multiplicity; applying a count changes the number of ingredients in the group.
- Supports existing mechanical crafting grid slots, including grids larger than 3×3, plus the starting ingredient, main result, and visible extra processing ingredients of sequenced assembly. Use JSON for shapes, empty mechanical crafting slots, new steps, transitional items, or additional result pools. Existing loops, probabilities, weights, and times are editable in **Values**.
- Display categories such as `sawing`, `packing`, `spout_filling`, and `draining` are not serializer IDs; the original JSON `type` is retained.
- Automatic shapeless and packing pages are proxy recipes and can originate from vanilla crafting recipes. The editor displays the true source and exports its original behavior. Proxy stonecutting pages that combine several results edit only the recipe matching the selected ID.
- The supplied source used Create 6.0.11 for reference and Create 6.0.10 + JEI 19.64.0.455 + NeoForge 21.1.251 for runtime checks. Create remains optional and is not bundled. Recipes without datapack resources are skipped.

Additional development checks in the updated source cover centering, right-click rejection and rollback, numeric edits, JSON completion/formatting, real EMI sidebar dragging, and recipe-page copy buttons with decorators disabled. Results and screenshots are written to `build/verification/client`.

For Astral Sorcery development checks, prepare the dependencies and run:

```powershell
.\tools\prepare-astral-tests.ps1
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' verifyClient
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' -Pviewer=both verifyClient
```

These opt-in checks cover actual altar pages, the EMI copy button, and manual Lumen field selection/editing. Third-party mods are used only for development and are not bundled.

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

Minecraft 1.21.1，NeoForge 21.1.65+，Java 21。Mod ID：`vjrm`，包：`com.pureneko.vjrm`。客户端安装 JEI 19.27+ 或 EMI 1.1.24+ 任意一个即可；两者都装时编辑器使用 EMI。本版基础验证组合：NeoForge 21.1.65 + JEI 19.27.0.350 / EMI 1.1.24+1.21.1。

## 使用

1. 进入世界后按 **F8** 打开编辑器。可在游戏控制设置中修改按键。
2. 点击「配方类型」，在可搜索、可滚动的列表中选择模板。列表包含原版模板和可识别的第三方数据包配方类型；第三方类型以已有配方为模板，保留原始字段。
3. EMI 配方页的「复制到 VJRM 编辑器」书本按钮**默认显示**，JEI 按钮沿用原有配置、首次安装默认隐藏。在控制设置的 Visual JEI Recipe Maker 分类绑定「显示/隐藏 JEI/EMI 配方复制按钮」（默认未绑定），按键切换显示或隐藏。开关保存在 `config/vjrm-client.toml`，重启后保留；EMI 使用 `showEmiCopyButton`，JEI 使用 `showJeiCopyButton`。
4. 从 JEI/EMI 侧栏拖动物品或流体到有绑定的配方槽位。下方显示玩家主物品栏和快捷栏；**只能通过拖拽填入，单击物品栏不会填入配方，操作不消耗物品**。
5. 拖拽替换保留该槽原有数量；合并原料槽会替换整组重复原料。点击配方槽位后，可输入物品/流体 ID 和数量，再按「应用修改」。输入 `#minecraft:logs` 等标签可修改输入标签。
6. 右键清除可移除的工作台输入；空工作台槽位可重新拖入原料。必需字段和最后一个输入受到 serializer 校验保护。按 Esc 返回。
7. 无法自动对应 JSON 字段的槽位会显示黄色边框。先点击槽位，选择字段，再按「绑定槽位」。没有对应选项时，点击「JSON字段」从当前配方 JSON 添加字段；先选槽位再添加可直接绑定。燃料、催化剂和纯展示槽本身不属于可导出的原料。
8. 点击顶部「数值」，搜索并修改已有数值字段，包括运行时间、魔力、能量、经验、概率、数量和嵌套参数。支持大整数、小数、科学计数法；常见能量等数字字符串保留字符串类型。整个草稿一起校验，取消则丢弃修改。
9. 「编辑 JSON」内的「复制到剪贴板」原样复制当前草稿。编辑器提供键名、字符串、数值、布尔值/null 和错误高亮，行号、行列位置、配对括号高亮、括号/引号补全、回车自动缩进、Tab 缩进、Ctrl+Z/Y 撤销重做、格式化及实时语法错误提示。Ctrl+Shift+F 格式化，Ctrl+S 应用；支持 Ctrl+A/C/V/X。应用前调用实际 serializer 校验。
10. 输入新配方 ID，例如 `vjrm:my_recipe`，选择客户端或服务端导出。默认拒绝覆盖，可显式打开覆盖。

界面会自动缩放，按整个屏幕水平、垂直居中，保留玩家物品栏，侧栏在编辑器两侧显示。JEI 侧栏自身需要启用，可用 Ctrl+O 切换。

更新记录统一在 [CHANGELOG.md](CHANGELOG.md)，包含各 Minecraft 版本的历史更新。

## 来源信息与槽位提示

配方来源显示 ID 和类别名称；代理配方显示真实来源及展示类别。悬停来源行可查看完整信息。配方类型检索也支持名称和 ID。

将鼠标移到配方槽位上，会显示原料名称、注册 ID、所属模组及浏览器原有文字说明。已绑定槽位追加 JSON 字段路径；没有绑定的展示槽也可查看原料身份。字段选择与绑定按钮位于配方图上方，配方图在展示区域内居中。

## 手动添加 JSON 字段

1. 点击要编辑的槽位，再点击配方图上方的「JSON字段」。也可以直接打开该入口，将字段加入可选列表后再绑定。
2. 搜索字段名或 JSON 内容并选择节点，或输入路径，例如 `/required_lumen/0`。数组下标从 0 开始；键名中的 `/` 写作 `~1`，`~` 写作 `~0`。
3. 选择字段类型和输入/输出用途，点击「添加／更新」。物品、流体和可选化学品类型可使用原有拖拽、ID/标签及数量操作。私有包装格式应选择内部原料或 ID 节点，保留外层结构。
4. 其他格式选择「原始 JSON」。回到编辑器后，在值栏输入有效 JSON，再按「应用修改」。字符串需要双引号；对象和数组可整体修改。例如 Lumen 节点可保留 `lumen`，只修改 `amount`。操作调用实际配方 serializer 校验，失败回滚。复杂结构也可通过「编辑 JSON」修改。
5. 「移除手动字段」只删除该草稿的手动定义，不删除配方数据。刷新、分辨率变化和同类型 JSON 编辑保留有效定义；换模板或更改配方类型后重置，导出不附加 VJRM 元数据。

Astral Sorcery 的原始 Recipe 展示对象现在可反查真实配方 ID，JEI 和 EMI/JEMI 共用该来源处理。使用 EMI 查看只有 JEI 适配的 Astral Sorcery 页面仍需同时安装 JEI。Astral Sorcery 2.0.2.39 的运行检查使用 NeoForge 21.1.252、JEI 19.64.0.455、ObserverLib 1.10.3.31 和 Curios 9.5.1；这些模组的依赖要求不改变 VJRM 自身的最低加载器声明。

## 配方浏览器与化学品支持

EMI 使用原配方完整组件，保留背景、动画、文字、箭头和流体槽。JEMI 转换配方使用其原组件。可以重建的配方按当前 JSON 重建；不能重建的第三方包装布局只更新槽位，其他动态文字仍来自原配方，界面会显示提示。

EMI 1.1.24 的 JEMI 需要 JEI API 与运行时。只装 EMI 可显示原版及模组原生 EMI UI；只有 JEI 适配的模组界面需要同时安装 JEI。复制按钮无需修改 EMI 开发设置。参考 [EMI 官方实现](https://github.com/emilyploszaj/emi)。

Mekanism 可选适配支持气体、灌注、颜料、浆液及 1.21 的统一化学品表示，包括 `chemicalInput`、`gasInput`、`input_chemical` 等路径、标签和替代原料。可绑定、拖拽、修改 ID/标签及 long 数量；没有 Mekanism 时不加载其 API。1.20.1 实测版本为 10.4.16.80，覆盖 730 个化学品字段。参考 [Mekanism 1.20.x 源码](https://github.com/mekanism/Mekanism/tree/1.20.x)。

加载器下限针对 VJRM；其他模组仍按其自身要求安装。JEI 19.64.0.455 需要 NeoForge 21.1.238+；旧加载器可搭配本版验证的 JEI 19.27.0.350。Forge 移植工程位于 [forge-1.20.1 分支](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/forge-1.20.1)。

## 原始配方读取与第三方支持

- 参考 JEI Swift Copy 的资源读取思路，按配方 ID 读取**服务端当前生效的数据包 JSON**，绕过客户端同步配方的重新编码，解决有序合成的 `Cannot encode unpacked recipe`。
- 支持单人游戏，以及**服务端安装 VJRM** 的多人游戏。读取配方不要求 OP；服务端写入导出仍检查权限。多人服未安装 VJRM 时会明确提示，原版模板与客户端导出仍可用。
- 保留原 JSON 的条件和额外字段，修改时只更新选中的原料、产物或数量。没有原始 JSON 或未加载的配方会跳过；已有有效 JSON 即使没有自动识别字段，也能打开后手动添加，不会把程序生成配方当作数据包配方导出。
- 类型列表结合服务端可用数据包和 JEI 类别建立，支持第三方 `Recipe`、`RecipeHolder` 以及能够提供真实配方 ID 的 JEI 包装对象。
- 标准类别通过编辑后的配方重建 JEI 页面。包装类别保留独立的来源布局，覆盖已绑定槽位中的物品/流体显示；其动态文字、时间图表及槽位结构仍来自原始展示对象，实际参数以 JSON 为准，界面会显示提示。
- 支持常见的物品、物品标签、物品堆、流体以及 NeoForge 带数量的嵌套原料结构。字段匹配有歧义时要求手动绑定，未知结构可使用 JSON 编辑。
- 不保证每个模组的私有格式、任意包装对象或自定义化学品都能自动映射。实际整合包中的专用格式仍可能需要额外适配。

## Create 兼容

- 修正将 `isSpecial()`（不显示在原版配方书）错误当作“不是数据包配方”的判断。Create 原生加工、机械合成和序列组装现在可从原始数据包读取、编辑并导出。
- 支持 Create 物品/流体输入、流体标签切换、概率产物、搅拌/压块中重复原料合并槽；拖拽替换保留重复次数，「应用修改」的数量可调整该组原料个数。
- 支持机械合成已有网格槽位（包括大于 3×3 的网格），以及序列组装的起始原料、主要产物和可见工序附加原料槽。形状、空机械合成槽、新工序、过渡物品及额外结果池通过 JSON 编辑；已有的循环次数、概率、权重和耗时等数值可在「数值」中修改。
- JEI `sawing`、`packing`、`spout_filling`、`draining` 等展示类别不等于 serializer ID；编辑器始终保留 JSON 中实际的 `type`。
- Create 的自动无序合成、自动压块等是**代理配方**：可能真实来源于 `minecraft:crafting_shapeless`、`minecraft:crafting_shaped`。界面与类型列表标明「代理配方」，显示真实来源类型；编辑时使用来源配方的标准 JEI 页面，导出也保留来源行为。合并多个切石结果的代理页面只编辑其配方 ID 对应的单份原始 JSON。
- 用于分析的源码版本为 6.0.11；实际运行验证为公开发行的 **Create 6.0.10 + JEI 19.64.0.455 + NeoForge 21.1.251**。Create 不是 VJRM 的强制依赖，不随发布 JAR 打包。没有数据包资源的程序生成配方仍跳过。

## 导出

- 客户端：`<游戏目录>/vjrm_exports/vjrm_generated/data/<namespace>/recipe/<path>.json`。
- 服务端：`<世界目录>/datapacks/vjrm_generated/data/<namespace>/recipe/<path>.json`。
- 两种导出都会创建完整数据包和 `pack.mcmeta`（Minecraft 1.21.1 数据包版本 48）。
- 导出后请手动执行 `/reload`；客户端导出的目录需要先复制到世界的 `datapacks`。
- 服务端导出权限由 `serverconfig/vjrm-server.toml` 的 `exportPermissionLevel` 控制，默认 OP 等级 2，可配置 0–4。
- 原始 JSON 读取限制为 240000 字符；服务端导出限制为 24000 字符，超过时可使用客户端导出。

## 构建与验证

```powershell
.\gradlew.bat build
.\gradlew.bat verifyRecipes
.\gradlew.bat runClient
# 仅 EMI / JEI 与 EMI 同时安装的开发运行
.\gradlew.bat -Pviewer=emi runClient
.\gradlew.bat -Pviewer=both runClient
```

`verifyRecipes` 启动隔离的 GameTest 服务端，验证原版 serializer、客户端同步导致的 unpacked 配方、原始资源读取、自定义 serializer、带数量的物品/流体及标签、元数据保留、网络载荷、无资源配方跳过和导出权限。

可选 `verifyClient` 使用 `run/client-check/saves/Vjrm UI Verification` 中的开发测试世界启动隔离客户端。需要先创建该测试存档（可复制 `run/client/saves/New World`）。测试通过屏幕输入方法检查两种尺寸的物品栏拖拽、JEI 侧栏、按钮显隐及保存、第三方包装类别目录与槽位更新，并在 `build/verification/client` 输出检查结果和实际渲染截图。

测试 serializer、JEI 包装类别和测试资源仅用于开发运行，均不进入发布 JAR。

当 Gradle 无法访问 NeoForge 配置端点时，可使用已有 `build/neoforge-moddev-config.json`，附加 `--init-script tools/local-launch-config.gradle`。依赖缓存齐全时可以附加 `--offline`。

Create 回归验证（仅开发运行，先将对应发行 JAR 放在指定路径）：

```powershell
# build/verification/deps/create-1.21.1-6.0.10.jar
.\gradlew.bat -PverifyCreate verifyRecipes verifyClient
```

真实 Create 检查覆盖 16 个配方样本的原始资源读取、逐字段替换、导出再解码、流体标签切换、重复原料、序列权重和服务端网络导出；客户端检查覆盖原生 JEI 槽位、类别别名、大机械网格、序列工序和三类代理配方。截图输出到 `build/verification/client/create-*.png`。

`verifyClient` 额外检查居中、右键失败原因和回滚、数值界面应用/拒绝、JSON 补全和格式化。EMI 用例使用 `-Pviewer=emi verifyClient`，共存用例使用 `-Pviewer=both verifyClient`；EMI 检查会从真实侧栏执行鼠标拖拽。截图及结果在 `build/verification/client`。

EMI 验证还会关闭 `showRecipeDecorators`，从真实配方页点击复制按钮，检查显隐、原始 JSON、来源名称和槽位提示；截图为 `emi-copy-page.png` 与 `emi-copy-editor.png`。

Astral Sorcery 额外客户端检查（仅用于开发验证，不打包第三方模组）：

```powershell
.\tools\prepare-astral-tests.ps1
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' verifyClient
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' -Pviewer=both verifyClient
```

截图包括真实祭坛、EMI 复制按钮及手动 Lumen 节点选择与修改。。

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
