package com.qanvil.currency;

import com.qanvil.QAnvilConfig;
import net.minecraft.world.entity.player.Player;

public final class QAnvilCosts {
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
                ? Math.max(1, vanillaLevelCost) * QAnvilConfig.CURRENCY_PER_LEVEL.get() : 0.0D;
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
        if (currencyCost < 0.0D || healthCost < 0.0D) {
            return false;
        }
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
        return QAnvilCurrencyBridge.format(value);
    }

    public static boolean currencyAvailable() {
        return QAnvilCurrencyBridge.isAvailable();
    }

    private static String mode() {
        return QAnvilConfig.COST_MODE.get().toLowerCase(java.util.Locale.ROOT);
    }
}
