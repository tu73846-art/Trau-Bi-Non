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
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
    WebView web = new WebView(this);
    WebSettings s = web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true);
    web.addJavascriptInterface(new Bridge(), "AndroidBridge"); web.setWebChromeClient(new WebChromeClient());
    setContentView(web); web.loadUrl("file:///android_asset/index.html");
  }
  private class Bridge {
    @JavascriptInterface public void pickRom() { runOnUiThread(() -> { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivityForResult(i,PICK_ROM); }); }
    @JavascriptInterface public void openMyBoy() { runOnUiThread(() -> { Intent i=getPackageManager().getLaunchIntentForPackage(MYBOY_PACKAGE); if(i!=null) startActivity(i); else Toast.makeText(MainActivity.this,"Chưa tìm thấy My Boy!",Toast.LENGTH_LONG).show(); }); }
  }
  @Override protected void onActivityResult(int r,int c,Intent d) { super.onActivityResult(r,c,d); if(r==PICK_ROM&&c==RESULT_OK&&d!=null&&d.getData()!=null){ Uri u=d.getData(); Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(u,"*/*"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); i.setPackage(MYBOY_PACKAGE); try{startActivity(i);}catch(ActivityNotFoundException e){i.setPackage(null); try{startActivity(Intent.createChooser(i,"Mở ROM bằng giả lập"));}catch(Exception ignored){Toast.makeText(this,"Không tìm thấy ứng dụng mở ROM",Toast.LENGTH_LONG).show();}} } }
}
