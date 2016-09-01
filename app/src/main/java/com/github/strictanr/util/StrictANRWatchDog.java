package com.github.strictanr.util;


/**
 * Created by wangxin on 8/29/16.
 */
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * A watchdog timer thread that detects when the UI thread has frozen.
 */

@SuppressWarnings("UnusedDeclaration")
public class StrictANRWatchDog extends Thread {

    public interface ANRListener {
        public void onAppNotResponding(StrictANRError error);
    }

    public interface InterruptionListener {
        public void onInterrupted(InterruptedException exception);
    }

    private static final int DEFAULT_ANR_TIMEOUT = 5000;

    private static final ANRListener DEFAULT_ANR_LISTENER = new ANRListener() {
        @Override public void onAppNotResponding(StrictANRError error) {
            throw error;
        }
    };

    private static final InterruptionListener DEFAULT_INTERRUPTION_LISTENER = new InterruptionListener() {
        @Override public void onInterrupted(InterruptedException exception) {
            Log.w("StrictANRWatchdog", "Interrupted: " + exception.getMessage());
        }
    };

    private ANRListener _anrListener = DEFAULT_ANR_LISTENER;
    private InterruptionListener _interruptionListener = DEFAULT_INTERRUPTION_LISTENER;

    private final Handler _uiHandler = new Handler(Looper.getMainLooper());
    private final int _timeoutInterval;

    private String _namePrefix = "";
    private boolean _logThreadsWithoutStackTrace = false;
    private boolean _ignoreDebugger = false;

    private volatile int _tick = 0;

    private volatile boolean _keepTracking = false;

    private final Runnable _ticker = new Runnable() {
        @Override public void run() {
            _tick = (_tick + 1) % Integer.MAX_VALUE;
        }
    };

    /**
     * Constructs a watchdog that checks the ui thread every {@value #DEFAULT_ANR_TIMEOUT} milliseconds
     */
    public StrictANRWatchDog() {
        this(DEFAULT_ANR_TIMEOUT);
    }

    /**
     * Constructs a watchdog that checks the ui thread every given interval
     *
     * @param timeoutInterval The interval, in milliseconds, between to checks of the UI thread.
     *                        It is therefore the maximum time the UI may freeze before being reported as ANR.
     */
    public StrictANRWatchDog(int timeoutInterval) {
        super();
        _timeoutInterval = timeoutInterval;
    }

    /**
     * Sets an interface for when an ANR is detected.
     * If not set, the default behavior is to throw an error and crash the application.
     *
     * @param listener The new listener or null
     * @return itself for chaining.
     */
    public StrictANRWatchDog setANRListener(ANRListener listener) {
        if (listener == null) {
            _anrListener = DEFAULT_ANR_LISTENER;
        }
        else {
            _anrListener = listener;
        }
        return this;
    }

    /**
     * Sets an interface for when the watchdog thread is interrupted.
     * If not set, the default behavior is to just log the interruption message.
     *
     * @param listener The new listener or null.
     * @return itself for chaining.
     */
    public StrictANRWatchDog setInterruptionListener(InterruptionListener listener) {
        if (listener == null) {
            _interruptionListener = DEFAULT_INTERRUPTION_LISTENER;
        }
        else {
            _interruptionListener = listener;
        }
        return this;
    }

    /**
     * Set the prefix that a thread's name must have for the thread to be reported.
     * Note that the main thread is always reported.
     * Default "".
     *
     * @param prefix The thread name's prefix for a thread to be reported.
     * @return itself for chaining.
     */
    public StrictANRWatchDog setReportThreadNamePrefix(String prefix) {
        if (prefix == null)
            prefix = "";
        _namePrefix = prefix;
        return this;
    }

    /**
     * Set that only the main thread will be reported.
     *
     * @return itself for chaining.
     */
    public StrictANRWatchDog setReportMainThreadOnly() {
        _namePrefix = null;
        return this;
    }

    /**
     * Set that all running threads will be reported,
     * even those from which no stack trace could be extracted.
     * Default false.
     *
     * @param logThreadsWithoutStackTrace Whether or not all running threads should be reported
     * @return itself for chaining.
     */
    public StrictANRWatchDog setLogThreadsWithoutStackTrace(boolean logThreadsWithoutStackTrace) {
        _logThreadsWithoutStackTrace = logThreadsWithoutStackTrace;
        return this;
    }

    /**
     * Set whether to ignore the debugger when detecting ANRs.
     * When ignoring the debugger, ANRWatchdog will detect ANRs even if the debugger is connected.
     * By default, it does not, to avoid interpreting debugging pauses as ANRs.
     * Default false.
     *
     * @param ignoreDebugger Whether to ignore the debugger.
     * @return itself for chaining.
     */
    public StrictANRWatchDog setIgnoreDebugger(boolean ignoreDebugger) {
        _ignoreDebugger = ignoreDebugger;
        return this;
    }

    @Override
    public void run() {
        setName("|Strict-ANR-WatchDog|");

        int lastTick;
        int lastIgnored = -1;
        while (!isInterrupted() && _keepTracking) {
            lastTick = _tick;
            _uiHandler.post(_ticker);
            try {
                Thread.sleep(_timeoutInterval);
            }
            catch (InterruptedException e) {
                _interruptionListener.onInterrupted(e);
                return ;
            }

            // If the main thread has not handled _ticker, it is blocked. ANR.
            if (_tick == lastTick) {
                if (!_ignoreDebugger && Debug.isDebuggerConnected()) {
                    if (_tick != lastIgnored)
                        Log.w("StrictANRWatchdog", "An ANR was detected but ignored because the debugger is connected (you can prevent this with setIgnoreDebugger(true))");
                    lastIgnored = _tick;
                    continue ;
                }

                StrictANRError error;
                if (_namePrefix != null)
                    error = new StrictANRError(_namePrefix, _logThreadsWithoutStackTrace);
                else
                    error = StrictANRError.NewMainOnly();
                _anrListener.onAppNotResponding(error);
            }
        }
        JLog.i("exit Strict ANR WatchDog thread.");
    }

    public void startTracking() {
        if(this.isAlive()) {
            return;
        }

        _keepTracking = true;
        start();
    }

    public void stopTracking() {
        _keepTracking = false;
    }

}