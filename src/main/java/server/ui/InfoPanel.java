package server.ui;

import server.MySQL;
import server.runtime.RuntimeStats;
import server.runtime.ServerService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public final class InfoPanel extends JPanel {
    private final ServerService service;
    private final JLabel lblUptime = new JLabel("-");
    private final JLabel lblUnique = new JLabel("-");
    private final JLabel lblTotal = new JLabel("-");
    private final JLabel lblUpdate = new JLabel("-");
    private final JLabel lblAuth = new JLabel("-");
    private final JLabel lblXkec = new JLabel("-");
    private final JLabel lblXosc = new JLabel("-");
    private final JLabel lblPres = new JLabel("-");
    private final JLabel lblToken = new JLabel("-");
    private final JLabel lblEngine = new JLabel("-");
    private final JLabel lblNoKV = new JLabel("-");
    private final JLabel lblRegistered = new JLabel("-");
    private final JLabel lblLifetimeClients = new JLabel("-");
    private final JLabel lblBannedClients = new JLabel("-");
    private final JLabel lblTokensTotal = new JLabel("-");
    private final JLabel lblTokensDay = new JLabel("-");
    private final JLabel lblTokensWeek = new JLabel("-");
    private final JLabel lblTokensMonth = new JLabel("-");
    private final JLabel lblTokensLifetime = new JLabel("-");
    private final Timer runtimeTimer;
    private final Timer dbTimer;
    private volatile boolean dbRefreshRunning = false;

    public InfoPanel(ServerService service) {
        super(new BorderLayout(12, 12));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        leftAlignDbValueLabels();
        JPanel top = new JPanel(new GridLayout(1, 2, 12, 12));
        top.add(buildRuntimeCard());
        top.add(buildCommandCard());
        JPanel db = buildDbCard();
        add(top, BorderLayout.NORTH);
        add(db, BorderLayout.CENTER);
        runtimeTimer = new Timer(1000, e -> refreshRuntime());
        runtimeTimer.start();
        dbTimer = new Timer(5000, e -> refreshDatabase());
        dbTimer.start();
        refreshRuntime();
        refreshDatabase();
    }

    private void leftAlignDbValueLabels() {
        lblRegistered.setHorizontalAlignment(SwingConstants.LEFT);
        lblLifetimeClients.setHorizontalAlignment(SwingConstants.LEFT);
        lblBannedClients.setHorizontalAlignment(SwingConstants.LEFT);
        lblTokensTotal.setHorizontalAlignment(SwingConstants.LEFT);
        lblTokensDay.setHorizontalAlignment(SwingConstants.LEFT);
        lblTokensWeek.setHorizontalAlignment(SwingConstants.LEFT);
        lblTokensMonth.setHorizontalAlignment(SwingConstants.LEFT);
        lblTokensLifetime.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private JPanel buildRuntimeCard() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(new TitledBorder("Uptime & Connection Stats"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        int r = 0;
        g.gridx = 0;
        g.gridy = r;
        left.add(new JLabel("Stealth Uptime:"), g);
        g.gridx = 1;
        left.add(lblUptime, g);
        r++;
        g.gridx = 0;
        g.gridy = r;
        left.add(new JLabel("Unique connections:"), g);
        g.gridx = 1;
        left.add(lblUnique, g);
        r++;
        g.gridx = 0;
        g.gridy = r;
        left.add(new JLabel("Total connections:"), g);
        g.gridx = 1;
        left.add(lblTotal, g);
        return left;
    }

    private JPanel buildCommandCard() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(new TitledBorder("Server Information"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        int k = 0;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("Update Checks:"), c);
        c.gridx = 1;
        right.add(lblUpdate, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("Console Logins:"), c);
        c.gridx = 1;
        right.add(lblAuth, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("XKE Challenge Count:"), c);
        c.gridx = 1;
        right.add(lblXkec, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("XOSC Challenge Count:"), c);
        c.gridx = 1;
        right.add(lblXosc, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("Total Presence Updates:"), c);
        c.gridx = 1;
        right.add(lblPres, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("New Token Usages:"), c);
        c.gridx = 1;
        right.add(lblToken, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("Engines Sent to Consoles:"), c);
        c.gridx = 1;
        right.add(lblEngine, c);
        k++;
        c.gridx = 0;
        c.gridy = k;
        right.add(new JLabel("No-KV Usages:"), c);
        c.gridx = 1;
        right.add(lblNoKV, c);
        return right;
    }

    private JPanel buildDbCard() {
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setBorder(new TitledBorder("Database"));
        GridBagConstraints base = new GridBagConstraints();
        base.insets = new Insets(4, 6, 4, 6);
        base.anchor = GridBagConstraints.WEST;
        int r = 0;
        addRow(mid, base, r++, "Total Clients:", lblRegistered);
        addRow(mid, base, r++, "Lifetime Clients:", lblLifetimeClients);
        addRow(mid, base, r++, "Banned Clients:", lblBannedClients);
        addRow(mid, base, r++, "Total Tokens:", lblTokensTotal);
        addRow(mid, base, r++, "Day Tokens:", lblTokensDay);
        addRow(mid, base, r++, "Week Tokens:", lblTokensWeek);
        addRow(mid, base, r++, "Month Tokens:", lblTokensMonth);
        addRow(mid, base, r , "Lifetime Tokens:", lblTokensLifetime);
        return mid;
    }

    private static void addRow(JPanel panel, GridBagConstraints base, int row, String name, JComponent value) {
        GridBagConstraints n = new GridBagConstraints();
        n.insets = base.insets;
        n.anchor = GridBagConstraints.WEST;
        n.gridx = 0;
        n.gridy = row;
        n.weightx = 0;
        n.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(name, SwingConstants.LEFT), n);
        GridBagConstraints v = new GridBagConstraints();
        v.insets = base.insets;
        v.anchor = GridBagConstraints.WEST;
        v.gridx = 1;
        v.gridy = row;
        v.weightx = 1.0;
        v.fill = GridBagConstraints.HORIZONTAL;
        panel.add(value, v);
    }

    public void stopTimers() {
        if (runtimeTimer != null) runtimeTimer.stop();
        if (dbTimer != null) dbTimer.stop();
    }

    private static String formatUptime(long millis) {
        if (millis <= 0) return "stopped";
        long s = millis / 1000L;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d");
        if (h > 0 || d > 0) sb.append(h).append("h");
        if (m > 0 || h > 0 || d > 0) sb.append(m).append("m");
        sb.append(s).append("s");
        return sb.toString();
    }

    public void refreshRuntime() {
        RuntimeStats.Snapshot snap = service.getStats().snapshot();
        long up = (snap.startMillis == 0) ? 0 : (snap.nowMillis - snap.startMillis);
        lblUptime.setText(formatUptime(up));
        lblUnique.setText(String.valueOf(snap.uniqueConnections));
        lblTotal.setText(String.valueOf(snap.totalConnections));
        lblUpdate.setText(String.valueOf(snap.getUpdateCalls));
        lblAuth.setText(String.valueOf(snap.authUserCalls));
        lblXkec.setText(String.valueOf(snap.getXKECCalls));
        lblXosc.setText(String.valueOf(snap.getXOSCCalls));
        lblPres.setText(String.valueOf(snap.getPresenceCalls));
        lblToken.setText(String.valueOf(snap.getTokenCalls));
        lblEngine.setText(String.valueOf(snap.getEngineCalls));
        lblNoKV.setText(String.valueOf(snap.getNoKVCalls));
    }

    private void refreshDatabase() {
        if (dbRefreshRunning) return;
        dbRefreshRunning = true;

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int totalClients = MySQL.CountClients();
                int lifetimeClients = MySQL.CountLifetimeClients();
                int bannedClients = MySQL.CountBannedClients();
                int totalTokens = MySQL.CountTokens();
                int dayTokens = MySQL.CountTokensExact(1);
                int weekTokens = MySQL.CountTokensExact(7);
                int monthTokens = MySQL.CountTokensExact(30);
                int lifetimeTokens = MySQL.CountTokensLifetime();
                return new int[] {
                        totalClients, lifetimeClients,
                        bannedClients, totalTokens,
                        dayTokens, weekTokens,
                        monthTokens, lifetimeTokens
                };
            }
            @Override protected void done() {
                try {
                    int[] v = get();
                    lblRegistered.setText(String.valueOf(v[0]));
                    lblLifetimeClients.setText(String.valueOf(v[1]));
                    lblBannedClients.setText(String.valueOf(v[2]));
                    lblTokensTotal.setText(String.valueOf(v[3]));
                    lblTokensDay.setText(String.valueOf(v[4]));
                    lblTokensWeek.setText(String.valueOf(v[5]));
                    lblTokensMonth.setText(String.valueOf(v[6]));
                    lblTokensLifetime.setText(String.valueOf(v[7]));
                } catch (Exception ignored) {
                } finally {
                    dbRefreshRunning = false;
                }
            }
        }.execute();
    }
}
