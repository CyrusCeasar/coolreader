package org.coolreader.crengine.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderActivity;

import static org.coolreader.crengine.Settings.PROP_APP_TRACKBALL_DISABLED;

public class ReaderSurface extends SurfaceView implements BookView {
    public static final Logger log = L.create("rv", Log.VERBOSE);
    private static final Logger alog = L.create("ra", Log.WARN);


    private final static boolean dontStretchWhileDrawing = true;
    private final static boolean centerPageInsteadOfResizing = true;

    private Properties mSettings;
    private ReaderActivity mActivity;
    private ReaderView mReaderView;

    public ReaderSurface(Context context, Properties settings, ReaderView readerView) {
        super(context);
        mActivity = (ReaderActivity) context;
        mSettings = settings;
        mReaderView= readerView;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            log.d("onDraw() called");
            draw();
        } catch (Exception e) {
            log.e("exception while drawing", e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        log.d("View.onDetachedFromWindow() is called");
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        log.d("onTrackballEvent(" + event + ")");
        if (mSettings.getBool(PROP_APP_TRACKBALL_DISABLED, false)) {
            log.d("trackball is disabled in settings");
            return true;
        }
        mActivity.onUserActivity();
        return super.onTrackballEvent(event);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        log.i("onSizeChanged(" + w + ", " + h + ")");
        super.onSizeChanged(w, h, oldw, oldh);
        mReaderView.requestResize(w, h);
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        if (visibility == VISIBLE)
            mReaderView.startStats();
        else
            mReaderView.stopStats();
        super.onWindowVisibilityChanged(visibility);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus)
            mReaderView.startStats();
        else
            mReaderView.stopStats();
        super.onWindowFocusChanged(hasWindowFocus);
    }

    protected void doDraw(Canvas canvas) {
        try {
            log.d("doDraw() called");
            if (mReaderView.isProgressActive()) {
                log.d("onDraw() -- drawing progress " + (mReaderView.currentProgressPosition / 100));
                mReaderView.drawPageBackground(canvas);
                mReaderView.doDrawProgress(canvas, mReaderView.currentProgressPosition, mReaderView.currentProgressTitle);
            } else if (mReaderView.mInitialized && mReaderView.mCurrentPageInfo != null && mReaderView.mCurrentPageInfo.bitmap != null) {
                log.d("onDraw() -- drawing page image");



                if (mReaderView.currentAnimation != null) {
                    mReaderView.currentAnimation.draw(canvas);
                    return;
                }

                Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                Rect src = new Rect(0, 0,mReaderView.mCurrentPageInfo.bitmap.getWidth(), mReaderView.mCurrentPageInfo.bitmap.getHeight());
                if (dontStretchWhileDrawing) {
                    if (dst.right > src.right)
                        dst.right = src.right;
                    if (dst.bottom > src.bottom)
                        dst.bottom = src.bottom;
                    if (src.right > dst.right)
                        src.right = dst.right;
                    if (src.bottom > dst.bottom)
                        src.bottom = dst.bottom;
                    if (centerPageInsteadOfResizing) {
                        int ddx = (canvas.getWidth() - dst.width()) / 2;
                        int ddy = (canvas.getHeight() - dst.height()) / 2;
                        dst.left += ddx;
                        dst.right += ddx;
                        dst.top += ddy;
                        dst.bottom += ddy;
                    }
                }
                if (dst.width() != canvas.getWidth() || dst.height() != canvas.getHeight())
                    canvas.drawColor(Color.rgb(32, 32, 32));
                mReaderView.drawDimmedBitmap(canvas, mReaderView.mCurrentPageInfo.bitmap, src, dst);
            } else {
                log.d("onDraw() -- drawing empty screen");
                mReaderView.drawPageBackground(canvas);
            }
        } catch (Exception e) {
            log.e("exception while drawing", e);
        }
    }

    @Override
    public void draw() {
        draw(false);
    }

    @Override
    public void draw(boolean isPartially) {
        drawCallback(c -> doDraw(c), null, isPartially);
    }
    public void drawCallback(DrawCanvasCallback callback, Rect rc, boolean isPartially) {

        //synchronized(surfaceLock) { }
        //log.v("draw() - in thread " + Thread.currentThread().getName());
        final SurfaceHolder holder = getHolder();
        //log.v("before synchronized(surfaceLock)");
        if (holder != null)
        //synchronized(surfaceLock)
        {
            Canvas canvas = null;
            long startTs = android.os.SystemClock.uptimeMillis();
            try {
                canvas = holder.lockCanvas(rc);
                //log.v("before draw(canvas)");
                if (canvas != null) {

                    callback.drawTo(canvas);
                }
            } finally {
                //log.v("exiting finally");
                if (canvas != null && getHolder() != null) {
                    //log.v("before unlockCanvasAndPost");
                    if (canvas != null && holder != null) {
                        holder.unlockCanvasAndPost(canvas);
                        //if ( rc==null ) {
                        long endTs = android.os.SystemClock.uptimeMillis();
                        mReaderView.updateAnimationDurationStats(endTs - startTs);
                        //}
                    }
                    //log.v("after unlockCanvasAndPost");
                }
            }
        }
        //log.v("exiting draw()");
    }


}