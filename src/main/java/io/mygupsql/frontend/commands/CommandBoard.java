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
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.frontend.commands;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;

import io.mygupsql.EventConsumer;
import io.mygupsql.EventProducer;
import io.mygupsql.GTk;
import io.mygupsql.backend.Conn;
import io.mygupsql.backend.SQLRequest;
import io.mygupsql.backend.Store;
import io.mygupsql.frontend.MaskingMouseListener;


public class CommandBoard extends TextPane implements EventProducer<CommandBoard.EventType>, Closeable {

    public enum EventType {
        /**
         * L.Exec, or Exec, has been clicked.
         */
        COMMAND_AVAILABLE,
        /**
         * Previous command has been cancelled.
         */
        COMMAND_CANCEL,
        /**
         * User clicked on the connection status label.
         */
        CONNECTION_STATUS_CLICKED
    }

    private static final long serialVersionUID = 1L;
    private static final Color CONNECTED_COLOR = new Color(69, 191, 84);
    private static final Font HEADER_FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 16);
    private static final Font HEADER_UNDERLINE_FONT = HEADER_FONT.deriveFont(Map.of(
            TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String STORE_FILE_NAME = "command-board.json";

    private final Store<Content> store;
    private final EventConsumer<CommandBoard, SQLRequest> eventConsumer;
    private final JComboBox<String> storeEntries;
    private final List<UndoManager> undoManagers;
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel connLabel;
    private Conn conn; // uses it when set
    private SQLRequest lastRequest;
    private Content content;

    /**
     * Constructor.
     *
     * @param eventConsumer receives the events fired as the user interacts
     */
    public CommandBoard(EventConsumer<CommandBoard, SQLRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        connLabel = new JLabel();
        connLabel.setFont(HEADER_FONT);
        connLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        connLabel.addMouseListener(new MaskingMouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                eventConsumer.onSourceEvent(CommandBoard.this, EventType.CONNECTION_STATUS_CLICKED, null);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
                connLabel.setFont(HEADER_UNDERLINE_FONT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                connLabel.setFont(HEADER_FONT);
            }
        });
        store = new Store<>(STORE_FILE_NAME, Content.class);
        store.loadEntriesFromFile();
        storeEntries = new JComboBox<>(store.entryNames());
        storeEntries.setEditable(false);
        storeEntries.setPreferredSize(new Dimension(150, 25));
        storeEntries.addActionListener(this::onStoreEntryChangeEvent);
        undoManagers = new ArrayList<>(); // une per store entry
        for (int idx =0; idx < store.size(); idx++) {
            undoManagers.add(new UndoManager() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (!"style change".equals(e.getEdit().getPresentationName())) {
                        super.undoableEditHappened(e);
                    }
                }
            });
        }
        JLabel commandBoardLabel = new JLabel("Command board:");
        commandBoardLabel.setFont(HEADER_FONT);
        commandBoardLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        commandBoardLabel.addMouseListener(new MaskingMouseListener(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    System.out.printf("2Mouse event: %d, clicks:%d%n", e.getButton(), e.getClickCount());
                } else if (e.getClickCount() == 1 && !e.isConsumed()) {
                    e.consume();
                    System.out.printf("1Mouse event: %d, clicks:%d%n", e.getButton(), e.getClickCount());
                }

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
                commandBoardLabel.setFont(HEADER_UNDERLINE_FONT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                commandBoardLabel.setFont(HEADER_FONT);
            }
        });
        JPanel buttons = GTk.createFlowPanel(
                commandBoardLabel,
                GTk.createHorizontalSpace(2),
                storeEntries,
                GTk.createHorizontalSpace(4),
                GTk.createEtchedFlowPanel(
                        GTk.createButton("", true, GTk.Icon.COMMAND_CLEAR,
                                "Clear selected board", this::onClearEvent),
                        GTk.createButton("", true, GTk.Icon.RELOAD,
                                "Reload last saved content for selected board", this::onReloadEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_SAVE,
                                "Save selected board", this::onSaveEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_ADD,
                                "Create new board", this::onCreateStoreEntryEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_REMOVE,
                                "Delete selected board", this::onDeleteStoreEntryEvent)),
                GTk.createHorizontalSpace(37),
                GTk.createEtchedFlowPanel(
                        execLineButton = GTk.createButton(
                                "L.Exec", false, GTk.Icon.EXEC_LINE,
                                "Execute entire line under caret", this::onExecLineEvent),
                        execButton = GTk.createButton(
                                "Exec", false, GTk.Icon.EXEC,
                                "Execute selected text", this::onExecEvent),
                        cancelButton = GTk.createButton(
                                "Cancel", false, GTk.Icon.EXEC_CANCEL,
                                "Cancel current execution", this::onCancelEvent)));
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(connLabel, BorderLayout.WEST);
        controlsPanel.add(buttons, BorderLayout.EAST);
        add(controlsPanel, BorderLayout.NORTH);
        refreshControls();
        storeEntries.setSelectedIndex(0);
    }

    /**
     * @return the database connection used by the command board
     */
    public Conn getConnection() {
        return conn;
    }

    /**
     * Sets the database connection used by the command board.
     *
     * @param conn the connection
     */
    public void setConnection(Conn conn) {
        this.conn = conn;
        refreshControls();
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        textPane.requestFocus();
    }

    /**
     * Replaces the content of the board, saving current content in the process.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onStoreEntryChangeEvent(ActionEvent event) {
        int idx = storeEntries.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                // save content of current board if there are changes (all boards in fact)
                onSaveEvent(event);
            }
            content = store.getEntry(idx, Content::new);
            textPane.setText(content.getContent());
            setUndoManager(undoManagers.get(idx));
        }
    }

    /**
     * Creates a new board and makes it active.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onCreateStoreEntryEvent(ActionEvent event) {
        String entryName = JOptionPane.showInputDialog(
                this,
                "Name",
                "New Command Board",
                JOptionPane.INFORMATION_MESSAGE);
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        store.addEntry(new Content(entryName), false);
        storeEntries.addItem(entryName);
        undoManagers.add(new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!"style change".equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        });
        storeEntries.setSelectedItem(entryName);
    }

    /**
     * Deletes the current board, selects default.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onDeleteStoreEntryEvent(ActionEvent event) {
        int idx = storeEntries.getSelectedIndex();
        if (idx > 0) {
            store.removeEntry(idx);
            storeEntries.removeItemAt(idx);
            undoManagers.remove(idx);
            storeEntries.setSelectedIndex(idx - 1);
        }
    }

    /**
     * Clears the content of the board.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onClearEvent(ActionEvent event) {
        textPane.setText("");
    }

    /**
     * Reloads the content of from the store.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onReloadEvent(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    /**
     * Saves the content to the store.
     *
     * @param event it is effectively ignored, so it can be null
     */
    private void onSaveEvent(ActionEvent event) {
        updateContent();
        store.asyncSaveToFile();
    }

    /**
     * If the connection is set, it fires COMMAND_AVAILABLE. The content of the
     * command is be the selected text on the board, or the full content if nothing
     * is selected.
     *
     * @param event it is effectively ignored, so it can be null
     */
    public void onExecEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    /**
     * If the connection is set, it fires COMMAND_AVAILABLE. The content of the
     * command is be the full line under the caret.
     *
     * @param event it is effectively ignored, so it can be null
     */
    public void onExecLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    /**
     * If the connection is set and open, it fires COMMAND_CANCEL.
     *
     * @param event it is effectively ignored, so it can be null
     */
    public void onCancelEvent(ActionEvent event) {
        if (conn == null || !conn.isOpen()) {
            JOptionPane.showMessageDialog(this, "Not connected");
            return;
        }
        if (lastRequest != null) {
            eventConsumer.onSourceEvent(this, EventType.COMMAND_CANCEL, lastRequest);
            lastRequest = null;
        }
    }

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd.trim() : getContent();
    }

    private void updateContent() {
        String txt = getContent();
        if (!content.getContent().equals(txt)) {
            content.setContent(txt);
        }
    }

    /**
     * Saves the content of the board to its store file.
     */
    @Override
    public void close() {
        undoManagers.clear();
        updateContent();
        store.close();
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Connection not set, assign one");
            return;
        }
        String command = commandSupplier.get();
        if (command == null || command.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Command not available, type something");
            return;
        }
        if (lastRequest != null) {
            eventConsumer.onSourceEvent(this, EventType.COMMAND_CANCEL, lastRequest);
            lastRequest = null;
        }
        lastRequest = new SQLRequest(content.getKey(), conn, command);
        eventConsumer.onSourceEvent(this, EventType.COMMAND_AVAILABLE, lastRequest);
    }

    private void refreshControls() {
        boolean isConnected = conn != null && conn.isOpen();
        String connKey = conn != null ? conn.getKey() : "None set";
        connLabel.setText(String.format("[%s]", connKey));
        connLabel.setForeground(isConnected ? CONNECTED_COLOR : Color.BLACK);
        connLabel.setIcon(isConnected ? GTk.Icon.CONN_UP.icon() : GTk.Icon.CONN_DOWN.icon());
        boolean hasText = textPane.getStyledDocument().getLength() > 0;
        execLineButton.setEnabled(hasText);
        execButton.setEnabled(hasText);
        cancelButton.setEnabled(true);
    }
}