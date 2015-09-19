package net.sf.emustudio.cpu.testsuite.injectors;

import java.util.Arrays;

public class Utils {

    public static <T> String toHexString(T... array) {
        String[] result = new String[array.length];

        for (int i = 0; i < array.length; i++) {
            result[i] = String.format("%02x", array[i]);
        }
        return Arrays.toString(result);
    }
}
