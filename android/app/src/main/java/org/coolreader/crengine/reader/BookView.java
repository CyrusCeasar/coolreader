package org.coolreader.crengine.reader;

public interface BookView {
        void draw();

        void draw(boolean isPartially);

        void invalidate();

        void onPause();

        void onResume();
    }
