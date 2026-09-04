package com.qanvil.currency;

import com.qanvil.QAnvilConfig;
import net.minecraft.world.entity.player.Player;

public final class QAnvilCosts {
    public static final double MAX_CURRENCY_COST = Integer.MAX_VALUE;

    private QAnvilCosts() {
    }

    public static String resolveCurrencyId() {
        String configured = QAnvilConfig.CURRENCY_ID.get();
        if (QAnvilCurrencyBridge.exists(configured)) {
            return configured;
        }

        String first = QAnvilCurrencyBridge.firstId();
        return first == null ? configured : first;
    }

    public static double defaultCurrencyCost(int vanillaLevelCost) {
        return usesCurrency() && QAnvilCurrencyBridge.isAvailable()
                ? normalizeCurrencyCost(Math.max(1, vanillaLevelCost)
                * QAnvilConfig.CURRENCY_PER_LEVEL.get()) : 0.0D;
    }

    public static double defaultHealthCost(int vanillaLevelCost) {
        return usesHealth() ? Math.max(1, vanillaLevelCost) * QAnvilConfig.HEALTH_PER_LEVEL.get() : 0.0D;
    }

    public static boolean usesCurrency() {
        return mode().equals("currency") || mode().equals("both");
    }

    public static boolean usesHealth() {
        return mode().equals("health") || mode().equals("both");
    }

    public static boolean canAfford(Player player, String currencyId, double currencyCost, double healthCost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!Double.isFinite(currencyCost) || !Double.isFinite(healthCost)
                || currencyCost < 0.0D || healthCost < 0.0D) {
            return false;
        }

        currencyCost = normalizeCurrencyCost(currencyCost);
        if (currencyCost > 0.0D && !QAnvilCurrencyBridge.isAvailable()) {
            return false;
        }
        if (currencyCost > 0.0D && !QAnvilCurrencyBridge.exists(currencyId)) {
            return false;
        }

        if (currencyCost > 0.0D && !QAnvilCurrencyBridge.has(player, currencyId, currencyCost)) {
            return false;
        }

        float remainingHealth = player.getHealth() - (float) healthCost;
        return !QAnvilConfig.KEEP_ONE_HEALTH.get() ? remainingHealth > 0.0F : remainingHealth >= 1.0F;
    }

    public static boolean charge(Player player, String currencyId, double currencyCost, double healthCost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        currencyCost = normalizeCurrencyCost(currencyCost);
        if (!canAfford(player, currencyId, currencyCost, healthCost)) {
            return false;
        }

        if (currencyCost > 0.0D && !QAnvilCurrencyBridge.take(player, currencyId, currencyCost)) {
            return false;
        }

        if (healthCost > 0.0D) {
            player.setHealth(player.getHealth() - (float) healthCost);
        }
        return true;
    }

    public static String formatCurrency(double value) {
        double normalized = normalizeCurrencyCost(value);
        if (Double.isFinite(normalized) && normalized >= 0.0D
                && normalized <= MAX_CURRENCY_COST) {
            return Long.toString((long) normalized);
        }
        return QAnvilCurrencyBridge.format(value);
    }

    public static boolean currencyAvailable() {
        return QAnvilCurrencyBridge.isAvailable();
    }

    /** Currency is settled in whole units; round fractional values upward. */
    public static double normalizeCurrencyCost(double cost) {
        if (Double.isNaN(cost) || cost < 0.0D) {
            return cost;
        }
        if (cost == 0.0D) {
            return 0.0D;
        }
        if (Double.isInfinite(cost)) {
            return MAX_CURRENCY_COST;
        }
        return Math.min(MAX_CURRENCY_COST, Math.ceil(cost));
    }

    private static String mode() {
        return QAnvilConfig.COST_MODE.get().toLowerCase(java.util.Locale.ROOT);
    }
}
