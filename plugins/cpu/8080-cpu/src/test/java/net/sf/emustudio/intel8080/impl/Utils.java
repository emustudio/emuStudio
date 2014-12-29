package net.sf.emustudio.intel8080.impl;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Utils {

    public static short[] concat (short[] a, short[] b) {
        int aLen = a.length;
        int bLen = b.length;

        short[] c = new short[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static <T> T[] concat (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
