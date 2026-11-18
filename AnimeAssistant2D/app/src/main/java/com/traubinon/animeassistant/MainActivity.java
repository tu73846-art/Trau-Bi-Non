package com.traubinon.animeassistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1); root.setPadding(28,36,28,28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFF101426);
        TextView title = new TextView(this);
        title.setText("TRÂU BI NON • ANIME ASSISTANT 2D");
        title.setTextColor(0xFFFFFFFF); title.setTextSize(21); title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));
        TextView desc = new TextView(this);
        desc.setText("\\nTương thích mục tiêu Android 5.0 trở lên.\\n\\nTrợ lý anime nổi trên màn hình khi bạn chơi game.\\n\\n1. Cấp quyền hiển thị trên ứng dụng khác (Android 6+).\\n2. Bật trợ lý nổi.\\n3. Mở My Boy và chạm bong bóng để trò chuyện.");
        desc.setTextColor(0xFFE1E5F5); desc.setTextSize(16);
        root.addView(desc);
        Button permission = new Button(this); permission.setText("CẤP QUYỀN HIỂN THỊ");
        permission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
                startActivity(i);
            } else {
                Toast.makeText(this,"Thiết bị Android 5.x: hãy bật quyền cửa sổ nổi trong cài đặt nếu hệ thống yêu cầu.",Toast.LENGTH_LONG).show();
            }
        });
        root.addView(permission);
        Button start = new Button(this); start.setText("BẬT TRỢ LÝ NỔI");
        start.setOnClickListener(v -> {
            if (hasOverlayPermission()) startService(new Intent(this, OverlayService.class));
            else Toast.makeText(this,"Hãy cấp quyền hiển thị trước",Toast.LENGTH_LONG).show();
        });
        root.addView(start);
        Button stop = new Button(this); stop.setText("TẮT TRỢ LÝ");
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        root.addView(stop);
        setContentView(root);
    }
}
