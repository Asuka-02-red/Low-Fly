package com.example.low_altitudereststop.core.trace;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OperationLogStore {

    private static final String FILE_NAME = "operation_log.txt";
    private final Context appContext;

    public OperationLogStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public void appendHttp(String requestId, String method, String path) {
        String line = System.currentTimeMillis() + "\t" + safe(requestId) + "\tHTTP\t" + safe(method) + "\t" + safe(path) + "\n";
        appendLine(line);
    }

    public void appendCrash(String tag, String message) {
        String line = System.currentTimeMillis() + "\t\tCRASH\t" + safe(tag) + "\t" + safe(message) + "\n";
        appendLine(line);
    }

    public void appendAudit(String tag, String message) {
        String line = System.currentTimeMillis() + "\t\tAUDIT\t" + safe(tag) + "\t" + safe(message) + "\n";
        appendLine(line);
    }

    public String readAll() {
        try {
            File file = new File(appContext.getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                return "";
            }
            byte[] buf = new byte[(int) file.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int read = in.read(buf);
                if (read <= 0) {
                    return "";
                }
            }
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void appendLine(String line) {
        try {
            File file = new File(appContext.getFilesDir(), FILE_NAME);
            try (FileOutputStream out = new FileOutputStream(file, true)) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
        }
    }

    private String safe(String v) {
        return v == null ? "" : v.replace("\t", " ").replace("\n", " ");
    }
}

