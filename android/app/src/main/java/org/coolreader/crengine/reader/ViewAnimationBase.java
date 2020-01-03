package org.coolreader.crengine.reader;

import android.graphics.Canvas;

public abstract class ViewAnimationBase implements ViewAnimationControl {
    //long startTimeStamp;
    boolean started;

    ReaderView mReaderView;

    public boolean isStarted() {
        return started;
    }

    public ViewAnimationBase(ReaderView readerView) {
        //startTimeStamp = android.os.SystemClock.uptimeMillis();
        mReaderView = readerView;
        mReaderView.cancelGc();
    }

    public void close() {
        mReaderView.animationScheduler.cancel();
        mReaderView.currentAnimation = null;
        mReaderView.mBookMarkManager.close();
        mReaderView.updateCurrentPositionStatus();
        mReaderView.scheduleGc();
    }

    public void draw() {
        draw(false);
    }

    public void draw(boolean isPartially) {
        mReaderView.surface.drawCallback(new DrawCanvasCallback() {
            @Override
            public void drawTo(Canvas c) {
                //	long startTs = android.os.SystemClock.uptimeMillis();
                draw(c);
            }
        }, null, isPartially);
    }
}