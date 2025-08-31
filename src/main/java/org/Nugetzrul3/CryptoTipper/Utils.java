package org.Nugetzrul3.CryptoTipper;

public class Utils {
    public static String getHashFormat(float hashes) {
        if (hashes < 1e3) {
            return hashes + " H/s";
        }
        else if (hashes > 1e3 && hashes < 1e6) {
            return hashes / 1e3 + " KH/s";
        }
        else if (hashes > 1e6 && hashes < 1e9) {
            return hashes / 1e6 + " MH/s";
        }
        else if (hashes > 1e9 && hashes < 1e12) {
            return hashes / 1e9 + " GH/s";
        }
        else if (hashes > 1e12 && hashes < 1e15) {
            return hashes / 1e12 + " TH/s";
        }
        else if (hashes > 1e15 && hashes < 1e18) {
            return hashes / 1e15 + " PH/s";
        }
        else if (hashes > 1e18 && hashes < 1e21) {
            return hashes / 1e18 + " EH/s";
        }
        else {
            return null;
        }
    }

    public static boolean isDouble(String amount) {
        try {
            Double.parseDouble(amount);
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }
}
