package server.ui;

import server.Globals;
import server.MySQL;
import server.runtime.ServerService;
import server.bot.BotDb;
import server.bot.DiscordBotService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {
    private final ServerService service = new ServerService();
    private final ConsolePanel console = new ConsolePanel();
    private final InfoPanel infoPanel = new InfoPanel(service);
    private final BotDb botDb = new BotDb();
    private final BotConsolePanel botConsole = new BotConsolePanel();
    private final DiscordBotService botService = new DiscordBotService(botDb, botConsole, service);
    private final JLabel lblBotStatus = new JLabel("Stopped");
    private final JButton btnBotStart = new JButton("Start Bot");
    private final JButton btnBotStop = new JButton("Stop Bot");
    private final JLabel lblStatus = new JLabel("Stopped");
    private final JTextField txtPort = new JTextField(String.valueOf(Globals.STEALTH_PORT), 6);
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnApplyPort = new JButton("Apply");
    private final JCheckBox chkFreeMode = new JCheckBox("Free Mode");
    private final JCheckBox chkNoKVMode = new JCheckBox("No KV Mode");
    private final JButton btnLoadSettings = new JButton("Load");
    private final JButton btnSaveSettings = new JButton("Save");
    private final JTextField txtSearch = new JTextField(24);
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnBan = new JButton("Ban selected");
    private final JButton btnUnban = new JButton("Unban selected");
    private final ClientTableModel clientModel = new ClientTableModel();
    private final JTable tblClients = new JTable(clientModel);
    private final TokenTableModel tokenModel = new TokenTableModel();
    private final JTable tblTokens = new JTable(tokenModel);

    public MainFrame() {
        super(Globals.STEALTH_NAME + " | Stealth Server");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", buildDashboard());
        tabs.addTab("Settings", buildSettings());
        tabs.addTab("Tokens", buildTokens());
        tabs.addTab("Info", infoPanel);
        tabs.setMinimumSize(new Dimension(0, 40));
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component comp = tabs.getComponentAt(i);
            if (comp instanceof JComponent) {
                ((JComponent) comp).setMinimumSize(new Dimension(0, 40));
            }
        }
        console.attachToSystemStreams();
        console.setMinimumSize(new Dimension(0, 30));
        console.setPreferredSize(new Dimension(10, 180));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, console);
        split.setResizeWeight(0.72);
        split.setDividerSize(8);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.7));
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    infoPanel.stopTimers();
                } catch (Exception ignored) {
                }
                try {
                    console.detachFromSystemStreams();
                } catch (Exception ignored) {
                }
                if (service.isRunning()) {
                    try {
                        service.stop();
                    } catch (Exception ignored) {
                    }
                }
                if (botService.isRunning()) {
                    botService.stop();
                }
            }
        });
        refreshUiState(false);
        refreshBotUi(botService.isRunning());
        loadSettingsFromDb();
        refreshClientList(null);
        refreshTokenList();
    }

    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel header = new JPanel(new GridLayout(1, 2, 12, 0));
        JPanel serverCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        serverCard.setBorder(new TitledBorder("Server"));
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 14f));
        btnApplyPort.addActionListener(e -> {
            if (service.isRunning()) {
                JOptionPane.showMessageDialog(this, "Stop server before changing the port.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                if (port < 1 || port > 65535) throw new NumberFormatException();
                service.setPort(port);
                Globals.STEALTH_PORT = port;
                System.out.println("[SERVER] Port set to " + port);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnStart.addActionListener(e -> doStart());
        btnStop.addActionListener(e -> doStop());
        serverCard.add(new JLabel("Status:"));
        serverCard.add(lblStatus);
        serverCard.add(new JSeparator(SwingConstants.VERTICAL));
        serverCard.add(new JLabel("Port:"));
        serverCard.add(txtPort);
        serverCard.add(btnApplyPort);
        serverCard.add(new JSeparator(SwingConstants.VERTICAL));
        serverCard.add(btnStart);
        serverCard.add(btnStop);
        JPanel botCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        botCard.setBorder(new TitledBorder("Discord Bot"));
        lblBotStatus.setFont(lblBotStatus.getFont().deriveFont(Font.BOLD, 14f));
        btnBotStart.addActionListener(e -> {
            if (!botService.isRunning()) {
                botService.startAsync();
            }
            refreshBotUi(botService.isRunning());
        });
        btnBotStop.addActionListener(e -> {
            if (botService.isRunning()) {
                botService.stop();
            }
            refreshBotUi(botService.isRunning());
        });
        botCard.add(new JLabel("Status:"));
        botCard.add(lblBotStatus);
        botCard.add(new JSeparator(SwingConstants.VERTICAL));
        botCard.add(btnBotStart);
        botCard.add(btnBotStop);
        header.add(serverCard);
        header.add(botCard);
        p.add(header, BorderLayout.NORTH);
        p.add(botConsole, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSettings() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        flags.setBorder(new TitledBorder("Global settings"));
        flags.add(chkFreeMode);
        flags.add(chkNoKVMode);
        flags.add(btnLoadSettings);
        flags.add(btnSaveSettings);
        btnLoadSettings.addActionListener(e -> loadSettingsFromDb());
        btnSaveSettings.addActionListener(e -> saveSettingsToDb());
        JPanel clients = new JPanel(new BorderLayout(8, 8));
        clients.setBorder(new TitledBorder("Client management"));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Search:"));
        top.add(txtSearch);
        top.add(btnSearch);
        top.add(btnRefresh);
        top.add(btnBan);
        top.add(btnUnban);
        btnSearch.addActionListener(e -> refreshClientList(txtSearch.getText().trim()));
        btnRefresh.addActionListener(e -> refreshClientList(null));
        btnBan.addActionListener(e -> banUnbanSelected(true));
        btnUnban.addActionListener(e -> banUnbanSelected(false));
        tblClients.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblClients.setAutoCreateRowSorter(true);
        clients.add(top, BorderLayout.NORTH);
        clients.add(new JScrollPane(tblClients), BorderLayout.CENTER);
        p.add(flags, BorderLayout.NORTH);
        p.add(clients, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTokens() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setBorder(new TitledBorder("Generate token"));
        JComboBox<String> cmbDuration = new JComboBox<>(new String[] {
                "Lifetime (999 days)", "1 Day", "1 Week (7)", "1 Month (30)", "Custom"
        });
        JSpinner spCustomDays = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        spCustomDays.setEnabled(false);
        cmbDuration.addActionListener(e -> spCustomDays.setEnabled("Custom".equals(cmbDuration.getSelectedItem())));
        JButton btnGenerate = new JButton("Generate Token");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnCopy = new JButton("Copy selected");
        btnGenerate.addActionListener(e -> {
            int computedDays = 999;
            String sel = (String) cmbDuration.getSelectedItem();
            if (sel != null) {
                if (sel.startsWith("1 Day")) computedDays = 1;
                else if (sel.startsWith("1 Week")) computedDays = 7;
                else if (sel.startsWith("1 Month")) computedDays = 30;
                else if (sel.startsWith("Custom")) computedDays = ((Number) spCustomDays.getValue()).intValue();
            }
            final int days = computedDays;
            final String token = generateRandomToken(16);
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return MySQL.InsertToken(token, days);
                }
                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            System.out.println("[SERVER] New token: " + token + " (" + days + " days)");
                            JOptionPane.showMessageDialog(MainFrame.this, "Token: " + token + "\nDays: " + days, "Token created", JOptionPane.INFORMATION_MESSAGE);
                            refreshTokenList();
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.this, "Insert failed.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        btnRefresh.addActionListener(e -> refreshTokenList());
        btnCopy.addActionListener(e -> {
            int row = tblTokens.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(MainFrame.this, "Select a token first.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int mrow = tblTokens.convertRowIndexToModel(row);
            server.Globals.TokenInfo t = tokenModel.getAt(mrow);
            if (t != null && t.Token != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(t.Token), null);
                System.out.println("[SERVER] Copied token: " + t.Token);
            }
        });
        top.add(new JLabel("Duration:"));
        top.add(cmbDuration);
        top.add(new JLabel("Custom days:"));
        top.add(spCustomDays);
        top.add(btnGenerate);
        top.add(new JSeparator(SwingConstants.VERTICAL));
        top.add(btnRefresh);
        top.add(btnCopy);
        tblTokens.setAutoCreateRowSorter(true);
        tblTokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(tblTokens), BorderLayout.CENTER);
        return p;
    }

    private void refreshBotUi(boolean running) {
        lblBotStatus.setText(running ? "Running" : "Stopped");
        btnBotStart.setEnabled(!running);
        btnBotStop.setEnabled(running);
    }

    private static String generateRandomToken(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private void doStart() {
        try {
            service.start();
            refreshUiState(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to start: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            refreshUiState(false);
        }
    }

    private void doStop() {
        try {
            service.stop();
        } finally {
            refreshUiState(false);
        }
    }

    private void refreshUiState(boolean running) {
        lblStatus.setText(running ? "Running" : "Stopped");
        txtPort.setEnabled(!running);
        btnApplyPort.setEnabled(!running);
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
    }

    private void loadSettingsFromDb() {
        new SwingWorker<Globals.Settings, Void>() {
            @Override
            protected Globals.Settings doInBackground() {
                Globals.Settings s = new Globals.Settings();
                MySQL.GetSettingsData(s);
                return s;
            }
            @Override
            protected void done() {
                try {
                    Globals.Settings s = get();
                    if (s != null) {
                        chkFreeMode.setSelected(s.FreeMode == 1);
                        chkNoKVMode.setSelected(s.NoKVMode == 1);
                        System.out.println("[SERVER] Loaded settings. Free Mode: " + s.FreeMode + ", No-KV Mode: " + s.NoKVMode);
                        Globals.setting = s;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Load settings failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void saveSettingsToDb() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                Globals.setting.FreeMode = chkFreeMode.isSelected() ? 1 : 0;
                Globals.setting.NoKVMode = chkNoKVMode.isSelected() ? 1 : 0;
                return MySQL.SaveSettingsData(Globals.setting);
            }
            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        System.out.println("[SERVER] Settings saved. Free Mode: " + Globals.setting.FreeMode + ", No-KV Mode: " + Globals.setting.NoKVMode);
                        JOptionPane.showMessageDialog(MainFrame.this, "Settings saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "Failed to save settings.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Save settings failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshClientList(String query) {
        final String q = (query == null || query.isEmpty()) ? null : query;
        new SwingWorker<List<Globals.ClientInfo>, Void>() {
            @Override
            protected List<Globals.ClientInfo> doInBackground() {
                return MySQL.ListClients(q, 500, 0);
            }
            @Override
            protected void done() {
                try {
                    List<Globals.ClientInfo> list = get();
                    clientModel.setData(list);
                    System.out.println("[SERVER] Loaded " + (list == null ? 0 : list.size()) + " clients.");
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Load clients failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshTokenList() {
        new SwingWorker<List<Globals.TokenInfo>, Void>() {
            @Override
            protected List<Globals.TokenInfo> doInBackground() {
                return MySQL.ListTokens(500, 0);
            }
            @Override
            protected void done() {
                try {
                    List<Globals.TokenInfo> list = get();
                    tokenModel.setData(list);
                    System.out.println("[SERVER] Loaded " + (list == null ? 0 : list.size()) + " tokens.");
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Load tokens failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void banUnbanSelected(boolean ban) {
        int[] sel = tblClients.getSelectedRows();
        if (sel == null || sel.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more clients first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<String> cpuKeys = new ArrayList<>();
        for (int vrow : sel) {
            int mrow = tblClients.convertRowIndexToModel(vrow);
            Globals.ClientInfo info = clientModel.getAt(mrow);
            if (info != null && info.CPUKey != null && !info.CPUKey.isEmpty()) {
                cpuKeys.add(info.CPUKey);
            }
        }
        if (cpuKeys.isEmpty()) return;
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                int ok = 0;
                for (String k : cpuKeys) if (MySQL.SetClientBanned(k, ban)) ok++;
                return ok;
            }
            @Override
            protected void done() {
                try {
                    int ok = get();
                    System.out.println("[SERVER] " + (ban ? "Banned " : "Unbanned ") + ok + " client(s).");
                    refreshClientList(txtSearch.getText().trim());
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Operation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
