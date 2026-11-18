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

    def test_myboy_mode_uses_embedded_mgba_without_external_app(self):
        self.assertIn('openRomInMyBoy(String value, String quality, String fps)', self.java)
        self.assertIn('launchRom(Uri.parse(value), quality, fps)', self.java)
        self.assertNotIn('MYBOY_PACKAGE', self.java)
        self.assertNotIn('isMyBoyInstalled()', self.java)
        self.assertIn('emulatorMode', self.html)
        self.assertIn("selectEmulator('myboy')", self.html)
        self.assertIn('không cần cài ứng dụng ngoài', self.html)

    def test_manifest_does_not_require_external_myboy(self):
        self.assertNotIn('com.fastemulator.gba', self.manifest)
        self.assertIn('android.permission.INTERNET', self.manifest)

    def test_myboy_ui_routes_rom_to_internal_emulator(self):
        self.assertRegex(self.html, r'AndroidBridge\\.openRomInMyBoy\\(g\\.uri,\\s*quality,\\s*fps\\)')
        self.assertIn('MY BOY! · TƯƠNG THÍCH', self.html)
        self.assertNotIn('AndroidBridge.isMyBoyInstalled()', self.html)

    def test_autocheat_online_search_and_import_are_present(self):
        self.assertIn("searchCheatsOnline()", self.html)
        self.assertIn("downloadCheatFile(", self.html)
        self.assertIn("parseLibretroCheatFile(", self.html)
        self.assertRegex(self.html, r"enabled\\s*:\\s*false")

    def test_external_gamepad_support_and_discovery(self):
        self.assertIn('getConnectedControllers()', self.java)
        self.assertIn('InputDevice.SOURCE_GAMEPAD', self.java)
        self.assertIn('InputDevice.SOURCE_CLASS_JOYSTICK', self.java)
        self.assertIn("openPanel('controller')", self.html)
        self.assertIn('refreshControllerList()', self.html)
        self.assertIn('android.hardware.usb.host', self.manifest)
    def test_physical_buttons_switch_games(self):
        emulator = (ROOT / "app/src/main/java/org/libsdl/app/EmulatorActivity.java").read_text()
        self.assertIn("KEYCODE_VOLUME_UP", emulator)
        self.assertIn("KEYCODE_VOLUME_DOWN", emulator)
        self.assertIn("KEYCODE_BUTTON_R1", emulator)
        self.assertIn("KEYCODE_BUTTON_L1", emulator)
        self.assertIn("rom_library_json", self.java)
        self.assertRegex(self.html, r"setGameLibrary\\(JSON\\.stringify\\(games\\),\\s*index\\)")

    def test_automatic_game_text_translation_overlay(self):
        emulator = (ROOT / "app/src/main/java/org/libsdl/app/EmulatorActivity.java").read_text()
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("PixelCopy.request", emulator)
        self.assertIn("LanguageIdentification", emulator)
        self.assertIn("Translation.getClient", emulator)
        self.assertIn("TranslateLanguage.VIETNAMESE", emulator)
        self.assertIn("DỊCH: BẬT", emulator)
        self.assertIn("com.google.mlkit:translate", gradle)
        self.assertIn("text-recognition-japanese", gradle)
        self.assertIn("text-recognition-chinese", gradle)

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
