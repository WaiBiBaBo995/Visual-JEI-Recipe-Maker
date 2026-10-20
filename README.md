# Visual JEI Recipe Maker 0.5.3 -English-

Minecraft 1.21.1, NeoForge 21.1.65+, Java 21. Mod ID: `vjrm`, package: `com.pureneko.vjrm`. On the client, install either JEI 19.27+ or EMI 1.1.24+; when both are installed, the editor uses EMI. The basic validation combination for this version: NeoForge 21.1.65 + JEI 19.27.0.350 / EMI 1.1.24+1.21.1.

## Usage

1. After entering a world, press **F8** to open the editor. You can change the key in the game controls settings.
2. Click **Recipe Type** and select a template from the searchable, scrollable list. The list includes vanilla templates and recognizable third-party datapack recipe types; third-party types use existing recipes as templates and preserve their original fields.
3. The **Copy to VJRM Editor** book button on EMI recipe pages is **shown by default**. The JEI button follows the existing configuration and is hidden by default on first install. Bind **Show/Hide JEI/EMI Recipe Copy Button** under the Visual JEI Recipe Maker category in controls settings (unbound by default) to toggle visibility. The toggle is saved in `config/vjrm-client.toml` and persists after restart; EMI uses `showEmiCopyButton`, JEI uses `showJeiCopyButton`.
4. Drag items or fluids from the JEI/EMI sidebar into bound recipe slots. The player's main inventory and hotbar are shown below; **you can only fill slots by dragging, clicking an inventory item will not fill the recipe**.
5. Drag-replacing preserves the slot's original count; merging ingredient slots replaces the entire duplicate ingredient group. After clicking a recipe slot, you can enter an item/fluid ID and count, then click **Apply Changes**. Entering tags such as `#minecraft:logs` can modify input tags.
6. Right-click to clear and remove crafting inputs; empty crafting slots can be filled again by dragging. Required fields and the last input are protected by serializer validation. Press Esc to return.
7. Slots that cannot automatically map to JSON fields show a yellow border. First click the slot, select the JSON field, then click **Bind Slot**. Fuel, catalyst, and display-only slots are not exportable ingredients.
8. Click **Values** at the top to search and modify existing numeric fields, including runtime, mana, energy, experience, probability, count, and nested parameters. Big integers, decimals, and scientific notation are supported; common numeric strings such as energy preserve the string type. The entire draft is validated together; Cancel discards changes.
9. **Copy to Clipboard** in **Edit JSON** copies the current draft as-is. The editor provides key, string, number, boolean/null, and error highlighting; line numbers, line/column position, matching bracket highlighting, bracket/quote completion, auto-indent on Enter, Tab indentation, Ctrl+Z/Y undo/redo, formatting, and live syntax error indications. Ctrl+Shift+F formats, Ctrl+S applies; Ctrl+A/C/V/X are supported. Before applying, the actual serializer validation is called.
10. Enter a new recipe ID, for example `vjrm:my_recipe`, and choose client-side or server-side export. Overwriting is refused by default and can be explicitly enabled.

The UI scales automatically, is centered horizontally and vertically across the whole screen, preserves the player inventory, and shows sidebars on both sides of the editor. The JEI sidebar itself must be enabled; toggle it with Ctrl+O.

## Source Information and Slot Tooltips

Recipe sources display the ID and category name; proxy recipes display the real source and display category. Hover over the source line to view complete information. Recipe type search also supports names and IDs.

Hover over a recipe slot to display the ingredient name, registry ID, mod, and the browser's original text description. Bound slots append the JSON field path; unbound display slots can also show ingredient identity. Field selection and bind buttons are located above the recipe diagram, and the recipe diagram is centered in the display area.

## Recipe Browser and Chemical Support

EMI uses complete original recipe components, preserving backgrounds, animations, text, arrows, and fluid slots. JEMI-converted recipes use their original components. Rebuildable recipes are rebuilt from the current JSON; non-rebuildable third-party wrapper layouts only update slots, while other dynamic text still comes from the original recipe, and the UI displays a notice.

EMI 1.1.24's JEMI requires the JEI API and runtime. With only EMI installed, vanilla and mod-native EMI UIs can be displayed; mod UIs that only have JEI adaptation require JEI to be installed as well. The copy button requires no changes to EMI development settings. See the [EMI official implementation](https://github.com/emilyploszaj/emi).

Optional Mekanism adaptation supports gases, infusions, pigments, slurries, and the unified chemical representation in 1.21, including `chemicalInput`, `gasInput`, `input_chemical` and other paths, tags, and alternative ingredients. You can bind, drag, modify IDs/tags, and edit long counts; when Mekanism is absent, its API is not loaded. The tested version for 1.20.1 is 10.4.16.80, covering 730 chemical fields. See [Mekanism 1.20.x source](https://github.com/mekanism/Mekanism/tree/1.20.x).

The loader minimum applies to VJRM; other mods must still be installed according to their own requirements. JEI 19.64.0.455 requires NeoForge 21.1.238+.

## Original Recipe Reading and Third-Party Support

- Following the resource-reading approach of JEI Swift Copy, recipes are read by recipe ID from the **server-side currently active datapack JSON**, bypassing re-encoding of client-synchronized recipes and resolving the shaped crafting `Cannot encode unpacked recipe` issue.
- Supports singleplayer and multiplayer games where **the server has VJRM installed**. Reading recipes does not require OP; server-side write exports still check permissions. When a multiplayer server does not have VJRM installed, a clear notice is shown; vanilla templates and client-side export remain available.
- Preserves the original JSON conditions and extra fields; when modifying, only the selected ingredient, output, or count is updated. Recipes without original JSON, that are not loaded, or that contain no editable fields are skipped, and program-generated recipes are not exported as datapack recipes.
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

# Visual JEI Recipe Maker 0.5.3 -中文-

Minecraft 1.21.1，NeoForge 21.1.65+，Java 21。Mod ID：`vjrm`，包：`com.pureneko.vjrm`。客户端安装 JEI 19.27+ 或 EMI 1.1.24+ 任意一个即可；两者都装时编辑器使用 EMI。本版基础验证组合：NeoForge 21.1.65 + JEI 19.27.0.350 / EMI 1.1.24+1.21.1。

## 使用

1. 进入世界后按 **F8** 打开编辑器。可在游戏控制设置中修改按键。
2. 点击「配方类型」，在可搜索、可滚动的列表中选择模板。列表包含原版模板和可识别的第三方数据包配方类型；第三方类型以已有配方为模板，保留原始字段。
3. EMI 配方页的「复制到 VJRM 编辑器」书本按钮**默认显示**，JEI 按钮沿用原有配置、首次安装默认隐藏。在控制设置的 Visual JEI Recipe Maker 分类绑定「显示/隐藏 JEI/EMI 配方复制按钮」（默认未绑定），按键切换显示或隐藏。开关保存在 `config/vjrm-client.toml`，重启后保留；EMI 使用 `showEmiCopyButton`，JEI 使用 `showJeiCopyButton`。
4. 从 JEI/EMI 侧栏拖动物品或流体到有绑定的配方槽位。下方显示玩家主物品栏和快捷栏；**只能通过拖拽填入**。
5. 拖拽替换保留该槽原有数量；合并原料槽会替换整组重复原料。点击配方槽位后，可输入物品/流体 ID 和数量，再按「应用修改」。输入 `#minecraft:logs` 等标签可修改输入标签。
6. 右键清除可移除的工作台输入；空工作台槽位可重新拖入原料。必需字段和最后一个输入受到 serializer 校验保护。按 Esc 返回。
7. 无法自动对应 JSON 字段的槽位会显示黄色边框。先点击槽位，选择 JSON 字段，再按「绑定槽位」。燃料、催化剂和纯展示槽不属于可导出的原料。
8. 点击顶部「数值」，搜索并修改已有数值字段，包括运行时间、魔力、能量、经验、概率、数量和嵌套参数。支持大整数、小数、科学计数法；常见能量等数字字符串保留字符串类型。整个草稿一起校验，取消则丢弃修改。
9. 「编辑 JSON」内的「复制到剪贴板」原样复制当前草稿。编辑器提供键名、字符串、数值、布尔值/null 和错误高亮，行号、行列位置、配对括号高亮、括号/引号补全、回车自动缩进、Tab 缩进、Ctrl+Z/Y 撤销重做、格式化及实时语法错误提示。Ctrl+Shift+F 格式化，Ctrl+S 应用；支持 Ctrl+A/C/V/X。应用前调用实际 serializer 校验。
10. 输入新配方 ID，例如 `vjrm:my_recipe`，选择客户端或服务端导出。默认拒绝覆盖，可显式打开覆盖。

界面会自动缩放，按整个屏幕水平、垂直居中，保留玩家物品栏，侧栏在编辑器两侧显示。JEI 侧栏自身需要启用，可用 Ctrl+O 切换。


## 来源信息与槽位提示

配方来源显示 ID 和类别名称；代理配方显示真实来源及展示类别。悬停来源行可查看完整信息。配方类型检索也支持名称和 ID。

将鼠标移到配方槽位上，会显示原料名称、注册 ID、所属模组及浏览器原有文字说明。已绑定槽位追加 JSON 字段路径；没有绑定的展示槽也可查看原料身份。字段选择与绑定按钮位于配方图上方，配方图在展示区域内居中。

## 配方浏览器与化学品支持

EMI 使用原配方完整组件，保留背景、动画、文字、箭头和流体槽。JEMI 转换配方使用其原组件。可以重建的配方按当前 JSON 重建；不能重建的第三方包装布局只更新槽位，其他动态文字仍来自原配方，界面会显示提示。

EMI 1.1.24 的 JEMI 需要 JEI API 与运行时。只装 EMI 可显示原版及模组原生 EMI UI；只有 JEI 适配的模组界面需要同时安装 JEI。复制按钮无需修改 EMI 开发设置。参考 [EMI 官方实现](https://github.com/emilyploszaj/emi)。

Mekanism 可选适配支持气体、灌注、颜料、浆液及 1.21 的统一化学品表示，包括 `chemicalInput`、`gasInput`、`input_chemical` 等路径、标签和替代原料。可绑定、拖拽、修改 ID/标签及 long 数量；没有 Mekanism 时不加载其 API。1.20.1 实测版本为 10.4.16.80，覆盖 730 个化学品字段。参考 [Mekanism 1.20.x 源码](https://github.com/mekanism/Mekanism/tree/1.20.x)。

加载器下限针对 VJRM；其他模组仍按其自身要求安装。JEI 19.64.0.455 需要 NeoForge 21.1.238+。

## 原始配方读取与第三方支持

- 参考 JEI Swift Copy 的资源读取思路，按配方 ID 读取**服务端当前生效的数据包 JSON**，绕过客户端同步配方的重新编码，解决有序合成的 `Cannot encode unpacked recipe`。
- 支持单人游戏，以及**服务端安装 VJRM** 的多人游戏。读取配方不要求 OP；服务端写入导出仍检查权限。多人服未安装 VJRM 时会明确提示，原版模板与客户端导出仍可用。
- 保留原 JSON 的条件和额外字段，修改时只更新选中的原料、产物或数量。没有原始 JSON、未加载或不包含可编辑字段的配方会跳过，不会把程序生成配方当作数据包配方导出。
- 类型列表结合服务端可用数据包和 JEI 类别建立，支持第三方 `Recipe`、`RecipeHolder` 以及能够提供真实配方 ID 的 JEI 包装对象。
- 标准类别通过编辑后的配方重建 JEI 页面。包装类别保留独立的来源布局，覆盖已绑定槽位中的物品/流体显示；其动态文字、时间图表及槽位结构仍来自原始展示对象，实际参数以 JSON 为准，界面会显示提示。
- 支持常见的物品、物品标签、物品堆、流体以及 NeoForge 带数量的嵌套原料结构。字段匹配有歧义时要求手动绑定，未知结构可使用 JSON 编辑。
- 不保证每个模组的私有格式、任意包装对象或自定义化学品都能自动映射。实际整合包中的专用格式仍可能需要额外适配。

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

