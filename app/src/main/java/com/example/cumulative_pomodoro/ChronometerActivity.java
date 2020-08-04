package com.example.cumulative_pomodoro;

import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

public class ChronometerActivity extends AppCompatActivity {
    private long breakDuration=0, accumulatedBreak=0;
    private long lastBreak=0;
    private enum State {PAUSE, WORK, BREAK};
    private State state= State.PAUSE;
    private int statisticsWorkCounter=0, statisticsBreakCounter=0, statisticsWorkTime=0, statisticsBreakTime=0;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chronometer);

        final TextView currentMode =  (TextView)findViewById(R.id.CurrentMode);
        final Chronometer chronometer = (Chronometer)findViewById(R.id.Timer);
        final TextView breakChronometer = (TextView)findViewById(R.id.BreakTimer);
        final TextView sessionInfo = (TextView)findViewById(R.id.SessionInfo);

        sessionInfo.setText(formatStatistics());

        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long deltaTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                if(state==State.WORK) {
                    breakDuration = accumulatedBreak + (long) (deltaTime * 0.2);// + SystemClock.elapsedRealtime();
                    breakChronometer.setText(formatMillistoHHMMSS(breakDuration));
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
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    chronometer.setCountDown(false);
                }
                breakChronometer.setText(formatMillistoHHMMSS(0));
                breakDuration=0;
                accumulatedBreak=0;
                state=State.PAUSE;

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
}