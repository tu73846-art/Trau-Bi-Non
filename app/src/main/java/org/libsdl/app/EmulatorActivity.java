package org.libsdl.app;

import android.os.Bundle;
import java.util.ArrayList;

/** Native mGBA game surface launched by the Trâu Bi Non ROM library. */
public final class EmulatorActivity extends SDLActivity {
    private String romPath;

    @Override
    protected void onCreate(Bundle state) {
        romPath = getIntent().getStringExtra("rom_path");
        super.onCreate(state);
    }

    @Override
    protected String[] getArguments() {
        if (romPath == null) return new String[0];
        String fps = getIntent().getStringExtra("fps_target");
        if (fps == null || (!fps.equals("30") && !fps.equals("60"))) fps = "60";
        boolean resample = getIntent().getBooleanExtra("resample_video", false);
        ArrayList<String> args = new ArrayList<>();
        args.add("-C");
        args.add("fpsTarget=" + fps);
        args.add("-C");
        args.add("resampleVideo=" + (resample ? "1" : "0"));
        args.add(romPath);
        return args.toArray(new String[0]);
    }
}
