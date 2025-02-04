package flopbot.util;

import java.text.DecimalFormat;

public class NumberFormat {

    public static String formatDouble(double value) {
        if (value == 0) {
            return "0.00";
        } else if (value == Math.floor(value)) {
            DecimalFormat intFormat = new DecimalFormat("#,##0");
            return intFormat.format(value);
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            return decimalFormat.format(value);
        }
    }
}
