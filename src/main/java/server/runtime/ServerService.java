package server.runtime;

import server.Globals;
import server.command.*;
import server.io.EndianIO;
import server.io.Style;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerService {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService pool;
    private final RuntimeStats stats = new RuntimeStats();

    public synchronized boolean isRunning() {
        return running.get();
    }

    public synchronized int getPort() {
        return Globals.STEALTH_PORT;
    }

    public synchronized void setPort(int port) {
        if (isRunning()) throw new IllegalStateException("Cannot change port while running.");
        Globals.STEALTH_PORT = port;
    }

    public RuntimeStats getStats() {
        return stats;
    }

    public synchronized void loadAssets() throws IOException {
        Globals.XKECResponse = Files.readAllBytes(new File("data/xkec/xke_resp.bin").toPath());
        Globals.XOSCResponse = Files.readAllBytes(new File("data/xosc/xos_resp.bin").toPath());
        Globals.XamHeader = Files.readAllBytes(new File("data/xosc/xam.bin").toPath());
        Globals.KernelHeader = Files.readAllBytes(new File("data/xosc/kernel.bin").toPath());
        Globals.TitleHeader = Files.readAllBytes(new File("data/xosc/FFFE07D1.bin").toPath());
        Globals.XOSCHeader = Files.readAllBytes(new File("data/xosc/xosc.bin").toPath());
    }

    public synchronized void start() throws IOException {
        if (running.get()) return;
        if (Globals.XKECResponse == null || Globals.XOSCResponse == null || Globals.XamHeader == null
                || Globals.KernelHeader == null || Globals.TitleHeader == null || Globals.XOSCHeader == null) {
            loadAssets();
        }
        serverSocket = new ServerSocket(Globals.STEALTH_PORT);
        pool = Executors.newCachedThreadPool();
        running.set(true);
        stats.resetAndStart();
        acceptThread = new Thread(this::acceptLoop, "server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("------------------------------------------------------");
        System.out.println("* " + Globals.STEALTH_NAME + " Online");
        System.out.println("* Listening on port: " + Globals.STEALTH_PORT);
        System.out.println("* Ready to accept connections from Xbox 360 Consoles!");
        System.out.println("------------------------------------------------------");
    }

    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
        if (acceptThread != null) {
            try {
                acceptThread.join(1000);
            } catch (InterruptedException ignored) {
            }
            acceptThread = null;
        }
        System.out.println("[SERVER] Stopped.");
    }

    private void acceptLoop() {
        try {
            while (running.get()) {
                final Socket client = serverSocket.accept();
                if (pool == null) {
                    client.close();
                    continue;
                }
                pool.execute(() -> {
                    try {
                        handleClient(client);
                    } catch (Throwable t) {
                        System.err.println("Client handling error: " + t.getMessage());
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (running.get()) System.err.println("Server accept error: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleClient(Socket client) throws Exception {
        try {
            String ip = client.getInetAddress().getHostAddress();
            stats.recordConnection(ip);
            InputStream netStream = client.getInputStream();
            byte[] header = new byte[0x18];
            if (netStream.read(header) != 0x18) { client.close(); return; }
            EndianIO headerIO = new EndianIO(header, Style.BigEndian);
            int command = headerIO.Reader.readInt32();
            int size    = headerIO.Reader.readInt32();
            byte[] data = new byte[size];
            if (netStream.read(data) != size) { client.close(); return; }
            server.io.EndianIO dataStream = new server.io.EndianIO(data, Style.BigEndian);
            dataStream.Writer = new server.io.Writer(client.getOutputStream(), Style.BigEndian);
            byte[] authKey = new byte[0x10];
            System.arraycopy(header, 8, authKey, 0, 0x10);
            switch (command) {
                case server.Globals.CommandGetUpdate:
                    stats.incGetUpdate();
                    GetUpdate.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandAuthUser:
                    stats.incAuthUser();
                    GetAuth.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetXKEC:
                    stats.incGetXKEC();
                    GetXKEC.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetXOSC:
                    stats.incGetXOSC();
                    GetXOSC.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetPresence:
                    stats.incGetPresence();
                    GetPresence.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetToken:
                    stats.incGetToken();
                    GetToken.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetEngine:
                    stats.incGetEngine();
                    GetEngine.Packet(authKey, client, dataStream, ip);
                    break;
                case server.Globals.CommandGetNoKV:
                    stats.incGetNoKV();
                    GetNoKV.Packet(authKey, client, dataStream, ip);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    client.close();
                    break;
            }
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }
}
