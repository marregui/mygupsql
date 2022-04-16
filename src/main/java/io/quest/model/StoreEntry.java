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

package io.quest.model;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * Base type for the entries persisted by a {@link Store}.
 */
public class StoreEntry implements WithUniqueId<String>, Comparable<StoreEntry> {

    private static final Comparator<String> COMPARING = (k1, k2) -> {
        String[] k1Parts = k1.split("\\.");
        String[] k2Parts = k2.split("\\.");
        if (k1Parts.length != k2Parts.length) {
            return Integer.compare(k1Parts.length, k2Parts.length);
        } else if (2 == k1Parts.length) {
            if (Objects.equals(k1Parts[0], k2Parts[0])) {
                return k1Parts[1].compareTo(k2Parts[1]);
            }
        }
        return k1.compareTo(k2);
    };

    private volatile String name;
    private final Map<String, String> attrs;

    public StoreEntry(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
        attrs = new TreeMap<>();
    }

    /**
     * Shallow copy constructor, attributes are a reference to the attributes of 'other'.
     * <p>
     * The {@link Store} uses this constructor to recycle the objects instantiated by the
     * JSON decoder, which produces instances of StoreItem that already contain an attribute
     * map. We do not need to instantiate yet another attribute map when we can recycle the
     * instance provided by the decoder.
     *
     * @param other store entry
     */
    public StoreEntry(StoreEntry other) {
        name = other.name;
        attrs = other.attrs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Attribute getter.
     *
     * @param attrName name of the attribute
     * @return the value associated with the attribute, or null if it does not exist
     */
    public String getAttr(String attrName) {
        return attrs.get(attrName);
    }

    /**
     * Attribute getter.
     *
     * @param attr an implementor of HasKey
     * @return the value associated with the attribute, or null if it does not exist
     */
    public String getAttr(WithUniqueId<String> attr) {
        return attrs.get(attr.getUniqueId());
    }

    /**
     * Attribute setter.
     * <p>
     * null values are stored as an empty string.
     *
     * @param attr  an implementor of HasKey
     * @param value value for the attribute
     */
    public void setAttr(WithUniqueId<String> attr, String value) {
        setAttr(attr, value, "");
    }

    /**
     * Attribute setter.
     *
     * @param attr         an implementor of HasKey
     * @param value        value for the attribute
     * @param defaultValue default value when the supplied value is null or empty
     */
    public void setAttr(WithUniqueId<String> attr, String value, String defaultValue) {
        attrs.put(attr.getUniqueId(), null == value || value.isEmpty() ? defaultValue : value);
    }

    /**
     * Attribute value setter.
     *
     * @param attrName     name of the attribute
     * @param value        value for the attribute
     * @param defaultValue default value when the supplied value is null or empty
     */
    public void setAttr(String attrName, String value, String defaultValue) {
        attrs.put(attrName, value == null || value.isEmpty() ? defaultValue : value);
    }

    /**
     * Attribute value setter.
     *
     * @param attrName name of the attribute
     * @param value    value for the attribute
     */
    public void setAttr(String attrName, String value) {
        attrs.put(attrName, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (false == o instanceof StoreEntry) {
            return false;
        }
        StoreEntry that = (StoreEntry) o;
        return name.equals(that.name) && attrs.equals(that.attrs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, attrs);
    }

    @Override
    public int compareTo(StoreEntry that) {
        if (this == that) {
            return 0;
        }
        if (null == that) {
            return -1;
        }
        return COMPARING.compare(getUniqueId(), that.getUniqueId());
    }

    /**
     * Changes as attributes are changed.
     *
     * @return the entry's unique id
     */
    @Override
    public String getUniqueId() {
        return String.format("%s.%s", name, attrs);
    }

    @Override
    public String toString() {
        return getUniqueId();
    }
}
