# Changelog -English-

## 1.0.2 — 2026-10-22

- Update all six editions to 1.0.2. JSON and supported GTCEu JS editors show native browser icons after recognized item/fluid ID strings, including manually defined fields. Recipe IDs, type IDs, and unrelated text do not receive icons merely because they resemble registry IDs.
- Icons affect display only: original text, copying, formatting, and exports contain no extra characters. Wrapping, mouse positioning, selection, and cursor movement account for icon space. Invalid JSON/JS clears stale icons.
- Recipe type search shows JEI category icons; older editions use EMI icons when EMI is selected. Names, IDs, search, and hover information remain available.
- Fluid icons use a full-size sample to avoid thin slivers caused by Fabric units. Minecraft 26.1.2 separates ingredient and text rendering layers while preserving highlighting and clipping.
- Minecraft 26.1.2 continues to support JEI only. GTCEu/KubeJS and Thaumaturge support remains limited to the platforms listed in [COMPATIBILITY.md](COMPATIBILITY.md).
- Remove parenthetical explanations from inventory labels in all languages. Resolve the exact version `1.0.2` into every loader descriptor, maintain bilingual READMEs and this changelog, and remove validation documents and their links.

## 1.0.1 — Multi-version release

- Ship the updated recipe editor, manual JSON field bindings, and gold field tooltips across six independent version branches.
- Remove the parenthetical explanation from the player inventory label in every language.
- Resolve exact version `1.0.1` into loader metadata during resource processing.
- Restore bilingual READMEs, update independent-branch links, and remove validation documents and their links.

## 0.8.0 — 2026-10-22

- Forge 1.20.1 and NeoForge 1.21.1: read GTCEu machine recipe sources, copy through JEI/EMI, and rebuild native pages. Support both the older `recipe.kind.GTRecipe` and newer APIs. Runtime recipes do not require matching datapack files.
- Edit GT recipes as JS while retaining numeric editing and field binding. Add input/output and per-tick channels, and generate `merge` scripts through GTCEu's KubeJS recipe entrypoints. Provide JS highlighting, indentation, bracket/quote pairing, undo/redo, formatting, shortcuts, and clipboard copying.
- Write GT scripts under the client/server installation root at `kubejs/server_scripts/vjrm/<namespace>/<recipe-path>.js`. Server exports retain permission, rate, overwrite, and recipe checks. Visual round-tripping parses the generated recipe declaration without executing JS.
- Forge adapts the JEI fluid tooltip interface still used by GTCEu to avoid load failures with current JEI interfaces.
- NeoForge 26.1.2: support Thaumaturge `AspectInstance` slots, IDs, amounts, dragging, tooltips, and manual bindings. Use public codecs and current registries, including additional aspects.
- Treat `components` arrays as separate inputs, bind duplicate ingredients by position, and support ingredients with data component conditions. Prefer matching recipe types when rebuilding native JEI pages so generic categories do not take precedence.
- This historical update affected the three editions above; the three Fabric editions remained at 0.7.0. Keep shared history in this file.

## 0.7.0 — Gold field tooltips and Fabric ports

- Show appended binding paths in vanilla `GOLD` across all six editions.
- Add independent Fabric 1.20.1, Fabric 1.21.1, and Fabric 26.1.2 projects, update NeoForge 26.1.2, and synchronize gold tooltips to Forge 1.20.1 and NeoForge 1.21.1.
- Integrate version-specific Fabric entrypoints, keybindings, screen events, networking, configuration, and fluid representations. Bridge native JEI/EMI fluid units while retaining components/NBT and original JSON quantities.
- Preserve centered UI, native recipe previews, rejection reasons and sounds, source IDs/names, JSON highlighting/copying, numeric editing, and manual JSON field binding.
- Fabric 1.20.1/1.21.1 support JEI alone, EMI alone, or both. EMI copy buttons are attached to actual recipe-page widgets, with JEMI handling JEI categories. Both 26.1.2 editions support JEI only.
- Fabric 26.1.2 uses JEI 29.47.0.111 source IDs and Fabric recipe synchronization events to index original Recipe objects. Server-side VJRM enables vanilla serializer synchronization without requiring JEI.
- Follow native recipe formats and datapack formats 15, 48, and 101.1. Older Fabric vanilla cooking outputs remain fixed at count one and reject unsupported edits.
- Fabric uses `config/vjrm-client.json` and `config/vjrm-server.json`; Forge/NeoForge retain their TOML configuration.

See the [Fabric installation guide](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/blob/fabric-1.21.1/README.md).

## 0.6.0 — Minecraft 26.1.2 NeoForge

Add an independent NeoForge 26.1.2 edition. At this historical release, NeoForge 1.21.1 and Forge 1.20.1 remained at 0.5.4.

- Port to Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25, and JEI 29.47.0.111; update Gradle and ModDev toolchains.
- Adapt GUI rendering-state extraction and keyboard/mouse events while preserving centering/scaling, native recipe UI, dragging/tooltips, and rejection reasons/sounds.
- Adapt recipe keys, registries, stack templates, and ingredients. Vanilla inputs use strings, `#tags`, and alternative arrays; update detection, replacement, tags, empty crafting slots, and templates accordingly.
- Resolve recipe sources and copying through JEI `getSourceRecipeIds` before display IDs. NeoForge synchronization events also map original Recipe objects to real IDs when integrations lack category ID callbacks.
- Preserve numeric editing, JSON highlighting/assistance, clipboard copying, manual fields, and slot binding. Validate edits through the actual recipe serializer, roll back failures, and retain other JSON fields.
- Export under `recipe/` with datapack format 101.1. Update server permissions and networking. Dedicated servers do not require JEI.
- This port uses JEI; older editions retain EMI/JEMI integrations.

See the [26.1.2 README](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/blob/neoforge-26.1.2/README.md). Older third-party runtime results do not establish compatibility with 26.1.2.

## 0.5.4

- Fix missing copy buttons for Astral Sorcery and similar JEI integrations that register raw Recipe objects without `getRegistryName`. NeoForge 1.21.1 resolves actual IDs by loaded object identity with reload-aware caching, rather than fuzzy output matching.
- EMI/JEMI shares source resolution for raw JEI display objects and continues reading actual server-side JSON.
- Add **JSON Fields** above the recipe diagram to search current JSON or enter JSON Pointer paths, including array indices and escaped `/` or `~` keys.
- Choose item, fluid, optional Mekanism chemical, or raw JSON types and input/output roles. Definitions join the selectable field list; selecting a slot first binds it immediately. Explicit bindings take precedence over default paths.
- Raw JSON fields edit scalars, objects, or arrays, including private structures such as Lumen nodes. Actual serializer validation rolls back failures and preserves other fields.
- Definitions survive resizing, refreshes, and same-type JSON edits; switching template/type resets them. Removing a definition does not delete data, and definitions are not exported.
- Valid server-side JSON can open for manual editing even if automatic detection finds no editable fields.

Historical Astral Sorcery 2.0.2.39 checks resolved 165 recipes across eight categories, preserved altar JSON through JEI copying, and exercised actual EMI/JEMI copy buttons plus Lumen field binding/editing. These runtime checks used NeoForge 1.21.1; Forge 1.20.1 received generic source/manual-field handling without taking cross-version Astral Sorcery as a dependency.

## 0.5.3

Update NeoForge 1.21.1 and Forge 1.20.1.

- Show recipe sources as “ID · name” consistently with type search. Proxy recipes show true source types and display categories; hover reveals long text.
- Fix missing EMI copy buttons in production by inserting book buttons after real recipe-page widgets are created, without relying on development-only decorators.
- Show EMI buttons by default through independent `showEmiCopyButton = true`. An existing `showJeiCopyButton = false` no longer keeps EMI hidden after upgrading. JEI retains its previous default/configuration. The controls keybind toggles and saves both states.
- Slot tooltips show names, registry IDs, mod names, original descriptions, and bound paths for items, fluids, and optional chemicals, including unbound display slots. Tooltips use normal text size even when the diagram scales down.
- Consolidate historical records into this shared changelog.

## 0.5.2

Update NeoForge 1.21.1 and Forge 1.20.1.

- Add **Copy to Clipboard** to copy the full current JSON draft without requiring validation, applying changes, or closing the page.
- Move field/binding controls above the recipe UI and center diagrams horizontally and vertically in their own display area.
- Support Mekanism chemical fields, tags, and alternatives such as `chemicalInput`, `gasInput`, `slurryInput`, and `input_chemical`. Preserve chemical types, extension fields, and long quantities; support binding, dragging, IDs, and tags.
- Fix item ingredient counts inside Mekanism `ingredient` / `amount` wrappers.
- Isolate optional chemical APIs so Mekanism is not required when absent.
- Lower NeoForge minimum from 21.1.251 to **21.1.65**, and Forge minimum from 47.4.10 to **47.1.1**. Historical minimum-loader builds/checks used JEI 19.27.0.350 / 15.74.0.225 and EMI 1.1.24.

Historical Mekanism 1.20.1-10.4.16.80 checks matched 730 fields: 248 gas, 44 infusion, 396 pigment, and 42 slurry. Each type completed four edits followed by decoding; client checks covered binding, replacement, ID input, quantity 9876543210, and complete machine layouts.

Other mods retain their own loader requirements. Older NeoForge can use JEI 19.27.0.350; JEI 19.64.0.455 itself requires NeoForge 21.1.238+.

## 0.5.1 — Complete EMI / JEMI recipe UI

Fix the 0.5.0 behavior that always used generic slot previews under EMI and lost the original recipe UI.

- Prefer the complete `EmiRecipe.addWidgets` tree, preserving backgrounds, textures, animation, arrows, flames, text, fluids, and extension widgets.
- Rebuild vanilla EMI layouts from current JSON, updating cooking time and experience widgets.
- Preserve JEMI's original rendering and rebuild supported categories through its JEI runtime without requiring EMI to load VJRM's JEI plugin.
- For third-party wrappers that cannot be rebuilt, retain original UI and override newly created preview slots without changing EMI's registered recipe. Dynamic text follows the source layout with a notice.
- Use generic JSON slots only when neither native EMI nor JEMI layout is available.
- Correct EMI/JEI differences for 3×2 shaped and some shapeless grids, including bindings and empty-slot insertion.

Historical verification covered release packaging, 42 recipe/file checks, JEI regression, EMI alone without JEI API classes, all eight vanilla templates, updated cooking widgets, native EMI item/fluid replacement and source isolation, actual JEMI category drawing, and 16 Create 6.0.10 recipe layouts including mechanical crafting/sequenced assembly. Screenshots covered vanilla EMI, native EMI machines, JEMI machines, and Create sequences.

Compatibility boundary: EMI 1.1.24 JEMI needs JEI API/runtime. EMI alone supports vanilla and native EMI pages; JEI-only mod pages require JEI alongside EMI. See the [JEMI implementation](https://github.com/emilyploszaj/emi/blob/1.21/xplat/src/main/java/dev/emi/emi/jemi/JemiPlugin.java).

Historical environment: Minecraft 1.21.1, NeoForge 21.1.251, Java 21, EMI 1.1.24+1.21.1, JEI 19.64.0.455.

## Visual JEI Recipe Maker 0.5.0

Target: Minecraft 1.21.1 / NeoForge 21.1.251 / Java 21.

1. Center the editor across the full screen and rescale on resolution changes.
2. Show specific right-click rejection reasons at the cursor, play the vanilla villager rejection sound, and roll back invalid operations.
3. Support JEI 19.27+ or EMI 1.1.24+; prefer EMI when both are installed. Include sidebar search, item/fluid dragging, template catalogs, and copy buttons.
4. Add **Values** for existing time, mana, energy, probability, loops, and nested fields. Support big integers, decimals, scientific notation, and common numeric strings while retaining JSON types/other fields. Validate changes as a group.
5. Add JSON syntax colors, line/column indicators, bracket matching/completion, auto-indentation, undo/redo, formatting, and live checks. Ctrl+S applies, Ctrl+Shift+F formats, Ctrl+Z/Y undo/redo.

Historical verification: release packaging excluded test/browser classes; 42 basic recipe/IO and 119 real Create 6.0.10 checks passed, totaling 161. Client checks covered JEI alone, EMI alone, coexistence, two UI scales, rejection/rollback, numeric editing, JSON assistance, actual EMI dragging, native Create layouts, sequences, large grids, and proxies. JSON/numeric/sidebar screenshots were inspected.

Original 0.5.0 limits: EMI used a generic JSON-based preview; JEI used native categories. Serializer behavior determines whether numeric edits affect machines. Add missing defaults or complex structures through JSON. Multiplayer original reading/server export requires server-side VJRM and existing permissions.

Historical installation: replace old VJRM with the matching 0.5.0 JAR and install JEI or EMI. Source packages included code, Gradle Wrapper, fixtures, and README, excluding worlds, build caches, and third-party JARs.

Historical test results describe the corresponding releases; they are not new runtime verification of 1.0.2.

# 更新记录 -中文-

## 1.0.2 — 2026-10-22

- 六个工程统一更新为 1.0.2。JSON 与旧版 GTCEu JS 编辑页在已识别的物品、流体 ID 字符串后显示浏览器原生图标，包含手动添加的字段；普通类型、配方 ID 和其他文字不会仅因格式像注册 ID 而添加图标。
- 图标只属于显示层，原文、复制、格式化和导出不插入额外字符。自动换行、鼠标定位、选择和光标移动按图标占位计算；编辑无效 JSON/JS 时清除过期图标。
- 配方类型检索列表加入对应 JEI 类别图标；旧版选择 EMI 时使用 EMI 图标，保留名称、ID、搜索和悬停信息。
- 流体图标使用满幅示例显示，避免 Fabric 单位使图标缩成细线；26.1.2 单独排列原料与文字的渲染层，保留高亮和裁切。
- 26.1.2 继续只支持 JEI。GTCEu/KubeJS 和 Thaumaturge 兼容范围保持原有版本范围。

- 所有语言的物品栏标题移除括号说明；所有加载器元数据写入精确版本 `1.0.2`；README 与更新记录保持中英双语，删除验证记录文件及相关链接。

## 1.0.1 — 多版本更新

- 同步新版配方编辑器、手动 JSON 字段绑定和金色字段提示至六个独立版本分支。
- 所有语言的玩家物品栏标题仅保留名称，移除括号说明。
- 所有模组元数据使用精确版本 `1.0.1`；Forge/NeoForge 在资源处理时展开版本。
- README 恢复中英双语并适配独立分支；移除验证记录文件及其链接。


## 0.8.0 — 2026-10-22

- 1.20.1 Forge、1.21.1 NeoForge：支持 GTCEu 机器配方的来源读取、JEI/EMI 复制和原生页面重建，兼容旧版 `recipe.kind.GTRecipe` 与新版 API。运行时配方无需对应数据包文件。
- GT 配方改为 JS 编辑，保留数值编辑、字段绑定，新增输入/输出及每 tick 通道操作；通过 GTCEu 的 KubeJS 配方入口生成 `merge` 脚本。提供 JS 高亮、缩进、括号/引号配对、撤销重做、格式化、快捷键及复制到剪贴板。
- GT 脚本写入客户端/服务端安装根目录的 `kubejs/server_scripts/vjrm/<命名空间>/<配方路径>.js`；服务端保持权限、频率、覆盖和配方校验。编辑器回填只解析生成的配方声明，不执行 JS。
- Forge 增加 GTCEu 仍使用的 JEI 流体 tooltip 接口适配，避免与当前 JEI 的接口差异导致加载失败。
- 26.1.2 NeoForge：支持 Thaumaturge `AspectInstance` 源质槽、ID、数量、拖拽、提示及手动绑定；通过公开 codec 解析当前注册表，包括附加源质。
- `components` 数组按多个独立输入识别，重复材料按位置分别绑定，支持带数据组件条件的材料；原生 JEI 页面重建优先匹配配方类型，避免被泛用类别抢占。
- 此次只更新上述三个工程；三个 Fabric 工程保持 0.7.0。所有更新记录仍集中在本文件。


## 0.7.0 — 金色字段提示与 Fabric 移植

- 槽位 tooltip 中追加的绑定字段路径改为原版金色 `GOLD`，适用于本次全部六个构建。
- 新增 Minecraft 1.20.1 Fabric、1.21.1 Fabric、26.1.2 Fabric 独立工程，同步更新 26.1.2 NeoForge；原有 1.20.1 Forge 与 1.21.1 NeoForge 也提供金色提示更新。
- Fabric 接入对应版本的入口、按键、界面事件、网络收发、配置和流体表示。流体与 JEI / EMI 原生单位直接桥接，保留组件或 NBT 和原 JSON 数量。
- 保留居中界面、原生配方预览、不可编辑原因与提示音、来源 ID / 名称、JSON 高亮与复制、数值编辑和手动 JSON 字段绑定。
- 1.20.1、1.21.1 Fabric 保留 JEI 单独安装、EMI 单独安装及共存；EMI 复制入口仍通过实际配方页组件加入，使用 JEMI 接入 JEI 类别。26.1.2 的两个加载器版本仅接入 JEI，不包含 EMI 集成。
- 26.1.2 Fabric 使用 JEI 29.47.0.111 的来源 ID API，并通过 Fabric 配方同步事件维护原始 Recipe 对象的真实来源索引；VJRM 服务端为原版 serializer 启用同步，无需强制安装 JEI。
- 遵循各版本原版数据格式，导出格式分别为 15、48、101.1。旧版 Fabric 原版烹饪产物数量固定为 1，拒绝不受 serializer 支持的数量修改。
- Fabric 使用 `config/vjrm-client.json` 和 `config/vjrm-server.json`；Forge/NeoForge 原配置方式保留。

安装组合见 [FABRIC.md](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/blob/fabric-1.21.1/README.md)。

## 0.6.0 — Minecraft 26.1.2 NeoForge

新增独立的 `neoforge-26.1.2/` 工程；1.21.1 NeoForge 与 1.20.1 Forge 工程继续保留 0.5.4。

- 移植至 Minecraft 26.1.2、NeoForge 26.1.2.122、Java 25、JEI 29.47.0.111，更新 Gradle 与 ModDev 工具链。
- 适配新的 GUI 渲染状态提取和键鼠事件，保留居中缩放、原生配方 UI、槽位拖拽与悬停提示、不可编辑原因和原版提示音。
- 适配新版配方键、注册表、物品堆模板及材料格式。原版输入使用字符串、`#标签` 和替代材料数组；字段识别、替换、标签编辑、空槽插入和模板同步更新。
- 配方来源与复制使用新版 JEI `getSourceRecipeIds`，优先解析真实来源 ID；原始 Recipe 对象通过 NeoForge 的同步配方事件维护身份索引，兼容没有类别 ID 回调的集成。
- 保留数值编辑、JSON 高亮与编辑辅助、复制草稿到剪贴板、JSON 节点手动添加字段及槽位绑定。修改调用实际 serializer 校验，失败回滚，保留其他 JSON 字段。
- 数据包目录使用 `recipe/`，`pack.mcmeta` 声明 101.1 格式；更新服务端导出权限检查和客户端网络接口。专用服务器无需 JEI。
- EMI 官方尚无 26.1.2 构建，因此此分支目前支持 JEI；旧版本的 EMI/JEMI 集成继续保留在原工程中。

安装和使用见 [26.1.2 README](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/blob/neoforge-26.1.2/README.md)。旧版第三方模组的运行结果不直接作为 26.1.2 的兼容性结论。

## 0.5.4

- 修复 Astral Sorcery 及类似 JEI 集成直接注册原始 `Recipe`、没有提供 `getRegistryName` 时缺少复制按钮的问题。1.21.1 根据已加载配方对象的身份反查原始 ID，复用缓存并检查重新加载后的有效性，不使用产物相同等模糊匹配。
- EMI 的 JEMI 配方也从原始 JEI 展示对象解析真实来源，支持没有原始 ID 的 Astral Sorcery 祭坛页面；继续按真实 ID 读取服务端当前生效的 JSON。
- 新增配方图上方的「JSON字段」入口：搜索并选择当前 JSON 的节点，或输入 JSON Pointer 路径，手动添加原先没有识别出的字段。支持数组及路径中的 `/`、`~` 转义。
- 可指定物品、流体、可选 Mekanism 化学品、原始 JSON，以及输入/输出用途。先选槽位再添加时直接绑定该槽位；也可添加到字段列表后手动绑定。显式绑定优先于浏览器默认路径。
- 原始 JSON 类型可在值栏编辑标量、对象或数组，用于 Lumen 等私有结构。所有修改仍通过实际 serializer 校验，失败回滚，其他节点保持不变。
- 手动字段保留在当前草稿中，刷新、缩放、同类型 JSON 编辑后保留；换模板或更改配方类型后重置，失效路径移除。移除手动定义不会删除 JSON 数据，定义不写入导出的配方。
- 服务端读取不再因为自动识别字段数为零而拒绝有效的原始配方，便于玩家通过 JSON 字段入口补充映射。

Astral Sorcery 2.0.2.39 实测：JEI 中解析 8 类、165 个配方的真实来源；JEI 复制祭坛配方完整保留 JSON；EMI/JEMI 实际配方页点击复制，并通过新入口绑定、修改真实 Lumen 字段及刷新原生 UI。Astral Sorcery 本体测试位于 1.21.1 NeoForge；1.20.1 Forge 同步提供手动字段与通用来源处理，不将其他 Minecraft 版本的 Astral Sorcery 作为强制依赖。

## 0.5.3

同时更新 Minecraft 1.21.1 NeoForge 与 1.20.1 Forge。

- 编辑器的配方来源显示“ID · 名称”，与配方类型检索保持一致；代理配方同时显示真实来源类型和展示类别。长文本悬停可查看完整内容。
- 修复 EMI 正式环境没有“复制到 VJRM 编辑器”按钮：直接在实际配方页组件创建完成后加入书本按钮，不再依赖默认仅在开发环境开启的配方装饰器。
- EMI 复制按钮默认显示，使用独立的 `showEmiCopyButton = true` 配置；旧版 `showJeiCopyButton = false` 不再导致 EMI 按钮首次升级后仍隐藏。JEI 保持原有默认隐藏和已有配置。控制设置中的显示/隐藏快捷键可切换并保存两个浏览器的按钮状态。
- 悬停配方槽位时，显示原料名称、注册 ID、所属模组及原有文字说明；适用于物品、流体和可选 Mekanism 化学品，也适用于尚未绑定 JSON 字段的槽位。已绑定槽位追加字段路径；提示按正常字号显示，避免随配方图缩小。
- 历史更新记录合并到此文件，两个版本共用一个 CHANGELOG.md。

## 0.5.2

同时更新 Minecraft 1.21.1 NeoForge 与 1.20.1 Forge 工程。

- JSON 编辑页增加「复制到剪贴板」，复制完整当前草稿，不要求 JSON 已通过校验，不会应用草稿或关闭页面。
- 将字段选择和绑定按钮放到配方 UI 上方；配方图在独立展示区域中水平、垂直居中。
- 支持 Mekanism 的化学品字段及标签/替代原料，包含 `chemicalInput`、`gasInput`、`slurryInput` 和 `input_chemical` 等路径；保留化学品类型、扩展字段和 long 数量。可用槽位绑定、拖拽、ID 与标签输入修改化学品。
- 修复 Mekanism 带 `ingredient` / `amount` 的物品原料数量读取。
- 可选化学品 API 独立处理，没有 Mekanism 时无需安装其依赖。
- NeoForge 最低版本从 21.1.251 降至 **21.1.65**；Forge 最低版本从 47.4.10 降至 **47.1.1**。使用最低声明版本构建并运行验证，测试搭配 JEI 19.27.0.350 / 15.74.0.225 与 EMI 1.1.24。

Mekanism 1.20.1-10.4.16.80 验证：识别并匹配 730 个化学品字段，气体 248、灌注 44、颜料 396、浆液 42；四种类型各完成四次修改后再解码。实际客户端验证化学槽位绑定、替换、ID 输入、9876543210 数量和完整机器界面。

其他模组自己的加载器要求仍然有效。较旧 NeoForge 可搭配 JEI 19.27.0.350；JEI 19.64.0.455 自身要求 NeoForge 21.1.238+。

## 0.5.1 — EMI / JEMI 完整配方 UI

修复 0.5.0 在 EMI 下始终使用通用槽位预览、丢失原配方 UI 的问题。

- 优先使用 `EmiRecipe.addWidgets` 提供的完整组件树，保留背景、纹理、动画、箭头、火焰、文字、流体槽和扩展组件。
- 原版配方按当前 JSON 重建 EMI 布局；烹饪时间和经验等变化会同步到重建组件。
- JEMI 转换配方保留兼容层的原绘制。能够重建的类别直接通过 JEMI 的 JEI 运行时重建，不依赖 VJRM 的 JEI 插件是否被 EMI 加载。
- 第三方包装布局不能重建时，保留原 UI，覆盖刚创建的预览槽位；不会改动 EMI 注册的原配方。动态文字仍按来源布局显示，沿用对应提示。
- 通用 JSON 槽位只用于原生 EMI/JEMI 布局均不可用的情况。
- 修正 EMI 与 JEI 在 3×2 有序和部分无序合成网格上的差异，确保字段绑定及空槽插入位置正确。

验证通过：

- 发布构建、42 项配方/文件集成检查、JEI 客户端回归。
- EMI 单独安装，无 JEI API 类仍可运行。
- 所有八类原版模板完整 EMI UI；背景和文字实际绘制；烹饪时间组件刷新。
- 原生 EMI 机器的物品/流体替换、原流体槽几何及渲染器保留、来源配方隔离。
- 实际 JEMI 类别转换，原 JEI 类别 draw 调用，以及替换后的槽位绘制。
- EMI + JEI + Create 6.0.10 下 16 种配方的原 UI 和原料替换，包含机械合成和序列组装。
- 截图检查：EMI 原版、原生 EMI 机器、JEMI 机器和 Create 序列组装。

兼容边界：EMI 1.1.24 的 JEMI 依赖 JEI API 和运行时。只装 EMI 时可显示原版及模组原生 EMI UI；只有 JEI 适配的模组仍需同时安装 JEI 才能通过 JEMI 使用其 UI。参考：https://github.com/emilyploszaj/emi/blob/1.21/xplat/src/main/java/dev/emi/emi/jemi/JemiPlugin.java

环境：Minecraft 1.21.1、NeoForge 21.1.251、Java 21、EMI 1.1.24+1.21.1、JEI 19.64.0.455。

## Visual JEI Recipe Maker 0.5.0

适用：Minecraft 1.21.1 / NeoForge 21.1.251 / Java 21。

1. 编辑器按整个屏幕水平、垂直居中，分辨率变化时重新缩放。
2. 右键无法清除的槽位在点击位置提示具体原因，播放原版村民拒绝音效；无效操作完整回滚。
3. JEI 19.27+、EMI 1.1.24+ 可二选一；同时安装时使用 EMI。支持侧栏搜索、物品/流体拖拽、模板目录与配方复制按钮。
4. 顶部「数值」可以搜索、编辑已有的时间、魔力、能量、概率、循环次数等数值。支持嵌套字段、大整数、小数、科学计数法及常见能量数字字符串；保留原 JSON 类型和其他字段。整组提交后校验，失败不更改配方。
5. JSON 编辑器提供语法颜色、行号、行列位置、括号匹配、括号/引号补全、自动缩进、撤销/重做、格式化和实时语法检查。Ctrl+S 应用、Ctrl+Shift+F 格式化、Ctrl+Z/Y 撤销/重做。

验证结果：

- 发布构建通过，JAR 不含测试代码或第三方浏览器类。
- 42 项基础配方/IO 集成检查通过。
- 119 项真实 Create 6.0.10 配方检查通过，包括加工时间、概率和循环次数修改；合计 161 项。
- JEI 单独运行、EMI 单独运行、JEI+EMI 共存的实际客户端检查通过。
- 两种 UI 尺寸居中、右键拒绝和回滚、数字编辑、JSON 编辑辅助与真实 EMI 侧栏鼠标拖拽均通过。
- JEI 原生 Create 布局、序列组装、大网格和代理配方客户端回归通过。
- JSON、数值界面和 EMI 侧栏截图已检查，保存在本目录。

使用边界：EMI 编辑器采用基于当前 JSON 的通用配方预览；JEI 沿用原生类别预览。数值字段是否影响机器行为由对应模组 serializer 决定；不存在的缺省字段或复杂结构可在 JSON 中添加。多人服务器读取原配方和服务端导出仍需服务端安装 VJRM，导出权限沿用原设置。

安装：将本目录的 0.5.0 JAR 放入 mods，替换旧版 VJRM；另装 JEI 或 EMI 任意一个。源码包提供完整源代码、Gradle Wrapper、测试与 README，不含游戏存档、构建缓存及第三方 JAR。

历史检查结果描述对应版本，不代表本次对 1.0.2 重新进行了游戏运行验证。
