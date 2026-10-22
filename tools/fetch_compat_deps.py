"""Fetch pinned, optional integration-test dependencies; never package them in VJRM."""
from pathlib import Path
import hashlib
import json
import shutil
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'build/compat-deps'
DEST.mkdir(parents=True, exist_ok=True)

for entry in json.loads((ROOT / 'tools/compat_dependencies.json').read_text(encoding='utf-8')):
    target = DEST / entry['name']
    if not target.exists() or hashlib.sha256(target.read_bytes()).hexdigest() != entry['sha256']:
        temporary = target.with_suffix('.download')
        print('Downloading', entry['name'], flush=True)
        urllib.request.urlretrieve(entry['url'], temporary)
        if hashlib.sha256(temporary.read_bytes()).hexdigest() != entry['sha256']:
            raise RuntimeError(f'Checksum mismatch: {entry["name"]}')
        temporary.replace(target)
    print('Verified', entry['name'])

# ForgeGradle's flat-directory deobfuscation resolver uses Maven-style artifact names.
for name in ('gt20', '20-kubejs', '20-rhino', '20-architectury-api'):
    shutil.copy2(DEST / f'{name}.jar', DEST / f'{name}-1.jar')
