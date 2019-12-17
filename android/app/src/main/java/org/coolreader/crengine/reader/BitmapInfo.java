package org.coolreader.crengine.reader;

import android.graphics.Bitmap;

import org.coolreader.crengine.ImageInfo;
import org.coolreader.crengine.PositionProperties;

class BitmapInfo {
    Bitmap bitmap;
    PositionProperties position;
    ImageInfo imageInfo;
    BitmapFactory mBitmapFactory;
    public BitmapInfo(BitmapFactory bitmapFactory){
        mBitmapFactory = bitmapFactory;
    }
        void recycle() {
            mBitmapFactory.release(bitmap);
            bitmap = null;
            position = null;
            imageInfo = null;
        }

        boolean isReleased() {
            return bitmap == null;
        }

        @Override
        public String toString() {
            return "BitmapInfo [position=" + position + "]";
        }

    }