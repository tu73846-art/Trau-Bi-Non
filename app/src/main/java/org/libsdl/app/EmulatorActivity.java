package org.libsdl.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/** Native mGBA game surface with hardware-button game switching. */
public final class EmulatorActivity extends SDLActivity {
    private String romPath;
    private JSONArray romLibrary;
    private int romIndex;
    private boolean switching;

    @Override
    protected void onCreate(Bundle state) {
        romPath = getIntent().getStringExtra("rom_path");
        try { romLibrary = new JSONArray(getIntent().getStringExtra("rom_library_json") == null ? "[]" : getIntent().getStringExtra("rom_library_json")); }
        catch (Exception ignored) { romLibrary = new JSONArray(); }
        romIndex = getIntent().getIntExtra("rom_index", 0);
        super.onCreate(state);
    }

    @Override
    protected String[] getArguments() {
        if (romPath == null) return new String[0];
        String fps = getIntent().getStringExtra("fps_target");
        if (fps == null || (!fps.equals("30") && !fps.equals("60"))) fps = "60";
        boolean resample = getIntent().getBooleanExtra("resample_video", false);
        ArrayList<String> args = new ArrayList<>();
        args.add("-C"); args.add("fpsTarget=" + fps);
        args.add("-C"); args.add("resampleVideo=" + (resample ? "1" : "0"));
        args.add(romPath);
        return args.toArray(new String[0]);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_BUTTON_R1) {
                switchGame(1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_BUTTON_L1) {
                switchGame(-1);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void switchGame(int direction) {
        if (switching || romLibrary == null || romLibrary.length() < 2) {
            Toast.makeText(this, "Hãy thêm ít nhất 2 game để chuyển bằng nút vật lý.", Toast.LENGTH_SHORT).show();
            return;
        }
        switching = true;
        int next = (romIndex + direction + romLibrary.length()) % romLibrary.length();
        try {
            JSONObject item = romLibrary.getJSONObject(next);
            String uriText = item.optString("uri", "");
            String name = item.optString("name", "game.gba").replaceAll("[^A-Za-z0-9._-]", "_");
            if (uriText.isEmpty()) throw new IllegalArgumentException("Thiếu đường dẫn ROM");
            File folder = getExternalFilesDir("roms");
            if (folder == null) folder = new File(getFilesDir(), "roms");
            if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Không tạo được thư mục ROM");
            File target = new File(folder, name);
            File temp = new File(folder, name + ".switching");
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(uriText));
                 FileOutputStream out = new FileOutputStream(temp, false)) {
                if (in == null) throw new IllegalStateException("Không đọc được ROM");
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.flush(); out.getFD().sync();
            }
            if (target.exists() && !target.delete()) throw new IllegalStateException("Không thay được ROM cũ");
            if (!temp.renameTo(target)) throw new IllegalStateException("Không lưu được ROM mới");
            Intent nextGame = new Intent(this, EmulatorActivity.class);
            nextGame.putExtra("rom_path", target.getAbsolutePath());
            nextGame.putExtra("rom_library_json", romLibrary.toString());
            nextGame.putExtra("rom_index", next);
            nextGame.putExtra("fps_target", getIntent().getStringExtra("fps_target"));
            nextGame.putExtra("resample_video", getIntent().getBooleanExtra("resample_video", false));
            startActivity(nextGame);
            finish();
        } catch (Exception e) {
            switching = false;
            Toast.makeText(this, "Không chuyển được game: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
