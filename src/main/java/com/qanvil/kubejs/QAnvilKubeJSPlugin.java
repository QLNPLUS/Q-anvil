package com.qanvil.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QAnvilKubeJSPlugin extends KubeJSPlugin {
    private static final Logger LOGGER = LogManager.getLogger("QAnvil");
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

    public static QAnvilKubeJSResult post(QAnvilUpdateEvent event) {
        InvocationState state = new InvocationState();
        UPDATE.forEachListener(ScriptType.SERVER, listener -> invokeListener(listener, event, state));
        if (!state.stopped) {
            UPDATE.forEachListener(ScriptType.STARTUP, listener -> invokeListener(listener, event, state));
        }
        return new QAnvilKubeJSResult(state.accepted, state.text);
    }

    private static void invokeListener(EventHandlerContainer listener, QAnvilUpdateEvent event,
                                       InvocationState state) {
        if (state.stopped) {
            return;
        }

        try {
            listener.handler.onEvent(event);
            captureText(event, state);
            if (event.isCanceled()) {
                state.accepted = false;
                state.stopped = true;
            }
        } catch (EventExit exit) {
            EventResult result = exit.result;
            if (result != null) {
                captureText(event, state);
                state.accepted = !result.interruptFalse() && !result.interruptDefault();
            }
            state.stopped = true;
        } catch (Throwable exception) {
            // Preserve a prompt that was set before a later script statement failed.
            state.text = event.getEventText();
            LOGGER.error("QAnvil KubeJS update handler failed at {}", listener, exception);
            state.stopped = true;
        }
    }

    private static void captureText(QAnvilUpdateEvent event, InvocationState state) {
        state.text = event.getEventText();
    }

    private static final class InvocationState {
        private boolean accepted = true;
        private boolean stopped;
        private String text = "";
    }
}
