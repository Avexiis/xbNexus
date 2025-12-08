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
import java.util.Arrays;
import java.util.Date;

public final class GetNoKV {
    private GetNoKV() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szAuthKey = Tools.BytesToHexString(authKey);
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | Sending KV to Console", new Date(), ip));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("CPU Key: " + szCPUKey);
        byte[] response = new byte[4];
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            if (Globals.setting.NoKVMode != 0 && Globals.client.NoKVMode != 0) {
                putIntBE(response, 0, Globals.StatusSuccess);
                w.write(response, 0, 4);
                File kvFile = new File("data/nokv/" + Globals.client.NoKVIndex + "/kv.bin");
                byte[] kv = Files.readAllBytes(kvFile.toPath());
                byte[] noKVData = new byte[0x798];
                System.arraycopy(Arrays.copyOfRange(kv, 0xD0, 0xD0 + 0x10), 0, noKVData, 0x0, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x298, 0x298 + 0x1D0), 0, noKVData, 0x10, 0x1D0);
                System.arraycopy(Arrays.copyOfRange(kv, 0x468, 0x468 + 0x390), 0, noKVData, 0x1E0, 0x390);
                System.arraycopy(Arrays.copyOfRange(kv, 0x9C8, 0x9C8 + 0x1A8), 0, noKVData, 0x570, 0x1A8);
                System.arraycopy(Arrays.copyOfRange(kv, 0x158, 0x158 + 0x10), 0, noKVData, 0x718, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x168, 0x168 + 0x10), 0, noKVData, 0x728, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x198, 0x198 + 0x10), 0, noKVData, 0x738, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x1A8, 0x1A8 + 0x10), 0, noKVData, 0x748, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x1F8, 0x1F8 + 0x10), 0, noKVData, 0x758, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x208, 0x208 + 0x10), 0, noKVData, 0x768, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x238, 0x238 + 0x10), 0, noKVData, 0x778, 0x10);
                System.arraycopy(Arrays.copyOfRange(kv, 0x248, 0x248 + 0x10), 0, noKVData, 0x788, 0x10);
                w.write(noKVData);
            } else {
                putIntBE(response, 0, Globals.StatusNoKVDisabled);
                w.write(response, 0, 4);
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