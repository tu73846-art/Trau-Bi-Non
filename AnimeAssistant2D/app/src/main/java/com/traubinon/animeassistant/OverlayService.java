package com.traubinon.animeassistant;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "anime_assistant_running";
    private static final int NOTIFICATION_ID = 4102;
    WindowManager wm; View bubble, panel; TextToSpeech tts; boolean opened=false; String character="Nữ";
    int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        tts=new TextToSpeech(this, status -> { if(status==TextToSpeech.SUCCESS) tts.setLanguage(new Locale("vi","VN")); });
        showBubble();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Anime Assistant đang chạy", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Thông báo cho biết trợ lý nổi đang hoạt động.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, pendingFlags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Anime Assistant đang chạy")
            .setContentText("Bong bóng ✦ vẫn hoạt động khi bạn dùng ứng dụng khác.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
    }

    WindowManager.LayoutParams params(int w,int h,int f) {
        return new WindowManager.LayoutParams(w,h,type,f, PixelFormat.TRANSLUCENT);
    }
    void showBubble() {
        TextView b=new TextView(this); b.setText("✦"); b.setTextSize(27); b.setTextColor(0xFFFFFFFF);
        b.setGravity(Gravity.CENTER); b.setBackgroundColor(0xFF7955D9); b.setElevation(12);
        bubble=b;
        WindowManager.LayoutParams p=params(62,62,flags); p.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;
        p.x=18; p.y=0;
        b.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy; int ox,oy; boolean moved;
            public boolean onTouch(View v, android.view.MotionEvent e){
                if(e.getAction()==0){sx=e.getRawX();sy=e.getRawY();ox=p.x;oy=p.y;moved=false;return true;}
                if(e.getAction()==2){float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.abs(dx)+Math.abs(dy)>12)moved=true;p.x=Math.max(0,ox-(int)dx);p.y=oy+(int)dy;wm.updateViewLayout(b,p);return true;}
                if(e.getAction()==1){if(!moved)togglePanel();return true;} return true;
            }});
        wm.addView(b,p);
    }
    void togglePanel(){ if(opened){wm.removeView(panel);panel=null;opened=false;} else showPanel(); }
    void showPanel(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(1); box.setPadding(18,14,18,14); box.setBackgroundColor(0xF21A2038);
        TextView head=new TextView(this); head.setText("Trợ lý • "+character+"     ✕");head.setTextColor(-1);head.setTextSize(18);
        head.setOnClickListener(v->togglePanel());box.addView(head);
        LinearLayout choices=new LinearLayout(this);
        Button boa=new Button(this);boa.setText("Nữ");boa.setOnClickListener(v->{character="Nữ";head.setText("Trợ lý • "+character+"     ✕");});
        Button gojo=new Button(this);gojo.setText("Nam");gojo.setOnClickListener(v->{character="Nam";head.setText("Trợ lý • "+character+"     ✕");});
        choices.addView(boa);choices.addView(gojo);box.addView(choices);
        TextView face=new TextView(this);face.setText("😊  😳  😠  😢");face.setTextSize(25);face.setTextColor(-1);box.addView(face);
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Nhắn gì đó...");box.addView(input);
        Button send=new Button(this);send.setText("GỬI / ĐỌC");send.setOnClickListener(v->{
            String msg=input.getText().toString().trim();
            if(msg.isEmpty()) msg="Xin chào! Mình đang ở đây trò chuyện cùng bạn.";
            Toast.makeText(this,character+": "+msg,Toast.LENGTH_LONG).show();
            if(tts!=null)tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"assistant");
        });box.addView(send);
        panel=box; WindowManager.LayoutParams p=params(300,-2,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        p.gravity=Gravity.CENTER;wm.addView(panel,p);opened=true;
    }
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){if(bubble!=null)wm.removeView(bubble);if(panel!=null)wm.removeView(panel);if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
