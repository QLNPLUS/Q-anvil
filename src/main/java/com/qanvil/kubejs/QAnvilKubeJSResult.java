package com.qanvil.kubejs;

public final class QAnvilKubeJSResult {
    private final boolean accepted;
    private final String text;

    public QAnvilKubeJSResult(boolean accepted, String text) {
        this.accepted = accepted;
        this.text = text == null ? "" : text;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getPromptText() {
        return text;
    }
}
