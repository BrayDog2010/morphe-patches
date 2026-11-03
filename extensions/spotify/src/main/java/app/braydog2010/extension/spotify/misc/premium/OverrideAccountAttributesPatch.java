package app.braydog2010.extension.spotify.misc.premium;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import app.morphe.extension.shared.Logger;

/**
 * Rewrites the account attributes (Spotify's "product state") the server sends, the same way the
 * iOS EeveeSpotify tweak forces its premium keys. Only attributes that already exist are changed,
 * and each new value is converted to the type the attribute already holds.
 */
@SuppressWarnings("unused")
public final class OverrideAccountAttributesPatch {
    private static final Map<String, String> FORCED = new HashMap<>();

    static {
        FORCED.put("type", "premium");
        FORCED.put("catalogue", "premium");
        FORCED.put("product", "premium");
        FORCED.put("name", "Spotify Premium");
        FORCED.put("player-license", "premium");
        FORCED.put("player-license-v2", "premium");
        FORCED.put("financial-product", "pr:premium,tc:0");

        FORCED.put("ads", "0");
        FORCED.put("ab-ad-player-targeting", "0");
        FORCED.put("allow-advertising-id-transmission", "0");
        FORCED.put("restrict-advertising-id-transmission", "1");
        FORCED.put("audio-ad-frequency", "0");
        FORCED.put("video-ad-frequency", "0");
        FORCED.put("ad-formats", "");

        FORCED.put("on-demand", "1");
        FORCED.put("unrestricted", "1");
        FORCED.put("shuffle-eligible", "1");
        FORCED.put("social-connect", "1");
        FORCED.put("tracks-in-collection-enabled", "1");
        FORCED.put("is-eligible-premium-unboxing", "1");
        FORCED.put("can_use_superbird", "1");
        FORCED.put("nft-disabled", "1");
        FORCED.put("high-bitrate", "1");
        FORCED.put("streaming-rules", "");
        FORCED.put("previous-streaming-rules", "");
        FORCED.put("premium-promotion-eligible", "0");
    }

    private static Field valueField;

    /**
     * Injection point. Called with the map the product state returns, before anything reads it.
     */
    public static void override(Map<String, ?> attributes) {
        try {
            if (attributes == null) return;

            int changed = 0;
            for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                String forced = FORCED.get(entry.getKey());
                Object attribute = entry.getValue();
                if (forced == null || attribute == null) continue;

                if (valueField == null) {
                    valueField = attribute.getClass().getDeclaredField("value_");
                    valueField.setAccessible(true);
                }

                Object current = valueField.get(attribute);
                Object replacement = convert(current, forced);
                if (replacement != null && !replacement.equals(current)) {
                    valueField.set(attribute, replacement);
                    changed++;
                }
            }

            final int count = changed;
            if (count > 0) {
                Logger.printDebug(() -> "Overrode account attributes: " + count);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "override failure", ex);
        }
    }

    private static Object convert(Object current, String forced) {
        if (current instanceof String) return forced;
        if (current instanceof Boolean) return "1".equals(forced) || "premium".equals(forced);
        if (current instanceof Long) {
            try {
                return Long.parseLong(forced);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}
