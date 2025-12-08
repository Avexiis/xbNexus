package server.ui;

import server.Globals;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class TokenTableModel extends AbstractTableModel {
    private final String[] cols = new String[] {
            "ID", "Token", "Days", "UsedBy", "Status"
    };
    private final List<Globals.TokenInfo> rows = new ArrayList<>();

    public void setData(List<Globals.TokenInfo> list) {
        rows.clear();
        if (list != null) rows.addAll(list);
        fireTableDataChanged();
    }

    public Globals.TokenInfo getAt(int row) {
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
        Globals.TokenInfo t = rows.get(rowIndex);
        switch (columnIndex) {
            case 0: return t.ID;
            case 1: return t.Token;
            case 2: return t.Days;
            case 3: return t.UsedBy;
            case 4: return t.Status;
        }
        return "";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0 || columnIndex == 2 || columnIndex == 4) return Integer.class;
        return String.class;
    }
}
