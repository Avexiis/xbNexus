package server;

import java.sql.*;
import java.util.*;
import java.util.Date;

public final class MySQL {
    private MySQL() {}

    private static Connection Setup() throws SQLException {
        String url = "jdbc:mysql://" + Globals.DB_HOST + ":" + Globals.DB_PORT + "/" + Globals.DB_NAME + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";
        String user = Globals.DB_USER;
        String pass = Globals.DB_PASS;
        return DriverManager.getConnection(url, user, pass);
    }

    private static void closeQuiet(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean GetClientData(Globals.ClientInfo data) {
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            db = Setup();
            ps = db.prepareStatement("select * from `clients` where `cpukey` = ? limit 1");
            ps.setString(1, data.CPUKey);
            rs = ps.executeQuery();
            if (rs.next()) {
                data.ID = rs.getInt("id");
                data.IP = rs.getString("ip");
                data.CPUKey = rs.getString("cpukey");
                data.Gamertag = rs.getString("gamertag");
                data.TitleID = rs.getString("titleid");
                Timestamp fo = rs.getTimestamp("firstonline");
                Timestamp lo = rs.getTimestamp("lastonline");
                Timestamp ex = rs.getTimestamp("expires");
                data.FirstOnline = fo != null ? new Date(fo.getTime()) : null;
                data.LastOnline = lo != null ? new Date(lo.getTime()) : null;
                data.Expires = ex != null ? new Date(ex.getTime()) : null;
                data.KVSerial = rs.getString("kvserial");
                data.KVStatus = rs.getInt("kvstatus");
                data.KVDays = rs.getInt("kvdays");
                data.TotalXKE = rs.getInt("totalxke");
                data.TotalXOS = rs.getInt("totalxos");
                data.Banned = rs.getInt("banned");
                data.NoKVMode = rs.getInt("nokvmode");
                data.NoKVPicked = rs.getInt("nokvpicked");
                data.NoKVIndex = rs.getInt("nokvindex");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static boolean GetTokenData(Globals.TokenInfo data) {
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            db = Setup();
            ps = db.prepareStatement("select * from `tokens` where `token` = ? limit 1");
            ps.setString(1, data.Token);
            rs = ps.executeQuery();
            if (rs.next()) {
                data.ID = rs.getInt("id");
                data.Token = rs.getString("token");
                data.Days = rs.getInt("days");
                data.UsedBy = rs.getString("usedby");
                data.Status = rs.getInt("status");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static boolean GetSettingsData(Globals.Settings data) {
        Connection db = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            db = Setup();
            st = db.createStatement();
            rs = st.executeQuery("select * from `settings`");
            if (rs.next()) {
                data.ID = rs.getInt("id");
                data.FreeMode = rs.getInt("freemode");
                data.NoKVMode = rs.getInt("nokvmode");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(rs);
            closeQuiet(st);
            closeQuiet(db);
        }
    }

    public static void AddClientData(Globals.ClientInfo data) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            ps = db.prepareStatement("insert into `clients` (ip, cpukey, expires) values (?, ?, ?)");
            ps.setString(1, data.IP);
            ps.setString(2, data.CPUKey);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()), Calendar.getInstance());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static void SaveClientData(Globals.ClientInfo data, String type, boolean xosc) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            switch (type) {
                case "auth":
                    ps = db.prepareStatement("update `clients` set ip = ?, lastonline = ?, nokvmode = ?, nokvpicked = ?, nokvindex = ? where cpukey = ?");
                    ps.setString(1, data.IP);
                    ps.setTimestamp(2, data.LastOnline != null ? new Timestamp(data.LastOnline.getTime()) : new Timestamp(System.currentTimeMillis()));
                    ps.setInt(3, data.NoKVMode);
                    ps.setInt(4, data.NoKVPicked);
                    ps.setInt(5, data.NoKVIndex);
                    ps.setString(6, data.CPUKey);
                    break;
                case "pres":
                    ps = db.prepareStatement("update `clients` set gamertag = ?, titleid = ?, kvstatus = ?, lastonline = NOW() where cpukey = ?");
                    ps.setString(1, data.Gamertag);
                    ps.setString(2, data.TitleID);
                    ps.setInt(3, data.KVStatus);
                    ps.setString(4, data.CPUKey);
                    break;
                case "kvdays":
                    ps = db.prepareStatement("update `clients` set kvdays = ? where cpukey = ?");
                    ps.setInt(1, data.KVDays);
                    ps.setString(2, data.CPUKey);
                    break;
                case "resetkv":
                    ps = db.prepareStatement("update `clients` set firstonline = ?, kvserial = ?, kvdays = 0, totalxke = 0, totalxos = 0 where cpukey = ?");
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setString(2, data.KVSerial);
                    ps.setString(3, data.CPUKey);
                    break;
                case "challs":
                    ps = db.prepareStatement("update `clients` set " + (xosc ? "totalxos" : "totalxke") + " = ? where cpukey = ?");
                    ps.setInt(1, xosc ? data.TotalXOS : data.TotalXKE);
                    ps.setString(2, data.CPUKey);
                    break;
                default:
                    return;
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static void SaveClientTime(Globals.TokenInfo tdata, Globals.ClientInfo cdata) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            ps = db.prepareStatement("update `tokens` set usedby = ?, status = 1 where token = ?");
            ps.setString(1, tdata.UsedBy);
            ps.setString(2, tdata.Token);
            ps.executeUpdate();
            ps.close();
            Date now = new Date();
            if (cdata.Expires != null && cdata.Expires.after(now)) {
                cdata.Expires = new Date(cdata.Expires.getTime() + (long)tdata.Days * 24L * 3600L * 1000L);
            } else {
                cdata.Expires = new Date(now.getTime() + (long)tdata.Days * 24L * 3600L * 1000L);
            }
            ps = db.prepareStatement("update `clients` set expires = ? where cpukey = ?");
            ps.setTimestamp(1, new Timestamp(cdata.Expires.getTime()));
            ps.setString(2, cdata.CPUKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static int GetSharedNoKVCount(int id) {
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            db = Setup();
            ps = db.prepareStatement("SELECT COUNT(0) AS Total FROM `clients` WHERE nokvmode = 1 AND nokvindex = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("Total");
            }
            return 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static int GetOnlineConsoles() {
        Connection db = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            db = Setup();
            st = db.createStatement();
            rs = st.executeQuery("SELECT COUNT(0) FROM `clients` WHERE `lastonline` > CONVERT_TZ(NOW(), 'SYSTEM', '+00:00') - INTERVAL 3 MINUTE");
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return 0;
        } finally {
            closeQuiet(rs); 
            closeQuiet(st);
            closeQuiet(db);
        }
    }

    public static boolean SaveSettingsData(Globals.Settings data) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            if (data.ID > 0) {
                ps = db.prepareStatement("update `settings` set freemode=?, nokvmode=? where id=?");
                ps.setInt(1, data.FreeMode);
                ps.setInt(2, data.NoKVMode);
                ps.setInt(3, data.ID);
            } else {
                ps = db.prepareStatement("update `settings` set freemode=?, nokvmode=? limit 1");
                ps.setInt(1, data.FreeMode);
                ps.setInt(2, data.NoKVMode);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static List<Globals.ClientInfo> ListClients(String query, int limit, int offset) {
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Globals.ClientInfo> out = new ArrayList<>();
        try {
            db = Setup();
            String base = "select id, cpukey, gamertag, titleid, lastonline, expires, banned from `clients`";
            boolean hasQuery = query != null && !query.isEmpty();
            if (hasQuery) {
                base += " where cpukey like ? or gamertag like ?";
            }
            base += " order by (lastonline is null), lastonline desc limit ? offset ?";
            ps = db.prepareStatement(base);
            int idx = 1;
            if (hasQuery) {
                String like = "%" + query + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, Math.max(1, limit));
            ps.setInt(idx, Math.max(0, offset));
            rs = ps.executeQuery();
            while (rs.next()) {
                Globals.ClientInfo c = new Globals.ClientInfo();
                c.ID = rs.getInt("id");
                c.CPUKey = rs.getString("cpukey");
                c.Gamertag = rs.getString("gamertag");
                c.TitleID = rs.getString("titleid");
                Timestamp lo = rs.getTimestamp("lastonline");
                Timestamp ex = rs.getTimestamp("expires");
                c.LastOnline = lo != null ? new Date(lo.getTime()) : null;
                c.Expires = ex != null ? new Date(ex.getTime()) : null;
                c.Banned = rs.getInt("banned");
                out.add(c);
            }
            return out;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return out;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static boolean SetClientBanned(String cpuKey, boolean banned) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            ps = db.prepareStatement("update `clients` set banned=? where cpukey=?");
            ps.setInt(1, banned ? 1 : 0);
            ps.setString(2, cpuKey);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static int CountClients() {
        Connection db = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            db = Setup();
            st = db.createStatement();
            rs = st.executeQuery("select count(0) from `clients`");
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(st);
            closeQuiet(db);
        }
    }

    public static List<Globals.TokenInfo> ListTokens(int limit, int offset) {
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Globals.TokenInfo> out = new ArrayList<>();
        try {
            db = Setup();
            String sql = "select id, token, days, usedby, status from `tokens` order by id desc limit ? offset ?";
            ps = db.prepareStatement(sql);
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, Math.max(0, offset));
            rs = ps.executeQuery();
            while (rs.next()) {
                Globals.TokenInfo t = new Globals.TokenInfo();
                t.ID = rs.getInt("id");
                t.Token = rs.getString("token");
                t.Days = rs.getInt("days");
                t.UsedBy = rs.getString("usedby");
                t.Status = rs.getInt("status");
                out.add(t);
            }
            return out;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return out;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static boolean InsertToken(String token, int days) {
        Connection db = null;
        PreparedStatement ps = null;
        try {
            db = Setup();
            ps = db.prepareStatement("insert into `tokens` (token, days, usedby, status) values (?, ?, ?, 0)");
            ps.setString(1, token);
            ps.setInt(2, days);
            ps.setString(3, "00000000000000000000000000000000");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } finally {
            closeQuiet(ps);
            closeQuiet(db);
        }
    }
    public static int CountLifetimeClients() {
        Connection db = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            db = Setup();
            String sql =
                    "SELECT COUNT(DISTINCT c.cpukey) " + "FROM clients c " + "LEFT JOIN tokens t ON t.usedby = c.cpukey AND t.status = 1 " +
                            "WHERE (t.days >= 999) " + "   OR (c.expires IS NOT NULL AND c.expires >= CONVERT_TZ(NOW(),'SYSTEM','+00:00') + INTERVAL 999 DAY)";
            ps = db.prepareStatement(sql);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static int CountBannedClients() {
        Connection db = null; Statement st = null; ResultSet rs = null;
        try {
            db = Setup(); st = db.createStatement();
            rs = st.executeQuery("SELECT COUNT(0) FROM `clients` WHERE `banned` = 1");
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage()); return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(st);
            closeQuiet(db);
        }
    }

    public static int CountTokens() {
        Connection db = null; Statement st = null; ResultSet rs = null;
        try {
            db = Setup(); st = db.createStatement();
            rs = st.executeQuery("SELECT COUNT(0) FROM `tokens`");
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage()); return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(st);
            closeQuiet(db);
        }
    }

    public static int CountTokensExact(int days) {
        Connection db = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            db = Setup();
            ps = db.prepareStatement("SELECT COUNT(0) FROM `tokens` WHERE `days` = ?");
            ps.setInt(1, days);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage()); return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }

    public static int CountTokensLifetime() {
        Connection db = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            db = Setup();
            ps = db.prepareStatement("SELECT COUNT(0) FROM `tokens` WHERE `days` >= 999");
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage()); return 0;
        } finally {
            closeQuiet(rs);
            closeQuiet(ps);
            closeQuiet(db);
        }
    }
}