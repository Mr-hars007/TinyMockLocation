package com.mrhars007.mocklocation;

public class PlusCodeUtils {
    private static final String CODE_ALPHABET = "23456789CFGHJMPQRVWX";
    private static final char SEPARATOR = '+';
    private static final char PADDING = '0';
    private static final double[] PAIR_RESOLUTIONS = {20.0, 1.0, 0.05, 0.0025, 0.000125};

    /**
     * Checks if a string looks like a Plus Code (contains '+' and valid characters).
     */
    public static boolean isPlusCode(String code) {
        if (code == null) return false;
        String clean = code.trim().toUpperCase();
        int sepIdx = clean.indexOf(SEPARATOR);
        if (sepIdx < 0 || sepIdx != clean.lastIndexOf(SEPARATOR)) {
            return false;
        }
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == SEPARATOR || c == PADDING) continue;
            if (CODE_ALPHABET.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes latitude and longitude into a 10-character Plus Code.
     */
    public static String encode(double lat, double lon) {
        lat = Math.max(-90.0, Math.min(90.0, lat));
        if (lon == 180.0) lon = 179.999999;
        lon = Math.max(-180.0, Math.min(180.0, lon));

        double south = lat + 90.0;
        double west = lon + 180.0;

        StringBuilder sb = new StringBuilder();
        int pairIdx = 0;
        while (sb.length() < 10) {
            double step = PAIR_RESOLUTIONS[pairIdx];
            int latDigit = (int) (south / step);
            int lonDigit = (int) (west / step);

            latDigit = Math.min(latDigit, 19);
            lonDigit = Math.min(lonDigit, 19);

            south -= latDigit * step;
            west -= lonDigit * step;

            sb.append(CODE_ALPHABET.charAt(latDigit));
            sb.append(CODE_ALPHABET.charAt(lonDigit));

            if (sb.length() == 8) {
                sb.append(SEPARATOR);
            }
            pairIdx++;
        }
        return sb.toString();
    }

    /**
     * Decodes a full or short Plus Code.
     * Returns double[]{ latitude, longitude }.
     */
    public static double[] decode(String code, double refLat, double refLon) throws IllegalArgumentException {
        if (!isPlusCode(code)) {
            throw new IllegalArgumentException("Invalid Plus Code format.");
        }
        String clean = code.trim().toUpperCase();
        int sepPos = clean.indexOf(SEPARATOR);

        if (sepPos < 8) {
            // Recover short code using reference location
            String refFull = encode(refLat, refLon);
            int paddingNeeded = 8 - sepPos;
            if (paddingNeeded % 2 != 0) {
                throw new IllegalArgumentException("Invalid short Plus Code alignment.");
            }
            clean = refFull.substring(0, paddingNeeded) + clean;
            sepPos = 8;
        }

        String rawCode = clean.replace(String.valueOf(SEPARATOR), "").replace(String.valueOf(PADDING), "");

        double south = -90.0;
        double west = -180.0;
        double latStep = 20.0;
        double lonStep = 20.0;

        int pairLen = Math.min(rawCode.length(), 10);
        int i = 0;
        int pairIdx = 0;

        while (i < pairLen) {
            double step = PAIR_RESOLUTIONS[pairIdx];

            char cLat = rawCode.charAt(i);
            int latDigit = CODE_ALPHABET.indexOf(cLat);
            if (latDigit < 0) throw new IllegalArgumentException("Invalid character: " + cLat);
            south += latDigit * step;
            latStep = step;
            i++;

            if (i >= pairLen) break;

            char cLon = rawCode.charAt(i);
            int lonDigit = CODE_ALPHABET.indexOf(cLon);
            if (lonDigit < 0) throw new IllegalArgumentException("Invalid character: " + cLon);
            west += lonDigit * step;
            lonStep = step;
            i++;

            pairIdx++;
        }

        int gridRows = 5;
        int gridCols = 4;

        while (i < rawCode.length()) {
            char cGrid = rawCode.charAt(i);
            int digit = CODE_ALPHABET.indexOf(cGrid);
            if (digit < 0) throw new IllegalArgumentException("Invalid character: " + cGrid);
            int row = digit / gridCols;
            int col = digit % gridCols;

            latStep /= gridRows;
            lonStep /= gridCols;

            south += row * latStep;
            west += col * lonStep;
            i++;
        }

        double centerLat = south + latStep / 2.0;
        double centerLon = west + lonStep / 2.0;

        centerLat = Math.max(-90.0, Math.min(90.0, centerLat));
        centerLon = Math.max(-180.0, Math.min(180.0, centerLon));

        return new double[]{centerLat, centerLon};
    }
}
