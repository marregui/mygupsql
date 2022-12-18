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

import java.util.Objects;

public class SlidingColumnImpl implements Column {

    private final double[] points;
    private final Object lock;
    private int writePtr = -1;
    private int readPtr;

    public SlidingColumnImpl(Object lock, int size) {
        this.lock = Objects.requireNonNull(lock);
        points = new double[size];
    }

    @Override
    public int size() {
        synchronized (lock) {
            return writePtr == -1 ? 0 : readPtr < writePtr ? writePtr - readPtr : points.length;
        }
    }

    @Override
    public void append(double value) {
        synchronized (lock) {
            writePtr = (writePtr + 1) % points.length;
            points[writePtr] = value;
            if (readPtr == writePtr) {
                readPtr = (writePtr + 1) % points.length;
            }
        }
    }

    @Override
    public double get(int i) {
        synchronized (lock) {
            double value = points[readPtr];
            readPtr = (readPtr + 1) % points.length;
            return value;
        }
    }

    @Override
    public double min() {
        double min = Double.MAX_VALUE;
        synchronized (lock) {
            for (int i = 0, n = size(); i < n; i++) {
                double p = points[(readPtr + i) % points.length];
                if (p < min) {
                    min = p;
                }
            }
        }
        return min;
    }

    @Override
    public double max() {
        double max = Double.MIN_VALUE;
        synchronized (lock) {
            for (int i = readPtr, n = readPtr + size(); i < n; i++) {
                double p = points[(readPtr + i) % points.length];
                if (p > max) {
                    max = p;
                }
            }
        }
        return max;
    }
}
