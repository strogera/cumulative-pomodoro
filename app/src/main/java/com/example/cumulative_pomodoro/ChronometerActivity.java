package com.example.cumulative_pomodoro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Layout;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class ChronometerActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.cumulative-pomodoro.MESSAGE";
    private long breakDuration=0, accumulatedBreak=0;
    private long lastBreak=0;
    private enum State {PAUSE, WORK, BREAK};
    private State state= State.PAUSE;
    private int statisticsWorkCounter=0, statisticsBreakCounter=0, statisticsWorkTime=0, statisticsBreakTime=0;
    private boolean isFullScreen=false;

    private String formatStatistics(){
        long avrWorkTime=0;
        long avrBreakTime=0;
        if(statisticsWorkCounter!=0){
            avrWorkTime=(long)statisticsWorkTime/statisticsWorkCounter;
        }
        if(statisticsBreakCounter!=0){
            avrBreakTime=(long)statisticsBreakTime/statisticsBreakCounter;
        }

        return String.format("Work Sessions: %d, AvrWorkTime: %s, Breaks: %d, AvrBreakTime: %s", statisticsWorkCounter,
                formatMillistoHHMMSS(avrWorkTime), statisticsBreakCounter,
                formatMillistoHHMMSS(avrBreakTime));

    }
    private String formatMillistoHHMMSS(long t){
        long seconds = (Math.abs(t) / 1000) % 60;
        long minutes = (Math.abs(t) /(1000 * 60)) % 60;
        long hours = Math.abs(t) / ( 1000 * 60 * 60);
        String hoursStr="";
        if (hours!=0){
            hoursStr=String.format("%02d:", hours);
        }
        if(t<0){
            return String.format("-%s%02d:%02d", hoursStr, Math.abs(minutes), Math.abs(seconds));
        }else{
            return String.format("%s%02d:%02d", hoursStr, minutes, seconds);
        }
    }
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        isFullScreen=true;

        final TextView sessionInfo = (TextView)findViewById(R.id.SessionInfo);
        sessionInfo.setVisibility(View.INVISIBLE);
        Button buttonReset = (Button)findViewById(R.id.buttonResetChronometer);
        buttonReset.setVisibility(View.INVISIBLE);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        isFullScreen=false;

        final TextView sessionInfo = (TextView)findViewById(R.id.SessionInfo);
        sessionInfo.setVisibility(View.VISIBLE);
        Button buttonReset = (Button)findViewById(R.id.buttonResetChronometer);
        buttonReset.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronometer);

        final TextView currentMode =  (TextView)findViewById(R.id.CurrentMode);
        final Chronometer chronometer = (Chronometer)findViewById(R.id.Timer);
        final TextView breakChronometer = (TextView)findViewById(R.id.BreakTimer);
        final TextView sessionInfo = (TextView)findViewById(R.id.SessionInfo);
        final ConstraintLayout layout= (ConstraintLayout)findViewById(R.id.ConstraintLayout);

        layout.setOnTouchListener(new ConstraintLayout.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(ChronometerActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if(isFullScreen)
                        showSystemUI();
                    else
                        hideSystemUI();
                    return super.onDoubleTap(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        sessionInfo.setText(formatStatistics());

        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long deltaTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                if(state==State.WORK) {
                    breakDuration = accumulatedBreak + (long) (deltaTime * 0.2);// + SystemClock.elapsedRealtime();
                    breakChronometer.setText(formatMillistoHHMMSS(breakDuration));
                }else if (state==State.BREAK){
                    if(Math.abs(deltaTime/500)==0){
                        sendNotification("Break is up", "Time for work");
                    }
                }
            }
        });
        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state==State.WORK) {
                    chronometer.stop();
                    long curTimeWorking=SystemClock.elapsedRealtime()-chronometer.getBase();
                    chronometer.setBase(breakDuration+SystemClock.elapsedRealtime());
                    lastBreak=breakDuration;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        chronometer.setCountDown(true);
                    }
                    state = State.BREAK;
                    chronometer.start();

                    currentMode.setText("Break");
                    breakChronometer.setVisibility(View.INVISIBLE);
                    statisticsWorkCounter++;
                    statisticsWorkTime+=curTimeWorking;
                }
                else if (state == State.BREAK){
                    chronometer.stop();
                    state=State.WORK;
                    accumulatedBreak=chronometer.getBase()-SystemClock.elapsedRealtime();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        chronometer.setCountDown(false);
                    }
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();

                    currentMode.setText("Work");
                    breakChronometer.setText(formatMillistoHHMMSS(accumulatedBreak));
                    breakChronometer.setVisibility(View.VISIBLE);
                    statisticsBreakCounter++;
                    statisticsBreakTime+=Math.abs(lastBreak-accumulatedBreak);
                }else if (state==State.PAUSE){
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        chronometer.setCountDown(false);
                    }
                    chronometer.start();
                    state=State.WORK;

                    currentMode.setText("Work");
                    breakChronometer.setVisibility(View.VISIBLE);
                    breakDuration=0;
                }
                sessionInfo.setText(formatStatistics());
            }
        });

        Button buttonReset = (Button)findViewById(R.id.buttonResetChronometer);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                state=State.PAUSE;
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    chronometer.setCountDown(false);
                }
                breakChronometer.setText(formatMillistoHHMMSS(0));
                breakDuration=0;
                accumulatedBreak=0;

                currentMode.setText("Pause");
                breakChronometer.setVisibility(View.VISIBLE);
                statisticsWorkTime=0;
                statisticsWorkCounter=0;
                statisticsBreakTime=0;
                statisticsBreakCounter=0;
                sessionInfo.setText(formatStatistics());

            }
        });

    }

    protected void sendNotification(String title, String message) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //Uri alarmSound = Settings.System.DEFAULT_NOTIFICATION_URI;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("YOUR_CHANNEL_ID",
                        "End of Break",
                        NotificationManager.IMPORTANCE_DEFAULT);
    //            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DESCRIPTION");
/*
                if(alarmSound != null) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build();
                    channel.setSound(alarmSound, audioAttributes);
                }

 */
            channel.setBypassDnd(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
        }
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "YOUR_CHANNEL_ID")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true);
            Intent intent = new Intent(getApplicationContext(), ChronometerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            //mBuilder.setDefaults(Notification.DEFAULT_SOUND);

            mBuilder.setOnlyAlertOnce(true);
            //mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            mBuilder.setContentIntent(pi);
            //mBuilder.setSound(alarmSound);
            mNotificationManager.notify(0, mBuilder.build());
            final MediaPlayer mp = MediaPlayer.create(this, alarmSound);
            mp.setLooping(false);
            mp.start();

    }
}