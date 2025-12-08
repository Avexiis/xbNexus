package server.ui;

import server.Globals;

import javax.swing.table.AbstractTableModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public final class ClientTableModel extends AbstractTableModel {
    private final String[] cols = new String[] {
            "ID", "CPUKey", "Gamertag", "TitleID", "LastOnline", "Expires", "Banned"
    };

    private final List<Globals.ClientInfo> rows = new ArrayList<>();
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public void setData(List<Globals.ClientInfo> list) {
        rows.clear();
        if (list != null) rows.addAll(list);
        fireTableDataChanged();
    }

    public Globals.ClientInfo getAt(int row) {
        if (row < 0 || row >= rows.size()) return null;
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int c) {
        return cols[c];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Globals.ClientInfo c = rows.get(rowIndex);
        switch (columnIndex) {
            case 0: return c.ID;
            case 1: return c.CPUKey;
            case 2: return c.Gamertag;
            case 3: return c.TitleID;
            case 4: return c.LastOnline == null ? "" : df.format(c.LastOnline);
            case 5: return c.Expires == null ? "" : df.format(c.Expires);
            case 6: return c.Banned;
        }
        return "";
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0 || columnIndex == 6) return Integer.class;
        return String.class;
    }
}
