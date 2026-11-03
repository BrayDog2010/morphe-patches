package app.braydog2010.extension.spotify.misc.lyrics;

import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.Logger;

/**
 * Replaces the lyrics Spotify returns with lyrics from LRCLIB or Musixmatch, the same idea as the
 * iOS EeveeSpotify custom lyrics. The response is rebuilt in Spotify's own wire format and parsed
 * back with the response class's own parser, so the app shows it like native lyrics.
 */
@SuppressWarnings("unused")
public final class CustomLyricsPatch {
    private static final int TIMEOUT_MS = 8000;
    private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]\\s*(.*)");

    private static final class Line {
        final long startMs;
        final String text;

        Line(long startMs, String text) {
            this.startMs = startMs;
            this.text = text;
        }
    }

    private static final class Lyrics {
        final List<Line> lines;
        final boolean synced;
        final String provider;

        Lyrics(List<Line> lines, boolean synced, String provider) {
            this.lines = lines;
            this.synced = synced;
            this.provider = provider;
        }
    }

    private static final class Track {
        final String title;
        final String artist;
        final String album;
        final long durationMs;

        Track(String title, String artist, String album, long durationMs) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.durationMs = durationMs;
        }

        String key() {
            return artist + "\u0000" + title + "\u0000" + durationMs;
        }
    }

    private static volatile Track currentTrack;
    private static final Map<String, Lyrics> CACHE = new ConcurrentHashMap<>();
    private static String musixmatchToken;

    /**
     * Injection point. Called with the player state's current track whenever it is read.
     */
    public static void captureTrack(Object optionalTrack) {
        try {
            Object track = unwrap(optionalTrack);
            if (track == null) return;

            Object metadata = invokeNoArg(track, "metadata");
            if (!(metadata instanceof Map)) return;

            Map<?, ?> map = (Map<?, ?>) metadata;
            String title = string(map.get("title"));
            String artist = string(map.get("artist_name"));
            if (title == null || artist == null) return;

            long duration = 0;
            try {
                duration = Long.parseLong(string(map.get("duration")));
            } catch (Exception ignored) {
            }
            currentTrack = new Track(title, artist, string(map.get("album_title")), duration);
        } catch (Exception ex) {
            Logger.printException(() -> "captureTrack failure", ex);
        }
    }

    /**
     * Injection point. Called with the color lyrics response before it is turned into UI state.
     */
    public static Object replaceLyrics(Object response) {
        try {
            Track track = currentTrack;
            if (track == null || response == null) return response;
            if (Looper.myLooper() == Looper.getMainLooper()) return response;

            Lyrics lyrics = CACHE.get(track.key());
            if (lyrics == null) {
                lyrics = fetch(track);
                if (lyrics == null) return response;
                CACHE.put(track.key(), lyrics);
            }

            byte[] original = (byte[]) invokeByteArray(response);
            if (original == null) return response;

            byte[] rebuilt = rebuild(original, lyrics);
            Object parsed = parse(response.getClass(), rebuilt);
            if (parsed != null) {
                Logger.printDebug(() -> "Replaced lyrics");
                return parsed;
            }
        } catch (Throwable t) {
            Logger.printException(() -> "replaceLyrics failure", t);
        }
        return response;
    }

    // region providers

    private static Lyrics fetch(Track track) {
        Lyrics lyrics = null;
        try {
            lyrics = fetchLrclib(track);
        } catch (Exception ex) {
            Logger.printException(() -> "LRCLIB failure", ex);
        }
        if (lyrics == null) {
            try {
                lyrics = fetchMusixmatch(track);
            } catch (Exception ex) {
                Logger.printException(() -> "Musixmatch failure", ex);
            }
        }
        return lyrics;
    }

    private static Lyrics fetchLrclib(Track track) throws Exception {
        String base = "https://lrclib.net/api/";
        String query = "track_name=" + enc(track.title) + "&artist_name=" + enc(track.artist);
        String url = base + "get?" + query
                + (track.album != null ? "&album_name=" + enc(track.album) : "")
                + (track.durationMs > 0 ? "&duration=" + (track.durationMs / 1000) : "");

        JSONObject song = null;
        String body = get(url, null);
        if (body != null) {
            song = new JSONObject(body);
        } else {
            String search = get(base + "search?" + query, null);
            if (search != null) {
                JSONArray results = new JSONArray(search);
                for (int i = 0; i < results.length(); i++) {
                    JSONObject candidate = results.getJSONObject(i);
                    if (!candidate.isNull("syncedLyrics")) {
                        song = candidate;
                        break;
                    }
                    if (song == null && !candidate.isNull("plainLyrics")) song = candidate;
                }
            }
        }
        if (song == null) return null;

        String synced = song.isNull("syncedLyrics") ? null : song.optString("syncedLyrics", null);
        if (synced != null && !synced.isEmpty()) {
            List<Line> lines = parseLrc(synced);
            if (!lines.isEmpty()) return new Lyrics(lines, true, "LRCLIB");
        }
        String plain = song.isNull("plainLyrics") ? null : song.optString("plainLyrics", null);
        if (plain != null && !plain.isEmpty()) return new Lyrics(plainLines(plain), false, "LRCLIB");
        return null;
    }

    private static Lyrics fetchMusixmatch(Track track) throws Exception {
        if (musixmatchToken == null) {
            String tokenBody = get("https://apic-desktop.musixmatch.com/ws/1.1/token.get"
                    + "?app_id=web-desktop-app-v1.0&user_language=en&t=" + System.currentTimeMillis(), null);
            if (tokenBody == null) return null;
            musixmatchToken = new JSONObject(tokenBody).getJSONObject("message").getJSONObject("body")
                    .optString("user_token", null);
            if (musixmatchToken == null || musixmatchToken.isEmpty()) {
                musixmatchToken = null;
                return null;
            }
        }

        String body = get("https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get"
                + "?format=json&namespace=lyrics_richsynched&subtitle_format=mxm&app_id=web-desktop-app-v1.0"
                + "&q_track=" + enc(track.title) + "&q_artist=" + enc(track.artist)
                + (track.durationMs > 0 ? "&f_subtitle_length=" + (track.durationMs / 1000) : "")
                + "&usertoken=" + enc(musixmatchToken), null);
        if (body == null) return null;

        JSONObject calls = new JSONObject(body).getJSONObject("message").getJSONObject("body")
                .optJSONObject("macro_calls");
        if (calls == null) return null;

        JSONObject subtitleCall = calls.optJSONObject("track.subtitles.get");
        if (subtitleCall != null) {
            JSONArray list = subtitleCall.getJSONObject("message").getJSONObject("body")
                    .optJSONArray("subtitle_list");
            if (list != null && list.length() > 0) {
                String subtitleBody = list.getJSONObject(0).getJSONObject("subtitle").optString("subtitle_body", "");
                JSONArray items = new JSONArray(subtitleBody);
                List<Line> lines = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    long ms = Math.round(item.getJSONObject("time").getDouble("total") * 1000);
                    lines.add(new Line(ms, item.optString("text", "")));
                }
                if (!lines.isEmpty()) return new Lyrics(lines, true, "Musixmatch");
            }
        }

        JSONObject lyricsCall = calls.optJSONObject("track.lyrics.get");
        if (lyricsCall != null) {
            JSONObject lyricsBody = lyricsCall.getJSONObject("message").optJSONObject("body");
            JSONObject lyrics = lyricsBody == null ? null : lyricsBody.optJSONObject("lyrics");
            String text = lyrics == null ? null : lyrics.optString("lyrics_body", null);
            if (text != null && !text.isEmpty()) return new Lyrics(plainLines(text), false, "Musixmatch");
        }
        return null;
    }

    private static List<Line> parseLrc(String lrc) {
        List<Line> lines = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher matcher = LRC_LINE.matcher(raw.trim());
            if (!matcher.matches()) continue;

            long minutes = Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            String fraction = matcher.group(3);
            long millis = 0;
            if (fraction != null) {
                millis = Long.parseLong((fraction + "00").substring(0, 3));
            }
            lines.add(new Line(minutes * 60000 + seconds * 1000 + millis, matcher.group(4)));
        }
        return lines;
    }

    private static List<Line> plainLines(String text) {
        List<Line> lines = new ArrayList<>();
        for (String raw : text.split("\n")) {
            lines.add(new Line(0, raw.trim()));
        }
        return lines;
    }

    private static String get(String url, String cookie) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Spotify-Lyrics");
            if (cookie != null) connection.setRequestProperty("Cookie", cookie);
            if (connection.getResponseCode() != 200) return null;

            try (InputStream stream = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) > 0) out.write(buffer, 0, read);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String enc(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    // endregion

    // region protobuf

    /**
     * Keeps every top-level field of the original response except the lyrics (field 1), then
     * adds a new lyrics message: sync type (1), lines (2: start ms (1), words (2)) and the
     * provider display name (5).
     */
    private static byte[] rebuild(byte[] original, Lyrics lyrics) throws Exception {
        ByteArrayOutputStream lyricsMessage = new ByteArrayOutputStream();
        writeVarintField(lyricsMessage, 1, lyrics.synced ? 1 : 0);
        for (Line line : lyrics.lines) {
            ByteArrayOutputStream lineMessage = new ByteArrayOutputStream();
            writeVarintField(lineMessage, 1, line.startMs);
            writeBytesField(lineMessage, 2, line.text.getBytes(StandardCharsets.UTF_8));
            writeBytesField(lyricsMessage, 2, lineMessage.toByteArray());
        }
        writeBytesField(lyricsMessage, 5, lyrics.provider.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeBytesField(out, 1, lyricsMessage.toByteArray());

        int i = 0;
        while (i < original.length) {
            int fieldStart = i;
            long[] tag = readVarint(original, i);
            i = (int) tag[1];
            int field = (int) (tag[0] >>> 3);
            int wire = (int) (tag[0] & 7);

            if (wire == 0) {
                i = (int) readVarint(original, i)[1];
            } else if (wire == 1) {
                i += 8;
            } else if (wire == 2) {
                long[] length = readVarint(original, i);
                i = (int) length[1] + (int) length[0];
            } else if (wire == 5) {
                i += 4;
            } else {
                throw new IllegalStateException("Unsupported wire type " + wire);
            }
            if (field != 1) out.write(original, fieldStart, i - fieldStart);
        }
        return out.toByteArray();
    }

    private static long[] readVarint(byte[] data, int index) {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = data[index++];
            result |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return new long[]{result, index};
            shift += 7;
        }
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        while ((value & ~0x7fL) != 0) {
            out.write((int) ((value & 0x7f) | 0x80));
            value >>>= 7;
        }
        out.write((int) value);
    }

    private static void writeVarintField(ByteArrayOutputStream out, int field, long value) {
        writeVarint(out, (long) field << 3);
        writeVarint(out, value);
    }

    private static void writeBytesField(ByteArrayOutputStream out, int field, byte[] value) {
        writeVarint(out, ((long) field << 3) | 2);
        writeVarint(out, value.length);
        out.write(value, 0, value.length);
    }

    // endregion

    // region reflection

    private static String string(Object value) {
        return value instanceof String ? (String) value : null;
    }

    /**
     * Spotify wraps the current track in its own Optional class with obfuscated method names, so
     * the track is found by trying its public no-argument getters until one has metadata().
     */
    private static Object unwrap(Object optional) throws Exception {
        if (optional == null) return null;
        if (hasMethod(optional.getClass(), "metadata")) return optional;

        for (java.lang.reflect.Method method : optional.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0 || method.getReturnType().isPrimitive()
                    || method.getDeclaringClass() == Object.class) continue;
            try {
                Object value = method.invoke(optional);
                if (value != null && hasMethod(value.getClass(), "metadata")) return value;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 0) return true;
        }
        return false;
    }

    private static Object invokeNoArg(Object target, String name) throws Exception {
        for (java.lang.reflect.Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == 0) {
                return method.invoke(target);
            }
        }
        return null;
    }

    /**
     * Finds the message's serializer: the public no-argument method that returns a byte array.
     */
    private static Object invokeByteArray(Object message) throws Exception {
        for (java.lang.reflect.Method method : message.getClass().getMethods()) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == byte[].class) {
                return method.invoke(message);
            }
        }
        return null;
    }

    /**
     * Parses bytes with the message class's own static parser(), looking for its byte[] entry.
     */
    private static Object parse(Class<?> messageType, byte[] bytes) throws Exception {
        Object parser = messageType.getMethod("parser").invoke(null);
        for (java.lang.reflect.Method method : parser.getClass().getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0] == byte[].class
                    && messageType.isAssignableFrom(method.getReturnType())
                    || parameters.length == 1 && parameters[0] == byte[].class
                    && method.getReturnType() == Object.class) {
                try {
                    Object result = method.invoke(parser, (Object) bytes);
                    if (messageType.isInstance(result)) return result;
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    // endregion
}
