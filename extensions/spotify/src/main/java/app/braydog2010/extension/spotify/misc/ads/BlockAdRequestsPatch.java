package app.braydog2010.extension.spotify.misc.ads;

import java.net.URI;
import java.util.Locale;

import app.morphe.extension.shared.Logger;

/**
 * Drops Spotify's ad, upsell and marketing requests, using the same rules as the iOS
 * EeveeSpotify ad blocker. Every request the native core makes passes through one Java method, so
 * a blocked address is swapped for one that cannot resolve and the core treats the fetch as failed.
 */
@SuppressWarnings("unused")
public final class BlockAdRequestsPatch {
    private static final String BLOCKED_URL = "https://blocked.invalid/";

    private static final String[] BLOCKED_PATHS = {
            "/ads/",
            "/ad-logic/",
            "/ad-on-app-open",
            "/dac/view/v1/",
            "/premium-marketing/upsellOffer".toLowerCase(Locale.ROOT),
            "/trials-facade/start-trial",
            "/ad-slot/",
            "/ad-inventory/",
            "/leavebehind",
            "/leave-behind",
            "/sponsored-content/",
            "/home-ads/",
            "/search-ads/",
    };

    private static final String[] BLOCKED_HOST_PARTS = {
            "doubleclick",
            "googlesyndication",
            "googleadservices",
            "app-measurement.com",
            "branch.io",
    };

    /**
     * Injection point. Called with the address of every request made by the native core.
     */
    public static String filterUrl(String url) {
        try {
            if (url == null) return url;

            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);

            for (String part : BLOCKED_HOST_PARTS) {
                if (host.contains(part)) return block(url);
            }
            if (host.equals("ad.spotify.com") || host.equals("ads.spotify.com") || host.startsWith("aet.")) {
                return block(url);
            }

            if (!host.contains("spotify") && !host.contains("spclient")) return url;

            if (path.endsWith("/ads")) return block(url);
            for (String blocked : BLOCKED_PATHS) {
                if (path.contains(blocked)) return block(url);
            }
            // Marketing messages (win-back fullscreens, Home Premium banners).
            if (path.contains("pendragon") && path.contains("fetchmessage")) return block(url);
        } catch (Exception ex) {
            Logger.printException(() -> "filterUrl failure", ex);
        }
        return url;
    }

    private static String block(String url) {
        Logger.printDebug(() -> "Blocked request: " + url);
        return BLOCKED_URL;
    }
}
