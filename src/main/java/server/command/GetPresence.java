package server.command;

import server.Globals;
import server.MySQL;
import server.Tools;
import server.io.EndianIO;
import server.io.Reader;
import server.io.Writer;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public final class GetPresence {
    private GetPresence() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szAuthKey = Tools.BytesToHexString(authKey);
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        String szGamertag = Tools.readUtf8TrimZeros(r.readBytes(0x10));
        String szTitleID = Integer.toHexString(r.readInt32()).toUpperCase();
        String titleName = Tools.TitleIdToString(szTitleID);
        int iKVStatus = r.readInt32();
        String szKVSerial = Tools.readAscii(r.readBytes(0xC));
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | Console Online", new Date(), ip));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("Gamertag: " + szGamertag);
        System.out.println("Title ID: " + szTitleID + " (" + titleName + ")");
        System.out.println("KV Status: " + iKVStatus);
        byte[] response = new byte[0x14];
        int flag = Globals.StatusSuccess;
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            boolean bIsBanned = Globals.client.Banned == 1;
            Date now = new Date();
            long remainingMillis = (Globals.client.Expires != null ? Globals.client.Expires.getTime() : now.getTime()) - now.getTime();
            long remainingDays = remainingMillis / (24L * 3600L * 1000L);
            boolean bIsLifetime = remainingDays >= 420;
            MySQL.GetSettingsData(Globals.setting);
            boolean bIsFreeMode = Globals.setting.FreeMode == 1;
            boolean bIsTimeExpired = Globals.client.Expires == null || !Globals.client.Expires.after(now);
            if (bIsBanned) {
                flag = Globals.StatusBanned;
            } else if (bIsLifetime) {
                flag = Globals.StatusLifetime;
            } else if (bIsFreeMode) {
                flag = Globals.StatusFreeMode;
            } else if (bIsTimeExpired) {
                flag = Globals.StatusTimeExpired;
            } else {
                flag = Globals.StatusSuccess;
            }
            if (Globals.client.KVSerial != null && Globals.client.KVSerial.equals(szKVSerial)) {
                long diff = now.getTime() - (Globals.client.FirstOnline != null ? Globals.client.FirstOnline.getTime() : now.getTime());
                Globals.client.KVDays = (int)(diff / (24L * 3600L * 1000L));
                MySQL.SaveClientData(Globals.client, "kvdays", false);
            }
            Globals.client.Gamertag = szGamertag;
            Globals.client.TitleID  = szTitleID;
            Globals.client.KVStatus = iKVStatus;
            MySQL.SaveClientData(Globals.client, "pres", false);
            int remDays = (int)Math.max(0, remainingDays);
            int remHours = (int)Math.max(0, (remainingMillis / (3600L * 1000L)) % 24);
            int remMinutes = (int)Math.max(0, (remainingMillis / (60L * 1000L)) % 60);
            putIntBE(response, 0x0, flag);
            putIntBE(response, 0x4, remDays);
            putIntBE(response, 0x8, remHours);
            putIntBE(response, 0xC, remMinutes);
            putIntBE(response, 0x10, Globals.client.KVDays);
            w.write(response, 0, response.length);
        }
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
}