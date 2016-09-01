package com.github.strictanr.testapp;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.strictanr.R;
import com.github.strictanr.util.JLog;
import com.github.strictanr.util.StrictANRError;
import com.github.strictanr.util.StrictANRWatchDog;

public class MainActivity extends AppCompatActivity {
    private TextView mStatusTV;
    private Boolean mKeepTracking;
    private StrictANRWatchDog mWatchDog = null;

    //set strict anr time interval to 2000 milliseconds.
    private final int mTimeInterval = 2000;

    private void initStrictANRWatchDog() {
        mWatchDog = new StrictANRWatchDog(mTimeInterval);

        mWatchDog.setANRListener(new StrictANRWatchDog.ANRListener() {
            @Override
            public void onAppNotResponding(StrictANRError error) {
                JLog.i("Strict ANR detected.");
                error.printAllStackTrace();
            }
        });
        mWatchDog.setIgnoreDebugger(true);
    }
    private void doSleep() {
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void toastMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(boolean keepTracking) {
        if(keepTracking) {
            mStatusTV.setText("tracking status: started");
        } else {
            mStatusTV.setText("tracking status: stopped");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusTV = (TextView)findViewById(R.id.status);
        TextView tv = (TextView)findViewById(R.id.strictANRInterval);
        tv.setText("Strict ANR Interval: " + mTimeInterval + " milliseconds");

        findViewById(R.id.simpleAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSleep();
            }
        });

        findViewById(R.id.startTracking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWatchDog == null) {
                    initStrictANRWatchDog();
                }
                mWatchDog.startTracking();
                mKeepTracking = true;
                updateStatus(mKeepTracking);
            }
        });

        findViewById(R.id.stopTracking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWatchDog.stopTracking();
                //It is never legal to start a thread more than once. In particular, a thread may not be restarted once it has completed execution.
                //If a Thread needs to be run more than once, then one should make an new instance of the Thread and call start on it.
                mWatchDog = null;
                mKeepTracking = false;
                updateStatus(mKeepTracking);
            }
        });

    }
}
