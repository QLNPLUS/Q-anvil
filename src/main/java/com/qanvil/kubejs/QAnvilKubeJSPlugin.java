package com.qanvil.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public final class QAnvilKubeJSPlugin extends KubeJSPlugin {
    public static final EventGroup QANVIL_EVENTS = EventGroup.of("QAnvilEvents");
    public static final EventHandler UPDATE = QANVIL_EVENTS.server(
            "update", () -> QAnvilUpdateEvent.class);

    @Override
    public void registerEvents() {
        QANVIL_EVENTS.register();
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow(QAnvilUpdateEvent.class);
    }

    public static boolean post(QAnvilUpdateEvent event) {
        return !UPDATE.post(event).interruptDefault();
    }
}
