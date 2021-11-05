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

package io.mygupsql;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.*;

import io.mygupsql.backend.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mygupsql.frontend.commands.CommandBoard;
import io.mygupsql.frontend.conns.ConnsManager;
import io.mygupsql.frontend.results.SQLResultsTable;

import static io.mygupsql.GTk.configureMenuItem;


public final class MyGupSql {

    public static final String NAME = "mygupsql";
    public static final String VERSION = "1.0.0-SNAPSHOT";

    private static final String BANNER = "\n" + // https://patorjk.com/software/taag/#p=display&h=0&f=Ivrit&t=mygupsql
            "                                                            _ \n" +
            "  _ __ ___    _   _    __ _   _   _   _ __    ___    __ _  | |\n" +
            " | '_ ` _ \\  | | | |  / _` | | | | | | '_ \\  / __|  / _` | | |\n" +
            " | | | | | | | |_| | | (_| | | |_| | | |_) | \\__ \\ | (_| | | |\n" +
            " |_| |_| |_|  \\__, |  \\__, |  \\__,_| | .__/  |___/  \\__, | |_|\n" +
            "              |___/   |___/          |_|               |_|    ";


    private static final Logger LOGGER = LoggerFactory.getLogger(MyGupSql.class);

    private final ConnsManager conns;
    private final SQLExecutor executor;
    private final CommandBoard commands;
    private final SQLResultsTable results;
    private final JMenuItem toggleConnsWidget;
    private final JMenuItem toggleConn;

    private MyGupSql() {
        LOGGER.info(BANNER);
        LOGGER.info("{} {}", NAME, VERSION);
        JFrame frame = GTk.createFrame();
        frame.setIconImage(GTk.Icon.APPLICATION.icon().getImage());
        int width = frame.getWidth();
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        executor = new SQLExecutor(); // input/output
        conns = new ConnsManager(frame, this::dispatchEvent); // input
        commands = new CommandBoard(this::dispatchEvent); // input
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(width, dividerHeight); // output
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setTitle(String.format("%s %s [store: %s]", NAME, VERSION, conns.getStorePath()));
        toggleConnsWidget = new JMenuItem();
        toggleConn = new JMenuItem();
        frame.setJMenuBar(createMenuBar());
        Runtime.getRuntime()
                .addShutdownHook(new Thread(this::close, getClass().getSimpleName() + "-shutdown-hook"));
        executor.start();
        conns.start();
        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenu connsMenu = new JMenu("Connections");
        connsMenu.setFont(GTk.MENU_FONT);
        connsMenu.add(
                configureMenuItem(
                        toggleConnsWidget,
                        GTk.Icon.CONN_SHOW,
                        "Show connections",
                        KeyEvent.VK_T,
                        this::onToggleConnsWidgetEvent));
        connsMenu.add(
                configureMenuItem(
                        toggleConn,
                        GTk.Icon.CONN_CONNECT,
                        "Connect",
                        KeyEvent.VK_O,
                        this::onToggleConnEvent));

        JMenu commandsMenu = new JMenu("Commands");
        commandsMenu.setFont(GTk.MENU_FONT);
        commandsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.EXEC_LINE,
                        "L.Exec",
                        KeyEvent.VK_L,
                        commands::onExecLineEvent));
        commandsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.EXEC,
                        "Exec",
                        KeyEvent.VK_ENTER,
                        commands::onExecEvent));
        commandsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.EXEC_CANCEL,
                        "Whack",
                        KeyEvent.VK_W,
                        commands::fireCancelEvent));
        commandsMenu.addSeparator();
        commandsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_FIND,
                        "Find",
                        KeyEvent.VK_F,
                        commands::onFindEvent));
        commandsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_REPLACE,
                        "Replace",
                        KeyEvent.VK_R,
                        commands::onReplaceEvent));

        JMenu resultsMenu = new JMenu("Results");
        resultsMenu.setFont(GTk.MENU_FONT);
        resultsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.PREV,
                        "PREV",
                        KeyEvent.VK_B,
                        results::onPrevButtonEvent));
        resultsMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.NEXT,
                        "NEXT",
                        KeyEvent.VK_N,
                        results::onNextButtonEvent));

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        JMenu menu = new JMenu("Menu");
        menu.setFont(GTk.MENU_FONT);
        menu.add(connsMenu);
        menu.add(commandsMenu);
        menu.add(resultsMenu);
        menuBar.add(menu);
        return menuBar;
    }

    private void onToggleConnEvent(ActionEvent event) {
        Conn conn = commands.getConnection();
        conns.onConnectEvent(conn);
        toggleConn.setText(conn != null && conn.isOpen() ? "Connect" : "Disconnect");
    }

    private void onToggleConnsWidgetEvent(ActionEvent event) {
        boolean wasVisible = conns.isVisible();
        if (!wasVisible) {
            conns.setLocation(MouseInfo.getPointerInfo().getLocation());
        }
        conns.setVisible(!wasVisible);
        toggleConnsWidget.setText(wasVisible ? "Show connections" : "Hide connections");
        toggleConnsWidget.setIcon((wasVisible ? GTk.Icon.CONN_SHOW : GTk.Icon.CONN_HIDE).icon());
    }

    private void dispatchEvent(EventProducer<?> source, Enum<?> event, Object data) {
        if (source instanceof CommandBoard) {
            onCommandBoardEvent(EventProducer.eventType(event), (SQLRequest) data);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(EventProducer.eventType(event), (SQLResponse) data);
        } else if (source instanceof ConnsManager) {
            onDBConnectionManagerEvent(EventProducer.eventType(event), data);
        }
    }

    private void onCommandBoardEvent(CommandBoard.EventType event, SQLRequest req) {
        switch (event) {
            case COMMAND_AVAILABLE:
                Conn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleConnEvent(null);
                }
                results.close();
                executor.submit(req, this::dispatchEvent);
                break;

            case COMMAND_CANCEL:
                executor.cancelSubmittedRequest(req);
                break;

            case CONNECTION_STATUS_CLICKED:
                onToggleConnsWidgetEvent(null);
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLResponse res) {
        GTk.invokeLater(() -> results.updateStats(event.name(), res));
        switch (event) {
            case STARTED:
                GTk.invokeLater(results::showInfiniteSpinner);
                break;

            case RESULTS_AVAILABLE:
            case COMPLETED:
                GTk.invokeLater(() -> results.onRowsAddedEvent(res));
                break;

            case CANCELLED:
                GTk.invokeLater(results::close);
                break;

            case FAILURE:
                GTk.invokeLater(results::close, () -> results.displayError(res.getError()));
                break;
        }
    }

    private void onDBConnectionManagerEvent(ConnsManager.EventType event, Object data) {
        switch (event) {
            case CONNECTION_SELECTED:
                commands.setConnection((Conn) data);
                if (conns.isVisible()) {
                    onToggleConnsWidgetEvent(null);
                }
                break;

            case CONNECTION_ESTABLISHED:
            case CONNECTION_CLOSED:
                Conn conn = (Conn) data;
                Conn current = commands.getConnection();
                if (current != null && current.equals(conn)) {
                    commands.setConnection(conn);
                    toggleConn.setText(conn.isOpen() ? "Disconnect" : "Connect");
                }
                break;

            case CONNECTIONS_LOST:
                @SuppressWarnings("unchecked")
                Set<Conn> droppedConns = (Set<Conn>) data;
                current = commands.getConnection();
                if (current != null) {
                    for (Conn dc : droppedConns) {
                        if (current.equals(dc)) {
                            commands.setConnection(dc);
                        }
                    }
                }
                break;

            case HIDE_REQUEST:
                onToggleConnsWidgetEvent(null);
                break;
        }
    }

    private void close() {
        commands.close();
        executor.close();
        conns.close();
        results.close();
    }

    /**
     * Starts the application.
     *
     * @param args none required
     */
    public static void main(String[] args) {
        final String lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            LOGGER.warn("CrossPlatformLookAndFeel [{}] unavailable", lookAndFeel);
        }
        GTk.invokeLater(MyGupSql::new);
    }
}
