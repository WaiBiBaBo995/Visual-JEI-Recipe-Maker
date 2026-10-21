"""Audit and package the release built from this independent version branch.

Run the Gradle build first. This checks packaging, not in-game compatibility.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = dict(re.findall(r'^([^#\s=]+)=(.*)$', (ROOT / 'gradle.properties').read_text(encoding='utf-8'), re.M))
VERSION = PROPERTIES['mod_version']
MC = PROPERTIES['minecraft_version']
RESOURCES = ROOT / 'src/main/resources'
LOADER = 'fabric' if (RESOURCES / 'fabric.mod.json').exists() else ('forge' if (RESOURCES / 'META-INF/mods.toml').exists() else 'neoforge')
MAJOR = 61 if MC == '1.20.1' else (65 if MC == '1.21.1' else 69)
NAME = f'visual-jei-recipe-maker-{VERSION}+{MC}-{LOADER}'
CANDIDATES = [p for p in (ROOT / 'build/libs').glob(f'*{VERSION}*.jar') if not p.stem.endswith(('-dev', '-sources', '-javadoc', '-dev-shadow'))]
assert len(CANDIDATES) == 1, f'Expected one release JAR, found {CANDIDATES}'
SOURCE = CANDIDATES[0]

with zipfile.ZipFile(SOURCE) as jar:
    assert jar.testzip() is None
    names = jar.namelist()
    classes = [n for n in names if n.endswith('.class')]
    assert classes and all(struct.unpack('>H', jar.read(n)[6:8])[0] == MAJOR for n in classes), 'Incorrect Java target'
    assert not any('Checks' in n or 'Verification' in n or 'TestMachine' in n or 'PortClientEntrypoint' in n or 'PortCheckEntrypoint' in n for n in names), 'Test fixtures in release'
    assert not any(n.startswith(('mezz/', 'dev/emi/', 'mekanism/', 'data/vjrm/')) for n in names), 'Third-party code or test recipes in release'
    for required in ('RecipeEditorScreen', 'JsonDraftScreen', 'ManualFieldScreen', 'NumericFieldsScreen'):
        assert f'com/pureneko/vjrm/client/{required}.class' in names, f'Missing {required}'
    for lang, label in {'en_us':'Player inventory', 'zh_cn':'玩家物品栏', 'zh_tw':'玩家物品欄'}.items():
        data = json.loads(jar.read(f'assets/vjrm/lang/{lang}.json'))
        assert data['vjrm.inventory'] == label, (lang, data['vjrm.inventory'])
        assert 'vjrm.json.copy' in data and 'vjrm.bound' in data
    manifest = jar.read('META-INF/MANIFEST.MF').decode('utf-8')
    assert f'Implementation-Version: {VERSION}' in manifest
    if LOADER == 'fabric':
        metadata = json.loads(jar.read('fabric.mod.json'))
        assert metadata['version'] == VERSION, metadata['version']
        assert metadata['depends']['minecraft'] == MC
        assert 'fabric-api' in metadata['depends']
        assert metadata['entrypoints']['jei_mod_plugin'] == ['com.pureneko.vjrm.client.VjrmJeiPlugin']
        assert not any(n in names for n in ('META-INF/mods.toml', 'META-INF/neoforge.mods.toml'))
        assert all(b'net/neoforged/' not in jar.read(n) and b'net/minecraftforge/' not in jar.read(n) for n in classes)
        editor = jar.read('com/pureneko/vjrm/client/RecipeEditorScreen.class')
        assert (b'net/minecraft/class_310' in editor) == (MC != '26.1.2'), 'Incorrect Fabric remapping'
    else:
        descriptor = 'META-INF/mods.toml' if LOADER == 'forge' else 'META-INF/neoforge.mods.toml'
        raw = jar.read(descriptor).decode('utf-8')
        assert '${' not in raw, 'Unresolved loader metadata'
        mods = tomllib.loads(raw)['mods']
        assert len(mods) == 1 and mods[0]['modId'] == 'vjrm' and mods[0]['version'] == VERSION, mods
    if MC == '26.1.2':
        assert not any('/Emi' in n or n == 'vjrm.mixins.json' for n in names)
    else:
        assert 'com/pureneko/vjrm/client/mixin/EmiRecipeDisplayMixin.class' in names

OUT = ROOT / 'dist'
OUT.mkdir(exist_ok=True)
target = OUT / f'{NAME}.jar'
shutil.copy2(SOURCE, target)
source_zip = OUT / f'{NAME}-sources.zip'
files = [ROOT / name for name in ('.gitignore', 'build.gradle', 'gradle.properties', 'gradlew.bat', 'settings.gradle', 'README.md', 'CHANGELOG.md')]
for directory in ('src', 'gradle', 'tools', '.github'):
    files.extend(p for p in (ROOT / directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
with zipfile.ZipFile(source_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for file in sorted(set(files)):
        if file.exists():
            relative = file.relative_to(ROOT)
            assert not file.name.startswith('VALIDATION')
            assert not any(p in ('build', 'run', '.gradle', '.git', 'dist', '__pycache__') for p in relative.parts)
            assert file.suffix != '.jar' or file.name == 'gradle-wrapper.jar'
            archive.write(file, 'visual-jei-recipe-maker/' + relative.as_posix())
checksums = OUT / f'{NAME}.sha256'
checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n' for p in (target, source_zip)),encoding='ascii')
print(f'PASS: {MC} {LOADER}, {len(classes)} classes, Java class version {MAJOR}, exact metadata version {VERSION}, three inventory translations, no test/third-party code')
print(target)
