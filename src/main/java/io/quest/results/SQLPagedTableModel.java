/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2019 - 2023, Miguel Arregui a.k.a. marregui
 */

package io.quest.results;

import java.sql.Types;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.table.AbstractTableModel;

import io.quest.sql.SQLExecutor;
import io.quest.sql.SQLType;
import io.quest.sql.Table;


/**
 * Adds paging to a default {@link javax.swing.table.TableModel} wrapping a
 * {@link Table}. Column metadata are accessed through a table supplier. The
 * table is built by a {@link SQLExecutor} and thus it will be null until the
 * SQL query execution is started.
 */
public class SQLPagedTableModel extends AbstractTableModel {
    private static final int PAGE_SIZE = 1000; // number of rows

    private final Supplier<Table> tableSupplier;
    private int currentPage;
    private int maxPage;
    private int pageStartOffset;
    private int pageEndOffset;

    SQLPagedTableModel(Supplier<Table> tableSupplier) {
        this.tableSupplier = Objects.requireNonNull(tableSupplier);
    }

    boolean canIncrPage() {
        return currentPage < maxPage;
    }

    boolean canDecrPage() {
        return currentPage > 0;
    }

    void incrPage() {
        if (canIncrPage()) {
            currentPage++;
            fireTableDataChanged();
        }
    }

    void decrPage() {
        if (canDecrPage()) {
            currentPage--;
            fireTableDataChanged();
        }
    }

    void refreshTableStructure() {
        super.fireTableStructureChanged();
    }

    @Override
    public void fireTableStructureChanged() {
        super.fireTableStructureChanged();
        fireTableDataChanged();
    }

    @Override
    public void fireTableDataChanged() {
        Table table = tableSupplier.get();
        if (table != null) {
            int size = table.size();
            pageStartOffset = PAGE_SIZE * currentPage;
            pageEndOffset = pageStartOffset + Math.min(size - pageStartOffset, PAGE_SIZE);
            maxPage = (size / PAGE_SIZE) - 1;
            if (size % PAGE_SIZE > 0) {
                maxPage++;
            }
        } else {
            currentPage = 0;
            maxPage = 0;
            pageStartOffset = 0;
            pageEndOffset = 0;
        }
        super.fireTableDataChanged();
    }

    public int getPageStartOffset() {
        return pageStartOffset;
    }

    public int getPageEndOffset() {
        return pageEndOffset;
    }

    @Override
    public int getRowCount() {
        Table table = tableSupplier.get();
        return table != null ? pageEndOffset - pageStartOffset : 0;
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        Table table = tableSupplier.get();
        if (table == null) {
            return "";
        }
        int idx = pageStartOffset + rowIdx;
        if (idx < table.size()) {
            return table.getValueAt(idx, colIdx);
        }
        return null;
    }

    public int getTableSize() {
        Table table = tableSupplier.get();
        return table != null ? table.size() : 0;
    }

    @Override
    public int getColumnCount() {
        Table table = tableSupplier.get();
        return table != null ? table.getColumnCount() : 0;
    }

    @Override
    public String getColumnName(int colIdx) {
        Table table = tableSupplier.get();
        if (table == null) {
            return "";
        }
        String type = SQLType.resolveName(table.getColumnType(colIdx));
        if (!type.isEmpty()) {
            type = " [" + type + "]";
        }
        return String.format("%s%s", table.getColumnName(colIdx), type);
    }

    public int getColumnType(int colIdx) {
        Table table = tableSupplier.get();
        if (table == null) {
            return Types.VARCHAR;
        }
        return table.getColumnType(colIdx);
    }

    @Override
    public Class<?> getColumnClass(int colIdx) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIdx, int colIdx) {
        return false;
    }
}
