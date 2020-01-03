package org.coolreader.crengine.reader;

import org.coolreader.crengine.Selection;

public interface MoveSelectionCallback {
        // selection is changed
        void onNewSelection(Selection selection);

        // cannot move selection
        void onFail();
    }