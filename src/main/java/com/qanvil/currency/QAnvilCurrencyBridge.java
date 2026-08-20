package com.qanvil.currency;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.List;

public final class QAnvilCurrencyBridge {
    private static final String CURRENCY_REGISTRY = "com.qshop.currency.CurrencyRegistry";
    private static final String WALLET_CAPABILITY = "com.qshop.wallet.WalletCapability";

    private QAnvilCurrencyBridge() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("qshop");
    }

    public static boolean exists(String id) {
        if (!isAvailable() || id == null || id.isBlank()) {
            return false;
        }
        try {
            return invokeStatic(CURRENCY_REGISTRY, "get", String.class, id) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static String firstId() {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object value = invokeStatic(CURRENCY_REGISTRY, "firstId");
            return value instanceof String string ? string : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static String displayName(String id) {
        if (!isAvailable()) {
            return id == null ? "" : id;
        }
        try {
            Object value = invokeStatic(CURRENCY_REGISTRY, "displayName", String.class, id);
            return value instanceof String string ? string : id;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return id == null ? "" : id;
        }
    }

    public static String format(double value) {
        if (!isAvailable()) {
            return String.format(java.util.Locale.ROOT, "%.2f", value);
        }
        try {
            Object formatted = invokeStatic(CURRENCY_REGISTRY, "format", double.class, value);
            return formatted instanceof String string ? string : Double.toString(value);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Double.toString(value);
        }
    }

    public static int indexOf(String id) {
        if (!isAvailable() || id == null) {
            return -1;
        }
        try {
            Object value = invokeStatic(CURRENCY_REGISTRY, "all");
            if (value instanceof List<?> currencies) {
                for (int index = 0; index < currencies.size(); index++) {
                    Object currency = currencies.get(index);
                    Object currencyId = currency.getClass().getField("id").get(currency);
                    if (id.equals(currencyId)) {
                        return index;
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The menu will fall back to the configured currency if the optional registry cannot be inspected.
        }
        return -1;
    }

    public static String idAt(int index) {
        if (!isAvailable() || index < 0) {
            return null;
        }
        try {
            Object value = invokeStatic(CURRENCY_REGISTRY, "all");
            if (value instanceof List<?> currencies && index < currencies.size()) {
                Object currencyId = currencies.get(index).getClass().getField("id").get(currencies.get(index));
                return currencyId instanceof String string ? string : null;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
        return null;
    }

    public static boolean has(Player player, String id, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        Object wallet = wallet(player);
        if (wallet == null) {
            return false;
        }
        try {
            Object value = wallet.getClass().getMethod("has", String.class, double.class)
                    .invoke(wallet, id, amount);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean take(Player player, String id, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        Object wallet = wallet(player);
        if (wallet == null) {
            return false;
        }
        try {
            Object value = wallet.getClass().getMethod("take", String.class, double.class)
                    .invoke(wallet, id, amount);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Object wallet(Player player) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return invokeStatic(WALLET_CAPABILITY, "get", Player.class, player);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static Object invokeStatic(String className, String methodName, Class<?> parameterType, Object value)
            throws ReflectiveOperationException {
        return Class.forName(className).getMethod(methodName, parameterType).invoke(null, value);
    }

    private static Object invokeStatic(String className, String methodName) throws ReflectiveOperationException {
        return Class.forName(className).getMethod(methodName).invoke(null);
    }
}
