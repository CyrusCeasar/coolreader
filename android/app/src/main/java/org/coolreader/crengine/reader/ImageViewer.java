package org.coolreader.crengine.reader;

import android.view.GestureDetector;
import android.view.MotionEvent;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderActivity;

public class ImageViewer extends GestureDetector.SimpleOnGestureListener {

    private ReaderActivity mActivity;
    private ImageInfo currentImage;
    private ReaderView mReaderView;
    final GestureDetector detector;
    int oldOrientation;

    public ImageViewer(ImageInfo image,ReaderActivity activity,ReaderView readerView) {
        lockOrientation();
        detector = new GestureDetector(this);
        if (image.bufHeight / image.height >= 2 && image.bufWidth / image.width >= 2) {
            image.scaledHeight *= 2;
            image.scaledWidth *= 2;
        }
        centerIfLessThanScreen(image);
        currentImage = image;
        mActivity = activity;
        mReaderView = readerView;
    }

    private void lockOrientation() {
        oldOrientation = mActivity.getScreenOrientation();
        if (oldOrientation == 4)
            mActivity.setScreenOrientation(mActivity.getOrientationFromSensor());
    }

    private void unlockOrientation() {
        if (oldOrientation == 4)
            mActivity.setScreenOrientation(oldOrientation);
    }

    private void centerIfLessThanScreen(ImageInfo image) {
        if (image.scaledHeight < image.bufHeight)
            image.y = (image.bufHeight - image.scaledHeight) / 2;
        if (image.scaledWidth < image.bufWidth)
            image.x = (image.bufWidth - image.scaledWidth) / 2;
    }

    private void fixScreenBounds(ImageInfo image) {
        if (image.scaledHeight > image.bufHeight) {
            if (image.y < image.bufHeight - image.scaledHeight)
                image.y = image.bufHeight - image.scaledHeight;
            if (image.y > 0)
                image.y = 0;
        }
        if (image.scaledWidth > image.bufWidth) {
            if (image.x < image.bufWidth - image.scaledWidth)
                image.x = image.bufWidth - image.scaledWidth;
            if (image.x > 0)
                image.x = 0;
        }
    }

    private void updateImage(ImageInfo image) {
        centerIfLessThanScreen(image);
        fixScreenBounds(image);
        if (!currentImage.equals(image)) {
            currentImage = image;
            mReaderView.drawPage();
        }
    }

    public void zoomIn() {
        ImageInfo image = new ImageInfo(currentImage);
        if (image.scaledHeight >= image.height) {
            int scale = image.scaledHeight / image.height;
            if (scale < 4)
                scale++;
            image.scaledHeight = image.height * scale;
            image.scaledWidth = image.width * scale;
        } else {
            int scale = image.height / image.scaledHeight;
            if (scale > 1)
                scale--;
            image.scaledHeight = image.height / scale;
            image.scaledWidth = image.width / scale;
        }
        updateImage(image);
    }

    public void zoomOut() {
        ImageInfo image = new ImageInfo(currentImage);
        if (image.scaledHeight > image.height) {
            int scale = image.scaledHeight / image.height;
            if (scale > 1)
                scale--;
            image.scaledHeight = image.height * scale;
            image.scaledWidth = image.width * scale;
        } else {
            int scale = image.height / image.scaledHeight;
            if (image.scaledHeight > image.bufHeight || image.scaledWidth > image.bufWidth)
                scale++;
            image.scaledHeight = image.height / scale;
            image.scaledWidth = image.width / scale;
        }
        updateImage(image);
    }

    public int getStep() {
        ImageInfo image = currentImage;
        int max = image.bufHeight;
        if (max < image.bufWidth)
            max = image.bufWidth;
        return max / 10;
    }

    public void moveBy(int dx, int dy) {
        ImageInfo image = new ImageInfo(currentImage);
        image.x += dx;
        image.y += dy;
        updateImage(image);
    }


     boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {
        int dx = (int) distanceX;
        int dy = (int) distanceY;
        moveBy(-dx, -dy);
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        ImageInfo image = new ImageInfo(currentImage);

        int x = (int) e.getX();
        int y = (int) e.getY();

        int zone = 0;
        int zw = mActivity.getDensityDpi() / 2;
        int w = image.bufWidth;
        int h = image.bufHeight;
        if (image.rotation == 0) {
            if (x < zw && y > h - zw)
                zone = 1;
            if (x > w - zw && y > h - zw)
                zone = 2;
        } else {
            if (x < zw && y < zw)
                zone = 1;
            if (x < zw && y > h - zw)
                zone = 2;
        }
        if (zone != 0) {
            if (zone == 1)
                zoomIn();
            else
                zoomOut();
            return true;
        }

        close();
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    public void close() {
        if (mReaderView.currentImageViewer == null)
            return;
        mReaderView.currentImageViewer = null;
        unlockOrientation();
        BackgroundThread.instance().postBackground(new Runnable() {
            @Override
            public void run() {
                mReaderView.doc.closeImage();
            }
        });
        mReaderView.drawPage();
    }

    public BitmapInfo prepareImage() {
        // called from background thread
        ImageInfo img = currentImage;
        img.bufWidth = mReaderView.internalDX;
        img.bufHeight = mReaderView.internalDY;
        if (mReaderView.mCurrentPageInfo != null) {
            if (img.equals(mReaderView.mCurrentPageInfo.imageInfo))
                return mReaderView.mCurrentPageInfo;
            mReaderView.mCurrentPageInfo.recycle();
            mReaderView.mCurrentPageInfo = null;
        }
        PositionProperties currpos = mReaderView.doc.getPositionProps(null);
        BitmapInfo bi = new BitmapInfo(mReaderView.factory);
        bi.imageInfo = new ImageInfo(img);
        bi.bitmap = mReaderView.factory.get(mReaderView.internalDX, mReaderView.internalDY);
        bi.position = currpos;
        mReaderView.doc.drawImage(bi.bitmap, bi.imageInfo);
        mReaderView.mCurrentPageInfo = bi;
        return mReaderView.mCurrentPageInfo;
    }

}