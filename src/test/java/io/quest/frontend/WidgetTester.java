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

package io.quest.frontend;

import static io.quest.frontend.GTk.frame;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import io.quest.frontend.editor.QuestPanel;
import io.quest.frontend.conns.ConnsManager;


public class WidgetTester {

    public static void test_InfiniteSpinnerPanel() {
        InfiniteSpinnerPanel spinner = new InfiniteSpinnerPanel();
        JButton button = new JButton("Close");
        button.addActionListener(e -> {
            if (spinner.isRunning()) {
                spinner.close();
                button.setText("Start");
            } else {
                spinner.start();
                button.setText("Close");
            }
        });
        JFrame frame = frame("InfiniteSpinnerPanel");
        frame.add(spinner, BorderLayout.CENTER);
        frame.add(button, BorderLayout.SOUTH);
        frame.setVisible(true);
        spinner.start();
    }

    public static void test_SQLConnectionManager() {
        @SuppressWarnings("resource")
        ConnsManager connMngr = new ConnsManager(null, (source, eventType, eventData) -> {
            System.out.printf("src: %s, type: %s -> %s%n", source, eventType, eventData);
        });
        connMngr.start();
        connMngr.setVisible(true);
    }

    public static void test_CommandBoard() {
        QuestPanel board = new QuestPanel((owner, event, request) -> {

        });
        JFrame frame = frame("QuestPanel");
        frame.add(board, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
//        test_InfiniteSpinnerPanel();
        test_SQLConnectionManager();
//        test_CommandBoard();
    }
}
