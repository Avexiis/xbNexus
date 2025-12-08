package server.ui;

import server.Globals;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConsolePanel extends JPanel {
    private static final Color LETTER_BLUE = new Color(0, 149, 255); //letters (A-Z, a-z)
    private static final Color DIGIT_BLUE = new Color(154, 225, 255); //digits
    private static final Color SYMBOL_WHITE = Color.WHITE;  //symbols
    private static final Color DATE_YELLOW = new Color(255, 234, 138); //timestamp, also used for True/False
    private static final Color PHRASE_GREEN = new Color(144, 238, 144); //phrases after '|'
    private static final Color LABEL_BLUE = new Color(154, 225, 255); //exact labels before colon, IPs, etc.
    private static final Color VALUE_WHITE = Color.WHITE; //full value after colon
    private static final Set<String> BRACKET_KEYWORDS = new HashSet<>(Arrays.asList(
            "INFO",
            "WARN",
            "ERROR",
            "SERVER"
    ));
    private static final List<String> EXACT_PHRASES = Arrays.asList(
            "XEX Update Check (NO MODULE FOUND)",
            "Engine Requested by Console",
            "Sending KV to Console",
            "New Console Registered",
            "Console Online",
            "Console Login",
            "XEX Update Check",
            "XKE Challenge",
            "XOS Challenge"
    );
    private static final Set<String> EXACT_LABELS = new HashSet<>(Arrays.asList(
            "Auth Key",
            "CPU Key",
            "KV Serial",
            "Title ID",
            "Engine Hash",
            "Client Hash",
            "Latest Hash",
            "Latest Size",
            "Gamertag",
            "KV Status",
            "Token",
            "Salt",
            "KV Digest",
            "Executed in"
    ));
    private static final Pattern TS_BRACKETED = Pattern.compile("^\\[(\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2} [AP]M)\\]");
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}(?:\\.\\d{1,3}){3})");
    private static final Pattern CRL_FCRT_TYPE = Pattern.compile("^Crl:\\s+(True|False),\\s+Fcrt:\\s+(True|False),\\s+Type\\s+([0-9]+)\\s+KV$");
    private static final SimpleAttributeSet ATTR_LETTER = color(LETTER_BLUE);
    private static final SimpleAttributeSet ATTR_DIGIT = color(DIGIT_BLUE);
    private static final SimpleAttributeSet ATTR_SYMBOL = color(SYMBOL_WHITE);
    private static final SimpleAttributeSet ATTR_DATE = color(DATE_YELLOW);
    private static final SimpleAttributeSet ATTR_PHRASE = color(PHRASE_GREEN);
    private static final SimpleAttributeSet ATTR_LABEL = color(LABEL_BLUE);
    private static final SimpleAttributeSet ATTR_VALUE = color(VALUE_WHITE);
    private static SimpleAttributeSet color(Color c) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        return a;
    }
    private final JTextPane area;
    private final StyledDocument doc;
    private PrintStream stdoutPrev;
    private PrintStream stderrPrev;
    private PrintStream teeOut;
    private PrintStream teeErr;
    private JFrame popout;

    public ConsolePanel() {
        super(new BorderLayout());
        setOpaque(true);
        area = new JTextPane();
        doc = area.getStyledDocument();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setForeground(LETTER_BLUE);
        area.setBackground(Color.BLACK);
        area.setCaretColor(DIGIT_BLUE);
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> area.setText(""));
        JButton copy = new JButton("Copy");
        copy.addActionListener(e -> {
            area.selectAll();
            area.copy();
            area.select(doc.getLength(), doc.getLength());
        });
        JButton pop = new JButton("Popout");
        pop.addActionListener(e -> togglePopout());
        tb.add(clear);
        tb.add(copy);
        tb.add(pop);
        add(tb, BorderLayout.NORTH);
    }

    public void attachToSystemStreams() {
        if (teeOut != null || teeErr != null) return;
        stdoutPrev = System.out;
        stderrPrev = System.err;
        ConsoleOutputStream outStream = new ConsoleOutputStream(doc);
        ConsoleOutputStream errStream = new ConsoleOutputStream(doc);
        teeOut = new PrintStream(new TeeOutputStream(stdoutPrev, outStream), true, StandardCharsets.UTF_8);
        teeErr = new PrintStream(new TeeOutputStream(stderrPrev, errStream), true, StandardCharsets.UTF_8);
        System.setOut(teeOut);
        System.setErr(teeErr);
    }

    public void detachFromSystemStreams() {
        if (teeOut == null && teeErr == null) return;
        try {
            if (stdoutPrev != null) System.setOut(stdoutPrev);
            if (stderrPrev != null) System.setErr(stderrPrev);
        } finally {
            if (teeOut != null) teeOut.close();
            if (teeErr != null) teeErr.close();
            teeOut = null;
            teeErr = null;
        }
    }

    private void togglePopout() {
        if (popout == null) {
            popout = new JFrame(Globals.STEALTH_NAME + " Console");
            popout.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            popout.setLayout(new BorderLayout());
            JTextPane dup = new JTextPane();
            dup.setEditable(false);
            dup.setFont(area.getFont());
            dup.setBackground(Color.BLACK);
            dup.setCaretColor(DIGIT_BLUE);
            dup.setForeground(LETTER_BLUE);
            dup.setDocument(area.getDocument());
            JScrollPane sp = new JScrollPane(dup);
            sp.setBorder(BorderFactory.createEmptyBorder());
            popout.add(sp, BorderLayout.CENTER);
            popout.setSize(900, 400);
            popout.setLocationRelativeTo(null);
        }
        popout.setVisible(!popout.isVisible());
    }

    private enum Cat {
        LETTER,
        DIGIT,
        SYMBOL
    }

    private static Cat cat(char c) {
        if (c >= '0' && c <= '9') return Cat.DIGIT;
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return Cat.LETTER;
        return Cat.SYMBOL;
    }

    private static void insertStyledGeneric(StyledDocument doc, String s) throws BadLocationException {
        if (s == null || s.isEmpty()) return;
        int i = 0, n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '[') {
                int end = s.indexOf(']', i + 1);
                if (end > i + 1) {
                    String inside = s.substring(i + 1, end).trim();
                    String norm = inside.toUpperCase(Locale.ROOT);
                    if (BRACKET_KEYWORDS.contains(norm)) {
                        doc.insertString(doc.getLength(), "[", ATTR_SYMBOL);
                        if (!inside.isEmpty()) doc.insertString(doc.getLength(), inside, ATTR_DIGIT);
                        doc.insertString(doc.getLength(), "]", ATTR_SYMBOL);
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
                    doc.insertString(doc.getLength(), run, ATTR_LETTER);
                    break;
                case DIGIT:
                    doc.insertString(doc.getLength(), run, ATTR_DIGIT);
                    break;
                default:
                    doc.insertString(doc.getLength(), run, ATTR_SYMBOL);
                    break;
            }
            i = j;
        }
    }

    private static int insertIfTimestamp(StyledDocument doc, String s, int pos) throws BadLocationException {
        if (pos >= s.length() || s.charAt(pos) != '[') return pos;
        Matcher m = TS_BRACKETED.matcher(s.substring(pos));
        if (!m.find()) return pos;
        doc.insertString(doc.getLength(), "[", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), m.group(1), ATTR_DATE);
        doc.insertString(doc.getLength(), "]", ATTR_SYMBOL);
        return pos + m.end();
    }

    private static int insertIfIPv4(StyledDocument doc, String s, int pos) throws BadLocationException {
        Matcher m = IPV4.matcher(s.substring(pos));
        if (!m.find()) return pos;
        doc.insertString(doc.getLength(), m.group(1), ATTR_LABEL);
        return pos + m.end();
    }

    private static int insertSpaces(StyledDocument doc, String s, int pos) throws BadLocationException {
        int i = pos;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        if (i > pos) doc.insertString(doc.getLength(), s.substring(pos, i), ATTR_SYMBOL);
        return i;
    }

    private static int insertIfChar(StyledDocument doc, String s, int pos, char ch, AttributeSet attr) throws BadLocationException {
        if (pos < s.length() && s.charAt(pos) == ch) {
            doc.insertString(doc.getLength(), String.valueOf(ch), attr);
            return pos + 1;
        }
        return pos;
    }

    private static int insertIfExactLabelLine(StyledDocument doc, String line) throws BadLocationException {
        for (String label : EXACT_LABELS) {
            if ("Executed in".equals(label)) continue;
            String prefix = label + ": ";
            if (line.startsWith(prefix)) {
                doc.insertString(doc.getLength(), label, ATTR_LABEL);
                doc.insertString(doc.getLength(), ": ", ATTR_SYMBOL);
                String val = line.substring(prefix.length());
                if (!val.isEmpty()) doc.insertString(doc.getLength(), val, ATTR_VALUE);
                return line.length();
            }
        }
        String execPrefix = "Executed in ";
        if (line.startsWith(execPrefix)) {
            doc.insertString(doc.getLength(), "Executed in", ATTR_LABEL);
            doc.insertString(doc.getLength(), " ", ATTR_SYMBOL);
            String val = line.substring(execPrefix.length());
            if (!val.isEmpty()) doc.insertString(doc.getLength(), val, ATTR_VALUE);
            return line.length();
        }
        return -1;
    }

    private static boolean insertIfCrlFcrtTypeLine(StyledDocument doc, String line) throws BadLocationException {
        Matcher m = CRL_FCRT_TYPE.matcher(line);
        if (!m.matches()) return false;
        doc.insertString(doc.getLength(), "Crl", ATTR_LABEL);
        doc.insertString(doc.getLength(), ":", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), " ", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), m.group(1), ATTR_DATE);
        doc.insertString(doc.getLength(), ",", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), " ", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), "Fcrt", ATTR_LABEL);
        doc.insertString(doc.getLength(), ":", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), " ", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), m.group(2), ATTR_DATE);
        doc.insertString(doc.getLength(), ",", ATTR_SYMBOL);
        doc.insertString(doc.getLength(), " ", ATTR_SYMBOL);
        String tail = "Type " + m.group(3) + " KV";
        doc.insertString(doc.getLength(), tail, ATTR_LABEL);
        return true;
    }

    private static int insertIfExactPhrase(StyledDocument doc, String s, int pos) throws BadLocationException {
        for (String phrase : EXACT_PHRASES) {
            int end = pos + phrase.length();
            if (end <= s.length() && s.regionMatches(false, pos, phrase, 0, phrase.length())) {
                doc.insertString(doc.getLength(), phrase, ATTR_PHRASE);
                return end;
            }
        }
        return pos;
    }

    private static void insertStyledServerLine(StyledDocument doc, String line) throws BadLocationException {
        int consumed = insertIfExactLabelLine(doc, line);
        if (consumed == line.length()) return;
        if (insertIfCrlFcrtTypeLine(doc, line)) return;
        int i = 0;
        int n = line.length();
        int next = insertIfTimestamp(doc, line, i);
        if (next > i) {
            i = next;
            i = insertSpaces(doc, line, i);
            int nextIp = insertIfIPv4(doc, line, i);
            if (nextIp > i) i = nextIp;
            i = insertSpaces(doc, line, i);
            int afterPipe = insertIfChar(doc, line, i, '|', ATTR_SYMBOL);
            if (afterPipe > i) {
                i = afterPipe;
                i = insertSpaces(doc, line, i);
                int afterPhrase = insertIfExactPhrase(doc, line, i);
                if (afterPhrase > i) {
                    i = afterPhrase;
                    if (i < n) insertStyledGeneric(doc, line.substring(i));
                    return;
                }
            }
            if (i < n) insertStyledGeneric(doc, line.substring(i));
            return;
        }
        insertStyledGeneric(doc, line);
    }

    private static void insertStyledServer(StyledDocument doc, String s) throws BadLocationException {
        if (s == null || s.isEmpty()) return;
        int start = 0;
        int idx;
        while ((idx = s.indexOf('\n', start)) >= 0) {
            String line = s.substring(start, idx);
            insertStyledServerLine(doc, line);
            doc.insertString(doc.getLength(), "\n", ATTR_SYMBOL);
            start = idx + 1;
        }
        if (start < s.length()) {
            insertStyledServerLine(doc, s.substring(start));
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
        public void write(byte[] bs, int off, int len) {
            if (bs == null || len <= 0) return;
            buf.append(new String(bs, off, len, StandardCharsets.UTF_8));
            int idx;
            while ((idx = buf.indexOf("\n")) >= 0) {
                final String toAppend = buf.substring(0, idx + 1);
                SwingUtilities.invokeLater(() -> {
                    try {
                        insertStyledServer(doc, toAppend);
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
                    insertStyledServer(doc, s);
                } catch (BadLocationException ignored) {
                }
            });
        }
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream a, b;
        TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }
        @Override
        public void write(int i) throws IOException {
            a.write(i);
            b.write(i);
        }
        @Override
        public void write(byte[] bs) throws IOException {
            a.write(bs);
            b.write(bs);
        }
        @Override
        public void write(byte[] bs, int offset, int length) throws IOException {
            a.write(bs, offset, length);
            b.write(bs, offset, length);
        }
        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }
        @Override
        public void close() throws IOException {
            try {
                a.close();
            } finally {
                b.close();
            }
        }
    }
}
