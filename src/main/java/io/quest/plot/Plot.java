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

package io.quest.plot;

import io.quest.EventConsumer;
import io.quest.EventProducer;
import io.quest.GTk;

import javax.swing.*;
import java.awt.*;

public class Plot extends JDialog implements EventProducer<Plot.EventType> {

    private final PlotCanvas canvas;

    public Plot(Frame owner, String title, EventConsumer<Plot, Object> eventConsumer) {
        super(owner, title);
        GTk.configureDialog(this, 0.78F, 0.66F, () -> eventConsumer.onSourceEvent(Plot.this, EventType.HIDE_REQUEST, null));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(BorderLayout.CENTER, canvas = new PlotCanvas());
    }

    public void setDataSet(Column x, Column y) {
        canvas.setDataSet(null, x, y);
    }


    public enum EventType {
        HIDE_REQUEST // Request to hide the metadata files explorer
    }
}
