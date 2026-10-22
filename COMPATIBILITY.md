# GTCEu / Thaumaturge Compatibility -English-

## GTCEu / KubeJS

Available on **Forge 1.20.1 and NeoForge 1.21.1**. Install the matching GTCEu, KubeJS, and their dependencies separately; they are not bundled. Newer KubeJS versions can require a newer loader than VJRM itself. The supplied NeoForge integration environment used NeoForge 21.1.199.

Copy a machine recipe from JEI/EMI or choose its template in **Recipe Type**. Runtime GT recipes need no datapack JSON. GT pages provide **Edit JS**, **JS Fields**, **Add Input**, **Add Output**, and client/server script export. Add items/fluids with counts and per-recipe or per-tick channels, subject to machine slot limits; right-click removes bound entries. **Values** edits duration in ticks, EU/t in `tickInputs`, chance, and other existing fields. Replacing materials retains surrounding chance/condition data.

The generated declaration uses `ServerEvents.recipes(event => { event.recipes["gtceu"]["machine_type"]("namespace:name").merge({...}); });`, preserving complete recipe data. Visual round-tripping accepts comments and edits to the generated ID/type/object but does not execute JS. Keep the declaration structure; add arbitrary functions/loops in an external editor after export. The JS editor supports highlighting, line numbers, completion, indentation, undo/redo, formatting, selection/clipboard operations, errors, Ctrl+S to apply, and Ctrl+Shift+F to format.

Scripts are written relative to the client/server installation root at `kubejs/server_scripts/vjrm/<namespace>/<recipe-path>.js`. For example, `vjrm:chemical_reactor/test` maps to `kubejs/server_scripts/vjrm/vjrm/chemical_reactor/test.js`. Machine-type ID prefixes are normalized to avoid duplication. Server exports still check permissions, frequency, decoding, path scope, and explicit overwriting. Restart or reload KubeJS recipes to apply exported scripts. Ordinary recipes retain JSON/datapack export. See [COMPATIBILITY.md](COMPATIBILITY.md).

## Thaumaturge

Available on **NeoForge 26.1.2** with JEI 29.x. Install Thaumaturge and its dependencies separately. Each `{ "aspect": "thaumaturge:ignis", "amount": 10 }` in `aspects` becomes an independent aspect field resolved through public codecs and the active datapack registry, including additional aspects. Drag JEI aspects, edit IDs and positive counts, or choose **Aspect** in manual fields. Thaumaturge still renders native icons, names, and research rules; tooltips append IDs, mod names, and gold bindings.

`components` arrays map to independent input slots. Duplicate materials bind by array position; alternatives within one entry remain one slot. Data component ingredient conditions are retained as whole ingredients, preventing inner aspect components from becoming extra slots. Replacements update the selected ingredient condition while retaining other recipe data. JSON editing/highlighting/copying and datapack export remain available. See [COMPATIBILITY.md](COMPATIBILITY.md).

# GTCEu / Thaumaturge 兼容 -中文-


当前六个工程版本均为 1.0.2。下述 GTCEu/KubeJS 与 Thaumaturge 兼容于 0.8.0 引入，仍仅适用于列明的平台；Fabric 此次同步编辑器图标和类别图标。

## GTCEu + KubeJS（1.20.1 Forge / 1.21.1 NeoForge）

安装对应游戏版本的 GTCEu、KubeJS 及它们自己的前置。VJRM 仍支持只安装 JEI 或 EMI；不会将 GTCEu、KubeJS 打包进 VJRM。使用较新的 KubeJS 时，加载器最低版本由该 KubeJS 版本决定，例如本次 1.21.1 联动环境使用 NeoForge 21.1.199。

从配方浏览器复制机器配方，或在「配方类型」中选择机器配方模板。运行时生成的 GT 配方也可以作为来源，无需对应的数据包 JSON 文件。普通配方继续使用 JSON 和原来的数据包导出。

- GT 机器配方显示「编辑 JS」「JS 字段」「添加输入」「添加输出」和两个脚本导出按钮。
- 新增材料时选择物品/流体、数量和每次配方/每 tick；机器类型的槽位容量仍然有效。右键移除已绑定的 GT 材料条目。
- 「数值」可以编辑 `duration`、`tickInputs` 中的 EU/t、概率及其他已有数值。运行时间以 tick 计。材料替换保留外层概率、条件和其他配方数据。
- JS 使用 GTCEu 注册的 KubeJS 机器配方入口：`ServerEvents.recipes(event => { event.recipes["gtceu"]["机器类型"]("命名空间:名称").merge({...}); });`。`merge` 中保留完整配方数据，包含物品、流体、每 tick 输入输出、条件、数据和概率信息。
- 可视化回填支持编辑这段声明中的 ID、类型和配方对象，也支持注释。请保留生成的声明结构；任意循环、函数或额外脚本逻辑可在导出后用外部编辑器添加。VJRM 的编辑器不会执行脚本。
- JS 页支持高亮、行号、成对括号/引号、缩进、撤销/重做、全选、剪切/复制/粘贴、格式化、错误提示及「复制到剪贴板」。Ctrl+S 应用，Ctrl+Shift+F 格式化，Ctrl+Z / Ctrl+Y 撤销和重做。

脚本导出位置：

| 目标 | 位置 |
| --- | --- |
| 客户端 | `<客户端根目录>/kubejs/server_scripts/vjrm/<命名空间>/<配方路径>.js` |
| 服务端 | `<服务端根目录>/kubejs/server_scripts/vjrm/<命名空间>/<配方路径>.js` |

例如 `vjrm:chemical_reactor/test` 对应 `kubejs/server_scripts/vjrm/vjrm/chemical_reactor/test.js`。命名空间使用目录表示，避免 Windows 文件名中的冒号。GT 的 KubeJS 联动会给实际配方 ID 添加机器类型前缀，编辑器会保留/规范化此前缀，避免重复添加。

服务端导出继续检查 `exportPermissionLevel`、导出频率、配方解码和路径范围。「允许覆盖」控制同名文件；取消编辑不会修改文件。导出的脚本在重启或 KubeJS 配方重载后生效。

## Thaumaturge（26.1.2 NeoForge）

安装 Thaumaturge 及其前置，使用 JEI 29.x。26.1.2 不包含 EMI 兼容代码。

- 将 `aspects` 中的每个 `{ "aspect": "thaumaturge:ignis", "amount": 10 }` 识别为独立的「源质」字段，使用模组公开 codec 和当前数据包注册表解析。
- 可以拖入 JEI 源质、输入源质 ID、修改正整数数量，并从手动字段页选择「源质」。原生 JEI 图标、名称和研究解锁规则仍由 Thaumaturge 渲染；提示补充 ID、模组名和金色绑定字段。
- `components` 数组逐项对应多个材料槽，重复材料按数组位置独立绑定，修改一个不会更改另一个；其中单个条目的可选材料仍属于一个槽。带 `neoforge:ingredient_type` 的数据组件材料条件也作为完整材料处理，避免把内部的源质组件误识别为额外槽位。
- 替换此类材料时替换该条目的材料条件，保留配方其余字段；继续使用 JSON 编辑、高亮、剪贴板和数据包导出。

更新记录统一见 [CHANGELOG.md](CHANGELOG.md)。
