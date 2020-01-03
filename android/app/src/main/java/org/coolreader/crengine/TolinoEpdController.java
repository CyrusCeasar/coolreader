package org.coolreader.crengine;

import android.graphics.Rect;
import android.view.View;

public class TolinoEpdController {
  private static final String LOG_TAG = TolinoEpdController.class.getSimpleName();

  public static boolean partialRefresh(View view, int mode, int waveform) {
    Rect r = new Rect();
    view.getGlobalVisibleRect(r);
    return partialRefresh(r.left, r.top, r.right, r.bottom, mode, waveform);
  }

  public static boolean partialRefresh(int left, int top, int right, int bottom, int mode, int waveform) {
    if ((right - left > 1) && (bottom - top > 1)) {
      return ScreenHelper.RegionalRefresh(left, top, right, bottom, mode, waveform) == 1;
    }
    return false;
  }


}
