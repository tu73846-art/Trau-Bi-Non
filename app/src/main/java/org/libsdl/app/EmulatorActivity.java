package org.libsdl.app;

import android.os.Bundle;

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
        return romPath == null ? new String[0] : new String[] { romPath };
    }
}
