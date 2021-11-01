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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
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

import static io.mygupsql.GTk.configureMenuItem;

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
    private static final Font FIND_FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 14);
    private static final Color FIND_FONT_COLOR = new Color(58, 138, 138);
    private static final Font HEADER_UNDERLINE_FONT = HEADER_FONT.deriveFont(Map.of(
            TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String STORE_FILE_NAME = "command-board.json";
    private final EventConsumer<CommandBoard, SQLRequest> eventConsumer;
    private final JComboBox<String> commandBoardEntryNames;
    private final List<UndoManager> undoManagers; // same order as boardEntries' model
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel commandBoardLabel;
    private final JLabel connLabel;
    private JMenu storeMenu;
    private JPanel findPanel;
    private JTextField findText;
    private JTextField replaceWithText;
    private JLabel findMatchesLabel;
    private Store<Content> store;
    private Conn conn; // uses it when set
    private SQLRequest lastRequest;
    private Content content;

    public CommandBoard(EventConsumer<CommandBoard, SQLRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        undoManagers = new ArrayList<>(5);
        commandBoardEntryNames = new JComboBox<>();
        commandBoardEntryNames.setEditable(false);
        commandBoardEntryNames.setPreferredSize(new Dimension(150, 25));
        commandBoardEntryNames.addActionListener(this::onChangeBoardEvent);
        createStoreMenu();
        JPanel buttons = GTk.createFlowPanel(
                commandBoardLabel = createLabel(
                        "Command board",
                        e -> storeMenu.getPopupMenu().show(e.getComponent(), e.getX() - 30, e.getY())),
                GTk.createHorizontalSpace(2),
                commandBoardEntryNames,
                GTk.createHorizontalSpace(4),
                GTk.createEtchedFlowPanel(
                        GTk.createButton("", true, GTk.Icon.COMMAND_CLEAR,
                                "Clear selected board", this::onClearBoardEvent),
                        GTk.createButton("", true, GTk.Icon.RELOAD,
                                "Reload last saved content for selected board", this::onReloadBoardEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_SAVE,
                                "Save selected board", this::onSaveBoardEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_ADD,
                                "Create new board", this::onCreateCommandBoardEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_REMOVE,
                                "Delete selected board", this::onDeleteCommandBoardEvent)),
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
                                "Cancel current execution", this::fireCancelEvent)));
        createFindPanel();
        JPanel controlsPanel = new JPanel(new BorderLayout(0, 0));
        controlsPanel.add(
                connLabel = createLabel(e -> eventConsumer.onSourceEvent(
                        CommandBoard.this,
                        EventType.CONNECTION_STATUS_CLICKED,
                        null)), BorderLayout.WEST);
        controlsPanel.add(buttons, BorderLayout.EAST);
        controlsPanel.add(findPanel, BorderLayout.SOUTH);
        add(controlsPanel, BorderLayout.NORTH);
        refreshControls();
        loadStoreEntries(STORE_FILE_NAME);
    }

    public Conn getConnection() {
        return conn;
    }

    public void setConnection(Conn conn) {
        this.conn = conn;
        refreshControls();
    }

    public void onExecEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    public void onExecLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    public void onFindEvent(ActionEvent event) {
        boolean wasVisible = findPanel.isVisible();
        if (!wasVisible) {
            findPanel.setVisible(true);
        } else {
            int matches = highlightContent(findText.getText());
            findMatchesLabel.setText(String.format("%d %s", matches, matches == 1 ? "match" : "matches"));
        }
        findText.requestFocusInWindow();
    }

    public void onReplaceEvent(ActionEvent event) {
        boolean wasVisible = findPanel.isVisible();
        if (!wasVisible) {
            findPanel.setVisible(true);
        } else {
            replaceContent(findText.getText(), replaceWithText.getText());
        }
        replaceWithText.requestFocusInWindow();
    }

    public void fireCancelEvent(ActionEvent event) {
        if (conn == null || !conn.isOpen()) {
            JOptionPane.showMessageDialog(this, "Not connected");
            return;
        }
        if (lastRequest != null) {
            eventConsumer.onSourceEvent(this, EventType.COMMAND_CANCEL, lastRequest);
            lastRequest = null;
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        return super.requestFocusInWindow() && textPane.requestFocusInWindow();
    }

    @Override
    public void close() {
        undoManagers.clear();
        commitContent();
        store.close();
    }

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd.trim() : getContent();
    }

    private void loadStoreEntries(String fileName) {
        store = new Store<>(fileName, Content.class) {
            @Override
            public Content[] defaultStoreEntries() {
                return new Content[]{new Content("default")};
            }
        };
        store.loadEntriesFromFile();
        commandBoardLabel.setToolTipText(fileName);
        undoManagers.clear();
        for (int idx = 0; idx < store.size(); idx++) {
            undoManagers.add(new UndoManager() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (!"style change".equals(e.getEdit().getPresentationName())) {
                        super.undoableEditHappened(e);
                    }
                }
            });
        }
        commandBoardEntryNames.removeAllItems();
        for (String item : store.entryNames()) {
            commandBoardEntryNames.addItem(item);
        }
        commandBoardEntryNames.setSelectedIndex(0);
    }

    private void onCloseFindReplaceViewEvent(ActionEvent event) {
        findPanel.setVisible(false);
    }

    private void onChangeBoardEvent(ActionEvent event) {
        int idx = commandBoardEntryNames.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                // save content of current board if there are changes (all boards in fact)
                onSaveBoardEvent(event);
            }
            content = store.getEntry(idx, Content::new);
            textPane.setText(content.getContent());
            setUndoManager(undoManagers.get(idx));
        }
    }

    private void onCreateCommandBoardEvent(ActionEvent event) {
        String entryName = JOptionPane.showInputDialog(
                this,
                "Name",
                "New Command Board",
                JOptionPane.INFORMATION_MESSAGE);
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        store.addEntry(new Content(entryName), false);
        commandBoardEntryNames.addItem(entryName);
        undoManagers.add(new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!"style change".equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        });
        commandBoardEntryNames.setSelectedItem(entryName);
    }

    private void onDeleteCommandBoardEvent(ActionEvent event) {
        int idx = commandBoardEntryNames.getSelectedIndex();
        if (idx > 0) {
            store.removeEntry(idx);
            commandBoardEntryNames.removeItemAt(idx);
            undoManagers.remove(idx);
            commandBoardEntryNames.setSelectedIndex(idx - 1);
        }
    }

    private void onBackupCommandBoardsEvent(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Backing up store");
        choose.setDialogType(JFileChooser.SAVE_DIALOG);
        choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
        choose.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == choose.showSaveDialog(this)) {
            File selectedFile = choose.getSelectedFile();
            try {
                if (!selectedFile.exists()) {
                    store.saveToFile(selectedFile);
                } else {
                    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            this,
                            "Override file?",
                            "Dilemma",
                            JOptionPane.YES_NO_OPTION)) {
                        store.saveToFile(selectedFile);
                    }
                }
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Could not save file '%s': %s",
                                selectedFile.getAbsolutePath(),
                                t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void onLoadCommandBoardsFromBackupEvent(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Loading store from backup");
        choose.setDialogType(JFileChooser.OPEN_DIALOG);
        choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
        choose.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == choose.showOpenDialog(this)) {
            File selectedFile = choose.getSelectedFile();
            try {
                loadStoreEntries(selectedFile.getName());
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Could not load file '%s': %s",
                                selectedFile.getAbsolutePath(),
                                t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void onClearBoardEvent(ActionEvent event) {
        textPane.setText("");
    }

    private void onReloadBoardEvent(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    private void onSaveBoardEvent(ActionEvent event) {
        if (commitContent()) {
            store.asyncSaveToFile();
        }
    }

    private boolean commitContent() {
        String txt = getContent();
        if (!content.getContent().equals(txt)) {
            content.setContent(txt);
            return true;
        }
        return false;
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        if (conn == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Connection not set, assign one");
            return;
        }
        String command = commandSupplier.get();
        if (command == null || command.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Command not available, type something");
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

    private void createFindPanel() {
        JLabel findLabel = new JLabel("Find");
        findLabel.setFont(HEADER_FONT);
        findLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        findText = new JTextField(35);
        findText.setFont(FIND_FONT);
        findText.setForeground(FIND_FONT_COLOR);
        findText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onFindEvent(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    replaceWithText.requestFocusInWindow();
                } else {
                    super.keyReleased(e);
                }
            }
        });
        JLabel replaceWithLabel = new JLabel("replace with");
        replaceWithLabel.setFont(HEADER_FONT);
        replaceWithLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        replaceWithText = new JTextField(35);
        replaceWithText.setFont(FIND_FONT);
        replaceWithText.setForeground(FIND_FONT_COLOR);
        replaceWithText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onReplaceEvent(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    findText.requestFocusInWindow();
                } else {
                    super.keyReleased(e);
                }
            }
        });
        findMatchesLabel = new JLabel("0 matches");
        findMatchesLabel.setFont(HEADER_FONT);
        findMatchesLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        findPanel = GTk.createFlowPanel(7, 2,
                findLabel,
                findText,
                replaceWithLabel,
                replaceWithText,
                findMatchesLabel,
                GTk.createButton(
                        "Find",
                        GTk.Icon.COMMAND_FIND,
                        "Find matching text in command board",
                        this::onFindEvent),
                GTk.createButton(
                        "Replace",
                        GTk.Icon.COMMAND_REPLACE,
                        "Replace the matching text in selected area",
                        this::onReplaceEvent),
                GTk.createButton(
                        "X",
                        GTk.Icon.NO_ICON,
                        "Close find/replace view",
                        this::onCloseFindReplaceViewEvent));
        findPanel.setVisible(false);
    }

    private void createStoreMenu() {
        storeMenu = new JMenu();
        storeMenu.setFont(GTk.MENU_FONT);
        storeMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_BACKUP,
                        "Backup to file",
                        GTk.NO_KEY_EVENT,
                        this::onBackupCommandBoardsEvent));
        storeMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_LOAD,
                        "Load from backup",
                        GTk.NO_KEY_EVENT,
                        this::onLoadCommandBoardsFromBackupEvent));
    }

    private JLabel createLabel(Consumer<MouseEvent> consumer) {
        return createLabel(null, consumer);
    }

    private JLabel createLabel(String text, Consumer<MouseEvent> consumer) {
        JLabel connLabel = new JLabel();
        connLabel.setText(text);
        connLabel.setFont(HEADER_FONT);
        connLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        connLabel.addMouseListener(new LabelMouseListener(connLabel) {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                consumer.accept(e);
            }
        });
        return connLabel;
    }

    private class LabelMouseListener implements MaskingMouseListener {
        private final JLabel label;

        private LabelMouseListener(JLabel label) {
            this.label = label;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setCursor(HAND_CURSOR);
            label.setFont(HEADER_UNDERLINE_FONT);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
            label.setFont(HEADER_FONT);
        }
    }
}
