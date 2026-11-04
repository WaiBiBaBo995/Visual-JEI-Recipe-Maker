# Visual JEI Recipe Maker

[![版本](https://img.shields.io/badge/version-1.0.5-blue)](CHANGELOG.md)
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
- [1.0.5 更新亮点](#105-更新亮点)
- [编辑配方](#编辑配方)
- [导出与生效](#导出与生效)
- [配方与数据包管理](#配方与数据包管理)
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

## 1.0.5 更新亮点

- 支持本地／世界数据包管理和原配方禁用，不改动原始文件。
- 双击配方编辑，支持名称弹窗、逐级返回与数据包名称补全。
- 支持备份保留天数配置与 ZIP 导出，六个工程统一为 **1.0.5**。

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

## 编辑配方

### 槽位、数量与绑定

- 拖拽替换保留槽位原有数量；合并原料槽会替换整组重复材料，「应用修改」的数量可调整该组的重复次数。
- 支持标签的字段可填写 `#minecraft:logs` 等标签，包括沉浸工程 `results/0/basePredicate` 这类输出谓词；输入物品 ID 时保留 `item` 结构，数量仍在外层 `count`。普通 `id` 物品堆栈输出使用物品 ID。右键移除可编辑的工作台输入，空工作台槽位可重新拖入材料。
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

手动字段和槽位绑定会自动保存到客户端实例的 `config/vjrm/field_profiles.json`，
重启后、跨存档和服务器均可复用。字段按配方类型恢复；槽位绑定还会核对 JEI／EMI、类别及槽位布局。
当前配方缺少的路径或不匹配的布局会跳过，保留配置供其他配方使用。
在 JSON 字段界面点击“重置此类型”可清除此类型保存的字段和绑定。
移除或重置字段定义不会修改配方 JSON，导出时也不会写入这些定义。

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
# 1.0.4 材料图标与类别图标检查
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

以上描述各次原始检查，并不表示对所有 1.0.4 构建重新进行了游戏运行验证。
若 NeoForge 配置端点无法访问，且已有 `build/neoforge-moddev-config.json`，可附加
`--init-script tools/local-launch-config.gradle`；仅在缓存齐全时使用 `--offline`。

</details>

## 反馈与许可

[提交问题](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/issues)时，请提供 Minecraft、
加载器、VJRM 与浏览器版本、相关模组和配方 ID、复现步骤及日志。字段映射问题尽可能附上原始 JSON。

VJRM 采用 [MIT 许可](LICENSE)。原始配方读取参考了 JEI Swift Copy 的思路；
配方布局和材料渲染使用相应的 JEI/EMI 与模组联动。
