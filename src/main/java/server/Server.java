package server;

import server.command.*;
import server.io.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;

public final class Server {
    private Server() {}

    public static void main(String[] args) {
        System.out.println("------------------------------------------------------");
        System.out.println("* " + Globals.STEALTH_NAME + " Online");
        System.out.println("* Listening on port: " + Globals.STEALTH_PORT);
        System.out.println("* Ready to accept connections from Xbox 360 Consoles!");
        System.out.println("------------------------------------------------------");
        try {
            Globals.XKECResponse = Files.readAllBytes(new File("data/xkec/xke_resp.bin").toPath());
            Globals.XOSCResponse = Files.readAllBytes(new File("data/xosc/xos_resp.bin").toPath());
            Globals.XamHeader = Files.readAllBytes(new File("data/xosc/xam.bin").toPath());
            Globals.KernelHeader = Files.readAllBytes(new File("data/xosc/kernel.bin").toPath());
            Globals.TitleHeader = Files.readAllBytes(new File("data/xosc/FFFE07D1.bin").toPath());
            Globals.XOSCHeader = Files.readAllBytes(new File("data/xosc/xosc.bin").toPath());
            ServerSocket serverSocket = new ServerSocket(Globals.STEALTH_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
        try {
            String ip = client.getInetAddress().getHostAddress();
            InputStream netStream = client.getInputStream();
            byte[] header = new byte[0x18];
            if (netStream.read(header) != 0x18) {
                client.close();
                return;
            }
            EndianIO headerIO = new EndianIO(header, Style.BigEndian);
            int command = headerIO.Reader.readInt32();
            int size = headerIO.Reader.readInt32();
            byte[] data = new byte[size];
            if (netStream.read(data) != size) {
                client.close();
                return;
            }
            EndianIO dataStream = new EndianIO(data, Style.BigEndian);
            dataStream.Writer = new server.io.Writer(client.getOutputStream(), Style.BigEndian);
            byte[] authKey = new byte[0x10];
            System.arraycopy(header, 8, authKey, 0, 0x10);
            switch (command) {
                case Globals.CommandGetUpdate:
                    GetUpdate.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandAuthUser:
                    GetAuth.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetXKEC:
                    GetXKEC.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetXOSC:
                    GetXOSC.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetPresence:
                    GetPresence.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetToken:
                    GetToken.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetEngine:
                    GetEngine.Packet(authKey, client, dataStream, ip);
                    break;
                case Globals.CommandGetNoKV:
                    GetNoKV.Packet(authKey, client, dataStream, ip);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    client.close();
                    break;
            }
        } catch (Exception e) {
            System.err.println("Client handling error: " + e.getMessage());
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }
}