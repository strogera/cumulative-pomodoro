package com.example.cumulative_pomodoro;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

public class ChronometerActivity extends AppCompatActivity {
    private long breakDuration=0, accumulatedBreak=0;
    private enum State {PAUSE, WORK, BREAK};
    private State state= State.PAUSE;

    private String formatMillistoMMSS(long t){
        long minutes = Math.abs(t) /(1000 * 60);
        long seconds = Math.abs(t) / 1000 % 60;
        if(t<0){
            return String.format("-%02d:%02d", Math.abs(minutes), Math.abs(seconds));
        }else{
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronometer);

        final TextView currentMode =  (TextView)findViewById(R.id.CurrentMode);
        final Chronometer chronometer = (Chronometer)findViewById(R.id.Timer);
        final TextView breakChronometer = (TextView)findViewById(R.id.BreakTimer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
           // breakChronometer.setCountDown(true);
        }
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long deltaTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                if(state==State.WORK) {
                    breakDuration = accumulatedBreak + (long) (deltaTime * 0.2);// + SystemClock.elapsedRealtime();
                    breakChronometer.setText(formatMillistoMMSS(breakDuration));
                }
            }
        });
        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state==State.WORK) {
                    chronometer.stop();
                    chronometer.setBase(breakDuration+SystemClock.elapsedRealtime());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        chronometer.setCountDown(true);
                    }
                    chronometer.start();

                    state = State.BREAK;
                    currentMode.setText("Break");
                    breakChronometer.setVisibility(View.INVISIBLE);
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
                    breakChronometer.setText(formatMillistoMMSS(accumulatedBreak));
                    breakChronometer.setVisibility(View.VISIBLE);

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
            }
        });

        Button buttonReset = (Button)findViewById(R.id.buttonResetChronometer);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    chronometer.setCountDown(false);
                }
                breakChronometer.setText(formatMillistoMMSS(0));
                breakDuration=0;
                accumulatedBreak=0;
                state=State.PAUSE;

                currentMode.setText("Pause");
                breakChronometer.setVisibility(View.VISIBLE);
            }
        });

    }
}