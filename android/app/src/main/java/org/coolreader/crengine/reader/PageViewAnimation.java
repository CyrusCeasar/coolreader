package org.coolreader.crengine.reader;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderCommand;

import static org.coolreader.crengine.reader.Accelerater.accelerate;
import static org.coolreader.crengine.reader.ReaderView.PAGE_ANIMATION_PAPER;
import static org.coolreader.crengine.reader.ReaderView.PAGE_ANIMATION_SLIDE2;

class PageViewAnimation extends ViewAnimationBase {


    private final static int SIN_TABLE_SIZE = 1024;
    private final static int SIN_TABLE_SCALE = 0x10000;
    private final static int PI_DIV_2 = (int) (Math.PI / 2 * SIN_TABLE_SCALE);
    /// sin table, for 0..PI/2
    private static int[] SIN_TABLE = new int[SIN_TABLE_SIZE + 1];
    private static int[] ASIN_TABLE = new int[SIN_TABLE_SIZE + 1];
    // mapping of 0..1 shift to angle
    private static int[] SRC_TABLE = new int[SIN_TABLE_SIZE + 1];
    // mapping of 0..1 shift to sin(angle)
    private static int[] DST_TABLE = new int[SIN_TABLE_SIZE + 1];

    // for dx=0..1 find such alpha (0..pi/2) that alpha - sin(alpha) = dx
    private static double shiftfn(double dx) {
        double a = 0;
        double b = Math.PI / 2;
        double c = 0;
        for (int i = 0; i < 15; i++) {
            c = (a + b) / 2;
            double cq = c - Math.sin(c);
            if (cq < dx)
                a = c;
            else
                b = c;
        }
        return c;
    }

    static {
        for (int i = 0; i <= SIN_TABLE_SIZE; i++) {
            double angle = Math.PI / 2 * i / SIN_TABLE_SIZE;
            int s = (int) Math.round(Math.sin(angle) * SIN_TABLE_SCALE);
            SIN_TABLE[i] = s;
            double x = (double) i / SIN_TABLE_SIZE;
            s = (int) Math.round(Math.asin(x) * SIN_TABLE_SCALE);
            ASIN_TABLE[i] = s;

            double dx = i * (Math.PI / 2 - 1.0) / SIN_TABLE_SIZE;
            angle = shiftfn(dx);
            SRC_TABLE[i] = (int) Math.round(angle * SIN_TABLE_SCALE);
            DST_TABLE[i] = (int) Math.round(Math.sin(angle) * SIN_TABLE_SCALE);
        }
    }


    int startX;
    int maxX;
    int page1;
    int page2;
    int direction;
    int currShift;
    int destShift;
    int pageCount;
    Paint divPaint;
    Paint[] shadePaints;
    Paint[] hilitePaints;
    private final boolean naturalPageFlip;
    private final boolean flipTwoPages;

    BitmapInfo image1;
    BitmapInfo image2;

    PageViewAnimation(ReaderView readerView, int startX, int maxX, int direction) {
        super(readerView);
        this.startX = startX;
        this.maxX = maxX;
        this.direction = direction;
        this.currShift = 0;
        this.destShift = 0;
        this.naturalPageFlip = (mReaderView.pageFlipAnimationMode == PAGE_ANIMATION_PAPER);
        this.flipTwoPages = (mReaderView.pageFlipAnimationMode == PAGE_ANIMATION_SLIDE2);

        long start = android.os.SystemClock.uptimeMillis();

        PositionProperties currPos = mReaderView.mCurrentPageInfo == null ? null : mReaderView.mCurrentPageInfo.position;
        if (currPos == null)
            currPos = mReaderView.doc.getPositionProps(null);
        page1 = currPos.pageNumber;
        page2 = currPos.pageNumber + direction;
        if (page2 < 0 || page2 >= currPos.pageCount) {
            mReaderView.currentAnimation = null;
            return;
        }
        this.pageCount = currPos.pageMode;
        image1 = mReaderView.preparePageImage(0);
        image2 = mReaderView.preparePageImage(direction);
        if (image1 == null || image2 == null) {
            return;
        }
        if (page1 == page2) {
            return;
        }
        page2 = image2.position.pageNumber;
        mReaderView.currentAnimation = this;
        divPaint = new Paint();
        divPaint.setStyle(Paint.Style.FILL);
        divPaint.setColor(mReaderView.mActivity.isNightMode() ? Color.argb(96, 64, 64, 64) : Color.argb(128, 128, 128, 128));
        final int numPaints = 16;
        shadePaints = new Paint[numPaints];
        hilitePaints = new Paint[numPaints];
        for (int i = 0; i < numPaints; i++) {
            shadePaints[i] = new Paint();
            hilitePaints[i] = new Paint();
            hilitePaints[i].setStyle(Paint.Style.FILL);
            shadePaints[i].setStyle(Paint.Style.FILL);
            if (mReaderView.mActivity.isNightMode()) {
                shadePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 0, 0, 0));
                hilitePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 64, 64, 64));
            } else {
                shadePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 0, 0, 0));
                hilitePaints[i].setColor(Color.argb((i + 1) * 96 / numPaints, 255, 255, 255));
            }
        }


        long duration = android.os.SystemClock.uptimeMillis() - start;
    }

    private void drawGradient(Canvas canvas, Rect rc, Paint[] paints, int startIndex, int endIndex) {
        int n = (startIndex < endIndex) ? endIndex - startIndex + 1 : startIndex - endIndex + 1;
        int dir = (startIndex < endIndex) ? 1 : -1;
        int dx = rc.right - rc.left;
        Rect rect = new Rect(rc);
        for (int i = 0; i < n; i++) {
            int index = startIndex + i * dir;
            int x1 = rc.left + dx * i / n;
            int x2 = rc.left + dx * (i + 1) / n;
            if (x2 > rc.right)
                x2 = rc.right;
            rect.left = x1;
            rect.right = x2;
            if (x2 > x1) {
                canvas.drawRect(rect, paints[index]);
            }
        }
    }

    private void drawShadow(Canvas canvas, Rect rc) {
        drawGradient(canvas, rc, shadePaints, shadePaints.length / 2, shadePaints.length / 10);
    }

    private final static int DISTORT_PART_PERCENT = 30;

    private void drawDistorted(Canvas canvas, Bitmap bmp, Rect src, Rect dst, int dir) {
        int srcdx = src.width();
        int dstdx = dst.width();
        int dx = srcdx - dstdx;
        int maxdistortdx = srcdx * DISTORT_PART_PERCENT / 100;
        int maxdx = maxdistortdx * (PI_DIV_2 - SIN_TABLE_SCALE) / SIN_TABLE_SCALE;
        int maxdistortsrc = maxdistortdx * PI_DIV_2 / SIN_TABLE_SCALE;

        int distortdx = dx < maxdistortdx ? dx : maxdistortdx;
        int distortsrcstart = -1;
        int distortsrcend = -1;
        int distortdststart = -1;
        int distortdstend = -1;
        int distortanglestart = -1;
        int distortangleend = -1;
        int normalsrcstart = -1;
        int normalsrcend = -1;
        int normaldststart = -1;
        int normaldstend = -1;

        if (dx < maxdx) {
            // start
            int index = dx >= 0 ? dx * SIN_TABLE_SIZE / maxdx : 0;
            if (index > DST_TABLE.length)
                index = DST_TABLE.length;
            int dstv = DST_TABLE[index] * maxdistortdx / SIN_TABLE_SCALE;
            distortdststart = distortsrcstart = dstdx - dstv;
            distortsrcend = srcdx;
            distortdstend = dstdx;
            normalsrcstart = normaldststart = 0;
            normalsrcend = distortsrcstart;
            normaldstend = distortdststart;
            distortanglestart = 0;
            distortangleend = SRC_TABLE[index];
            distortdx = maxdistortdx;
        } else if (dstdx > maxdistortdx) {
            // middle
            distortdststart = distortsrcstart = dstdx - maxdistortdx;
            distortsrcend = distortsrcstart + maxdistortsrc;
            distortdstend = dstdx;
            normalsrcstart = normaldststart = 0;
            normalsrcend = distortsrcstart;
            normaldstend = distortdststart;
            distortanglestart = 0;
            distortangleend = PI_DIV_2;
        } else {
            // end
            normalsrcstart = normaldststart = normalsrcend = normaldstend = -1;
            distortdx = dstdx;
            distortsrcstart = 0;
            int n = maxdistortdx >= dstdx ? maxdistortdx - dstdx : 0;
            distortsrcend = ASIN_TABLE[SIN_TABLE_SIZE * n / maxdistortdx] * maxdistortsrc / SIN_TABLE_SCALE;
            distortdststart = 0;
            distortdstend = dstdx;
            distortangleend = PI_DIV_2;
            n = maxdistortdx >= distortdx ? maxdistortdx - distortdx : 0;
            distortanglestart = ASIN_TABLE[SIN_TABLE_SIZE * (maxdistortdx - distortdx) / maxdistortdx];
        }

        Rect srcrc = new Rect(src);
        Rect dstrc = new Rect(dst);
        if (normalsrcstart < normalsrcend) {
            if (dir > 0) {
                srcrc.left = src.left + normalsrcstart;
                srcrc.right = src.left + normalsrcend;
                dstrc.left = dst.left + normaldststart;
                dstrc.right = dst.left + normaldstend;
            } else {
                srcrc.right = src.right - normalsrcstart;
                srcrc.left = src.right - normalsrcend;
                dstrc.right = dst.right - normaldststart;
                dstrc.left = dst.right - normaldstend;
            }
            mReaderView.drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
        }
        if (distortdststart < distortdstend) {
            int n = distortdx / 5 + 1;
            int dst0 = SIN_TABLE[distortanglestart * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE;
            int src0 = distortanglestart * maxdistortdx / SIN_TABLE_SCALE;
            for (int i = 0; i < n; i++) {
                int angledelta = distortangleend - distortanglestart;
                int startangle = distortanglestart + i * angledelta / n;
                int endangle = distortanglestart + (i + 1) * angledelta / n;
                int src1 = startangle * maxdistortdx / SIN_TABLE_SCALE - src0;
                int src2 = endangle * maxdistortdx / SIN_TABLE_SCALE - src0;
                int dst1 = SIN_TABLE[startangle * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE - dst0;
                int dst2 = SIN_TABLE[endangle * SIN_TABLE_SIZE / PI_DIV_2] * maxdistortdx / SIN_TABLE_SCALE - dst0;
                int hiliteIndex = startangle * hilitePaints.length / PI_DIV_2;
                Paint[] paints;
                if (dir > 0) {
                    dstrc.left = dst.left + distortdststart + dst1;
                    dstrc.right = dst.left + distortdststart + dst2;
                    srcrc.left = src.left + distortsrcstart + src1;
                    srcrc.right = src.left + distortsrcstart + src2;
                    paints = hilitePaints;
                } else {
                    dstrc.right = dst.right - distortdststart - dst1;
                    dstrc.left = dst.right - distortdststart - dst2;
                    srcrc.right = src.right - distortsrcstart - src1;
                    srcrc.left = src.right - distortsrcstart - src2;
                    paints = shadePaints;
                }
                mReaderView.drawDimmedBitmap(canvas, bmp, srcrc, dstrc);
                canvas.drawRect(dstrc, paints[hiliteIndex]);
            }
        }
    }

    @Override
    public void move(int duration, boolean accelerated) {
        if (duration > 0 && mReaderView.pageFlipAnimationSpeedMs != 0) {
            int steps = (int) (duration / mReaderView.getAvgAnimationDrawDuration()) + 2;
            int x0 = currShift;
            int x1 = destShift;
            if ((x0 - x1) < 10 && (x0 - x1) > -10)
                steps = 2;
            for (int i = 1; i < steps; i++) {
                int x = x0 + (x1 - x0) * i / steps;
                currShift = accelerated ? accelerate(x0, x1, x) : x;
                draw();
            }
        }
        currShift = destShift;
        draw();
    }

    @Override
    public void stop(int x, int y) {
        if (mReaderView.currentAnimation == null)
            return;
        //if ( started ) {
        boolean moved = false;
        if (x != -1) {
            int threshold = mReaderView.mActivity.getPalmTipPixels() * 7 / 8;
            if (direction > 0) {
                // |  <=====  |
                int dx = startX - x;
                if (dx > threshold)
                    moved = true;
            } else {
                // |  =====>  |
                int dx = x - startX;
                if (dx > threshold)
                    moved = true;
            }
            int duration;
            if (moved) {
                destShift = maxX;
                duration = 300; // 500 ms forward
            } else {
                destShift = 0;
                duration = 200; // 200 ms cancel
            }
            move(duration, false);
        } else {
            moved = true;
        }
        mReaderView.doc.doCommand(ReaderCommand.DCMD_GO_PAGE_DONT_SAVE_HISTORY.nativeId, moved ? page2 : page1);
        //}
        mReaderView.mBookMarkManager.scheduleSaveCurrentPositionBookmark();
        close();
        // preparing images for next page flip
        mReaderView.preparePageImage(0);
        mReaderView.preparePageImage(direction);
        mReaderView.updateCurrentPositionStatus();
        //if ( started )
        //	drawPage();
    }

    @Override
    public void update(int x, int y) {
        int delta = direction > 0 ? startX - x : x - startX;
        if (delta <= 0)
            destShift = 0;
        else if (delta < maxX)
            destShift = delta;
        else
            destShift = maxX;
    }

    public void animate() {
        //log.d("animate() is called");
        if (currShift != destShift) {
            started = true;
            if (mReaderView.pageFlipAnimationSpeedMs == 0)
                currShift = destShift;
            else {
                int delta = currShift - destShift;
                if (delta < 0)
                    delta = -delta;
                long avgDraw = mReaderView.getAvgAnimationDrawDuration();
                int maxStep = mReaderView.pageFlipAnimationSpeedMs > 0 ? (int) (maxX * 1000 / avgDraw / mReaderView.pageFlipAnimationSpeedMs) : maxX;
                int step;
                if (delta > maxStep * 2)
                    step = maxStep;
                else
                    step = (delta + 3) / 4;
                //int step = delta<3 ? 1 : (delta<5 ? 2 : (delta<10 ? 3 : (delta<15 ? 6 : (delta<25 ? 10 : (delta<50 ? 15 : 30)))));
                if (currShift < destShift)
                    currShift += step;
                else if (currShift > destShift)
                    currShift -= step;
            }
            //pointerCurrPos = pointerDestPos;
            draw();
            if (currShift != destShift)
                mReaderView.scheduleAnimation();
        }
    }

    public void draw(Canvas canvas) {
//			BitmapInfo image1 = mCurrentPageInfo;
//			BitmapInfo image2 = mNextPageInfo;
        if (image1.isReleased() || image2.isReleased())
            return;
        int w = image1.bitmap.getWidth();
        int h = image1.bitmap.getHeight();
        int div;
        if (direction > 0) {
            // FORWARD
            div = w - currShift;
            Rect shadowRect = new Rect(div, 0, div + w / 10, h);
            if (naturalPageFlip) {
                if (this.pageCount == 2) {
                    int w2 = w / 2;
                    if (div < w2) {
                        // left - part of old page
                        Rect src1 = new Rect(0, 0, div, h);
                        Rect dst1 = new Rect(0, 0, div, h);
                        mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                        // left, resized part of new page
                        Rect src2 = new Rect(0, 0, w2, h);
                        Rect dst2 = new Rect(div, 0, w2, h);
                        //canvas.drawBitmap(image2.bitmap, src2, dst2, null);
                        drawDistorted(canvas, image2.bitmap, src2, dst2, -1);
                        // right, new page
                        Rect src3 = new Rect(w2, 0, w, h);
                        Rect dst3 = new Rect(w2, 0, w, h);
                        mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

                    } else {
                        // left - old page
                        Rect src1 = new Rect(0, 0, w2, h);
                        Rect dst1 = new Rect(0, 0, w2, h);
                        mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                        // right, resized old page
                        Rect src2 = new Rect(w2, 0, w, h);
                        Rect dst2 = new Rect(w2, 0, div, h);
                        //canvas.drawBitmap(image1.bitmap, src2, dst2, null);
                        drawDistorted(canvas, image1.bitmap, src2, dst2, 1);
                        // right, new page
                        Rect src3 = new Rect(div, 0, w, h);
                        Rect dst3 = new Rect(div, 0, w, h);
                        mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src3, dst3);

                        if (div > 0 && div < w)
                            drawShadow(canvas, shadowRect);
                    }
                } else {
                    Rect src1 = new Rect(0, 0, w, h);
                    Rect dst1 = new Rect(0, 0, w - currShift, h);
                    //log.v("drawing " + image1);
                    //canvas.drawBitmap(image1.bitmap, src1, dst1, null);
                    drawDistorted(canvas, image1.bitmap, src1, dst1, 1);
                    Rect src2 = new Rect(w - currShift, 0, w, h);
                    Rect dst2 = new Rect(w - currShift, 0, w, h);
                    //log.v("drawing " + image1);
                    mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);

                    if (div > 0 && div < w)
                        drawShadow(canvas, shadowRect);
                }
            } else {
                if (flipTwoPages) {
                    Rect src1 = new Rect(currShift, 0, w, h);
                    Rect dst1 = new Rect(0, 0, w - currShift, h);
                    //log.v("drawing " + image1);
                    mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                    Rect src2 = new Rect(0, 0, currShift, h);
                    Rect dst2 = new Rect(w - currShift, 0, w, h);
                    //log.v("drawing " + image1);
                    mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
                } else {
                    Rect src1 = new Rect(currShift, 0, w, h);
                    Rect dst1 = new Rect(0, 0, w - currShift, h);
                    //log.v("drawing " + image1);
                    mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                    Rect src2 = new Rect(w - currShift, 0, w, h);
                    Rect dst2 = new Rect(w - currShift, 0, w, h);
                    //log.v("drawing " + image1);
                    mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
                }
            }
        } else {
            // BACK
            div = currShift;
            Rect shadowRect = new Rect(div, 0, div + 10, h);
            if (naturalPageFlip) {
                if (this.pageCount == 2) {
                    int w2 = w / 2;
                    if (div < w2) {
                        // left - part of old page
                        Rect src1 = new Rect(0, 0, div, h);
                        Rect dst1 = new Rect(0, 0, div, h);
                        mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
                        // left, resized part of new page
                        Rect src2 = new Rect(0, 0, w2, h);
                        Rect dst2 = new Rect(div, 0, w2, h);
                        //canvas.drawBitmap(image1.bitmap, src2, dst2, null);
                        drawDistorted(canvas, image1.bitmap, src2, dst2, -1);
                        // right, new page
                        Rect src3 = new Rect(w2, 0, w, h);
                        Rect dst3 = new Rect(w2, 0, w, h);
                        mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);
                    } else {
                        // left - old page
                        Rect src1 = new Rect(0, 0, w2, h);
                        Rect dst1 = new Rect(0, 0, w2, h);
                        mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src1, dst1);
                        // right, resized old page
                        Rect src2 = new Rect(w2, 0, w, h);
                        Rect dst2 = new Rect(w2, 0, div, h);
                        //canvas.drawBitmap(image2.bitmap, src2, dst2, null);
                        drawDistorted(canvas, image2.bitmap, src2, dst2, 1);
                        // right, new page
                        Rect src3 = new Rect(div, 0, w, h);
                        Rect dst3 = new Rect(div, 0, w, h);
                        mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src3, dst3);

                        if (div > 0 && div < w)
                            drawShadow(canvas, shadowRect);
                    }
                } else {
                    Rect src1 = new Rect(currShift, 0, w, h);
                    Rect dst1 = new Rect(currShift, 0, w, h);
                    mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                    Rect src2 = new Rect(0, 0, w, h);
                    Rect dst2 = new Rect(0, 0, currShift, h);
                    //canvas.drawBitmap(image2.bitmap, src2, dst2, null);
                    drawDistorted(canvas, image2.bitmap, src2, dst2, 1);

                    if (div > 0 && div < w)
                        drawShadow(canvas, shadowRect);
                }
            } else {
                if (flipTwoPages) {
                    Rect src1 = new Rect(0, 0, w - currShift, h);
                    Rect dst1 = new Rect(currShift, 0, w, h);
                    mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                    Rect src2 = new Rect(w - currShift, 0, w, h);
                    Rect dst2 = new Rect(0, 0, currShift, h);
                    mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
                } else {
                    Rect src1 = new Rect(currShift, 0, w, h);
                    Rect dst1 = new Rect(currShift, 0, w, h);
                    mReaderView.drawDimmedBitmap(canvas, image1.bitmap, src1, dst1);
                    Rect src2 = new Rect(w - currShift, 0, w, h);
                    Rect dst2 = new Rect(0, 0, currShift, h);
                    mReaderView.drawDimmedBitmap(canvas, image2.bitmap, src2, dst2);
                }
            }
        }
        if (div > 0 && div < w) {
            canvas.drawLine(div, 0, div, h, divPaint);
        }
    }
}