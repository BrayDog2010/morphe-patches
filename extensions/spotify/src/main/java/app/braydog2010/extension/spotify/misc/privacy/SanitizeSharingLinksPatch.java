package app.braydog2010.extension.spotify.misc.privacy;

import app.morphe.extension.shared.Logger;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {
    private static final Set<String> TRACKING_PARAMS = Set.of(
            "si",           // Share tracking parameter.
            "utm_source"    // Share source, such as "copy-link".
    );

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    /**
     * Injection point. The text may be a bare link or a message that contains links, so only the
     * links in it are cleaned.
     */
    public static String sanitizeSharingLink(String text) {
        if (text == null || text.isEmpty() || text.indexOf('?') < 0) {
            return text;
        }

        try {
            Matcher matcher = URL_PATTERN.matcher(text);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(sanitizeUrl(matcher.group())));
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception ex) {
            Logger.printInfo(() -> "[Spotify] Failed to sanitize share URL", ex);
            return text;
        }
    }

    private static String sanitizeUrl(String url) {
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0) {
            return url; // No query string to sanitize.
        }

        String baseUrl = url.substring(0, queryIndex);
        String query = url.substring(queryIndex + 1);

        StringBuilder sanitizedQuery = new StringBuilder();
        for (String pair : query.split("&")) {
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

        return sanitizedQuery.length() > 0 ? baseUrl + "?" + sanitizedQuery : baseUrl;
    }
}
