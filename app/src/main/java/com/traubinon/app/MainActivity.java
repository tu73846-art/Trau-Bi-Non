package com.traubinon.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.DocumentsContract;
import android.content.ContentResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
  private static String gameLibraryJson = "[]";
  private static int selectedGameIndex = 0;
  private static final int PICK_ROM = 410;
  private static final int PICK_ROM_FOLDER = 411;
  private WebView web;
  private TextToSpeech speech;
  private boolean speechReady = false;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
    speech = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS) {
        speechReady = true;
        speech.setLanguage(new Locale("vi", "VN"));
      }
    });
    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    web.addJavascriptInterface(new Bridge(), "AndroidBridge");
    web.setWebChromeClient(new WebChromeClient());
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }

  // Route J36 Ultra hardware controls to the launcher while leaving emulator input to SDL.
  @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
    int code = event.getKeyCode();
    boolean controllerKey =
      code == android.view.KeyEvent.KEYCODE_DPAD_UP ||
      code == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
      code == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
      code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_A ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_B ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_X ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_Y ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_START ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_SELECT ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_L1 ||
      code == android.view.KeyEvent.KEYCODE_BUTTON_R1;
    if (controllerKey) {
      if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0 && web != null) {
        String action = null;
        switch (code) {
          case android.view.KeyEvent.KEYCODE_DPAD_LEFT: action = "LEFT"; break;
          case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: action = "RIGHT"; break;
          case android.view.KeyEvent.KEYCODE_DPAD_UP: action = "UP"; break;
          case android.view.KeyEvent.KEYCODE_DPAD_DOWN: action = "DOWN"; break;
          case android.view.KeyEvent.KEYCODE_BUTTON_A:
          case android.view.KeyEvent.KEYCODE_BUTTON_START: action = "CONFIRM"; break;
          case android.view.KeyEvent.KEYCODE_BUTTON_B: action = "BACK"; break;
          case android.view.KeyEvent.KEYCODE_BUTTON_X:
          case android.view.KeyEvent.KEYCODE_BUTTON_L1: action = "PREVIOUS"; break;
          case android.view.KeyEvent.KEYCODE_BUTTON_Y:
          case android.view.KeyEvent.KEYCODE_BUTTON_R1: action = "NEXT"; break;
          case android.view.KeyEvent.KEYCODE_BUTTON_SELECT: action = "MENU"; break;
        }
        if (action != null) web.evaluateJavascript("handleHardwareKey('" + action + "')", null);
      }
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  private void speakVietnamese(String text, boolean animeVoice) {
    if (speech == null || !speechReady || text == null || text.trim().isEmpty()) return;
    String clean = text.trim();
    if (clean.length() > 1800) clean = clean.substring(0, 1800);
    final String phrase = clean;
    runOnUiThread(() -> {
      if (speech == null || !speechReady) return;
      speech.stop();
      speech.setLanguage(new Locale("vi", "VN"));
      if (animeVoice) {
        speech.setPitch(1.28f);
        speech.setSpeechRate(0.92f);
        try {
          for (Voice voice : speech.getVoices()) {
            if (voice != null && voice.getLocale() != null
                && voice.getLocale().getLanguage().equals("vi")
                && voice.getName().toLowerCase(Locale.ROOT).matches(".*(female|woman|girl|nu|linh|mai).*")) {
              speech.setVoice(voice);
              break;
            }
          }
        } catch (Exception ignored) { }
      } else {
        speech.setPitch(1.0f);
        speech.setSpeechRate(1.0f);
      }
      speech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "trau-bi-non-ai");
    });
  }

  private class Bridge {
    @JavascriptInterface public void speakText(String text, boolean animeVoice) {
      speakVietnamese(text, animeVoice);
    }
    @JavascriptInterface public void stopSpeaking() {
      runOnUiThread(() -> { if (speech != null) speech.stop(); });
    }

    @JavascriptInterface public void pickRom() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_ROM);
      });
    }

    @JavascriptInterface public void pickRomFolder() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_ROM_FOLDER);
      });
    }

    // My Boy! compatibility mode runs through the bundled mGBA core.
    // It deliberately does not launch or require the separate My Boy! application.
    @JavascriptInterface public void openRomInMyBoy(String value, String quality, String fps) {
      runOnUiThread(() -> {
        if (value == null || value.trim().isEmpty()) {
          Toast.makeText(MainActivity.this, "Chưa chọn ROM để chạy.", Toast.LENGTH_LONG).show();
          return;
        }
        try {
          launchRom(Uri.parse(value), quality, fps);
        } catch (Exception e) {
          Toast.makeText(MainActivity.this, "Không mở được ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
      });
    }

    @JavascriptInterface public void copyText(String value) {
      runOnUiThread(() -> {
        android.content.ClipboardManager clipboard =
          (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
          clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AutoCheat", value));
          Toast.makeText(MainActivity.this, "Đã sao chép mã Cheat.", Toast.LENGTH_SHORT).show();
        }
      });
    }


    @JavascriptInterface public String getConnectedControllers() {
      JSONArray devices = new JSONArray();
      try {
        for (int id : android.view.InputDevice.getDeviceIds()) {
          android.view.InputDevice device = android.view.InputDevice.getDevice(id);
          if (device == null || device.isVirtual()) continue;
          int sources = device.getSources();
          boolean gamepad = (sources & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD;
          boolean joystick = (sources & android.view.InputDevice.SOURCE_CLASS_JOYSTICK) != 0;
          boolean dpad = (sources & android.view.InputDevice.SOURCE_DPAD) == android.view.InputDevice.SOURCE_DPAD;
          if (!gamepad && !joystick && !dpad) continue;
          JSONObject item = new JSONObject();
          item.put("name", device.getName()); item.put("id", id);
          item.put("type", gamepad || joystick ? "gamepad" : "dpad");
          devices.put(item);
        }
      } catch (Exception ignored) { }
      return devices.toString();
    }
    @JavascriptInterface public void searchCheats(String gameName) {
      runOnUiThread(() -> {
        try {
          String query = (gameName == null ? "" : gameName) + " GBA cheats GameShark";
          Intent i = new Intent(Intent.ACTION_VIEW,
              Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
          startActivity(i);
        } catch (Exception e) {
          Toast.makeText(MainActivity.this, "Không mở được tìm kiếm cheat.", Toast.LENGTH_LONG).show();
        }
      });
    }

    @JavascriptInterface public void saveCheats(String gameName, String content) {
      runOnUiThread(() -> {
        try {
          String base = gameName == null ? "" : gameName.trim();
          base = base.replaceAll("[^A-Za-z0-9._-]", "_");
          int dot = base.lastIndexOf('.');
          if (dot > 0) base = base.substring(0, dot);
          if (base.isEmpty()) throw new IllegalArgumentException("Tên game không hợp lệ");
          File folder = new File(getFilesDir(), "tbn_cheats");
          if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Không tạo được thư mục cheat");
          File target = new File(folder, base + ".cheats");
          try (FileOutputStream out = new FileOutputStream(target, false)) {
            out.write((content == null ? "" : content).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
          }
          Toast.makeText(MainActivity.this, "Đã lưu AutoCheat cho " + base + ". Mở lại game để áp dụng.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          Toast.makeText(MainActivity.this, "Không lưu được AutoCheat: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
      });
    }

    @JavascriptInterface public void setGameLibrary(String json, int index) {
      try {
        JSONArray list = new JSONArray(json == null ? "[]" : json);
        gameLibraryJson = list.toString();
        selectedGameIndex = Math.max(0, Math.min(index, Math.max(0, list.length() - 1)));
      } catch (Exception ignored) {
        gameLibraryJson = "[]";
        selectedGameIndex = 0;
      }
    }

    @JavascriptInterface public void openRom(String value, String quality, String fps) {
      runOnUiThread(() -> launchRom(Uri.parse(value), quality, fps));
    }
  }

  private void scanRomFolder(Uri treeUri) {
    JSONArray found = new JSONArray();
    try {
      getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      scanDocuments(treeUri, DocumentsContract.getTreeDocumentId(treeUri), found, 0);
      if (web != null) {
        String payload = found.toString();
        web.post(() -> web.evaluateJavascript("addGamesFromAndroid(" + JSONObject.quote(payload) + ")", null));
      }
      Toast.makeText(this, "Đã tìm thấy " + found.length() + " game GBA.", Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      Toast.makeText(this, "Không quét được thư mục: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void scanDocuments(Uri treeUri, String parentId, JSONArray found, int depth) throws Exception {
    if (depth > 5 || found.length() >= 1000) return;
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId);
    String[] columns = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
    try (Cursor cur = getContentResolver().query(children, columns, null, null, null)) {
      if (cur == null) return;
      while (cur.moveToNext() && found.length() < 1000) {
        String id = cur.getString(0), name = cur.getString(1);
        String mime = cur.getString(2);
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) { scanDocuments(treeUri, id, found, depth + 1); continue; }
        if (name == null) continue;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (!(lower.endsWith(".gba") || lower.endsWith(".gbc") || lower.endsWith(".gb") || lower.endsWith(".zip"))) continue;
        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
        JSONObject item = new JSONObject(); item.put("name", name); item.put("uri", fileUri.toString()); found.put(item);
      }
    }
  }

  private String displayName(Uri uri) {
    String name = null;
    try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
      if (c != null && c.moveToFirst()) {
        int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (col >= 0) name = c.getString(col);
      }
    } catch (Exception ignored) { }
    if (name == null || name.trim().isEmpty()) name = uri.getLastPathSegment();
    if (name == null || name.trim().isEmpty()) name = "game.gba";
    return name.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private void launchRom(Uri uri, String quality, String fps) {
    File folder = getExternalFilesDir("roms");
    if (folder == null) folder = new File(getFilesDir(), "roms");
    if (!folder.exists() && !folder.mkdirs()) {
      Toast.makeText(this, "Không tạo được thư mục ROM.", Toast.LENGTH_LONG).show();
      return;
    }
    String name = displayName(uri);
    File target = new File(folder, name);
    File temp = new File(folder, name + ".part");
    File backup = new File(folder, name + ".previous");
    try {
      if (temp.exists() && !temp.delete()) throw new IllegalStateException("Không dọn được tệp ROM tạm");
      try (InputStream in = getContentResolver().openInputStream(uri);
           FileOutputStream out = new FileOutputStream(temp, false)) {
        if (in == null) throw new IllegalStateException("Không đọc được ROM");
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        out.flush();
        out.getFD().sync();
      }
      if (backup.exists() && !backup.delete()) throw new IllegalStateException("Không dọn được bản ROM dự phòng");
      boolean hadTarget = target.exists();
      if (hadTarget && !target.renameTo(backup)) throw new IllegalStateException("Không thể bảo vệ ROM cũ");
      if (!temp.renameTo(target)) {
        if (hadTarget) backup.renameTo(target);
        throw new IllegalStateException("Không thể hoàn tất sao chép ROM");
      }
      if (backup.exists()) backup.delete();
      Intent game = new Intent(this, org.libsdl.app.EmulatorActivity.class);
      game.putExtra("rom_path", target.getAbsolutePath());
      game.putExtra("rom_library_json", gameLibraryJson);
      game.putExtra("rom_index", selectedGameIndex);
      String fpsValue = fps == null ? "60" : fps.replaceAll("[^0-9]", "");
      if (!"30".equals(fpsValue) && !"60".equals(fpsValue)) fpsValue = "60";
      game.putExtra("fps_target", fpsValue);
      game.putExtra("resample_video", "Lọc hình ảnh (mGBA)".equals(quality));
      startActivity(game);
    } catch (Exception e) {
      if (temp.exists()) temp.delete();
      if (backup.exists() && !target.exists()) backup.renameTo(target);
      Toast.makeText(this, "Không mở được ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  @Override protected void onDestroy() {
    if (speech != null) {
      speech.stop();
      speech.shutdown();
      speech = null;
    }
    super.onDestroy();
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_ROM_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
      scanRomFolder(data.getData());
      return;
    }
    if (requestCode == PICK_ROM && resultCode == RESULT_OK && data != null && data.getData() != null) {
      Uri uri = data.getData();
      try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
      catch (SecurityException ignored) { }
      String name = displayName(uri);
      final String safeName = org.json.JSONObject.quote(name);
      final String safeUri = org.json.JSONObject.quote(uri.toString());
      if (web != null) web.post(() -> web.evaluateJavascript(
        "addGameFromAndroid(" + safeName + "," + safeUri + ")", null));
      Toast.makeText(this, "Đã thêm ROM vào thư viện Trâu Bi Non.", Toast.LENGTH_SHORT).show();
    }
  }
}
