package org.libsdl.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/** Native mGBA surface with hardware game switching and optional on-device OCR translation. */
public final class EmulatorActivity extends SDLActivity {
    private String romPath;
    private JSONArray romLibrary;
    private int romIndex;
    private boolean switching;

    private final Handler translationHandler = new Handler(Looper.getMainLooper());
    private boolean translationEnabled = false;
    private boolean ocrBusy = false;
    private String lastOcrText = "";
    private String activeSourceLanguage = "";
    private TextView translationButton;
    private TextView translationOverlay;
    private TextRecognizer latinRecognizer;
    private TextRecognizer japaneseRecognizer;
    private Translator translator;

    private final Runnable translationLoop = new Runnable() {
        @Override public void run() {
            if (translationEnabled && !isFinishing()) captureAndTranslate();
            translationHandler.postDelayed(this, 2400);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        romPath = getIntent().getStringExtra("rom_path");
        try { romLibrary = new JSONArray(getIntent().getStringExtra("rom_library_json") == null ? "[]" : getIntent().getStringExtra("rom_library_json")); }
        catch (Exception ignored) { romLibrary = new JSONArray(); }
        romIndex = getIntent().getIntExtra("rom_index", 0);
        super.onCreate(state);
        setupTranslationOverlay();
        translationHandler.postDelayed(translationLoop, 1600);
    }

    private void setupTranslationOverlay() {
        if (mLayout == null) return;
        latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        japaneseRecognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());

        translationButton = new TextView(this);
        translationButton.setText("DỊCH: TẮT");
        translationButton.setTextColor(Color.WHITE);
        translationButton.setTextSize(12);
        translationButton.setPadding(18, 12, 18, 12);
        translationButton.setGravity(Gravity.CENTER);
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(0xDD073448);
        buttonBg.setCornerRadius(18);
        buttonBg.setStroke(2, 0xFF19DCFF);
        translationButton.setBackground(buttonBg);
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonParams.setMargins(8, 8, 12, 8);
        mLayout.addView(translationButton, buttonParams);
        translationButton.setOnClickListener(v -> {
            translationEnabled = !translationEnabled;
            translationButton.setText(translationEnabled ? "DỊCH: BẬT" : "DỊCH: TẮT");
            if (!translationEnabled && translationOverlay != null) translationOverlay.setVisibility(android.view.View.GONE);
            if (translationEnabled && translationOverlay != null) {
                translationOverlay.setText("Đang nhận diện chữ trong game…");
                translationOverlay.setVisibility(android.view.View.VISIBLE);
            }
        });

        translationOverlay = new TextView(this);
        translationOverlay.setTextColor(Color.WHITE);
        translationOverlay.setTextSize(16);
        translationOverlay.setPadding(14, 10, 14, 10);
        translationOverlay.setGravity(Gravity.CENTER);
        translationOverlay.setBackgroundColor(0xD9001018);
        translationOverlay.setVisibility(android.view.View.GONE);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        textParams.setMargins(10, 8, 10, 20);
        mLayout.addView(translationOverlay, textParams);
    }

    private void captureAndTranslate() {
        if (Build.VERSION.SDK_INT < 24 || mSurface == null || ocrBusy || !mSurface.getHolder().getSurface().isValid()) return;
        int width = mSurface.getWidth(), height = mSurface.getHeight();
        if (width < 2 || height < 2) return;
        ocrBusy = true;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            PixelCopy.request((SurfaceView) mSurface, bitmap, result -> {
                if (result != PixelCopy.SUCCESS || !translationEnabled) {
                    bitmap.recycle(); ocrBusy = false; return;
                }
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                Tasks.whenAllSuccess(latinRecognizer.process(image), japaneseRecognizer.process(image))
                    .addOnSuccessListener(results -> {
                        bitmap.recycle();
                        ocrBusy = false;
                        Text best = null;
                        for (Object value : results) {
                            if (value instanceof Text) {
                                Text candidate = (Text) value;
                                if (best == null || candidate.getText().length() > best.getText().length()) best = candidate;
                            }
                        }
                        if (best != null) recognizeLanguageAndTranslate(best.getText());
                    })
                    .addOnFailureListener(error -> { bitmap.recycle(); ocrBusy = false; });
            }, translationHandler);
        } catch (Exception error) {
            bitmap.recycle(); ocrBusy = false;
        }
    }

    private void recognizeLanguageAndTranslate(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.length() < 2 || text.equals(lastOcrText)) return;
        lastOcrText = text;
        LanguageIdentification.getClient().identifyLanguage(text)
            .addOnSuccessListener(language -> {
                if (language == null || "und".equals(language)) {
                    showTranslation("Chưa nhận diện được ngôn ngữ.");
                    return;
                }
                if ("vi".equals(language)) {
                    showTranslation(text);
                    return;
                }
                String source = TranslateLanguage.fromLanguageTag(language);
                if (source == null || TranslateLanguage.VIETNAMESE.equals(source)) {
                    showTranslation("Ngôn ngữ này chưa được hỗ trợ dịch tự động.");
                    return;
                }
                if (translator == null || !language.equals(activeSourceLanguage)) {
                    if (translator != null) translator.close();
                    activeSourceLanguage = language;
                    TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(source)
                        .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                        .build();
                    translator = Translation.getClient(options);
                }
                showTranslation("Đang tải mô hình dịch " + language + "…");
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener(v -> translator.translate(text)
                        .addOnSuccessListener(this::showTranslation)
                        .addOnFailureListener(e -> showTranslation("Không dịch được đoạn chữ này.")))
                    .addOnFailureListener(e -> showTranslation("Cần Internet lần đầu để tải mô hình dịch."));
            })
            .addOnFailureListener(e -> showTranslation("Không nhận diện được chữ trong game."));
    }

    private void showTranslation(String text) {
        if (translationOverlay == null || !translationEnabled) return;
        translationOverlay.setText(text);
        translationOverlay.setVisibility(android.view.View.VISIBLE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_BUTTON_R1) {
                switchGame(1); return true;
            }
            if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_BUTTON_L1) {
                switchGame(-1); return true;
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

    @Override protected void onDestroy() {
        translationHandler.removeCallbacksAndMessages(null);
        if (latinRecognizer != null) latinRecognizer.close();
        if (japaneseRecognizer != null) japaneseRecognizer.close();
        if (translator != null) translator.close();
        super.onDestroy();
    }
}
