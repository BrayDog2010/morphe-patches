package app.morphe.extension.tiktok.navigation;

/** A tab a settings row can offer: the key stored in the setting, and the name shown for it. */
public final class TabOption {
    public final String key;
    public final String label;

    TabOption(String key, String label) {
        this.key = key;
        this.label = label;
    }
}
