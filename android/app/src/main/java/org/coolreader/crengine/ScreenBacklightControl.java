package org.coolreader.crengine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;

public class ScreenBacklightControl implements Settings{
    PowerManager.WakeLock wl = null;
    private static final Logger log = L.create("ba");
    BaseActivity mContext;
    private Runnable backlightTimerTask = null;
    private static long lastUserActivityTime;

    public ScreenBacklightControl(BaseActivity context) {
        mContext = context;
    }

    long lastUpdateTimeStamp;


    public void onUserActivity(){
        onUserActivity1();
        // Hack
        //if ( backlightControl.isHeld() )
        BackgroundThread.instance().executeGUI(new Runnable() {
            @Override
            public void run() {
                try {
                    float b;
                    int dimmingAlpha = 255;
                    // screenBacklightBrightness is 0..100
                    if (screenBacklightBrightness >= 0) {
                        int percent = screenBacklightBrightness;
                        if (!allowLowBrightness() && percent < MIN_BRIGHTNESS_IN_BROWSER)
                            percent = MIN_BRIGHTNESS_IN_BROWSER;
                        float minb = MIN_BACKLIGHT_LEVEL_PERCENT / 100.0f;
                        if ( percent >= 10 ) {
                            // real brightness control, no colors dimming
                            b = (percent - 10) / (100.0f - 10.0f); // 0..1
                            b = minb + b * (1-minb); // minb..1
                            if (b < minb) // BRIGHTNESS_OVERRIDE_OFF
                                b = minb;
                            else if (b > 1.0f)
                                b = 1.0f; //BRIGHTNESS_OVERRIDE_FULL
                        } else {
                            // minimal brightness with colors dimming
                            b = minb;
                            dimmingAlpha = 255 - (11-percent) * 180 / 10;
                        }
                    } else {
                        // system
                        b = -1.0f; //BRIGHTNESS_OVERRIDE_NONE
                    }
                    setDimmingAlpha(dimmingAlpha);
                    //log.v("Brightness: " + b + ", dim: " + dimmingAlpha);
                    updateBacklightBrightness(b);
                    updateButtonsBrightness(keyBacklightOff ? 0.0f : -1.0f);
                } catch ( Exception e ) {
                    // ignore
                }
            }
        });
    }
    private boolean keyBacklightOff = true;
    public boolean isKeyBacklightDisabled() {
        return keyBacklightOff;
    }
    @SuppressLint("InvalidWakeLockTag")
    private void onUserActivity1() {
        lastUserActivityTime = Utils.timeStamp();
        if (Utils.timeInterval(lastUpdateTimeStamp) < 5000)
            return;
        lastUpdateTimeStamp = android.os.SystemClock.uptimeMillis();
        if (!isWakeLockEnabled())
            return;
        if (wl == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    /* | PowerManager.ON_AFTER_RELEASE */, "cr3");
        }
        if (!mContext.isStarted()) {
            release();
            return;
        }

        if (!isHeld()) {
            wl.acquire();
        }

        if (backlightTimerTask == null) {
            backlightTimerTask = new BacklightTimerTask();
            BackgroundThread.instance().postGUI(backlightTimerTask,
                    screenBacklightDuration / 10);
        }
    }

    public boolean isHeld() {
        return wl != null && wl.isHeld();
    }

    public void release() {
        if (wl != null && wl.isHeld()) {
            log.d("ScreenBacklightControl: wl.release()");
            wl.release();
        }
        backlightTimerTask = null;
        lastUpdateTimeStamp = 0;
    }

    private class BacklightTimerTask implements Runnable {

        @Override
        public void run() {
            if (backlightTimerTask == null)
                return;
            long interval = Utils.timeInterval(lastUserActivityTime);
//				log.v("ScreenBacklightControl: timer task, lastActivityMillis = "
//						+ interval);
            int nextTimerInterval = screenBacklightDuration / 20;
            boolean dim = false;
            if (interval > screenBacklightDuration * 8 / 10) {
                nextTimerInterval = nextTimerInterval / 8;
                dim = true;
            }
            if (interval > screenBacklightDuration) {
                release();
            } else {
                BackgroundThread.instance().postGUI(backlightTimerTask, nextTimerInterval);
                if (dim) {
                    updateBacklightBrightness(-0.9f); // reduce by 9%
                }
            }
        }

    }
    private void updateBacklightBrightness(float b) {
        Window wnd = mContext.getWindow();
        if (wnd != null) {
            WindowManager.LayoutParams attrs =  wnd.getAttributes();
            boolean changed = false;
            if (b < 0 && b > -0.99999f) {
                //log.d("dimming screen by " + (int)((1 + b)*100) + "%");
                b = -b * attrs.screenBrightness;
                if (b < 0.15)
                    return;
            }
            float delta = attrs.screenBrightness - b;
            if (delta < 0)
                delta = -delta;
            if (delta > 0.01) {
                attrs.screenBrightness = b;
                changed = true;
            }
            if ( changed ) {
                log.d("Window attribute changed: " + attrs);
                wnd.setAttributes(attrs);
            }
        }
    }


    private void updateButtonsBrightness(float buttonBrightness) {
        Window wnd = mContext.getWindow();
        if (wnd != null) {
            WindowManager.LayoutParams attrs =  wnd.getAttributes();
            boolean changed = false;
            // hack to set buttonBrightness field
            //float buttonBrightness = keyBacklightOff ? 0.0f : -1.0f;
            if (!brightnessHackError)
                try {
                    Field bb = attrs.getClass().getField("buttonBrightness");
                    if (bb != null) {
                        Float oldValue = (Float)bb.get(attrs);
                        if (oldValue == null || oldValue.floatValue() != buttonBrightness) {
                            bb.set(attrs, buttonBrightness);
                            changed = true;
                        }
                    }
                } catch ( Exception e ) {
                    log.e("WindowManager.LayoutParams.buttonBrightness field is not found, cannot turn buttons backlight off");
                    brightnessHackError = true;
                }
            //attrs.buttonBrightness = 0;
            if (changed) {
                log.d("Window attribute changed: " + attrs);
                wnd.setAttributes(attrs);
            }
            if (keyBacklightOff)
                turnOffKeyBacklight();
        }
    }

    private final static int MIN_BACKLIGHT_LEVEL_PERCENT = DeviceInfo.MIN_SCREEN_BRIGHTNESS_PERCENT;

    protected void setDimmingAlpha(int alpha) {
        // override it
    }

    protected boolean allowLowBrightness() {
        // override to force higher brightness in non-reading mode (to avoid black screen on some devices when brightness level set to small value)
        return true;
    }

    private final static int MIN_BRIGHTNESS_IN_BROWSER = 12;


    private int screenBacklightBrightness = -1; // use default
    //private boolean brightnessHackError = false;
    private boolean brightnessHackError = DeviceInfo.SAMSUNG_BUTTONS_HIGHLIGHT_PATCH;

    private int currentKeyBacklightLevel = 1;

    public int getKeyBacklight() {
        return currentKeyBacklightLevel;
    }


    public boolean setKeyBacklight(int value) {
        currentKeyBacklightLevel = value;
        // Try ICS way
        if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
            mContext.setSystemUiVisibility();
        }
        // thread safe
        return Engine.getInstance(mContext).setKeyBacklight(value);
    }
    private void turnOffKeyBacklight() {
        if (!mContext.isStarted())
            return;
        if (DeviceInfo.getSDKLevel() >= DeviceInfo.HONEYCOMB) {
            setKeyBacklight(0);
        }
        // repeat again in short interval
        if (!Engine.getInstance(mContext).setKeyBacklight(0)) {
            //log.w("Cannot control key backlight directly");
            return;
        }
        // repeat again in short interval
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (!mContext.isStarted())
                    return;
                if (!Engine.getInstance(mContext).setKeyBacklight(0)) {
                    //log.w("Cannot control key backlight directly (delayed)");
                }
            }
        };
        BackgroundThread.instance().postGUI(task, 1);
        //BackgroundThread.instance().postGUI(task, 10);
    }
    public void setKeyBacklightDisabled(boolean disabled) {
        keyBacklightOff = disabled;
        onUserActivity();
    }

    public void setScreenBacklightLevel( int percent )
    {
        if ( percent<-1 )
            percent = -1;
        else if ( percent>100 )
            percent = -1;
        screenBacklightBrightness = percent;
        onUserActivity();
    }


    public static final int DEF_SCREEN_BACKLIGHT_TIMER_INTERVAL = 3 * 60 * 1000;



    public boolean isWakeLockEnabled() {
        return screenBacklightDuration > 0;
    }

    /**
     * @param backlightDurationMinutes 0 = system default, 1 == 3 minutes, 2..5 == 2..5 minutes
     */
    public void setScreenBacklightDuration(int backlightDurationMinutes)
    {
        if (backlightDurationMinutes == 1)
            backlightDurationMinutes = 3;
        if (screenBacklightDuration != backlightDurationMinutes * 60 * 1000) {
            screenBacklightDuration = backlightDurationMinutes * 60 * 1000;
            if (screenBacklightDuration == 0)
                release();
            else
                onUserActivity1();
        }
    }

    private int mScreenUpdateMode = 0;
    public int getScreenUpdateMode() {
        return mScreenUpdateMode;
    }
    public void setScreenUpdateMode( int screenUpdateMode, View view ) {
        //if (mReaderView != null) {
        mScreenUpdateMode = screenUpdateMode;
        if (EinkScreen.UpdateMode != screenUpdateMode || EinkScreen.UpdateMode == 2) {
            EinkScreen.ResetController(screenUpdateMode, view);
        }
        //}
    }

    private int screenBacklightDuration = DEF_SCREEN_BACKLIGHT_TIMER_INTERVAL;
    private int mScreenUpdateInterval = 0;
    public int getScreenUpdateInterval() {
        return mScreenUpdateInterval;
    }
    public void setScreenUpdateInterval( int screenUpdateInterval, View view ) {
        mScreenUpdateInterval = screenUpdateInterval;
        if (EinkScreen.UpdateModeInterval != screenUpdateInterval) {
            EinkScreen.UpdateModeInterval = screenUpdateInterval;
            EinkScreen.ResetController(mScreenUpdateMode, view);
        }
    }

}