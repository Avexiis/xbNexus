package server.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public final class AppLauncher {
    private AppLauncher() {}

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            MainFrame f = new MainFrame();
            f.setVisible(true);
        });
    }
}
