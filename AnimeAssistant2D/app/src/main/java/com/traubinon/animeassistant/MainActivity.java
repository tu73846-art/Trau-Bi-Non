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
        desc.setText("\nTrợ lý nổi có thể tiếp tục chạy khi bạn chuyển sang ứng dụng hoặc trò chơi khác.\n\n1. Cấp quyền hiển thị trên ứng dụng khác.\n2. Bấm BẬT TRỢ LÝ NỔI.\n3. Chuyển sang game; bong bóng ✦ sẽ vẫn hiển thị.\n4. Dùng thông báo “Anime Assistant đang chạy” để biết dịch vụ còn hoạt động.");
        desc.setTextColor(0xFFE1E5F5); desc.setTextSize(16);
        root.addView(desc);
        Button permission = new Button(this); permission.setText("CẤP QUYỀN HIỂN THỊ");
        permission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
                startActivity(i);
            } else {
                Toast.makeText(this,"Đã có quyền hiển thị cửa sổ nổi.",Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(permission);
        Button start = new Button(this); start.setText("BẬT TRỢ LÝ NỔI / CHẠY NỀN");
        start.setOnClickListener(v -> {
            if (!hasOverlayPermission()) {
                Toast.makeText(this,"Hãy cấp quyền hiển thị trước",Toast.LENGTH_LONG).show();
                return;
            }
            Intent service = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            Toast.makeText(this,"Trợ lý đang chạy nền",Toast.LENGTH_SHORT).show();
        });
        root.addView(start);
        Button stop = new Button(this); stop.setText("TẮT TRỢ LÝ");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this,"Đã tắt trợ lý nổi",Toast.LENGTH_SHORT).show();
        });
        root.addView(stop);
        setContentView(root);
    }
}
