package com.bancoppel.HipotecarioPLD.Util;

public class sanitizeLog {

	public static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\n\r\t]", "")
                    .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                    .trim();
    }
}

