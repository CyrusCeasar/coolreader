package org.coolreader.crengine.reader;

import android.graphics.Canvas;

public interface ViewAnimationControl {
        void update(int x, int y);

        void stop(int x, int y);

        void animate();

        void move(int duration, boolean accelerated);

        boolean isStarted();

        void draw(Canvas canvas);
    }