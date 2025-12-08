package server.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class EndianIO implements Closeable {
    private final Style style;
    private final String filePath;
    private final boolean isFile;
    private final boolean isMemory;
    private boolean opened;
    private InputStream in;
    private OutputStream out;
    public Reader Reader;
    public Writer Writer;

    public EndianIO(InputStream stream, OutputStream outStream, Style style) {
        this.style = style;
        this.filePath = "";
        this.isFile = false;
        this.isMemory = false;
        this.in = stream;
        this.out = outStream;
        open();
    }

    public EndianIO(byte[] buffer, Style style) {
        this.style = style;
        this.filePath = "";
        this.isFile = false;
        this.isMemory = true;
        this.in = new ByteArrayInputStream(buffer);
        this.out = new ByteArrayOutputStream();
        open();
    }

    public EndianIO(String filepath, Style style, boolean createIfMissing) {
        this.style = style;
        this.filePath = filepath;
        this.isFile = true;
        this.isMemory = false;
        open(createIfMissing);
    }

    public void open() {
        open(false);
    }

    private void open(boolean createIfMissing) {
        if (opened) return;
        try {
            if (isFile) {
                if (createIfMissing) {
                    this.in = new FileInputStream(filePath);
                    this.out = new FileOutputStream(filePath, true);
                } else {
                    this.in = new FileInputStream(filePath);
                    this.out = new FileOutputStream(filePath);
                }
            }
            this.Reader = new Reader(this.in, this.style);
            this.Writer = new Writer(this.out, this.style);
            this.opened = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isOpened() {
        return opened;
    }

    @Override
    public void close() {
        if (!opened) return;
        try {
            if (in != null) in.close();
        } catch (Exception ignored) {
        }
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {
        }
        opened = false;
    }
}