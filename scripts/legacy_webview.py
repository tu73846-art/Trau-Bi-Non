#!/usr/bin/env python3
import pathlib, re, subprocess, tempfile, sys
path = pathlib.Path("app/src/main/assets/index.html")
html = path.read_text(encoding="utf-8")
matches = list(re.finditer(r"<script>([\s\S]*?)</script>", html, re.I))
if len(matches) != 1:
    raise SystemExit("Expected exactly one inline application script")
source = matches[0].group(1)
with tempfile.TemporaryDirectory() as td:
    src = pathlib.Path(td) / "app.js"
    out = pathlib.Path(td) / "app.compat.js"
    src.write_text(source, encoding="utf-8")
    subprocess.run(["npx", "--yes", "esbuild", str(src), "--target=chrome44", "--charset=utf8", "--outfile=" + str(out)], check=True)
    compiled = out.read_text(encoding="utf-8")
html = html[:matches[0].start(1)] + compiled + html[matches[0].end(1):]
path.write_text(html, encoding="utf-8")
print("Transpiled launcher JavaScript for Android 5.1 WebView (Chrome 44)")
