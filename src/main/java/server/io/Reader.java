package server.io;

import java.io.IOException;
import java.io.InputStream;

public final class Reader {
    private final InputStream in;
    private final Style style;

    public Reader(InputStream in, Style style) {
        this.in = in;
        this.style = style;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        int total = 0;
        while (total < length) {
            int n = in.read(buffer, offset + total, length - total);
            if (n < 0) break;
            total += n;
        }
        return total;
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] b = new byte[length];
        int got = read(b, 0, length);
        if (got != length) throw new IOException("Unexpected EOF: need " + length + " got " + got);
        return b;
    }

    public short readInt16() throws IOException {
        return readInt16(style);
    }

    public short readInt16(Style style) throws IOException {
        byte[] b = readBytes(2);
        if (style == Style.BigEndian) {
            return (short)(((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
        } else {
            return (short)(((b[1] & 0xFF) << 8) | (b[0] & 0xFF));
        }
    }

    public int readUInt16() throws IOException {
        return readUInt16(style);
    }

    public int readUInt16(Style style) throws IOException {
        byte[] b = readBytes(2);
        if (style == Style.BigEndian) {
            return ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
        } else {
            return ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
        }
    }

    public int readInt32() throws IOException {
        return readInt32(style);
    }

    public int readInt32(Style style) throws IOException {
        byte[] b = readBytes(4);
        if (style == Style.BigEndian) {
            return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
        } else {
            return ((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[0] & 0xFF);
        }
    }

    public long readUInt32() throws IOException {
        return readUInt32(style);
    }

    public long readUInt32(Style style) throws IOException {
        long value = readInt32(style);
        return value & 0xFFFFFFFFL;
    }

    public long readInt64() throws IOException {
        return readInt64(style);
    }

    public long readInt64(Style style) throws IOException {
        byte[] b = readBytes(8);
        if (style == Style.BigEndian) {
            return ((long)(b[0] & 0xFF) << 56) |
                    ((long)(b[1] & 0xFF) << 48) |
                    ((long)(b[2] & 0xFF) << 40) |
                    ((long)(b[3] & 0xFF) << 32) |
                    ((long)(b[4] & 0xFF) << 24) |
                    ((long)(b[5] & 0xFF) << 16) |
                    ((long)(b[6] & 0xFF) << 8)  |
                    ((long)(b[7] & 0xFF));
        } else {
            return ((long)(b[7] & 0xFF) << 56) |
                    ((long)(b[6] & 0xFF) << 48) |
                    ((long)(b[5] & 0xFF) << 40) |
                    ((long)(b[4] & 0xFF) << 32) |
                    ((long)(b[3] & 0xFF) << 24) |
                    ((long)(b[2] & 0xFF) << 16) |
                    ((long)(b[1] & 0xFF) << 8)  |
                    ((long)(b[0] & 0xFF));
        }
    }

    public long readUInt64() throws IOException {
        return readInt64(style);
    }

    public boolean readBoolean() throws IOException {
        int value = in.read();
        if (value < 0) throw new IOException("EOF on boolean");
        return value != 0;
    }

    public int readUByte() throws IOException {
        int value = in.read();
        if (value < 0) throw new IOException("EOF on byte");
        return value & 0xFF;
    }

    public void skip(long n) throws IOException {
        long left = n;
        while (left > 0) {
            long s = in.skip(left);
            if (s <= 0) {
                if (in.read() < 0) break;
                s = 1;
            }
            left -= s;
        }
    }
}