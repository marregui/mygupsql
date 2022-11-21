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

package io.quest.frontend.editor;

import io.quest.model.EventConsumer;
import io.quest.model.EventProducer;
import io.quest.frontend.GTk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;


public class FindReplace extends JPanel implements EventProducer<FindReplace.EventType> {

    public enum EventType {
        FIND, REPLACE
    }

    private static final long serialVersionUID = 1L;
    private static final Color FIND_FONT_COLOR = new Color(58, 138, 138);
    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
    private final EventConsumer<FindReplace, Object> eventConsumer;
    private final JTextField findText;
    private final JCheckBox findTextIsRegex;
    private final JTextField replaceWithText;
    private final JLabel findMatchesLabel;

    public FindReplace(EventConsumer<FindReplace, Object> eventConsumer) {
        this.eventConsumer = eventConsumer;
        findText = new JTextField(20) {
            @Override
            public String getText() {
                String txt = super.getText();
                if (txt != null && !findTextIsRegex.isSelected()) {
                    txt = SPECIAL_REGEX_CHARS.matcher(txt).replaceAll("\\\\$0");
                }
                return txt;
            }
        };
        setupSearchTextField(findText, this::fireFindEvent);
        findTextIsRegex = new JCheckBox("regex?", false);
        findTextIsRegex.setBackground(Color.BLACK);
        findTextIsRegex.setForeground(Color.WHITE);
        replaceWithText = new JTextField(20);
        setupSearchTextField(replaceWithText, this::fireReplaceEvent);
        findMatchesLabel = createLabel("  0 matches");
        setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 4));
        setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY.darker()));
        add(createLabel("Find"));
        add(findText);
        add(findTextIsRegex);
        add(createLabel("replace All with"));
        add(replaceWithText);
        add(GTk.horizontalSpace(4));
        add(findMatchesLabel);
        add(GTk.horizontalSpace(4));
        add(createButton(
                "X",
                GTk.Icon.NO_ICON,
                "Close find/replace view",
                75,
                this::onCloseFindReplaceView));
        setVisible(false);
    }

    public void updateMatches(int matches) {
        findMatchesLabel.setText(String.format(
                "%4d %s",
                matches,
                matches == 1 ? "match" : "matches"));
    }

    @Override
    public boolean requestFocusInWindow() {
        return super.requestFocusInWindow() && findText.requestFocusInWindow();
    }

    public String getFind() {
        return findText.getText();
    }

    public String getReplace() {
        return replaceWithText.getText();
    }

    private void fireFindEvent(ActionEvent event) {
        eventConsumer.onSourceEvent(this, EventType.FIND, null);
    }

    private void fireReplaceEvent(ActionEvent event) {
        eventConsumer.onSourceEvent(this, EventType.REPLACE, null);
    }

    private void onCloseFindReplaceView(ActionEvent event) {
        setVisible(false);
    }

    private void setupSearchTextField(JTextField field, ActionListener listener) {
        field.setFont(GTk.TABLE_HEADER_FONT);
        field.setBackground(Color.BLACK);
        field.setForeground(Color.YELLOW);

        // cmd-a, select the full content
        GTk.addCmdKeyAction(KeyEvent.VK_A, field, e -> field.selectAll());
        // cmd-c, copy to clipboard, selection or current line
        GTk.addCmdKeyAction(KeyEvent.VK_C, field, e -> {
            String selected = field.getSelectedText();
            if (selected == null) {
                selected = field.getText();
            }
            if (!selected.equals("")) {
                GTk.setClipboardContent(selected);
            }
        });
        // cmd-v, paste content of clipboard into selection or caret position
        final StringBuilder sb = new StringBuilder();
        GTk.addCmdKeyAction(KeyEvent.VK_V, field, e -> {
            try {
                String data = GTk.getClipboardContent();
                if (data != null && !data.isEmpty()) {
                    int start = field.getSelectionStart();
                    int end = field.getSelectionEnd();
                    String text = field.getText();
                    sb.setLength(0);
                    sb.append(text, 0, start);
                    sb.append(data);
                    sb.append(text, end, text.length());
                    field.setText(sb.toString());
                }
            } catch (Exception fail) {
                // do nothing
            }
        });
        // cmd-left, jump to the beginning of the line
        GTk.addCmdKeyAction(KeyEvent.VK_LEFT, field,
                e -> field.setCaretPosition(0));
        // cmd-right, jump to the end of the line
        GTk.addCmdKeyAction(KeyEvent.VK_RIGHT, field,
                e -> field.setCaretPosition(field.getText().length()));
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    listener.actionPerformed(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    replaceWithText.requestFocusInWindow();
                } else {
                    super.keyReleased(e);
                }
            }
        });
    }


    private static JButton createButton(String text, GTk.Icon icon, String tooltip, int width, ActionListener listener) {
        JButton button = GTk.button(text, icon, tooltip, listener);
        button.setFont(GTk.MENU_FONT);
        button.setBackground(Color.BLACK);
        button.setForeground(FIND_FONT_COLOR);
        button.setPreferredSize(new Dimension(width, 22));
        return button;
    }

    private static JLabel createLabel(String title) {
        JLabel label = new JLabel(title);
        label.setFont(GTk.MENU_FONT);
        label.setBackground(Color.BLACK);
        label.setForeground(FIND_FONT_COLOR);
        return label;
    }
}