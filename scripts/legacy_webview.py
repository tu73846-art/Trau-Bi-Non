#!/usr/bin/env python3
import json, pathlib, re, subprocess, tempfile
path = pathlib.Path("app/src/main/assets/index.html")
html = path.read_text(encoding="utf-8")
matches = list(re.finditer(r"<script>([\s\S]*?)</script>", html, re.I))
if len(matches) != 1:
    raise SystemExit("Expected exactly one inline application script")
source = matches[0].group(1)
with tempfile.TemporaryDirectory() as td:
    td = pathlib.Path(td)
    src = td / "app.js"
    out = td / "app.compat.js"
    config = td / "babel.config.json"
    src.write_text(source, encoding="utf-8")
    subprocess.run(["npm", "install", "--prefix", str(td), "--no-audit", "--no-fund", "@babel/core", "@babel/cli", "@babel/preset-env"], check=True)
    config.write_text(json.dumps({"presets": [["@babel/preset-env", {"targets": {"chrome": "44"}}]]}), encoding="utf-8")
    babel = td / "node_modules" / ".bin" / "babel"
    subprocess.run([str(babel), str(src), "--config-file", str(config), "--out-file", str(out)], check=True)
    compiled = out.read_text(encoding="utf-8")
html = html[:matches[0].start(1)] + compiled + html[matches[0].end(1):]
path.write_text(html, encoding="utf-8")
print("Transpiled launcher JavaScript for Android 5.1 WebView (Chrome 44)")
