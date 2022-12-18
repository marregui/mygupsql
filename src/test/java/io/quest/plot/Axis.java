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

package io.quest.plot;


import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Axis {
    private static final int X_RANGE_NUMBER_OF_TICKS = 15;
    private static final int Y_RANGE_NUMBER_OF_TICKS = 10;
    private static final int X_AXIS_SIGNIFICANT_FIGURES = 3;
    private static final int Y_AXIS_SIGNIFICANT_FIGURES = 3;
    private static final int TICK_LENGTH = 10;
    private static final String X_TPT = String.format("%%.%df", X_AXIS_SIGNIFICANT_FIGURES);
    private static final String Y_TPT = String.format("%%.%df", Y_AXIS_SIGNIFICANT_FIGURES);
    private final String labelZero;
    private final String[] labels;
    private final int[] labelWidths;
    private final int[] labelHeights;
    private final int[] tickPositions;


    private Axis(String[] labels, int[] labelWidths, int[] labelHeights, int[] tickPositions, String labelZero) {
        this.labels = labels;
        this.labelWidths = labelWidths;
        this.labelHeights = labelHeights;
        this.tickPositions = tickPositions;
        this.labelZero = labelZero;
    }

    public static Axis forX(Graphics2D g2, double min, double range, double scale) {
        return getInstance(g2, min, range, scale, false, X_RANGE_NUMBER_OF_TICKS, X_TPT);
    }

    public static Axis forY(Graphics2D g2, double min, double range, double scale) {
        return getInstance(g2, min, range, scale, true, Y_RANGE_NUMBER_OF_TICKS, Y_TPT);
    }

    private static Axis getInstance(
            Graphics2D g2,
            double min,
            double range,
            double scale,
            boolean invert,
            int numTicks,
            String tpt
    ) {
        double interval = range / numTicks;
        double start = Math.ceil(min / interval) * interval - min;
        int tickNo = (int) (Math.abs(range - start) / interval + 1);
        if (tickNo > 0) {
            int[] tickPos = new int[tickNo];
            String[] labels = new String[tickNo];
            int[] labelWidths = new int[tickNo];
            int[] labelHeights = new int[tickNo];
            int sign = invert ? -1 : 1;
            double offset = 0;
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < tickNo; i++) {
                double pos = start + offset;
                tickPos[i] = sign * (int) (pos * scale);
                String label = String.format(tpt, (pos + min));
                labels[i] = label;
                Rectangle2D bounds = fm.getStringBounds(label, g2);
                labelWidths[i] = (int) bounds.getWidth();
                labelHeights[i] = (int) bounds.getHeight();
                offset += interval;
            }
            return new Axis(labels, labelWidths, labelHeights, tickPos, String.format(tpt, 0.0));
        }
        return null;
    }

    public static String formatX(double value) {
        return String.format(X_TPT, value);
    }

    public static String formatY(double value) {
        return String.format(Y_TPT, value);
    }

    public int getYPositionOfZeroLabel() {
        for (int i = 0; i < labels.length; i++) {
            if (labelZero.equals(labels[i])) {
                return tickPositions[i];
            }
        }
        return -1;
    }

    public int getSize() {
        return labels.length;
    }

    public String getLabel(int n) {
        return labels[n];
    }

    public int getLabelWidth(int n) {
        return labelWidths[n];
    }

    public int getLabelHeight(int n) {
        return labelHeights[n];
    }

    public int getTickPosition(int n) {
        return tickPositions[n];
    }

    public int getTickLength() {
        return TICK_LENGTH;
    }
}
