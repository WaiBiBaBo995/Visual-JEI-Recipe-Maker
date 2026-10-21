# 更新记录

## 1.0.1 — 多版本更新 / Multi-version update

- 同步新版配方编辑器、手动 JSON 字段绑定和金色字段提示至六个独立版本分支。 / Ship the updated recipe editor, manual JSON field bindings, and gold field tooltips across six independent version branches.
- 所有语言的玩家物品栏标题仅保留名称，移除括号说明。 / Remove the parenthetical explanation from the player inventory label in every language.
- 所有模组元数据使用精确版本 `1.0.1`；Forge/NeoForge 在资源处理时展开版本。 / Resolve exact version `1.0.1` into all loader metadata during resource processing.
- README 恢复中英双语并适配独立分支；移除验证记录文件及其链接。 / Restore bilingual READMEs, update independent-branch links, and remove validation documents and their links.

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
