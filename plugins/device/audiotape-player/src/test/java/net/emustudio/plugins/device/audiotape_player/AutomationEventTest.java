/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AutomationEventTest {

    @Test
    public void testConstructorWithTypeAndParameter() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/path/to/file.tap");
        assertEquals(AutomationEvent.Type.LOAD_TAPE, event.getType());
        assertEquals("/path/to/file.tap", event.getParameter());
    }

    @Test
    public void testConstructorWithTypeOnly() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.PLAY);
        assertEquals(AutomationEvent.Type.PLAY, event.getType());
        assertEquals("", event.getParameter());
    }

    @Test(expected = NullPointerException.class)
    public void testNullTypeThrows() {
        new AutomationEvent(null, "param");
    }

    @Test(expected = NullPointerException.class)
    public void testNullParameterThrows() {
        new AutomationEvent(AutomationEvent.Type.PLAY, null);
    }

    // --- Description tests ---

    @Test
    public void testDescriptionLoadTapeWithPath() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/file.tap");
        assertEquals("Load tape: /file.tap", event.getDescription());
    }

    @Test
    public void testDescriptionLoadTapeWithoutPath() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "");
        assertEquals("Load tape", event.getDescription());
    }

    @Test
    public void testDescriptionDelayWithSeconds() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.DELAY, "5");
        assertEquals("Delay: 5s", event.getDescription());
    }

    @Test
    public void testDescriptionDelayWithoutSeconds() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.DELAY, "");
        assertEquals("Delay", event.getDescription());
    }

    @Test
    public void testDescriptionPlay() {
        assertEquals("Play tape", new AutomationEvent(AutomationEvent.Type.PLAY).getDescription());
    }

    @Test
    public void testDescriptionStop() {
        assertEquals("Stop tape", new AutomationEvent(AutomationEvent.Type.STOP).getDescription());
    }

    @Test
    public void testDescriptionReset() {
        assertEquals("Reset tape", new AutomationEvent(AutomationEvent.Type.RESET).getDescription());
    }

    @Test
    public void testDescriptionUnload() {
        assertEquals("Unload tape", new AutomationEvent(AutomationEvent.Type.UNLOAD).getDescription());
    }

    // --- Serialization tests ---

    @Test
    public void testSerializeLoadTape() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/path/file.tap");
        assertEquals("LOAD_TAPE:/path/file.tap", event.serialize());
    }

    @Test
    public void testSerializeDelay() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.DELAY, "10");
        assertEquals("DELAY:10", event.serialize());
    }

    @Test
    public void testSerializePlay() {
        assertEquals("PLAY:", new AutomationEvent(AutomationEvent.Type.PLAY).serialize());
    }

    @Test
    public void testSerializeStop() {
        assertEquals("STOP:", new AutomationEvent(AutomationEvent.Type.STOP).serialize());
    }

    @Test
    public void testSerializeReset() {
        assertEquals("RESET:", new AutomationEvent(AutomationEvent.Type.RESET).serialize());
    }

    @Test
    public void testSerializeUnload() {
        assertEquals("UNLOAD:", new AutomationEvent(AutomationEvent.Type.UNLOAD).serialize());
    }

    // --- Deserialization tests ---

    @Test
    public void testDeserializeLoadTape() {
        AutomationEvent event = AutomationEvent.deserialize("LOAD_TAPE:/path/file.tap");
        assertNotNull(event);
        assertEquals(AutomationEvent.Type.LOAD_TAPE, event.getType());
        assertEquals("/path/file.tap", event.getParameter());
    }

    @Test
    public void testDeserializeDelay() {
        AutomationEvent event = AutomationEvent.deserialize("DELAY:5");
        assertNotNull(event);
        assertEquals(AutomationEvent.Type.DELAY, event.getType());
        assertEquals("5", event.getParameter());
    }

    @Test
    public void testDeserializePlayEmptyParam() {
        AutomationEvent event = AutomationEvent.deserialize("PLAY:");
        assertNotNull(event);
        assertEquals(AutomationEvent.Type.PLAY, event.getType());
        assertEquals("", event.getParameter());
    }

    @Test
    public void testDeserializeNull() {
        assertNull(AutomationEvent.deserialize(null));
    }

    @Test
    public void testDeserializeEmpty() {
        assertNull(AutomationEvent.deserialize(""));
    }

    @Test
    public void testDeserializeNoColon() {
        assertNull(AutomationEvent.deserialize("PLAY"));
    }

    @Test
    public void testDeserializeUnknownType() {
        assertNull(AutomationEvent.deserialize("UNKNOWN_TYPE:param"));
    }

    @Test
    public void testDeserializePathWithColons() {
        // e.g. Windows path C:\file.tap → "LOAD_TAPE:C:\file.tap"
        AutomationEvent event = AutomationEvent.deserialize("LOAD_TAPE:C:\\file.tap");
        assertNotNull(event);
        assertEquals("C:\\file.tap", event.getParameter());
    }

    // --- Batch serialization/deserialization ---

    @Test
    public void testSerializeAll() {
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/file.tap"),
                new AutomationEvent(AutomationEvent.Type.DELAY, "3"),
                new AutomationEvent(AutomationEvent.Type.PLAY)
        );
        List<String> serialized = AutomationEvent.serializeAll(events);
        assertEquals(3, serialized.size());
        assertEquals("LOAD_TAPE:/file.tap", serialized.get(0));
        assertEquals("DELAY:3", serialized.get(1));
        assertEquals("PLAY:", serialized.get(2));
    }

    @Test
    public void testDeserializeAll() {
        List<String> strings = Arrays.asList("LOAD_TAPE:/file.tap", "DELAY:3", "PLAY:");
        List<AutomationEvent> events = AutomationEvent.deserializeAll(strings);
        assertEquals(3, events.size());
        assertEquals(AutomationEvent.Type.LOAD_TAPE, events.get(0).getType());
        assertEquals(AutomationEvent.Type.DELAY, events.get(1).getType());
        assertEquals(AutomationEvent.Type.PLAY, events.get(2).getType());
    }

    @Test
    public void testDeserializeAllSkipsMalformed() {
        List<String> strings = Arrays.asList("PLAY:", "INVALID", "STOP:");
        List<AutomationEvent> events = AutomationEvent.deserializeAll(strings);
        assertEquals(2, events.size());
        assertEquals(AutomationEvent.Type.PLAY, events.get(0).getType());
        assertEquals(AutomationEvent.Type.STOP, events.get(1).getType());
    }

    @Test
    public void testDeserializeAllNull() {
        List<AutomationEvent> events = AutomationEvent.deserializeAll(null);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testDeserializeAllEmpty() {
        List<AutomationEvent> events = AutomationEvent.deserializeAll(Collections.emptyList());
        assertTrue(events.isEmpty());
    }

    // --- Roundtrip ---

    @Test
    public void testSerializeDeserializeRoundtrip() {
        List<AutomationEvent> original = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/path/to/tape.tzx"),
                new AutomationEvent(AutomationEvent.Type.DELAY, "2"),
                new AutomationEvent(AutomationEvent.Type.PLAY),
                new AutomationEvent(AutomationEvent.Type.STOP),
                new AutomationEvent(AutomationEvent.Type.RESET),
                new AutomationEvent(AutomationEvent.Type.UNLOAD)
        );
        List<String> serialized = AutomationEvent.serializeAll(original);
        List<AutomationEvent> deserialized = AutomationEvent.deserializeAll(serialized);

        assertEquals(original.size(), deserialized.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).getType(), deserialized.get(i).getType());
            assertEquals(original.get(i).getParameter(), deserialized.get(i).getParameter());
        }
    }

    // --- toString ---

    @Test
    public void testToStringMatchesDescription() {
        AutomationEvent event = new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "/file.tap");
        assertEquals(event.getDescription(), event.toString());
    }

    // --- Type display names ---

    @Test
    public void testAllTypesHaveDisplayNames() {
        for (AutomationEvent.Type type : AutomationEvent.Type.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }
}

