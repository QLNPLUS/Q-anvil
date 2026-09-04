package com.qanvil.kubejs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class QAnvilKubeJSBridge {
    private static final String EVENT_CLASS = "com.qanvil.kubejs.QAnvilUpdateEvent";
    private static final String PLUGIN_CLASS = "com.qanvil.kubejs.QAnvilKubeJSPlugin";

    private QAnvilKubeJSBridge() {
    }

    public static boolean post(QAnvilEventData data) {
        if (!ModList.get().isLoaded("kubejs")) {
            return true;
        }

        try {
            Class<?> eventType = Class.forName(EVENT_CLASS);
            Constructor<?> constructor = eventType.getConstructor(QAnvilEventData.class);
            Object event = constructor.newInstance(data);
            Method post = Class.forName(PLUGIN_CLASS).getMethod("post", eventType);
            Object postResult = post.invoke(null, event);
            boolean accepted = Boolean.TRUE.equals(postResult.getClass().getMethod("isAccepted").invoke(postResult));
            Object eventText = postResult.getClass().getMethod("getPromptText").invoke(postResult);
            data.promptText = eventText == null ? "" : eventText.toString();

            data.output = ((ItemStack) eventType.getMethod("getOutput").invoke(event)).copy();
            data.cost = ((Number) eventType.getMethod("getCost").invoke(event)).intValue();
            data.materialCost = ((Number) eventType.getMethod("getMaterialCost").invoke(event)).intValue();
            data.materialCostSet = (Boolean) eventType.getMethod("isMaterialCostSet").invoke(event);
            data.currencyId = (String) eventType.getMethod("getCurrencyId").invoke(event);
            data.currencyCost = ((Number) eventType.getMethod("getCurrencyCost").invoke(event)).doubleValue();
            data.healthCost = ((Number) eventType.getMethod("getHealthCost").invoke(event)).doubleValue();
            return accepted;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // KubeJS is optional. A missing or incompatible integration must not break the anvil itself.
            return true;
        }
    }
}
