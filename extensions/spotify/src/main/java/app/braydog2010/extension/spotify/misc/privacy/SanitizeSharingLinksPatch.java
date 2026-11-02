package app.braydog2010.extension.spotify.misc.privacy;

import app.morphe.extension.shared.Logger;

import java.util.Set;

@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {
    private static final Set<String> TRACKING_PARAMS = Set.of(
            "si",           // Share tracking parameter.
            "utm_source"    // Share source, such as "copy-link".
    );

    /**
     * Injection point.
     */
    public static String sanitizeSharingLink(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        try {
            int queryIndex = url.indexOf('?');
            if (queryIndex < 0) {
                return url; // No query string to sanitize.
            }

            String baseUrl = url.substring(0, queryIndex);
            String query = url.substring(queryIndex + 1);

            StringBuilder sanitizedQuery = new StringBuilder();
            String[] pairs = query.split("&");

            for (String pair : pairs) {
                String key = pair;
                int eqIndex = pair.indexOf('=');
                if (eqIndex >= 0) {
                    key = pair.substring(0, eqIndex);
                }

                if (!TRACKING_PARAMS.contains(key)) {
                    if (sanitizedQuery.length() > 0) {
                        sanitizedQuery.append('&');
                    }
                    sanitizedQuery.append(pair);
                }
            }

            String result = baseUrl;
            if (sanitizedQuery.length() > 0) {
                result += "?" + sanitizedQuery;
            }

            return result;
        } catch (Exception ex) {
            Logger.printInfo(() -> "[Spotify] Failed to sanitize share URL", ex);
            return url;
        }
    }
}
