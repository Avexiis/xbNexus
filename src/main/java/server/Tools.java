package server;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class Tools {
    private Tools() {}
    private static final Object titleLock = new Object();
    private static Map<String, String> titleById = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static String titleJsonResource = "server/io/TitleIDs.json";
    private static String titleJsonExternalPath = null;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static final class TitleIdEntry {
        @SerializedName("TitleID") String titleID;
        @SerializedName("Title") String title;
    }

    static {
        tryLoadTitles();
    }

    public static void SetTitleIdJsonPath(String path) {
        if (path == null || path.trim().isEmpty())
            throw new IllegalArgumentException("Path must be non-empty.");
        synchronized (titleLock) {
            if (path.startsWith("classpath:")) {
                titleJsonResource = path.substring("classpath:".length());
                titleJsonExternalPath = null;
            } else {
                titleJsonExternalPath = path;
            }
            tryLoadTitles();
        }
    }

    public static void ReloadTitles() {
        synchronized (titleLock) {
            tryLoadTitles();
        }
    }

    public static Map<String,String> GetTitleMap() {
        synchronized (titleLock) {
            return new HashMap<>(titleById);
        }
    }

    public static String TitleIdToString(String titleId) {
        String original = titleId == null ? "" : titleId;
        String key = normalizeTitleId(original);
        if (!key.isEmpty()) {
            synchronized (titleLock) {
                String v = titleById.get(key);
                if (v != null && !v.isEmpty()) return v;
            }
        }
        return "Unknown Title ID: " + original;
    }

    private static String normalizeTitleId(String id) {
        if (id == null) return "";
        String s = id.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        return s.toUpperCase(Locale.ROOT);
    }

    private static void tryLoadTitles() {
        Reader reader = null;
        try {
            if (titleJsonExternalPath != null) {
                File f = new File(titleJsonExternalPath);
                if (f.exists()) {
                    reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                }
            }
            if (reader == null) {
                InputStream in = Tools.class.getClassLoader().getResourceAsStream(titleJsonResource);
                if (in == null) {
                    titleById = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    return;
                }
                reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            }
            Gson gson = new Gson();
            List<TitleIdEntry> entries = gson.fromJson(reader, new TypeToken<List<TitleIdEntry>>(){}.getType());
            Map<String,String> next = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            if (entries != null) {
                for (TitleIdEntry e : entries) {
                    if (e == null) continue;
                    String key = normalizeTitleId(e.titleID);
                    if (key.isEmpty()) continue;
                    next.put(key, e.title != null ? e.title : "Undefined");
                }
            }
            titleById = next;
        } catch (Exception ex) {
            titleById = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static String BytesToHexString(byte[] data) {
        if (data == null) return "";
        char[] out = new char[data.length * 2];
        int i = 0;
        for (byte b : data) {
            out[i++] = HEX[(b >>> 4) & 0x0F];
            out[i++] = HEX[b & 0x0F];
        }
        return new String(out);
    }

    public static boolean CompareBytes(byte[] a, byte[] b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    public static byte[] SHA(byte[] data) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return sha1.digest(data == null ? new byte[0] : data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] HMACSHA(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key == null ? new byte[0] : key, "HmacSHA1"));
            return mac.doFinal(data == null ? new byte[0] : data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean IsSectionEmpty(byte[] data, int size) {
        if (data == null || size <= 0) return true;
        if (size > data.length) size = data.length;
        for (int i = 0; i < size; i++) if (data[i] != 0) return false;
        return true;
    }

    public static String readAscii(byte[] bytes) {
        if (bytes == null) return "";
        String s = new String(bytes, StandardCharsets.US_ASCII);
        int zero = s.indexOf('\0');
        return zero >= 0 ? s.substring(0, zero) : s;
    }

    public static String readUtf8TrimZeros(byte[] bytes) {
        if (bytes == null) return "";
        String s = new String(bytes, StandardCharsets.UTF_8);
        int i = s.indexOf('\0');
        return i >= 0 ? s.substring(0, i) : s;
    }
}
