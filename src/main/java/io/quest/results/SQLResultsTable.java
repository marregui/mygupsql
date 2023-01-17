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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import io.quest.GTk;
import io.quest.sql.SQLExecutionResponse;
import io.quest.sql.SQLType;
import io.quest.sql.Table;
import io.quest.InfiniteSpinner;
import io.quest.editor.Editor;


public class SQLResultsTable extends JPanel implements Closeable {
    private static final Dimension STATUS_LABEL_SIZE = new Dimension(600, 35);
    private static final Dimension NAVIGATION_LABEL_SIZE = new Dimension(300, 35);
    private static final Dimension NAVIGATION_BUTTON_SIZE = new Dimension(100, 35);
    private static final int TABLE_ROW_HEIGHT = 30;
    private static final int TABLE_HEADER_HEIGHT = 50;
    private final JTable table;
    private final JScrollPane tableScrollPanel;
    private final SQLPagedTableModel tableModel;
    private final AtomicReference<Table> results;
    private final Editor questPanel;
    private final JLabel rowRangeLabel;
    private final JLabel statsLabel;
    private final JButton prevButton;
    private final JButton nextButton;
    private final InfiniteSpinner infiniteSpinner;
    private Component currentModePanel;
    private Mode mode;

    public SQLResultsTable(int width, int height) {
        Dimension size = new Dimension(width, height);
        results = new AtomicReference<>();
        tableModel = new SQLPagedTableModel(results::get);
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setGridColor(GTk.EDITOR_KEYWORD_FOREGROUND_COLOR.darker().darker().darker());
        table.setFont(GTk.TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new SQLCellRenderer(results::get));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        GTk.setupTableCmdKeyActions(table);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(GTk.TABLE_HEADER_FONT);
        header.setForeground(GTk.QUEST_APP_BACKGROUND_COLOR);
        statsLabel = new JLabel();
        statsLabel.setFont(GTk.MENU_FONT);
        statsLabel.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        statsLabel.setForeground(Color.WHITE);
        statsLabel.setPreferredSize(STATUS_LABEL_SIZE);
        statsLabel.setHorizontalAlignment(JLabel.RIGHT);
        rowRangeLabel = new JLabel();
        rowRangeLabel.setFont(GTk.MENU_FONT);
        rowRangeLabel.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        rowRangeLabel.setForeground(Color.WHITE);
        rowRangeLabel.setPreferredSize(NAVIGATION_LABEL_SIZE);
        rowRangeLabel.setHorizontalAlignment(JLabel.RIGHT);
        prevButton = GTk.button(GTk.Icon.RESULTS_PREV, "Go to previous page", this::onPrevButton);
        prevButton.setFont(GTk.MENU_FONT);
        prevButton.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        prevButton.setForeground(Color.WHITE);
        prevButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        nextButton = GTk.button(GTk.Icon.RESULTS_NEXT, "Go to next page", this::onNextButton);
        nextButton.setFont(GTk.MENU_FONT);
        nextButton.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        nextButton.setForeground(Color.WHITE);
        nextButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        JPanel southPanel = GTk.flowPanel(statsLabel, rowRangeLabel, prevButton, nextButton);
        southPanel.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        questPanel = new Editor(false, false);
        tableScrollPanel = new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPanel.getViewport().setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        infiniteSpinner = new InfiniteSpinner();
        infiniteSpinner.setSize(size);
        changeMode(Mode.TABLE);
        setLayout(new BorderLayout());
        setPreferredSize(size);
        setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        add(currentModePanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        updateRowNavigationComponents();
    }

    public SQLPagedTableModel getTable() {
        return tableModel;
    }

    public void updateStats(String eventType, SQLExecutionResponse res) {
        if (res != null) {
            statsLabel.setText(String.format(
                    "[%s]  Exec: %5d,  Fetch: %5d,  Total: %6d (ms)",
                    eventType,
                    res.getExecMillis(),
                    res.getFetchMillis(),
                    res.getTotalMillis()));
        } else {
            statsLabel.setText("");
        }
    }

    public void onRowsAdded(SQLExecutionResponse res) {
        Table table = res.getTable();
        if (results.compareAndSet(null, table)) {
            resetTableHeader();
        } else if (table.size() > 0) {
            tableModel.fireTableDataChanged();
        }
        updateRowNavigationComponents();
        infiniteSpinner.close();
        if (table.isSingleRowSingleVarcharColumn()) {
            questPanel.displayMessage((String) table.getValueAt(0, 0));
            changeMode(Mode.MESSAGE);
        } else {
            if (table.size() == 0) {
                questPanel.displayMessage("OK.\n\nNo results for query:\n" + res.getSqlCommand());
                changeMode(Mode.MESSAGE);
            } else {
                changeMode(Mode.TABLE);
            }
        }
    }

    @Override
    public void close() {
        Table table = results.getAndSet(null);
        if (table != null) {
            table.close();
        }
        tableModel.fireTableStructureChanged();
        infiniteSpinner.close();
        updateStats(null, null);
        updateRowNavigationComponents();
        changeMode(Mode.TABLE);
    }

    public void displayMessage(String message) {
        questPanel.displayMessage(message);
        changeMode(Mode.MESSAGE);
    }

    public void displayError(Throwable error) {
        questPanel.displayError(error);
        changeMode(Mode.MESSAGE);
    }

    public void displayError(String error) {
        questPanel.displayError(error);
        changeMode(Mode.MESSAGE);
    }

    public void showInfiniteSpinner() {
        infiniteSpinner.start();
        changeMode(Mode.INFINITE);
    }

    public void onPrevButton(ActionEvent event) {
        if (prevButton.isEnabled() && tableModel.canDecrPage()) {
            tableModel.decrPage();
            updateRowNavigationComponents();
        }
    }

    public void onNextButton(ActionEvent event) {
        if (nextButton.isEnabled() && tableModel.canIncrPage()) {
            tableModel.incrPage();
            updateRowNavigationComponents();
        }
    }

    private void updateRowNavigationComponents() {
        prevButton.setEnabled(tableModel.canDecrPage());
        nextButton.setEnabled(tableModel.canIncrPage());
        int start = tableModel.getPageStartOffset();
        int end = tableModel.getPageEndOffset();
        int tableSize = tableModel.getTableSize();
        if (tableSize > 0) {
            start++;
        }
        rowRangeLabel.setText(String.format(
                "Rows %d to %d of %-10d", start, end, tableSize));
    }

    private void resetTableHeader() {
        tableModel.refreshTableStructure();
        JTableHeader header = table.getTableHeader();
        header.setForeground(Color.WHITE);
        header.setBackground(GTk.QUEST_APP_BACKGROUND_COLOR);
        header.setPreferredSize(new Dimension(0, TABLE_HEADER_HEIGHT));
        Table t = results.get();
        TableColumnModel tcm = table.getColumnModel();
        int numCols = tcm.getColumnCount();
        int tableWidth = 0;
        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            TableColumn col = tcm.getColumn(colIdx);
            int width = SQLType.resolveColWidth(t, colIdx);
            tableWidth += width;
            col.setMinWidth(width);
            col.setPreferredWidth(width);
        }
        table.setAutoResizeMode(tableWidth < getWidth() ?
                JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
        tableModel.fireTableDataChanged();
    }

    private void changeMode(Mode newMode) {
        if (mode != newMode) {
            mode = newMode;
            Component toRemove = currentModePanel;
            switch (newMode) {
                case TABLE -> currentModePanel = tableScrollPanel;
                case INFINITE -> currentModePanel = infiniteSpinner;
                case MESSAGE -> currentModePanel = questPanel;
            }
            if (toRemove != null) {
                remove(toRemove);
            }
            add(currentModePanel, BorderLayout.CENTER);
            validate();
            repaint();
        }
    }

    private enum Mode {INFINITE, TABLE, MESSAGE}
}
