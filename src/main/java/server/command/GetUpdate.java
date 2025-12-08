package server.command;

import server.Globals;
import server.Tools;
import server.io.EndianIO;
import server.io.Reader;
import server.io.Writer;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;

public final class GetUpdate {
    private GetUpdate() {}

    private static final String CLASSPATH_XEX = "data/client/xbNexus.xex"; //inside the jar
    private static final File FS_XEX = new File("data/client/xbNexus.xex"); //dev fallback

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szAuthKey  = Tools.BytesToHexString(authKey);
        byte[] clientHash = r.readBytes(0x14);
        byte[] latestModule = loadLatestModuleBytes();
        if (latestModule == null || latestModule.length == 0) {
            System.out.println(timeLine(ip, "XEX Update Check (NO MODULE FOUND)"));
            System.out.println("Auth Key: " + szAuthKey);
            System.out.println("Client Hash: " + Tools.BytesToHexString(clientHash));
            sendStatusOnly(w, Globals.StatusSuccess);
            finishAndClose(start, socket);
            return;
        }
        byte[] latestHash = Tools.SHA(latestModule);
        boolean outdated  = !Tools.CompareBytes(clientHash, latestHash);
        System.out.println(timeLine(ip, "XEX Update Check"));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("Client Hash: " + Tools.BytesToHexString(clientHash));
        System.out.println("Latest Hash: " + Tools.BytesToHexString(latestHash));
        System.out.println("Latest Size: " + latestModule.length + " bytes");
        byte[] header = new byte[0x1C];
        int status = Globals.HashCheck ? (outdated ? Globals.StatusXexOutdated : Globals.StatusSuccess) : Globals.StatusSuccess;
        putIntBE(header, 0, status);
        if (outdated) {
            putIntBE(header, 4, latestModule.length);
            System.arraycopy(latestHash, 0, header, 8, 0x14);
        }
        w.write(header, 0, header.length);
        if (outdated) {
            w.write(latestModule, 0, latestModule.length);
        }
        finishAndClose(start, socket);
    }

    private static void sendStatusOnly(Writer w, int status) throws IOException {
        byte[] header = new byte[0x1C];
        putIntBE(header, 0, status);
        w.write(header, 0, header.length);
    }

    private static String timeLine(String ip, String what) {
        return String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | %3$s", new Date(), ip, what);
    }

    private static void putIntBE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte)((value >>> 24) & 0xFF);
        buffer[offset + 1] = (byte)((value >>> 16) & 0xFF);
        buffer[offset + 2] = (byte)((value >>> 8) & 0xFF);
        buffer[offset + 3] = (byte)( value & 0xFF);
    }

    private static void finishAndClose(long start, Socket socket) {
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Executed in " + elapsed + "ms\n");
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }

    private static byte[] loadLatestModuleBytes() {
        try (InputStream in = GetUpdate.class.getClassLoader().getResourceAsStream(CLASSPATH_XEX)) {
            if (in != null) return readAll(in);
        } catch (IOException ignored) {}
        try {
            if (FS_XEX.exists() && FS_XEX.isFile()) return Files.readAllBytes(FS_XEX.toPath());
        } catch (IOException ignored) {}

        return null;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(128 * 1024);
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) >= 0) {
            if (n > 0) bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
