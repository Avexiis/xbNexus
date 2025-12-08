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

public final class GetXOSC {
    private GetXOSC() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        byte[] cpuKeyDigest = r.readBytes(0x10);
        byte[] kvDigest = r.readBytes(0x14);
        byte[] spoofedMACAddress = r.readBytes(0x6);
        byte[] phaseLevel = r.readBytes(0x1);
        byte[] inquiryData = r.readBytes(0x24);
        byte[] kvSerial = r.readBytes(0xC);
        byte[] gameRegion = r.readBytes(0x2);
        byte[] oddFeatures = r.readBytes(0x2);
        byte[] policyFlashSize = r.readBytes(0x4);
        byte[] consoleId = r.readBytes(0x5);
        byte[] magic = r.readBytes(0x10);
        byte[] xamAlloc = r.readBytes(8);
        int consoleIndex = r.readUByte();
        boolean crl = r.readBoolean();
        boolean fcrt = r.readBoolean();
        boolean type1KV = r.readBoolean();
        byte[] xosc = Globals.XOSCResponse;
        byte[] resp = new byte[4];
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            Globals.client.TotalXOS += 1;
            MySQL.SaveClientData(Globals.client, "challs", true);
        }
        putIntBE(resp, 0, Globals.StatusSuccess);
        w.write(resp, 0, 4);
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | XOS Challenge", new Date(), ip));
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("KV Serial: " + Tools.readAscii(kvSerial));
        System.out.println("KV Digest: " + Tools.BytesToHexString(kvDigest));
        System.out.println(String.format("Crl: %s, Fcrt: %s, Type %s KV", (crl ? "True" : "False"), (fcrt ? "True" : "False"), (type1KV ? "1" : "2")));
        putShortBE(xosc, 0xE, type1KV ? (short)0x01BD : (short)0x01BF);
        System.arraycopy(cpuKeyDigest, 0, xosc, 0x50, 0x10);
        byte[] titleDigests = API.ComputeTitleDigests(kvDigest, consoleIndex, spoofedMACAddress, magic, xamAlloc);
        System.arraycopy(titleDigests, 0, xosc, 0x60, 0x10);
        System.arraycopy(API.FuseHashes[consoleIndex], 0, xosc, 0x70, 0x10);
        System.arraycopy(phaseLevel, 0, xosc, 0x83, 1);
        System.arraycopy(inquiryData, 0, xosc, 0xF0, 0x24);
        System.arraycopy(inquiryData, 0, xosc, 0x114, 0x24);
        System.arraycopy(kvSerial, 0, xosc, 0x138, 0xC);
        putShortBE(xosc, 0x146, type1KV ? (short)0xD81E : (short)0xD83E);
        System.arraycopy(gameRegion, 0, xosc, 0x148, 2);
        System.arraycopy(oddFeatures, 0, xosc, 0x14A, 2);
        byte[] reversedPolicyFlashSize = new byte[4];
        for (int i = 0; i < 4; i++) {
            reversedPolicyFlashSize[i] = policyFlashSize[3 - i];
        }
        System.arraycopy(reversedPolicyFlashSize, 0, xosc, 0x150, 4);
        putIntBE(xosc, 0x158, API.ComputeHVStatusFlags(crl, fcrt));
        putLongBE(xosc, 0x170, API.PCIEHardwareFlags[consoleIndex]);
        System.arraycopy(consoleId, 0, xosc, 0x1A0, 5);
        putIntBE(xosc, 0x1D0, API.HardwareFlags[consoleIndex]);
        w.write(xosc, 0, xosc.length);
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

    private static void putLongBE(byte[] buffer, int offset, long value) {
        buffer[offset] = (byte)((value >>> 56) & 0xFF);
        buffer[offset + 1] = (byte)((value >>> 48) & 0xFF);
        buffer[offset + 2] = (byte)((value >>> 40) & 0xFF);
        buffer[offset + 3] = (byte)((value >>> 32) & 0xFF);
        buffer[offset + 4] = (byte)((value >>> 24) & 0xFF);
        buffer[offset + 5] = (byte)((value >>> 16) & 0xFF);
        buffer[offset + 6] = (byte)((value >>> 8) & 0xFF);
        buffer[offset + 7] = (byte)(value & 0xFF);
    }
}