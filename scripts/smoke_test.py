#!/usr/bin/env python3
"""Fast source-level smoke tests for the Trâu Bi Non Android app."""
from html.parser import HTMLParser
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ScriptCollector(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_script = False
        self.parts = []
        self.current = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() == "script":
            self.in_script = True
            self.current = []

    def handle_data(self, data):
        if self.in_script:
            self.current.append(data)

    def handle_endtag(self, tag):
        if tag.lower() == "script" and self.in_script:
            self.parts.append("".join(self.current))
            self.in_script = False

class SmokeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java = (ROOT / "app/src/main/java/com/traubinon/app/MainActivity.java").read_text()
        cls.html = (ROOT / "app/src/main/assets/index.html").read_text()
        cls.manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()

    def test_myboy_bridge_checks_install_and_grants_uri(self):
        self.assertIn('MYBOY_PACKAGE = "com.fastemulator.gba"', self.java)
        self.assertIn("isMyBoyInstalled()", self.java)
        self.assertIn("getPackageInfo(MYBOY_PACKAGE", self.java)
        self.assertIn("FLAG_GRANT_READ_URI_PERMISSION", self.java)
        self.assertIn("ClipData.newUri", self.java)
        self.assertIn('setPackage(MYBOY_PACKAGE)', self.java)

    def test_manifest_declares_myboy_visibility_and_internet(self):
        self.assertIn('android:name="com.fastemulator.gba"', self.manifest)
        self.assertIn('android.permission.INTERNET', self.manifest)

    def test_ui_checks_myboy_before_rom_handoff(self):
        self.assertIn("AndroidBridge.isMyBoyInstalled()", self.html)
        self.assertIn("launchMyBoyCurrent()", self.html)
        self.assertIn("openRomInMyBoy(g.uri)", self.html)

    def test_autocheat_online_search_and_import_are_present(self):
        self.assertIn("searchCheatsOnline()", self.html)
        self.assertIn("downloadCheatFile(", self.html)
        self.assertIn("parseLibretroCheatFile(", self.html)
        self.assertIn("enabled:false", self.html)

    def test_external_gamepad_support_and_discovery(self):
        self.assertIn('getConnectedControllers()', self.java)
        self.assertIn('InputDevice.SOURCE_GAMEPAD', self.java)
        self.assertIn('InputDevice.SOURCE_CLASS_JOYSTICK', self.java)
        self.assertIn("openPanel('controller')", self.html)
        self.assertIn('refreshControllerList()', self.html)
        self.assertIn('android.hardware.usb.host', self.manifest)
    def test_inline_javascript_parses(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node.js is not installed; native build checks still run")
        parser = ScriptCollector()
        parser.feed(self.html)
        self.assertTrue(parser.parts, "No inline script blocks found")
        for index, source in enumerate(parser.parts):
            with tempfile.NamedTemporaryFile("w", suffix=".js", encoding="utf-8") as temp:
                temp.write(source)
                temp.flush()
                result = subprocess.run(
                    [node, "--check", temp.name],
                    capture_output=True, text=True
                )
            self.assertEqual(result.returncode, 0, result.stderr or f"Script {index} failed")

if __name__ == "__main__":
    unittest.main(verbosity=2)
