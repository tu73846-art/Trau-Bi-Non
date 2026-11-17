package com.traubinon.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
  private static final int PICK_ROM = 410;
  private static final String MYBOY_PACKAGE = "com.fastemulator.gba";
  private WebView web;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
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

  private class Bridge {
    @JavascriptInterface public void pickRom() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_ROM);
      });
    }

    @JavascriptInterface public void openMyBoy() {
      runOnUiThread(() -> {
        Intent i = getPackageManager().getLaunchIntentForPackage(MYBOY_PACKAGE);
        if (i != null) {
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(i);
        } else Toast.makeText(MainActivity.this, "Chưa tìm thấy My Boy! Hãy cài My Boy trước.", Toast.LENGTH_LONG).show();
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

    @JavascriptInterface public void openRom(String value) {
      runOnUiThread(() -> launchRom(Uri.parse(value)));
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

  private void launchRom(Uri uri) {
    File folder = getExternalFilesDir("roms");
    if (folder == null) folder = new File(getFilesDir(), "roms");
    if (!folder.exists() && !folder.mkdirs()) {
      Toast.makeText(this, "Không tạo được thư mục ROM.", Toast.LENGTH_LONG).show();
      return;
    }
    String name = displayName(uri);
    File target = new File(folder, name);
    try (InputStream in = getContentResolver().openInputStream(uri);
         FileOutputStream out = new FileOutputStream(target)) {
      if (in == null) throw new IllegalStateException("Không đọc được ROM");
      byte[] buffer = new byte[8192];
      int count;
      while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
      out.flush();
      Intent game = new Intent(this, org.libsdl.app.EmulatorActivity.class);
      game.putExtra("rom_path", target.getAbsolutePath());
      startActivity(game);
    } catch (Exception e) {
      Toast.makeText(this, "Không mở được ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
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
