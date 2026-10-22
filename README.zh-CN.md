# Visual JEI Recipe Maker

[![版本](https://img.shields.io/badge/version-1.0.2-blue)](CHANGELOG.md)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62b47a)](#安装)
[![加载器](https://img.shields.io/badge/loader-NeoForge-orange)](#安装)

**在游戏内通过 JEI 或 EMI 制作、修改配方，导出为数据包或受支持的 GTCEu 脚本。**

[English](README.md) | 简体中文

[下载](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/releases) ·
[更新记录](CHANGELOG.md) · [联动指南](COMPATIBILITY.md) ·
[反馈问题](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/issues)

VJRM 让整合包作者直接从熟悉的配方页面开始：复制已有配方或选择模板，拖入材料，
调整数量和机器参数，再导出结果。修改物品、流体时会保留周围的条件和额外配方字段；
应用修改前，使用对应配方的实际 serializer 校验。

当前分支适用于 **Minecraft 1.21.1 / NeoForge**。其他 Forge、Fabric 版本见[版本与分支](#版本与分支)。

## 目录

- [安装](#安装)
- [快速上手](#快速上手)
- [1.0.2 更新亮点](#102-更新亮点)
- [编辑配方](#编辑配方)
- [导出与生效](#导出与生效)
- [兼容与限制](#兼容与限制)
- [常见问题](#常见问题)
- [版本与分支](#版本与分支)
- [开发与验证](#开发与验证)
- [反馈与许可](#反馈与许可)

## 安装

| 组件 | 当前分支要求 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.65 或更新版本 |
| Java | 21 |
| 客户端配方浏览器 | JEI 19.27+ **或** EMI 1.1.24+ |

1. 在[发布页](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/releases)下载
   **同时匹配 Minecraft 版本与加载器**的 VJRM JAR。
2. 放入游戏实例的 `mods` 目录，替换旧版 VJRM JAR。
3. 客户端另装对应版本的 JEI 或 EMI 及其前置；两者同时安装时，VJRM 优先使用 **EMI**。
4. 多人游戏中，如需读取服务端原始配方或直接导出到服务端，请在服务端也安装 VJRM。
   专用服务器无需安装 JEI 或 EMI。

上述加载器下限针对 VJRM；其他模组可能要求更高版本。例如 JEI 19.64.0.455 要求
NeoForge 21.1.238+。当前分支的基础依赖版本为 JEI `19.27.0.350` 和 EMI `1.1.24+1.21.1`。

## 快速上手

1. **打开编辑器。** 进入世界后按 **F8**；可在游戏控制设置中修改按键。
2. **选择起点。** 点击「配方类型」，按名称或 ID 搜索模板；也可以在 JEI/EMI 配方页
   点击书本形状的「复制到 VJRM 编辑器」按钮。
3. **替换材料。** 从浏览器侧栏拖动物品或流体到已绑定槽位，也可从下方物品栏拖入物品。
   **拖拽不消耗物品；仅单击物品栏中的物品不会填入配方。**
4. **调整配方。** 点击槽位，编辑 ID／标签与数量，再按「应用修改」。已有数值参数使用
   「数值」编辑，完整配方使用「编辑 JSON」。
5. **填写配方 ID。** 创建新配方时，可使用 `vjrm:my_recipe` 这样的 ID。
6. **导出并加载。** 选择客户端或服务端导出，然后按[导出与生效](#导出与生效)操作。
   导出文件后仍需重载配方。

> **找不到复制按钮？** EMI 默认显示，JEI 首次安装默认隐藏。
> 在「控制设置 → Visual JEI Recipe Maker」中绑定「显示/隐藏 JEI/EMI 配方复制按钮」
> （默认未绑定），再按该键切换显示。

## 1.0.2 更新亮点

- **物品、流体 ID 旁显示原生图标**：适用于 JSON 和受支持的 GTCEu JS 页面，也包含手动字段识别的 ID。
- **配方类型搜索显示类别图标**：保留名称、ID 与悬停提示，更容易辨认类别。
- **图标不改变文本**：仅影响显示，不向草稿、复制、格式化或导出内容插入字符；换行、选择与光标定位考虑图标占位。
- **显示细节优化**：无效草稿清理过期图标，流体图标使用满幅示例，物品栏标题保留各语言名称并移除括号说明。

配方 ID、类型 ID 和其他普通字符串不会仅因格式类似注册 ID 就显示图标。
完整更新历史与平台差异见 [CHANGELOG.md](CHANGELOG.md)。

## 编辑配方

### 槽位、数量与绑定

- 拖拽替换保留槽位原有数量；合并原料槽会替换整组重复材料，「应用修改」的数量可调整该组的重复次数。
- 支持的输入可填写 `#minecraft:logs` 等标签。右键移除可编辑的工作台输入，空工作台槽位可重新拖入材料。
  必需字段和最后一个输入仍受 serializer 校验保护；无效修改会回滚。
- **黄色边框**表示需要手动映射字段。先选中槽位，再在配方图上方选择字段并点击「绑定槽位」。
  燃料、催化剂和纯展示槽不属于可导出的原料。
- 悬停槽位可查看名称、注册 ID、所属模组及浏览器原有说明；已绑定槽位追加**金色字段路径**。
  来源行显示真实配方与类别，代理页面还会标明展示类别。

### 数值与 JSON 编辑器

「数值」支持搜索已有的时间、能量、魔力、经验、概率、数量及嵌套参数，支持大整数、小数、
科学计数法与常见数字字符串，并保留 JSON 类型。整个草稿一起校验；取消会丢弃修改。
缺省字段或复杂结构可通过「编辑 JSON」补充。

JSON 编辑器提供语法／错误高亮、行列位置、括号匹配与补全、自动缩进、撤销重做、格式化和剪贴板操作。
「复制到剪贴板」原样复制当前草稿，尚未完成的草稿也可以复制。

| 快捷键 | 操作 |
| --- | --- |
| `Ctrl+S` | 校验并应用草稿 |
| `Ctrl+Shift+F` | 格式化 |
| `Ctrl+Z` / `Ctrl+Y` | 撤销／重做 |
| `Ctrl+A` / `Ctrl+C` / `Ctrl+V` / `Ctrl+X` | 全选／复制／粘贴／剪切 |
| `Tab` | 缩进 |

### 手动添加 JSON 字段

1. 选中槽位，打开配方图上方的「JSON字段」；也可以先添加字段，之后再绑定。
2. 搜索当前 JSON 的节点，或输入 `/required_lumen/0` 这样的 JSON Pointer。
   数组下标从 0 开始；对象键名中的 `/` 写作 `~1`，`~` 写作 `~0`。
3. 选择物品、流体、可选的 Mekanism 化学品或「原始 JSON」，设置输入／输出用途，点击「添加／更新」。
   私有包装格式应选择内部原料或 ID 节点，以保留外层结构。
4. 选择「原始 JSON」时，在值栏输入有效 JSON 后应用。字符串需加双引号，对象和数组可以整体修改。
   修改仍通过对应配方的 serializer 校验，失败回滚。

移除手动字段只删除定义，不删除配方数据。有效定义在刷新、缩放和同类型 JSON 编辑后保留；
切换模板或配方类型后重置，导出时不会写入这些定义。

## 导出与生效

### 数据包配方

| 目标 | 导出的配方文件 |
| --- | --- |
| 客户端 | `<游戏目录>/vjrm_exports/vjrm_generated/data/<namespace>/recipe/<path>.json` |
| 服务端 | `<世界目录>/datapacks/vjrm_generated/data/<namespace>/recipe/<path>.json` |

例如 `vjrm:my_recipe` 对应生成数据包中的 `data/vjrm/recipe/my_recipe.json`。
两种方式都会生成 `pack.mcmeta`，使用 Minecraft 1.21.1 数据包格式 **48**。

- **客户端导出**：将整个 `vjrm_generated` 文件夹复制到目标世界的 `datapacks` 目录，再在该世界执行 `/reload`。
- **服务端导出**：直接写入世界的数据包目录，之后仍需执行 `/reload`。
- **默认禁止覆盖**；替换已导出的同名文件时，需显式开启允许覆盖。
- 服务端导出默认需要 **OP 等级 2**。可在 `<世界目录>/serverconfig/vjrm-server.toml`
  中配置 `exportPermissionLevel`（0–4）。读取原始配方不要求 OP。
- 原始 JSON 读取上限为 **240,000 字符**，服务端导出载荷上限为 **24,000 字符**。
  草稿超过服务端导出限制时，可改用客户端导出。

### GTCEu 脚本

受支持的 GTCEu 配方使用 JS 导出。需另装对应版本的 **GTCEu、KubeJS 及其前置**。
配方页提供「编辑 JS」「JS 字段」「添加输入」「添加输出」及客户端／服务端脚本导出。
材料支持每次配方和每 tick 通道，仍受机器槽位容量限制；「数值」可编辑耗时、EU/t、概率等已有数值。
运行时生成的 GT 配方无需对应数据包 JSON。

脚本相对于客户端或服务端安装根目录写入 `kubejs/server_scripts/vjrm/<namespace>/<recipe-path>.js`。
例如 `vjrm:chemical_reactor/test` 对应 `kubejs/server_scripts/vjrm/vjrm/chemical_reactor/test.js`。
重启或重载 KubeJS 配方后生效。

生成的 `ServerEvents.recipes` 声明使用 GTCEu 机器配方入口和 `.merge({...})` 保留完整配方数据。
可视化回填需要保留该声明结构；编辑器仅解析，不执行 JS。任意函数、循环可在导出后用外部编辑器添加。
服务端仍检查权限、频率、路径、配方有效性及覆盖设置。详细用法见[联动指南](COMPATIBILITY.md)。

## 兼容与限制

| 联动 | 支持范围与边界 |
| --- | --- |
| JEI / EMI | 使用原生配方布局，共存时优先 EMI。EMI 1.1.24 的 JEMI 展示仅有 JEI 适配的页面时，需要 JEI API 与运行时。 |
| Create | 支持数据包加工配方、已有机械合成槽位（包含大于 3×3 的网格）与序列组装；形状、新工序、过渡物品及额外结果池通过 JSON 编辑。 |
| Mekanism | 可选物品／流体与化学品编辑，支持标签和 long 数量；1.21 使用统一化学品表示，未安装时不加载其 API。 |
| Astral Sorcery | 可从原始配方对象解析来源 ID，Lumen 等私有结构可使用手动字段；只有 JEI 适配的页面通过 EMI/JEMI 展示时仍需 JEI。 |
| GTCEu / KubeJS | 适用于 NeoForge 1.21.1 和 Forge 1.20.1，导出 JS。详见 [COMPATIBILITY.md](COMPATIBILITY.md)。 |
| Thaumaturge | 仅适用于 **NeoForge 26.1.2** 分支。详见 [COMPATIBILITY.md](COMPATIBILITY.md)。 |

VJRM 按真实配方 ID 读取**服务端当前生效的数据包 JSON**，保留条件和额外字段，避免重新编码客户端同步配方。
第三方模板使用已有配方。Create 的 `packing` 等展示类别可能是代理页面；导出以真实来源配方类型和行为为准。
合并多个切石结果的代理页面仅编辑选中 ID 对应的单份配方。

自动识别取决于模组格式：有歧义的字段需要手动绑定，未知结构可直接编辑 JSON。
数据包编辑会跳过没有已加载数据包资源的配方；受支持的 GTCEu 运行时配方使用前述独立 JS 流程。

能够重建的页面会按编辑后的配方刷新；其他第三方包装布局只更新绑定槽位，动态文字、图表与布局仍来自原始展示对象，
界面会显示相应提示。

## 常见问题

| 现象 | 处理方式 |
| --- | --- |
| 找不到复制按钮 | 使用前述控制设置中的快捷键开启，JEI 默认隐藏；同时确认页面存在可用的来源配方。 |
| JEI 侧栏不显示 | 使用 `Ctrl+O` 切换 JEI 自身的侧栏。 |
| 槽位无法修改 | 黄色槽位先手动绑定；展示槽和 serializer 要求的必需输入不能任意删除。 |
| 多人服无法读取原配方 | 服务端需安装 VJRM；未安装时仍可使用原版模板和客户端导出。 |
| 导出后游戏没有变化 | 安装生成的数据包并执行 `/reload`；JS 配方需重载或重启 KubeJS。 |
| 服务端导出被拒绝 | 检查权限、载荷大小与覆盖设置；连续快速导出时稍后再试。 |

复制按钮设置保存在 `config/vjrm-client.toml`：`showJeiCopyButton` 与 `showEmiCopyButton`。

## 版本与分支

每个分支都是独立工程，安装时同时匹配 Minecraft 版本与加载器。
26.1.2 分支**仅支持 JEI**，各平台的专用联动范围有所不同。

| Minecraft | 加载器 | 分支 | 游戏 Java |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | [main](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/main) | 21 |
| 1.20.1 | Forge | [forge-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/forge-1.20.1) | 17 |
| 1.20.1 | Fabric | [fabric-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.20.1) | 17 |
| 1.21.1 | Fabric | [fabric-1.21.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.21.1) | 21 |
| 26.1.2 | Fabric | [fabric-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-26.1.2) | 25 |
| 26.1.2 | NeoForge | [neoforge-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/neoforge-26.1.2) | 25 |

## 开发与验证

使用 Java 21 和仓库自带的 Gradle Wrapper 构建、启动当前分支：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

<details>
<summary>验证任务、可选联动与历史参考环境</summary>

```powershell
.\gradlew.bat verifyRecipes
# 仅 EMI / JEI 与 EMI 共存的开发客户端
.\gradlew.bat -Pviewer=emi runClient
.\gradlew.bat -Pviewer=both runClient
```

`verifyRecipes` 启动隔离的 GameTest 服务端，覆盖 serializer、同步后的 unpacked 配方、原始资源读取、
物品／流体标签与数量、元数据保留、网络载荷、无资源配方处理及导出权限。

运行 `verifyClient` 前，先创建 `run/client-check/saves/Vjrm UI Verification`
（也可将 `run/client/saves/New World` 复制到该位置），再执行：

```powershell
.\gradlew.bat verifyClient
.\gradlew.bat -Pviewer=emi verifyClient
.\gradlew.bat -Pviewer=both verifyClient
# 1.0.2 材料图标与类别图标检查
.\gradlew.bat -PverifyIcons verifyClient
```

客户端检查包含居中与缩放、物品栏／侧栏拖拽、复制按钮显隐与保存、包装布局、右键拒绝与回滚、数值编辑、
JSON 辅助及手动字段。EMI 检查会在关闭装饰器后操作真实配方页按钮。
结果和渲染截图输出到 `build/verification/client`；图标检查另输出到 `build/verification/icons`。

可选 Create 检查：先将 `create-1.21.1-6.0.10.jar` 放入 `build/verification/deps`，再执行：

```powershell
.\gradlew.bat -PverifyCreate verifyRecipes verifyClient
```

检查覆盖原始资源读取、字段替换、导出后重新解码、流体标签、重复原料、序列组装、大网格和代理布局。

可选 Astral Sorcery 检查：

```powershell
.\tools\prepare-astral-tests.ps1
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' verifyClient
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' -Pviewer=both verifyClient
```

测试 serializer、包装类别与测试资源仅用于开发，不进入发布 JAR；第三方模组不会随 VJRM 打包。

历史参考环境（详见 [CHANGELOG.md](CHANGELOG.md)）：

- Create：源码参考 6.0.11；运行检查使用 Create 6.0.10、JEI 19.64.0.455 和 NeoForge 21.1.251。
- Astral Sorcery 2.0.2.39：NeoForge 21.1.252、JEI 19.64.0.455、ObserverLib 1.10.3.31、Curios 9.5.1。
- Mekanism 10.4.16.80：**Forge 1.20.1** 历史检查覆盖 730 个化学品字段，结果针对该平台。
- 已提供的 NeoForge GTCEu/KubeJS 联动环境使用 NeoForge 21.1.199。

以上描述各次原始检查，并不表示对所有 1.0.2 构建重新进行了游戏运行验证。
若 NeoForge 配置端点无法访问，且已有 `build/neoforge-moddev-config.json`，可附加
`--init-script tools/local-launch-config.gradle`；仅在缓存齐全时使用 `--offline`。

</details>

## 反馈与许可

[提交问题](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/issues)时，请提供 Minecraft、
加载器、VJRM 与浏览器版本、相关模组和配方 ID、复现步骤及日志。字段映射问题尽可能附上原始 JSON。

VJRM 的模组元数据声明采用 **MIT** 许可。原始配方读取参考了 JEI Swift Copy 的思路；
配方布局和材料渲染使用相应的 JEI/EMI 与模组联动。
