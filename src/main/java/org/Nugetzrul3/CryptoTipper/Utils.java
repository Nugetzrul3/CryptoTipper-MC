package org.Nugetzrul3.CryptoTipper;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {
    public static String getHashFormat(float hashes) {
        if (hashes < 1e3) {
            return hashes + " H/s";
        } else if (hashes > 1e3 && hashes < 1e6) {
            return hashes / 1e3 + " KH/s";
        } else if (hashes > 1e6 && hashes < 1e9) {
            return hashes / 1e6 + " MH/s";
        } else if (hashes > 1e9 && hashes < 1e12) {
            return hashes / 1e9 + " GH/s";
        } else if (hashes > 1e12 && hashes < 1e15) {
            return hashes / 1e12 + " TH/s";
        } else if (hashes > 1e15 && hashes < 1e18) {
            return hashes / 1e15 + " PH/s";
        } else if (hashes > 1e18 && hashes < 1e21) {
            return hashes / 1e18 + " EH/s";
        } else {
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

    public static JsonObject calculateSupplyAndReward(int height) {
        JsonObject result = new JsonObject();

        int halvings = 300_000;
        BigDecimal reward = BigDecimal.valueOf(satoshis((int) (300 - (300 * 0.1))));
        int halving_count = 0;
        BigDecimal supply = reward;

        while (height > halvings) {
            BigDecimal total = BigDecimal.valueOf(halvings).multiply(supply);
            reward = reward.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
            height -= halvings;
            halving_count += 1;

            supply = supply.add(total);
        }

        supply = supply.add(BigDecimal.valueOf(height).multiply(reward));
        reward = reward.divide(BigDecimal.valueOf(Math.pow(10, 8)), RoundingMode.CEILING);
        supply = supply.divide(BigDecimal.valueOf(Math.pow(10, 8)), RoundingMode.CEILING);

        result.addProperty("halvings", halving_count);
        result.addProperty("supply", supply.toPlainString());    // toPlainString() avoids scientific notation
        result.addProperty("reward", reward.toPlainString());    // toPlainString() avoids scientific notation

        return result;
    }

    private static double satoshis(int value) {
        return Math.ceil(value * Math.pow(10, 8));
    }
}
