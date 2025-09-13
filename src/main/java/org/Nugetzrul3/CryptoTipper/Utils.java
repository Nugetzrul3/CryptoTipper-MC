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

        final int HALVINGS_INTERVAL = 300_000;
        BigDecimal reward = satoshis(270);
        int halvingCount = 0;
        BigDecimal supply = BigDecimal.ZERO;

        while (height > HALVINGS_INTERVAL) {
            BigDecimal total = BigDecimal.valueOf(HALVINGS_INTERVAL).multiply(reward);
            supply = supply.add(total);

            reward = reward.divide(BigDecimal.valueOf(2), RoundingMode.DOWN);
            height -= HALVINGS_INTERVAL;
            halvingCount++;
        }

        supply = supply.add(BigDecimal.valueOf(height).multiply(reward));

        BigDecimal divisor = BigDecimal.TEN.pow(8);
        BigDecimal finalReward = reward.divide(divisor, 8, RoundingMode.DOWN);
        BigDecimal finalSupply = supply.divide(divisor, 8, RoundingMode.DOWN);

        result.addProperty("halvings", halvingCount);
        result.addProperty("supply", finalSupply.toPlainString());
        result.addProperty("reward", finalReward.toPlainString());

        return result;
    }

    private static BigDecimal satoshis(int value) {
        return BigDecimal.valueOf(value).multiply(BigDecimal.TEN.pow(8));
    }
}
