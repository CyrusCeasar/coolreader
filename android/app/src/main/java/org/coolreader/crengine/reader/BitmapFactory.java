package org.coolreader.crengine.reader;

import android.graphics.Bitmap;

import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.VMRuntimeHack;

import java.util.ArrayList;

public class BitmapFactory {
    public static final int MAX_FREE_LIST_SIZE = 2;
    ArrayList<Bitmap> freeList = new ArrayList<Bitmap>();
    ArrayList<Bitmap> usedList = new ArrayList<Bitmap>();

    private final VMRuntimeHack runtime;

    public BitmapFactory(VMRuntimeHack runtime) {
        this.runtime = runtime;
    }

    public synchronized Bitmap get(int dx, int dy) {
        for (int i = 0; i < freeList.size(); i++) {
            Bitmap bmp = freeList.get(i);
            if (bmp.getWidth() == dx && bmp.getHeight() == dy) {
                // found bitmap of proper size
                freeList.remove(i);
                usedList.add(bmp);
                //log.d("BitmapFactory: reused free bitmap, used list = " + usedList.size() + ", free list=" + freeList.size());
                return bmp;
            }
        }
        for (int i = freeList.size() - 1; i >= 0; i--) {
            Bitmap bmp = freeList.remove(i);
            runtime.trackAlloc(bmp.getWidth() * bmp.getHeight() * 2);
            //log.d("Recycling free bitmap "+bmp.getWidth()+"x"+bmp.getHeight());
            //bmp.recycle(); //20110109
        }
        Bitmap bmp = Bitmap.createBitmap(dx, dy, DeviceInfo.BUFFER_COLOR_FORMAT);
        runtime.trackFree(dx * dy * 2);
        //bmp.setDensity(0);
        usedList.add(bmp);
        //log.d("Created new bitmap "+dx+"x"+dy+". New bitmap list size = " + usedList.size());
        return bmp;
    }

    public synchronized void compact() {
        while (freeList.size() > 0) {
            //freeList.get(0).recycle();//20110109
            Bitmap bmp = freeList.remove(0);
            runtime.trackAlloc(bmp.getWidth() * bmp.getHeight() * 2);
        }
    }

    public synchronized void release(Bitmap bmp) {
        for (int i = 0; i < usedList.size(); i++) {
            if (usedList.get(i) == bmp) {
                freeList.add(bmp);
                usedList.remove(i);
                while (freeList.size() > MAX_FREE_LIST_SIZE) {
                    //freeList.get(0).recycle(); //20110109
                    Bitmap b = freeList.remove(0);
                    runtime.trackAlloc(b.getWidth() * b.getHeight() * 2);
                    //b.recycle();
                }
                return;
            }
        }
        // unknown bitmap, just recycle
        //bmp.recycle();//20110109
    }
}