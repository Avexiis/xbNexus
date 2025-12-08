package server.command;

import server.API;
import server.Globals;
import server.MySQL;
import server.Tools;
import server.io.EndianIO;
import server.io.Reader;
import server.io.Writer;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public final class GetXKEC {
    private GetXKEC() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        byte[] hvSalt = r.readBytes(0x10);
        byte[] cpuKeyHash = r.readBytes(0x14);
        String szKVSerial = Tools.readAscii(r.readBytes(0xC));
        int consoleIndex = r.readUByte();
        boolean crl = r.readBoolean();
        boolean fcrt = r.readBoolean();
        boolean type1KV = r.readBoolean();
        byte[] xkec = Globals.XKECResponse;
        byte[] resp = new byte[4];
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            if (Globals.client.KVSerial != null && Globals.client.KVSerial.equals(szKVSerial)) {
                Date now = new Date();
                long diff = now.getTime() - (Globals.client.FirstOnline != null ? Globals.client.FirstOnline.getTime() : now.getTime());
                Globals.client.KVDays = (int)(diff / (24L * 3600L * 1000L));
                MySQL.SaveClientData(Globals.client, "kvdays", false);
            }
            Globals.client.TotalXKE += 1;
            MySQL.SaveClientData(Globals.client, "challs", false);
        }
        putIntBE(resp, 0, Globals.StatusSuccess);
        w.write(resp, 0, 4);
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | XKE Challenge", new Date(), ip));
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("Salt: " + Tools.BytesToHexString(hvSalt));
        System.out.println(String.format("Crl: %s, Fcrt: %s, Type %s KV", (crl ? "True" : "False"), (fcrt ? "True" : "False"), (type1KV ? "1" : "2")));
        putShortBE(xkec, 0x2E, type1KV ? (short)0xD81E : (short)0xD83E);
        putIntBE(xkec, 0x34, API.ComputeUpdateSequence(cpuKeyHash));
        putIntBE(xkec, 0x38, API.ComputeHVStatusFlags(crl, fcrt));
        putIntBE(xkec, 0x3C, API.ConsoleTypeFlags[consoleIndex]);
        byte[] saltData = API.FindSalt(hvSalt);
        if (saltData != null) {
            System.arraycopy(saltData, 0x10, xkec, 0x50, 0x14);
            System.arraycopy(saltData, 0x26, xkec, 0xF8, 2);
            System.arraycopy(saltData, 0x2A, xkec, 0xFA, 6);
        }
        System.arraycopy(cpuKeyHash, 0, xkec, 0x64, 0x14);
        w.write(xkec, 0, xkec.length);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Executed in " + elapsed + "ms\n");
        socket.close();
    }

    private static void putIntBE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte)((value >>> 24) & 0xFF);
        buffer[offset + 1] = (byte)((value >>> 16) & 0xFF);
        buffer[offset + 2] = (byte)((value >>> 8) & 0xFF);
        buffer[offset + 3] = (byte)(value & 0xFF);
    }

    private static void putShortBE(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte)((value >>> 8) & 0xFF);
        buffer[offset + 1] = (byte)(value & 0xFF);
    }
}