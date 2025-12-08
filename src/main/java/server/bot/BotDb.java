package server.bot;

import server.Globals;

import java.sql.*;
import java.util.Optional;

public final class BotDb {
    public static final class ClientRow {
        public int id;
        public String cpukey;
        public String gamertag;
        public String titleid;
        public Timestamp firstonline;
        public Timestamp lastonline;
        public Integer kvdays;
        public Integer banned;
        public String discordId;
        public Integer consoleLinked;
        public Integer totalxke;
        public Integer totalxos;
    }

    public static final class ChallengeTotals {
        public final int totalXke;
        public final int totalXos;
        public ChallengeTotals(int totalXke, int totalXos) {
            this.totalXke = totalXke;
            this.totalXos = totalXos;
        }
    }

    private Connection connect() throws SQLException {
        String url = "jdbc:mysql://" + Globals.DB_HOST + ":" + Globals.DB_PORT + "/" + Globals.DB_NAME + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";
        return DriverManager.getConnection(url, Globals.DB_USER, Globals.DB_PASS);
    }

    public Optional<ClientRow> getUserByDiscordId(String discordId) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM clients WHERE discord_id=? LIMIT 1")) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<ClientRow> getUserByCpuKey(String cpuKey) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM clients WHERE cpukey=? LIMIT 1")) {
            ps.setString(1, cpuKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public int getHighestIdFromClients() throws SQLException {
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(id) AS id FROM clients")) {
            if (rs.next()) return rs.getInt("id");
        }
        return 0;
    }

    public void insertToken(String token, int days, String usedBy, int status) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("INSERT INTO tokens(token,days,usedby,status) VALUES(?,?,?,?)")) {
            ps.setString(1, token);
            ps.setInt(2, days);
            ps.setString(3, usedBy);
            ps.setInt(4, status);
            ps.executeUpdate();
        }
    }

    public boolean linkDiscordIdToCpuKey(String discordId, String cpuKey) throws SQLException {
        try (Connection c = connect()) {
            try (PreparedStatement ps1 = c.prepareStatement("UPDATE clients SET discord_id=? WHERE cpukey=?")) {
                ps1.setString(1, discordId);
                ps1.setString(2, cpuKey);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = c.prepareStatement("UPDATE clients SET console_linked=1 WHERE cpukey=?")) {
                ps2.setString(1, cpuKey);
                return ps2.executeUpdate() > 0;
            }
        }
    }

    public boolean unlinkByDiscordId(String discordId) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("UPDATE clients SET discord_id=NULL, console_linked=0 WHERE discord_id=?")) {
            ps.setString(1, discordId);
            return ps.executeUpdate() > 0;
        }
    }

    public ChallengeTotals getGlobalChallengeTotals() throws SQLException {
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(totalxke),0) AS sxke, COALESCE(SUM(totalxos),0) AS sxos FROM clients")) {
            if (rs.next()) {
                return new ChallengeTotals(rs.getInt("sxke"), rs.getInt("sxos"));
            }
            return new ChallengeTotals(0, 0);
        }
    }

    private static ClientRow map(ResultSet rs) throws SQLException {
        ClientRow r = new ClientRow();
        r.id = rs.getInt("id");
        r.cpukey = rs.getString("cpukey");
        r.gamertag = rs.getString("gamertag");
        r.titleid = rs.getString("titleid");
        r.firstonline = rs.getTimestamp("firstonline");
        r.lastonline = rs.getTimestamp("lastonline");
        r.kvdays = getIntOrNull(rs, "kvdays");
        r.banned = getIntOrNull(rs, "banned");
        r.discordId = rs.getString("discord_id");
        r.consoleLinked = getIntOrNull(rs, "console_linked");
        r.totalxke = getIntOrNull(rs, "totalxke");
        r.totalxos = getIntOrNull(rs, "totalxos");
        return r;
    }

    private static Integer getIntOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }
}
