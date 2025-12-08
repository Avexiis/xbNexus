package server.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public final class Writer {
    private final OutputStream out;
    private final Style style;

    public Writer(OutputStream out, Style style) {
        this.out = Objects.requireNonNull(out, "out");
        this.style = style;
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int offset, int length) throws IOException {
        out.write(b, offset, length);
    }

    public void writeByte(int value) throws IOException {
        out.write(value & 0xFF);
    }

    public void writeInt16(short value) throws IOException {
        writeInt16(value, style);
    }

    public void writeInt16(short value, Style style) throws IOException {
        byte[] b = new byte[2];
        if (style == Style.BigEndian) {
            b[0] = (byte)((value >>> 8) & 0xFF);
            b[1] = (byte)(value & 0xFF);
        } else {
            b[1] = (byte)((value >>> 8) & 0xFF);
            b[0] = (byte)(value & 0xFF);
        }
        out.write(b);
    }

    public void writeUInt16(int value) throws IOException {
        writeUInt16(value, style);
    }

    public void writeUInt16(int value, Style style) throws IOException {
        byte[] b = new byte[2];
        if (style == Style.BigEndian) {
            b[0] = (byte)((value >>> 8) & 0xFF);
            b[1] = (byte)(value & 0xFF);
        } else {
            b[1] = (byte)((value >>> 8) & 0xFF);
            b[0] = (byte)(value & 0xFF);
        }
        out.write(b);
    }

    public void writeInt32(int value) throws IOException {
        writeInt32(value, style);
    }

    public void writeInt32(int value, Style style) throws IOException {
        byte[] b = new byte[4];
        if (style == Style.BigEndian) {
            b[0] = (byte)((value >>> 24) & 0xFF);
            b[1] = (byte)((value >>> 16) & 0xFF);
            b[2] = (byte)((value >>> 8) & 0xFF);
            b[3] = (byte)(value & 0xFF);
        } else {
            b[3] = (byte)((value >>> 24) & 0xFF);
            b[2] = (byte)((value >>> 16) & 0xFF);
            b[1] = (byte)((value >>> 8) & 0xFF);
            b[0] = (byte)(value & 0xFF);
        }
        out.write(b);
    }

    public void writeUInt32(long value) throws IOException {
        writeUInt32(value, style);
    }

    public void writeUInt32(long value, Style style) throws IOException {
        writeInt32((int)(value & 0xFFFFFFFFL), style);
    }

    public void writeInt64(long value) throws IOException {
        writeInt64(value, style);
    }

    public void writeInt64(long value, Style style) throws IOException {
        byte[] b = new byte[8];
        if (style == Style.BigEndian) {
            b[0] = (byte)((value >>> 56) & 0xFF);
            b[1] = (byte)((value >>> 48) & 0xFF);
            b[2] = (byte)((value >>> 40) & 0xFF);
            b[3] = (byte)((value >>> 32) & 0xFF);
            b[4] = (byte)((value >>> 24) & 0xFF);
            b[5] = (byte)((value >>> 16) & 0xFF);
            b[6] = (byte)((value >>> 8) & 0xFF);
            b[7] = (byte)(value & 0xFF);
        } else {
            b[7] = (byte)((value >>> 56) & 0xFF);
            b[6] = (byte)((value >>> 48) & 0xFF);
            b[5] = (byte)((value >>> 40) & 0xFF);
            b[4] = (byte)((value >>> 32) & 0xFF);
            b[3] = (byte)((value >>> 24) & 0xFF);
            b[2] = (byte)((value >>> 16) & 0xFF);
            b[1] = (byte)((value >>> 8) & 0xFF);
            b[0] = (byte)(value & 0xFF);
        }
        out.write(b);
    }
}