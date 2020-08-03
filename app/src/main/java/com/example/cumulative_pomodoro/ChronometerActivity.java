package com.example.cumulative_pomodoro;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChronometerActivity extends AppCompatActivity {
    private long breakDuration=0;
    private enum State {PAUSE, WORK, BREAK};
    private State state= State.PAUSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronometer);

        final TextView currentMode =  (TextView)findViewById(R.id.CurrentMode);
        final Chronometer chronometer = (Chronometer)findViewById(R.id.Timer);
        final Chronometer breakChronometer = (Chronometer)findViewById(R.id.BreakTimer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            breakChronometer.setCountDown(true);
        }
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long systemCurrTime = SystemClock.elapsedRealtime();
                long chronometerBaseTime = chronometer.getBase();
                long deltaTime = systemCurrTime - chronometerBaseTime;
                breakChronometer.setBase(breakDuration+(long)(deltaTime*0.2)+SystemClock.elapsedRealtime());
            }
        });
        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state==State.WORK) {
                    chronometer.stop();
                    breakChronometer.start();
                    state = State.BREAK;
                    currentMode.setText("Break");
                }
                else if (state == State.BREAK){
                    breakChronometer.stop();
                    breakDuration = breakChronometer.getBase() - SystemClock.elapsedRealtime();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    state=State.WORK;
                    currentMode.setText("Work");

                }else if (state==State.PAUSE){
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    state=State.WORK;
                    currentMode.setText("Work");


                }


            }
        });

        Button buttonReset = (Button)findViewById(R.id.buttonResetChronometer);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(state==State.WORK)
                  chronometer.stop();
                else if (state==State.BREAK)
                    breakChronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                breakChronometer.setBase(SystemClock.elapsedRealtime());
                breakDuration=0;
                state=State.PAUSE;
                currentMode.setText("Pause");


            }
        });

    }
}