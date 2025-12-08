package server;

import java.util.Date;

public final class Globals {
    private Globals() {}

    /**Name of your stealth server*/
    public static final String STEALTH_NAME = "xbNexus";

    /**Network Ports*/
    public static int STEALTH_PORT = 4880;
    public static final int DB_PORT = 3306;

    /**Database Info and Login*/
    public static final String DB_HOST = "localhost";
    public static final String DB_NAME = "xbnexus";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "";

    /**Xbox/Challenge API Configs*/
    public static byte[] XamHeader = null;
    public static byte[] KernelHeader = null;
    public static byte[] TitleHeader = null;
    public static byte[] XOSCHeader = null;
    public static byte[] XKECResponse = null;
    public static byte[] XOSCResponse = null;
    public static final int CommandGetUpdate = 1;
    public static final int CommandAuthUser = 2;
    public static final int CommandGetXKEC = 3;
    public static final int CommandGetXOSC = 4;
    public static final int CommandGetPresence = 5;
    public static final int CommandGetToken = 6;
    public static final int CommandGetEngine = 7;
    public static final int CommandGetNoKV = 8;
    public static final int StatusSuccess = 1;
    public static final int StatusXexOutdated = 2;
    public static final int StatusTimeExpired = 3;
    public static final int StatusFailed = 4;
    public static final int StatusBanned = 5;
    public static final int StatusFreeMode = 6;
    public static final int StatusLifetime = 7;
    public static final int StatusNoKVDisabled = 8;
    public static boolean HashCheck = true;

    /**Clients DB table schema*/
    public static class ClientInfo {
        public int ID;
        public String IP;
        public String CPUKey;
        public String Gamertag;
        public String TitleID;
        public Date FirstOnline;
        public Date LastOnline;
        public Date Expires;
        public String KVSerial;
        public int KVStatus;
        public int KVDays;
        public int TotalXKE;
        public int TotalXOS;
        public int Banned;
        public int NoKVMode;
        public int NoKVPicked;
        public int NoKVIndex;
    }

    /**Tokens DB table schema*/
    public static class TokenInfo {
        public int ID;
        public String Token;
        public int Days;
        public String UsedBy;
        public int Status;
    }
    /**Client Settings*/
    public static class Settings {
        public int ID;
        public int FreeMode;
        public int NoKVMode;
    }

    public static ClientInfo client = new ClientInfo();
    public static TokenInfo token = new TokenInfo();
    public static Settings  setting = new Settings();
}