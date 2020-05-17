package com.dds.skywebrtc.client;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class SimpleActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    public static SimpleActivityLifecycleCallbacks instance = new SimpleActivityLifecycleCallbacks();

    private boolean inBackground;

    private int started;
    private int stopped;

    private BackgroundObserver mObserver;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        started++;
        if (inBackground) {
            inBackground = false;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        stopped++;
        if (started == stopped) {
            inBackground = true;
            if (mObserver != null) {
                mObserver.onBackground();
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public void setBackgroundObserver(BackgroundObserver observer) {
        mObserver = observer;
    }

    public interface BackgroundObserver{
        void onBackground();
    }
}
