package org.coolreader.crengine.reader;

import android.graphics.Canvas;

public interface ViewAnimationControl {
        public void update(int x, int y);

        public void stop(int x, int y);

        public void animate();

        public void move(int duration, boolean accelerated);

        public boolean isStarted();

        abstract void draw(Canvas canvas);
    }