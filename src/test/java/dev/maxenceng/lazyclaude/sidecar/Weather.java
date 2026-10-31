package dev.maxenceng.lazyclaude.sidecar;

public enum Weather {
    SUNNY("fine and bright");

    private final String text;

    Weather(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
