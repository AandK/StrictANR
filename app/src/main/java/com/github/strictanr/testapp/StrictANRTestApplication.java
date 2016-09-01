package com.github.strictanr.testapp;

/**
 * Created by wangxin on 8/31/16.
 */
import android.app.Application;
import android.os.Looper;
import com.github.strictanr.util.StrictANRWatchDog;


public class StrictANRTestApplication extends Application {

//    private final int mTimeInterval = 2000;
//    //set strict anr time interval to 2000 milliseconds.
//    StrictANRWatchDog anrWatchDog = new StrictANRWatchDog(mTimeInterval);

    @Override
    public void onCreate() {
        super.onCreate();
    }

}