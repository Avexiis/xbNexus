package server.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Locale;

public final class BotConsolePanel extends JPanel {
    private static final Color LETTER_BLUE = new Color(0, 149, 255); //letters (A-Z, a-z)
    private static final Color LIGHT_BLUE = new Color(154, 225, 255); //digits and [KEYWORD] text
    private static final Color SYMBOL_WHITE = Color.WHITE; //everything else
    private static final Set<String> KEYWORDS_UPPER = new HashSet<>(Arrays.asList(
            "ERROR",
            "INFO",
            "WARN",
            "DISCORD",
            "SERVER",
            "DISCORD-BOT",
            "JDA MAINWS-READTHREAD",
            "JDA MAINWS-WRITETHREAD"
    ));
    private final JTextPane pane = new JTextPane();
    private final StyledDocument doc = pane.getStyledDocument();
    private final ConsoleOutputStream consoleOut;
    private final PrintStream printStream;

    public BotConsolePanel() {
        super(new BorderLayout());
        setOpaque(true);
        pane.setEditable(false);
        pane.setBackground(Color.BLACK);
        pane.setCaretColor(LIGHT_BLUE);
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        pane.setForeground(LETTER_BLUE); //default
        pane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane sp = new JScrollPane(pane);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> pane.setText(""));
        JButton copy = new JButton("Copy");
        copy.addActionListener(e -> {
            pane.selectAll();
            pane.copy();
            pane.select(doc.getLength(), doc.getLength());
        });
        tb.add(clear);
        tb.add(copy);
        add(tb, BorderLayout.NORTH);
        this.consoleOut = new ConsoleOutputStream(doc);
        this.printStream = new PrintStream(consoleOut, true, StandardCharsets.UTF_8);
    }

    public OutputStream outputStream() {
        return consoleOut;
    }

    public PrintStream stream() {
        return printStream;
    }

    public void println(String s) {
        printStream.println(s);
    }

    public void log(String level, String msg) {
        printStream.println("[" + level + "]" + msg);
    }

    private enum Cat { LETTER, DIGIT, SYMBOL }

    private static Cat cat(char c) {
        if (c >= '0' && c <= '9') return Cat.DIGIT;
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return Cat.LETTER;
        return Cat.SYMBOL;
    }

    private static void insertStyled(StyledDocument doc, String s) throws BadLocationException {
        if (s == null || s.isEmpty()) return;
        SimpleAttributeSet attrLetter = new SimpleAttributeSet();
        StyleConstants.setForeground(attrLetter, LETTER_BLUE);
        SimpleAttributeSet attrDigit = new SimpleAttributeSet();
        StyleConstants.setForeground(attrDigit, LIGHT_BLUE);
        SimpleAttributeSet attrSymbol = new SimpleAttributeSet();
        StyleConstants.setForeground(attrSymbol, SYMBOL_WHITE);
        SimpleAttributeSet attrKeyword = new SimpleAttributeSet();
        StyleConstants.setForeground(attrKeyword, LIGHT_BLUE);
        int i = 0, n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '[') {
                int end = s.indexOf(']', i + 1);
                if (end > i + 1) {
                    String inside = s.substring(i + 1, end);
                    String norm = inside.toUpperCase(Locale.ROOT);
                    if (KEYWORDS_UPPER.contains(norm)) {
                        doc.insertString(doc.getLength(), "[", attrSymbol);
                        if (!inside.isEmpty()) doc.insertString(doc.getLength(), inside, attrKeyword);
                        doc.insertString(doc.getLength(), "]", attrSymbol);
                        i = end + 1;
                        continue;
                    }
                }
            }
            Cat c0 = cat(ch);
            int j = i + 1;
            while (j < n && s.charAt(j) != '[' && cat(s.charAt(j)) == c0) j++;
            String run = s.substring(i, j);
            switch (c0) {
                case LETTER:
                    doc.insertString(doc.getLength(), run, attrLetter);
                    break;
                case DIGIT:
                    doc.insertString(doc.getLength(), run, attrDigit);
                    break;
                default:
                    doc.insertString(doc.getLength(), run, attrSymbol);
                    break;
            }
            i = j;
        }
    }

    private static final class ConsoleOutputStream extends OutputStream {
        private final StyledDocument doc;
        private final StringBuilder buf = new StringBuilder();
        ConsoleOutputStream(StyledDocument doc) {
            this.doc = Objects.requireNonNull(doc, "doc");
        }
        @Override
        public void write(int b) {
            buf.append((char) (b & 0xFF));
            if (b == '\n') flushNow();
        }
        @Override
        public void write(byte[] b, int off, int len) {
            if (b == null || len <= 0) return;
            buf.append(new String(b, off, len, StandardCharsets.UTF_8));
            int idx;
            while ((idx = buf.indexOf("\n")) >= 0) {
                final String toAppend = buf.substring(0, idx + 1);
                SwingUtilities.invokeLater(() -> {
                    try {
                        insertStyled(doc, toAppend);
                    } catch (BadLocationException ignored) {
                    }
                });
                buf.delete(0, idx + 1);
            }
        }
        @Override
        public void flush() {
            flushNow();
        }
        private void flushNow() {
            if (buf.length() == 0) return;
            final String s = buf.toString();
            buf.setLength(0);
            SwingUtilities.invokeLater(() -> {
                try {
                    insertStyled(doc, s);
                } catch (BadLocationException ignored) {
                }
            });
        }
    }
}
