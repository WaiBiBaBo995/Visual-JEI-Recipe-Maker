# Visual JEI Recipe Maker

[![Version](https://img.shields.io/badge/version-1.0.2-blue)](CHANGELOG.md)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62b47a)](#installation)
[![Loader](https://img.shields.io/badge/loader-NeoForge-orange)](#installation)

**Create and edit Minecraft recipes in-game with JEI or EMI, then export them as datapacks or supported GTCEu scripts.**

English | [简体中文](README.zh-CN.md)

[Downloads](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/releases) ·
[Changelog](CHANGELOG.md) · [Integration guide](COMPATIBILITY.md) ·
[Report an issue](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/issues)

VJRM helps modpack authors work from familiar recipe pages: copy an existing recipe or choose a template,
drag in ingredients, adjust counts and machine values, and export the result. Item and fluid editing
preserves the surrounding conditions and extra recipe fields; changes are checked with the actual
recipe serializer before they are applied.

This branch targets **Minecraft 1.21.1 / NeoForge**. See [other versions](#other-versions) for the
matching Forge and Fabric branches.

## Contents

- [Installation](#installation)
- [Quick start](#quick-start)
- [What's new in 1.0.2](#whats-new-in-102)
- [Editing recipes](#editing-recipes)
- [Exporting and applying recipes](#exporting-and-applying-recipes)
- [Compatibility and limits](#compatibility-and-limits)
- [Troubleshooting](#troubleshooting)
- [Other versions](#other-versions)
- [Development](#development)
- [Feedback and license](#feedback-and-license)

## Installation

| Component | Requirement for this branch |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.65 or newer |
| Java | 21 |
| Client recipe browser | JEI 19.27+ **or** EMI 1.1.24+ |

1. Download the VJRM JAR matching **both your Minecraft version and loader** from
   [Releases](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/releases).
2. Put it in the instance's `mods` directory, replacing any older VJRM JAR.
3. Install the matching JEI or EMI build and its dependencies on the client.
   When both browsers are installed, VJRM uses **EMI**.
4. For multiplayer, install VJRM on the server to read original server recipes and export files there.
   The dedicated server does not need JEI or EMI.

The loader minimum above applies to VJRM. Other mods can require a newer loader: for example,
JEI 19.64.0.455 requires NeoForge 21.1.238+. This branch's base dependency versions are
JEI `19.27.0.350` and EMI `1.1.24+1.21.1`.

## Quick start

1. **Open the editor.** Enter a world and press **F8**; the key can be changed in Controls.
2. **Choose a starting recipe.** Select **Recipe Type** to search templates by name or ID,
   or use the book-shaped **Copy to VJRM Editor** button on a JEI/EMI recipe page.
3. **Replace ingredients.** Drag items or fluids from the browser sidebar into bound slots.
   You can also drag items from the inventory below. **Dragging consumes no items;
   clicking an inventory item alone does not fill a recipe slot.**
4. **Adjust the result.** Select a slot to edit its ID/tag and count, then choose **Apply Changes**.
   Use **Values** for existing numeric parameters or **Edit JSON** for the full recipe.
5. **Set a recipe ID.** For a new recipe, enter an ID such as `vjrm:my_recipe`.
6. **Export and load it.** Choose client or server export, then follow the
   [application steps](#exporting-and-applying-recipes). Exporting alone does not reload recipes.

> **Missing the copy button?** EMI shows it by default; JEI hides it on first install.
> In Controls → Visual JEI Recipe Maker, bind **Show/Hide JEI/EMI Recipe Copy Button**
> (unbound by default), then use that key to toggle visibility.

## What's new in 1.0.2

- **Item and fluid icons beside recognized IDs** in JSON and supported GTCEu JS pages,
  including IDs recognized through manual fields.
- **Category icons in recipe type search**, alongside the existing names, IDs, and tooltips.
- **Text stays intact:** icons affect only rendering. Draft text, copying, formatting, and exports
  contain no added characters; wrapping, selection, and cursor placement account for icon space.
- **Cleaner display:** stale icons clear on invalid drafts, fluid samples fill their icons,
  and inventory titles keep their local-language names without the parenthetical explanation.

Recipe IDs, type IDs, and unrelated strings do not gain icons merely because they resemble registry IDs.
See [CHANGELOG.md](CHANGELOG.md) for the full release history and platform-specific changes.

## Editing recipes

### Slots, counts, and bindings

- Drag replacement keeps the slot's existing count. A merged ingredient slot replaces the whole
  duplicate ingredient group; applying a count changes that group's multiplicity.
- Enter tags such as `#minecraft:logs` for supported inputs. Right-click removes editable crafting
  inputs, and empty crafting slots can be filled again. Required fields and the last input remain
  subject to serializer validation; invalid edits roll back.
- A **yellow border** marks a slot that needs a field mapping. Select the slot, choose the field
  above the recipe diagram, and click **Bind Slot**. Fuel, catalysts, and display-only slots are
  not exportable ingredients.
- Hover over a slot for its name, registry ID, mod, and original browser description.
  Bound slots also show their field path in **gold**. The source line identifies the real recipe
  and category; proxy pages also identify their display category.

### Values and the JSON editor

**Values** searches existing numeric fields, including time, energy, mana, experience, probability,
counts, and nested parameters. It supports large integers, decimals, scientific notation, and common
numeric strings while preserving their JSON types. The full draft is validated together;
Cancel discards changes. Add missing fields or complex structures through **Edit JSON**.

The JSON editor provides syntax/error highlighting, line and column indicators, bracket matching and
completion, automatic indentation, undo/redo, formatting, and clipboard actions.
**Copy to Clipboard** copies the current draft as-is, including unfinished drafts.

| Shortcut | Action |
| --- | --- |
| `Ctrl+S` | Apply the draft after validation |
| `Ctrl+Shift+F` | Format |
| `Ctrl+Z` / `Ctrl+Y` | Undo / redo |
| `Ctrl+A` / `Ctrl+C` / `Ctrl+V` / `Ctrl+X` | Select all / copy / paste / cut |
| `Tab` | Indent |

### Manual JSON fields

1. Select a slot and open **JSON Fields** above the diagram, or add a field first and bind it later.
2. Search the current JSON for a node, or enter a JSON Pointer such as `/required_lumen/0`.
   Array indices start at zero; escape `/` as `~1` and `~` as `~0` in object keys.
3. Choose item, fluid, optional Mekanism chemical, or **Raw JSON**, set the input/output role,
   and click **Add / Update**. For private wrapper formats, select the inner ingredient or ID node
   to preserve its surrounding structure.
4. For **Raw JSON**, enter valid JSON in the value box and apply. Quote strings; objects and arrays
   can be edited as a whole. Changes still use the recipe's serializer and roll back on failure.

Removing a manual field deletes its definition, not recipe data. Valid definitions survive refreshes,
resizing, and same-type JSON edits; changing the template or recipe type resets them.
Definitions are not included in exports.

## Exporting and applying recipes

### Datapack recipes

| Target | Exported recipe file |
| --- | --- |
| Client | `<game directory>/vjrm_exports/vjrm_generated/data/<namespace>/recipe/<path>.json` |
| Server | `<world directory>/datapacks/vjrm_generated/data/<namespace>/recipe/<path>.json` |

For example, `vjrm:my_recipe` becomes `data/vjrm/recipe/my_recipe.json` inside the generated pack.
Both targets create `pack.mcmeta` with Minecraft 1.21.1 datapack format **48**.

- **Client export:** copy the entire `vjrm_generated` folder into the target world's `datapacks`
  directory, then run `/reload` in that world.
- **Server export:** files go directly into the world's datapack directory; run `/reload` afterward.
- File overwriting is **disabled by default**. Enable it explicitly when replacing an exported file.
- Server export requires OP level **2** by default. Configure `exportPermissionLevel` (0–4) in
  `<world directory>/serverconfig/vjrm-server.toml`. Reading original recipes does not require OP.
- Original JSON reading is limited to **240,000 characters**; server export payloads are limited to
  **24,000 characters**. Use client export if a draft exceeds the server export limit.

### GTCEu scripts

Supported GTCEu recipes use JS export instead of datapacks. Install matching **GTCEu, KubeJS, and
their dependencies** separately. The recipe page provides **Edit JS**, **JS Fields**, **Add Input**,
**Add Output**, and client/server script export. Inputs and outputs support per-recipe and per-tick
channels within the machine's slot limits; **Values** edits duration, EU/t, chance, and other existing
numbers. Runtime GT recipes do not need a source datapack JSON file.

Scripts go to `kubejs/server_scripts/vjrm/<namespace>/<recipe-path>.js` relative to the client or server
installation root. For example, `vjrm:chemical_reactor/test` maps to
`kubejs/server_scripts/vjrm/vjrm/chemical_reactor/test.js`. Restart or reload KubeJS recipes to apply them.

The generated `ServerEvents.recipes` declaration uses GTCEu's machine recipe entry and `.merge({...})`
to retain recipe data. Keep that declaration structure for visual round-tripping; the editor parses
it without executing JS. Add arbitrary functions or loops in an external editor after export.
Server exports retain permission, rate, path, validation, and overwrite checks.
See [the integration guide](COMPATIBILITY.md) for details.

## Compatibility and limits

| Integration | Support and boundaries |
| --- | --- |
| JEI / EMI | Native recipe layouts; EMI is preferred when both are installed. EMI 1.1.24's JEMI needs JEI API/runtime for pages provided only through JEI. |
| Create | Datapack processing, existing mechanical crafting slots (including grids larger than 3×3), and sequenced assembly. Edit shapes, new steps, transitional items, and extra result pools through JSON. |
| Mekanism | Optional item/fluid and chemical editing, including tags and long quantities. The 1.21 integration uses unified chemicals; no Mekanism API is loaded when absent. |
| Astral Sorcery | Resolves raw recipe objects to source IDs; manual fields support private structures such as Lumen. EMI/JEMI pages with only JEI integration also require JEI. |
| GTCEu / KubeJS | Supported on NeoForge 1.21.1 and Forge 1.20.1; exports JS. See [COMPATIBILITY.md](COMPATIBILITY.md). |
| Thaumaturge | Available only on the **NeoForge 26.1.2** branch. See [COMPATIBILITY.md](COMPATIBILITY.md). |

VJRM reads the **server's currently active datapack JSON** by real recipe ID, preserving conditions
and extra fields instead of re-encoding client-synchronized recipes. Third-party templates use existing
recipes. Create display categories such as `packing` can be proxies; exports follow the real source
recipe type and behavior. A proxy page with multiple stonecutting results edits only the selected ID.

Automatic mapping depends on the mod's format. Ambiguous fields need manual binding; unknown structures
can be edited as JSON. Recipes without a loaded datapack resource are skipped for datapack editing;
supported GTCEu runtime recipes use the separate JS workflow above.

Rebuildable pages refresh from the edited recipe. Other third-party wrapper layouts update bound slots
while dynamic text, charts, and layout remain tied to the original display object; the UI shows a notice.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Copy button missing | Enable it with the Controls toggle above; JEI defaults to hidden. A usable source recipe must also be available. |
| JEI sidebar missing | Toggle JEI's own sidebar with `Ctrl+O`. |
| Slot will not accept edits | Bind yellow slots manually. Display-only slots and serializer-required inputs cannot be freely removed. |
| Original recipe unavailable in multiplayer | Install VJRM on the server. Without it, vanilla templates and client export remain available. |
| Export did not change the game | Install the generated datapack and run `/reload`, or reload/restart KubeJS for JS recipes. |
| Server export rejected | Check permissions, payload size, and overwrite setting; wait before retrying rapid exports. |

Copy-button settings persist in `config/vjrm-client.toml`: `showJeiCopyButton` and `showEmiCopyButton`.

## Other versions

Each branch is an independent project. Choose a JAR matching both Minecraft and the loader.
The 26.1.2 branches support **JEI only**; integration support varies by branch.

| Minecraft | Loader | Branch | Game Java |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | [main](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/main) | 21 |
| 1.20.1 | Forge | [forge-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/forge-1.20.1) | 17 |
| 1.20.1 | Fabric | [fabric-1.20.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.20.1) | 17 |
| 1.21.1 | Fabric | [fabric-1.21.1](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-1.21.1) | 21 |
| 26.1.2 | Fabric | [fabric-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/fabric-26.1.2) | 25 |
| 26.1.2 | NeoForge | [neoforge-26.1.2](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/tree/neoforge-26.1.2) | 25 |

## Development

Build and launch this branch with Java 21 and the included Gradle Wrapper:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

<details>
<summary>Verification tasks, optional integrations, and historical reference environments</summary>

```powershell
.\gradlew.bat verifyRecipes
# Development clients: EMI only / JEI and EMI together
.\gradlew.bat -Pviewer=emi runClient
.\gradlew.bat -Pviewer=both runClient
```

`verifyRecipes` starts an isolated GameTest server covering serializers, unpacked synchronized recipes,
original resource reading, item/fluid tags and counts, metadata preservation, network payloads,
missing-resource handling, and export permissions.

For `verifyClient`, first create `run/client-check/saves/Vjrm UI Verification`
(or copy `run/client/saves/New World` there), then run:

```powershell
.\gradlew.bat verifyClient
.\gradlew.bat -Pviewer=emi verifyClient
.\gradlew.bat -Pviewer=both verifyClient
# 1.0.2 ingredient and category icon checks
.\gradlew.bat -PverifyIcons verifyClient
```

Client checks cover centering and resizing, inventory/sidebar dragging, copy-button visibility and
persistence, wrapper layouts, right-click rejection and rollback, numeric editing, JSON assistance,
and manual fields. EMI checks exercise actual recipe-page buttons with decorators disabled.
Results and rendered screenshots are written to `build/verification/client`;
icon checks also write to `build/verification/icons`.

For optional Create checks, place `create-1.21.1-6.0.10.jar` in `build/verification/deps`, then run:

```powershell
.\gradlew.bat -PverifyCreate verifyRecipes verifyClient
```

These checks cover original resource reading, field replacement, export/re-decoding, fluid tags,
duplicate ingredients, sequenced assembly, large grids, and proxy layouts.

For optional Astral Sorcery checks:

```powershell
.\tools\prepare-astral-tests.ps1
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' verifyClient
.\gradlew.bat -PverifyAstral '-Pneo_version=21.1.252' '-Pjei_version=19.64.0.455' -Pviewer=both verifyClient
```

Test serializers, wrapper categories, and resources are development-only and excluded from release
JARs; third-party mods are not bundled.

Historical reference environments (see [CHANGELOG.md](CHANGELOG.md)):

- Create: source reference 6.0.11; runtime checks used Create 6.0.10, JEI 19.64.0.455,
  and NeoForge 21.1.251.
- Astral Sorcery 2.0.2.39: NeoForge 21.1.252, JEI 19.64.0.455, ObserverLib 1.10.3.31,
  and Curios 9.5.1.
- Mekanism 10.4.16.80: the **Forge 1.20.1** checks covered 730 chemical fields.
  These are historical results for that platform.
- The supplied NeoForge GTCEu/KubeJS integration environment used NeoForge 21.1.199.

These references describe their original checks, not a new runtime verification of every 1.0.2 build.
If the NeoForge configuration endpoint is unavailable and `build/neoforge-moddev-config.json` exists,
append `--init-script tools/local-launch-config.gradle`. Use `--offline` only with a complete cache.

</details>

## Feedback and license

[Open an issue](https://github.com/WaiBiBaBo995/Visual-JEI-Recipe-Maker/issues) with the Minecraft,
loader, VJRM, and browser versions, the affected mod and recipe ID, reproduction steps, and relevant logs.
For field-mapping problems, include the source JSON when possible.

VJRM's mod metadata declares **MIT**. Resource-reading behavior was informed by JEI Swift Copy;
recipe layouts and ingredient rendering use the corresponding JEI/EMI and mod integrations.
