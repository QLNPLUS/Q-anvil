package com.qanvil;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.ModLoadingContext;

public final class QAnvilConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> CURRENCY_ID = BUILDER
            .comment("The QShop currency id used by the anvil. If it does not exist, QShop's first currency is used.")
            .define("currency_id", "coins", value -> value instanceof String string && !string.isBlank());
    public static final ForgeConfigSpec.ConfigValue<String> COST_MODE = BUILDER
            .comment("Cost mode: health, currency, or both.")
            .define("cost_mode", "health", value -> value instanceof String string
                    && (string.equalsIgnoreCase("health")
                    || string.equalsIgnoreCase("currency")
                    || string.equalsIgnoreCase("both")));
    public static final ForgeConfigSpec.DoubleValue CURRENCY_PER_LEVEL = BUILDER
            .comment("QShop currency charged per vanilla anvil level of cost.")
            .defineInRange("currency_per_level", 1.0D, 0.0D, Double.MAX_VALUE);
    public static final ForgeConfigSpec.DoubleValue HEALTH_PER_LEVEL = BUILDER
            .comment("Player health points charged per vanilla anvil level of cost.")
            .defineInRange("health_per_level", 1.0D, 0.0D, 40.0D);
    public static final ForgeConfigSpec.BooleanValue KEEP_ONE_HEALTH = BUILDER
            .comment("When true, the anvil cannot be used if paying the health cost would leave the player below one health point.")
            .define("keep_one_health", true);
    public static final ForgeConfigSpec.IntValue MAX_INPUT_STACK_SIZE = BUILDER
            .comment("Maximum stack size for stackable items in the Q Anvil left and right input slots. Items with a native stack limit of 1 remain non-stackable. Default 64 preserves vanilla behavior.")
            .defineInRange("max_input_stack_size", 64, 1, 9999);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private QAnvilConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC);
    }

    public static void onConfigLoad(ModConfigEvent event) {
        // The values are read on demand so config reloads take effect immediately.
    }

    public static int maxInputStackSize() {
        return Math.max(1, Math.min(9999, MAX_INPUT_STACK_SIZE.get()));
    }
}
