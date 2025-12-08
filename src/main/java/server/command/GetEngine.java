package server.command;

import server.Globals;
import server.MySQL;
import server.Tools;
import server.io.EndianIO;
import server.io.Reader;
import server.io.Writer;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;

public final class GetEngine {
    private GetEngine() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szAuthKey = Tools.BytesToHexString(authKey);
        byte[] engineHash = r.readBytes(0x14);
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        String titleID = Tools.BytesToHexString(r.readBytes(0x4));
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | Engine Requested by Console", new Date(), ip));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("Title ID: " + titleID);
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            String enginePath = "data/engine/" + titleID + "/Engine.xex";
            File f = new File(enginePath);
            if (!f.exists()) {
                byte[] response = new byte[4];
                int code = Globals.StatusFailed;
                response[0] = (byte)((code >>> 24) & 0xFF);
                response[1] = (byte)((code >>> 16) & 0xFF);
                response[2] = (byte)((code >>> 8)  & 0xFF);
                response[3] = (byte)(code & 0xFF);
            } else {
                byte[] latestEngine = Files.readAllBytes(f.toPath());
                byte[] latestHash = Tools.SHA(latestEngine);
                boolean outdated = !Tools.CompareBytes(engineHash, latestHash);
                System.out.println("Engine Hash: " + Tools.BytesToHexString(engineHash));
                byte[] response = new byte[0x1C];
                int status = outdated ? Globals.StatusXexOutdated : Globals.StatusSuccess;
                putIntBE(response, 0, status);
                if (outdated) {
                    putIntBE(response, 4, latestEngine.length);
                    System.arraycopy(latestHash, 0, response, 8, 0x14);
                }
                w.write(response, 0, response.length);
                if (outdated) {
                    w.write(latestEngine, 0, latestEngine.length);
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Executed in " + elapsed + "ms\n");
        socket.close();
    }

    private static void putIntBE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte)((value >>> 24) & 0xFF);
        buffer[offset +1] = (byte)((value >>> 16) & 0xFF);
        buffer[offset +2] = (byte)((value >>> 8) & 0xFF);
        buffer[offset +3] = (byte)(value & 0xFF);
    }
}