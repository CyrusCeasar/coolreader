package org.coolreader.crengine.reader;

import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.Settings;

public class ZoneClickHelper implements Settings {



    public static int getTapZone(int x, int y, int dx, int dy) {
        int x1 = dx / 3;
        int x2 = dx * 2 / 3;
        int y1 = dy / 3;
        int y2 = dy * 2 / 3;
        int zone = 0;
        if (y < y1) {
            if (x < x1)
                zone = 1;
            else if (x < x2)
                zone = 2;
            else
                zone = 3;
        } else if (y < y2) {
            if (x < x1)
                zone = 4;
            else if (x < x2)
                zone = 5;
            else
                zone = 6;
        } else {
            if (x < x1)
                zone = 7;
            else if (x < x2)
                zone = 8;
            else
                zone = 9;
        }
        return zone;
    }

    public static ReaderAction findTapZoneAction(boolean doubleTapSelectionEnabled,Properties mSettings,int secondaryTapActionType, int zone, int tapActionType) {
        ReaderAction action = ReaderAction.NONE;
        boolean isSecondaryAction = (secondaryTapActionType == tapActionType);
        if (tapActionType == TAP_ACTION_TYPE_SHORT) {
            action = ReaderAction.findForTap(zone, mSettings);
        } else {
            if (isSecondaryAction)
                action = ReaderAction.findForLongTap(zone, mSettings);
            else if (doubleTapSelectionEnabled || tapActionType == TAP_ACTION_TYPE_LONGPRESS)
                action = ReaderAction.START_SELECTION;
        }
        return action;
    }

}
