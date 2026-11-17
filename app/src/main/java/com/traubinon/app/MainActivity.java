package com.traubinon.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.widget.Toast;

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
        if (i != null) startActivity(i);
        else Toast.makeText(MainActivity.this, "Chưa tìm thấy My Boy! Hãy cài My Boy trước.", Toast.LENGTH_LONG).show();
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

    @JavascriptInterface public void openRom(String value) {
      runOnUiThread(() -> launchRom(Uri.parse(value)));
    }
  }

  private void launchRom(Uri uri) {
    Intent i = new Intent(Intent.ACTION_VIEW);
    i.setDataAndType(uri, "*/*");
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    i.setPackage(MYBOY_PACKAGE);
    try {
      startActivity(i);
    } catch (ActivityNotFoundException e) {
      i.setPackage(null);
      try {
        startActivity(Intent.createChooser(i, "Mở ROM bằng giả lập"));
      } catch (Exception ignored) {
        Toast.makeText(this, "Không tìm thấy ứng dụng hỗ trợ mở ROM.", Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_ROM && resultCode == RESULT_OK && data != null && data.getData() != null) {
      Uri uri = data.getData();
      try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
      catch (SecurityException ignored) { }
      String name = uri.getLastPathSegment();
      if (name == null || name.trim().isEmpty()) name = "ROM đã chọn";
      final String safeName = org.json.JSONObject.quote(name);
      final String safeUri = org.json.JSONObject.quote(uri.toString());
      if (web != null) web.post(() -> web.evaluateJavascript(
        "addGameFromAndroid(" + safeName + "," + safeUri + ")", null));
      Toast.makeText(this, "Đã thêm ROM vào thư viện Trâu Bi Non.", Toast.LENGTH_SHORT).show();
    }
  }
}