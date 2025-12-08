package server.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TitleIds {
    private TitleIds() {}
    private static final Object LOCK = new Object();
    private static volatile Map<String, String> map = new HashMap<>();
    private static volatile String classpathResource = "server/io/TitleIDs.json";
    private static volatile String externalPath = null;

    public static void setJsonPath(String path) {
        if (path == null || path.trim().isEmpty()) return;
        synchronized (LOCK) {
            if (path.startsWith("classpath:")) {
                classpathResource = path.substring("classpath:".length());
                externalPath = null;
            } else {
                externalPath = path;
            }
            reload();
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            Reader reader = null;
            try {
                if (externalPath != null) {
                    File f = new File(externalPath);
                    if (f.exists()) {
                        reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
                    }
                }
                if (reader == null) {
                    InputStream in = TitleIds.class.getClassLoader().getResourceAsStream(classpathResource);
                    if (in == null) {
                        map = new HashMap<>();
                        return;
                    }
                    reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                }
                Gson gson = new Gson();
                Type t = new TypeToken<List<Item>>() {}.getType();
                List<Item> items = gson.fromJson(reader, t);
                Map<String, String> next = new HashMap<>();
                if (items != null) {
                    for (Item it : items) {
                        if (it == null) continue;
                        String key = normalize(it.TitleID);
                        if (!key.isEmpty()) {
                            next.put(key, it.Title != null ? it.Title : "Undefined");
                        }
                    }
                }
                map = next;
            } catch (Exception ex) {
                map = new HashMap<>();
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String lookup(String titleIdHex) {
        String key = normalize(titleIdHex);
        if (key.isEmpty()) return "UnknownTitleID:" + titleIdHex;
        String v = map.get(key);
        return (v != null) ? v : ("UnknownTitleID:" + titleIdHex);
    }

    private static String normalize(String id) {
        if (id == null) return "";
        String s = id.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        return s.toUpperCase(Locale.ROOT);
    }

    private static final class Item {
        public String TitleID;
        public String Title;
    }
}
