package com.github.strictanr.util;

/**
 * Created by wangxin on 8/29/16.
 */
import android.os.Looper;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"Convert2Diamond", "UnusedDeclaration"})
public class StrictANRError extends Error {

    private static class $ implements Serializable {
        private final String _name;
        private final StackTraceElement[] _stackTrace;

        private class _Thread extends Throwable {
            private _Thread(_Thread other) {
                super(_name, other);
            }

            @Override
            public Throwable fillInStackTrace() {
                setStackTrace(_stackTrace);
                return this;
            }
        }

        private $(String name, StackTraceElement[] stackTrace) {
            _name = name;
            _stackTrace = stackTrace;
        }
    }

    private static final long serialVersionUID = 1L;

    private StrictANRError($._Thread st) {
        super("Application Not Responding", st);
    }

    public StrictANRError(String prefix, boolean logThreadsWithoutStackTrace) {
        final Thread mainThread = Looper.getMainLooper().getThread();

        mStackTraces = new TreeMap<Thread, StackTraceElement[]>(new Comparator<Thread>() {
            @Override
            public int compare(Thread lhs, Thread rhs) {
                if (lhs == rhs)
                    return 0;
                if (lhs == mainThread)
                    return 1;
                if (rhs == mainThread)
                    return -1;
                return rhs.getName().compareTo(lhs.getName());
            }
        });

        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
            if (
                    entry.getKey() == mainThread
                            || (
                            entry.getKey().getName().startsWith(prefix)
                                    && (
                                    logThreadsWithoutStackTrace
                                            ||
                                            entry.getValue().length > 0
                            )
                    )
                    )
                mStackTraces.put(entry.getKey(), entry.getValue());

        // Sometimes main is not returned in getAllStackTraces() - ensure that we list it
        if (!mStackTraces.containsKey(mainThread)) {
            mStackTraces.put(mainThread, mainThread.getStackTrace());
        }

/*        $._Thread tst = null;
        for (Map.Entry<Thread, StackTraceElement[]> entry : mStackTraces.entrySet()) {
//            JLog.i(entry.getKey().toString());//ahking
            tst = new $(getThreadTitle(entry.getKey()), entry.getValue()).new _Thread(tst);
        }*/
    }

    @Override
    public Throwable fillInStackTrace() {
        setStackTrace(new StackTraceElement[]{});
        return this;
    }

    private Map<Thread, StackTraceElement[]> mStackTraces = null;

    StrictANRError New(String prefix, boolean logThreadsWithoutStackTrace) {
        final Thread mainThread = Looper.getMainLooper().getThread();

        mStackTraces = new TreeMap<Thread, StackTraceElement[]>(new Comparator<Thread>() {
            @Override
            public int compare(Thread lhs, Thread rhs) {
                if (lhs == rhs)
                    return 0;
                if (lhs == mainThread)
                    return 1;
                if (rhs == mainThread)
                    return -1;
                return rhs.getName().compareTo(lhs.getName());
            }
        });

        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
            if (
                    entry.getKey() == mainThread
                            || (
                            entry.getKey().getName().startsWith(prefix)
                                    && (
                                    logThreadsWithoutStackTrace
                                            ||
                                            entry.getValue().length > 0
                            )
                    )
                    )
                mStackTraces.put(entry.getKey(), entry.getValue());

        // Sometimes main is not returned in getAllStackTraces() - ensure that we list it
        if (!mStackTraces.containsKey(mainThread)) {
            mStackTraces.put(mainThread, mainThread.getStackTrace());
        }

        $._Thread tst = null;
        for (Map.Entry<Thread, StackTraceElement[]> entry : mStackTraces.entrySet()) {
            tst = new $(getThreadTitle(entry.getKey()), entry.getValue()).new _Thread(tst);
        }

        return new StrictANRError(tst);
    }

    static StrictANRError NewMainOnly() {
        final Thread mainThread = Looper.getMainLooper().getThread();
        final StackTraceElement[] mainStackTrace = mainThread.getStackTrace();
        int stackLength = mainStackTrace.length;
        JLog.i(stackLength);
        return new StrictANRError(new $(getThreadTitle(mainThread), mainStackTrace).new _Thread(null));
    }

    public static String getThreadTitle(Thread thread) {
        return thread.getName() + " (state = " + thread.getState() + ")";
    }

    private Map<Thread, StackTraceElement[]> getAllStackTrace() {
        return mStackTraces;
    }

    public void printAllStackTrace() {
        JLog.setGlobalTag("StrictANR");

        Map<Thread, StackTraceElement[]> stackTraces = getAllStackTrace();
        JLog.i("Detected Application Not Responding, " + "start tracking!");
        for (Map.Entry<Thread, StackTraceElement[]> trace : stackTraces.entrySet()) {
            JLog.printAnrStackTrace(StrictANRError.getThreadTitle(trace.getKey()), trace.getValue());
        }
        JLog.i("Detected Application Not Responding, " + "end tracking!");
        JLog.setGlobalTag("");
    }
}
