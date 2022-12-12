package io.quest.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Row implements UniqueId<Long> {

    private final long uniqueId;
    private final Object[] values;
    private final AtomicReference<String> toString;

    public Row(long uniqueId, Object[] values) {
        this.uniqueId = uniqueId;
        this.values = Objects.requireNonNull(values);
        toString = new AtomicReference<>();
    }

    @Override
    public Long getUniqueId() {
        return uniqueId;
    }

    public Object getValueAt(int idx) {
        return values[idx];
    }

    public void clear() {
        Arrays.fill(values, null);
        toString.set(null);
    }


    @Override
    public int hashCode() {
        return Long.hashCode(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Row)) {
            return false;
        }
        Row that = (Row) o;
        return uniqueId == that.uniqueId;
    }

    @Override
    public String toString() {
        String str = toString.get();
        if (str == null) {
            StringBuilder sb = new StringBuilder();
            if (null != values) {
                for (Object o : values) {
                    sb.append(null != o ? o : "null").append(", ");
                }
                if (values.length > 0) {
                    sb.setLength(sb.length() - 2);
                }
            }
            str = sb.toString();
            if (!toString.compareAndSet(null, str)) {
                str = toString.get();
            }
        }
        return str;
    }
}
