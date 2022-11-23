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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.quest.frontend.editor.QuestPanel.Content;

public class StoreTest {

    private static class TStore<T extends StoreEntry> extends Store<T> {
        public TStore(String fileName, Class<? extends StoreEntry> clazz) {
            super(fileName, clazz);
        }

        @Override
        public T[] defaultStoreEntries() {
            return null;
        }
    }

    @Test
    public void test_persist_load_DBConnection() {
        String fileName = deleteIfExists("test-db-connection-persistence.json");
        try {
            ConnAttrs conn = new ConnAttrs("master-node-0");
            conn.setAttr("host", "prometheus");
            conn.setAttr("port", "5433");
            conn.setAttr("username", "root");
            conn.setAttr("password", "secret password");
            try (Store<ConnAttrs> store = new TStore<>(fileName, ConnAttrs.class)) {
                store.addEntry(conn, true);
            }

            ConnAttrs pConn;
            try (Store<ConnAttrs> store = new TStore<>(fileName, ConnAttrs.class)) {
                store.loadFromFile();
                assertThat(store.size(), is(1));
                pConn = store.entries().get(0);
            }
            assertThat(pConn.getName(), is("master-node-0"));
            assertThat(pConn.getHost(), is("prometheus"));
            assertThat(pConn.getPort(), is("5433"));
            assertThat(pConn.getUsername(), is("root"));
            assertThat(pConn.getPassword(), is("secret password"));
            assertThat(pConn.getUri(), is("jdbc:postgresql://prometheus:5433/main"));
            assertThat(pConn.getUniqueId(), is("master-node-0 root@prometheus:5433/main"));
            assertThat(conn, Matchers.is(pConn));
        } finally {
            deleteIfExists(fileName);
        }
    }

    @Test
    public void test_persist_load_Content() {
        String fileName = deleteIfExists("test-command-board-content-persistence.json");
        try {
            Content content = new Content();
            content.setContent("Audentes fortuna  iuvat");
            try (Store<Content> store = new TStore<>(fileName, Content.class)) {
                store.addEntry(content, true);
            }

            Content rcontent;
            try (Store<Content> store = new TStore<>(fileName, Content.class)) {
                store.loadFromFile();
                assertThat(store.size(), is(1));
                rcontent = store.entries().get(0);
            }
            assertThat(rcontent.getName(), is("default"));
            assertThat(rcontent.getContent(), is("Audentes fortuna  iuvat"));
            assertThat(rcontent.getUniqueId(), is(rcontent.getName()));
            assertThat(rcontent, is(content));
        } finally {
            deleteIfExists(fileName);
        }
    }

    @Test
    public void test_iterator() {
        String fileName = deleteIfExists("test-store-iterator.json");
        try {
            try (Store<StoreEntry> store = new TStore<>(fileName, StoreEntry.class)) {
                for (int i = 0; i < 10; i++) {
                    StoreEntry entry = new StoreEntry("entry_" + i);
                    entry.setAttr("id", String.valueOf(i));
                    entry.setAttr("age", "14_000");
                    store.addEntry(entry, i == 0);
                }
            }
            List<StoreEntry> entries;
            try (Store<StoreEntry> store = new TStore<>(fileName, StoreEntry.class)) {
                store.loadFromFile();
                assertThat(store.size(), is(10));
                entries = store.entries();
                int i = 0;
                for (StoreEntry entry : store) {
                    assertThat(entry.getName(), is("entry_" + i));
                    assertThat(entry.getAttr("id"), is(String.valueOf(i)));
                    assertThat(entry.getAttr("age"), is("14_000"));
                    i++;
                }
            }
            assertThat(entries, is(Collections.<StoreEntry>emptyList()));
        } finally {
            deleteIfExists(fileName);
        }
    }

    private static String deleteIfExists(String fileName) {
        if (fileName != null) {
            File file = new File(Store.ROOT_PATH, fileName);
            if (file.exists()) {
                file.delete();
            }
            assertThat(file.exists(), is(false));
        }
        return fileName;
    }
}
