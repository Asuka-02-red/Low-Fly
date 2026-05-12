package com.example.low_altitudereststop.core.storage;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文件缓存工具，提供基于本地文件的字符串缓存读写功能，
 * 支持过期时间检测、按前缀批量删除等操作。
 */
public class FileCache {

    private static final String META_SUFFIX = ".meta";

    private final Context appContext;

    public FileCache(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void write(String name, String content) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        try {
            File file = new File(appContext.getFilesDir(), name);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
            }
            writeMetadata(name, String.valueOf(System.currentTimeMillis()));
        } catch (Exception ignored) {
        }
    }

    public String read(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            File file = new File(appContext.getFilesDir(), name);
            if (!file.exists()) {
                return null;
            }
            byte[] buf = new byte[(int) file.length()];
            try (FileInputStream in = new FileInputStream(file)) {
                int read = in.read(buf);
                if (read <= 0) {
                    return null;
                }
            }
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String readFresh(String name, long maxAgeMillis) {
        if (!isFresh(name, maxAgeMillis)) {
            return null;
        }
        return read(name);
    }

    public boolean isFresh(String name, long maxAgeMillis) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        try {
            String content = readMetadata(name);
            if (content == null || content.trim().isEmpty()) {
                return false;
            }
            long timestamp = Long.parseLong(content.trim());
            return System.currentTimeMillis() - timestamp <= maxAgeMillis;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void writeMetadata(String name, String content) {
        File metaFile = new File(appContext.getFilesDir(), name + META_SUFFIX);
        try (FileOutputStream out = new FileOutputStream(metaFile)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private String readMetadata(String name) {
        File metaFile = new File(appContext.getFilesDir(), name + META_SUFFIX);
        if (!metaFile.exists()) {
            return null;
        }
        byte[] buf = new byte[(int) metaFile.length()];
        try (FileInputStream in = new FileInputStream(metaFile)) {
            int read = in.read(buf);
            if (read <= 0) {
                return null;
            }
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void delete(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        try {
            File file = new File(appContext.getFilesDir(), name);
            if (file.exists()) {
                file.delete();
            }
            File metaFile = new File(appContext.getFilesDir(), name + META_SUFFIX);
            if (metaFile.exists()) {
                metaFile.delete();
            }
        } catch (Exception ignored) {
        }
    }

    public void deleteByPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return;
        }
        try {
            File[] files = appContext.getFilesDir().listFiles((dir, name) -> name != null && name.startsWith(prefix));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        } catch (Exception ignored) {
        }
    }
}

