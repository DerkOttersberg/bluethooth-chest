package com.derk.easyinventorycrafter.util;

public final class FormatUtils {
    private FormatUtils() {}

    public static String formatCount(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        if (count < 1000000) {
            return (count / 1000) + "k";
        }
        if (count < 1000000000) {
            return (count / 1000000) + "M";
        }
        return (count / 1000000000) + "B";
    }
}
