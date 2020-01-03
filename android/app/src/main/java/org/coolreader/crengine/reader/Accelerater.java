package org.coolreader.crengine.reader;

public class Accelerater {

    private static final int[] accelerationShape = new int[]{
            0, 6, 24, 54, 95, 146, 206, 273, 345, 421, 500, 578, 654, 726, 793, 853, 904, 945, 975, 993, 1000
    };

    static public int accelerate(int x0, int x1, int x) {
        if (x < x0)
            x = x0;
        if (x > x1)
            x = x1;
        int intervals = accelerationShape.length - 1;
        int pos = x1 > x0 ? 100 * intervals * (x - x0) / (x1 - x0) : x1;
        int interval = pos / 100;
        int part = pos % 100;
        if (interval < 0)
            interval = 0;
        else if (interval > intervals)
            interval = intervals;
        int y = interval == intervals ? 100000 : accelerationShape[interval] * 100 + (accelerationShape[interval + 1] - accelerationShape[interval]) * part;
        return x0 + (x1 - x0) * y / 100000;
    }
}
