package org.coolreader.crengine.reader;

import org.coolreader.crengine.Selection;

public interface MoveSelectionCallback {
        // selection is changed
        public void onNewSelection(Selection selection);

        // cannot move selection
        public void onFail();
    }