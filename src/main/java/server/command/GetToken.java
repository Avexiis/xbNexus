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

public final class GetToken {
    private GetToken() {}

    public static void Packet(byte[] authKey, Socket socket, EndianIO dataStream, String ip) throws IOException {
        long start = System.currentTimeMillis();
        Reader r = dataStream.Reader;
        Writer w = dataStream.Writer;
        String szAuthKey = Tools.BytesToHexString(authKey);
        String szCPUKey = Tools.BytesToHexString(r.readBytes(0x10));
        String szToken = Tools.readUtf8TrimZeros(r.readBytes(0x10));
        System.out.println(String.format("[%1$tD %1$tI:%1$tM:%1$tS %1$Tp] %2$s | New Console Registered", new Date(), ip));
        System.out.println("Auth Key: " + szAuthKey);
        System.out.println("CPU Key: " + szCPUKey);
        System.out.println("Token: " + szToken);
        Globals.client.CPUKey = szCPUKey;
        if (MySQL.GetClientData(Globals.client)) {
            Globals.token.Token = szToken;
            if (MySQL.GetTokenData(Globals.token)) {
                if (Globals.token.Status != 1) {
                    Globals.token.Token  = szToken;
                    Globals.token.UsedBy = szCPUKey;
                    Globals.client.CPUKey = szCPUKey;
                    MySQL.SaveClientTime(Globals.token, Globals.client);
                    w.writeUInt32(Globals.StatusSuccess);
                    w.writeUInt32(Globals.token.Days);
                } else {
                    w.writeUInt32(Globals.StatusFailed);
                }
            } else {
                w.writeUInt32(Globals.StatusFailed);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Executed in " + elapsed + "ms\n");
        socket.close();
    }
}