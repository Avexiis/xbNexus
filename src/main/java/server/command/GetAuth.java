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
import java.util.Random;

public final class GetAuth {

    private GetAuth() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        String szAuthKey = Tools.BytesToHexString(authKey);
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        int noKV = r.readInt32();
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        String szKVSerial = Tools.readAscii(r.readBytes(0xC));
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | Console Login", new Date(), ip));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("KV Serial: " + szKVSerial);
        byte[] response = new byte[4];
        int noKVIndex;
        int noKVPool = 10;
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            if (Globals.client.NoKVMode == 1 && Globals.client.NoKVPicked == 0) {
                while (true) {
                    noKVIndex = 1 + new Random().nextInt(noKVPool - 1 + 1 - 1);
                    if (MySQL.GetSharedNoKVCount(noKVIndex) < 2) {
                        Globals.client.NoKVPicked = 1;
                        break;
                    }
                }
            } else {
                if (Globals.client.Banned == 1) {
                    noKV = 0;
                    noKVIndex = 0;
                } else {
                    noKVIndex = Globals.client.NoKVIndex;
                }
            }
            if (Globals.client.NoKVMode == 0) {
                if (Globals.client.KVSerial == null || !Globals.client.KVSerial.equals(szKVSerial)) {
                    Globals.client.KVSerial = szKVSerial;
                    MySQL.SaveClientData(Globals.client, "resetkv", false);
                }
            }
            Globals.client.IP = ip;
            Globals.client.LastOnline = new Date();
            Globals.client.NoKVMode = noKV;
            Globals.client.NoKVIndex = (Globals.client.NoKVMode == 1 && Globals.client.NoKVPicked == 1) ? 0 : (Globals.client.NoKVIndex == 0 ? 0 : Globals.client.NoKVIndex);
            if (Globals.client.NoKVMode == 1 && Globals.client.NoKVPicked == 1) {
                Globals.client.NoKVIndex = 0;
            }
            if (Globals.client.NoKVMode == 1 && Globals.client.NoKVPicked == 1) {
                Globals.client.NoKVIndex = noKVIndex;
            }
            MySQL.SaveClientData(Globals.client, "auth", false);
        } else {
            Globals.client.IP = ip;
            Globals.client.CPUKey = szCPUKey;
            MySQL.AddClientData(Globals.client);
        }
        response[0] = (byte)((Globals.StatusSuccess >>> 24) & 0xFF);
        response[1] = (byte)((Globals.StatusSuccess >>> 16) & 0xFF);
        response[2] = (byte)((Globals.StatusSuccess >>> 8) & 0xFF);
        response[3] = (byte)(Globals.StatusSuccess & 0xFF);
        w.write(response, 0, 4);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Executed in " + elapsed + "ms\n");
        socket.close();
    }
}