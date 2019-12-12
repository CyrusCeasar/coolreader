package org.coolreader.crengine;

import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;

public class BaseListView  extends ListView {
	public BaseListView(Context context, boolean fastScrollEnabled) {
		super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setFastScrollEnabled(fastScrollEnabled);
	}


}
