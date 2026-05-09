/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single automation event for the tape player.
 * Events are serialized to/from strings for storage in plugin settings.
 * <p>
 * Format: "TYPE:param" where param is optional (empty string if not needed).
 */
public class AutomationEvent {

    public enum Type {
        LOAD_TAPE("Load tape"), DELAY("Delay"), PLAY("Play tape"), STOP("Stop tape"), RESET("Reset tape"), UNLOAD("Unload tape");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Type type;
    private final String parameter; // file path for LOAD_TAPE, seconds for DELAY, empty for others

    public AutomationEvent(Type type, String parameter) {
        this.type = Objects.requireNonNull(type);
        this.parameter = Objects.requireNonNull(parameter);
    }

    public AutomationEvent(Type type) {
        this(type, "");
    }

    public Type getType() {
        return type;
    }

    public String getParameter() {
        return parameter;
    }

    /**
     * Returns human-readable description of this event.
     */
    public String getDescription() {
        switch (type) {
            case LOAD_TAPE:
                return parameter.isEmpty() ? type.getDisplayName() : type.getDisplayName() + ": " + parameter;
            case DELAY:
                return parameter.isEmpty() ? type.getDisplayName() : type.getDisplayName() + ": " + parameter + "s";
            default:
                return type.getDisplayName();
        }
    }

    /**
     * Serialize event to string for storage.
     */
    public String serialize() {
        return type.name() + ":" + parameter;
    }

    /**
     * Deserialize event from stored string.
     *
     * @return parsed event, or null if the string is malformed
     */
    public static AutomationEvent deserialize(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        int colonIndex = s.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }
        String typeName = s.substring(0, colonIndex);
        String param = s.substring(colonIndex + 1);
        try {
            Type type = Type.valueOf(typeName);
            return new AutomationEvent(type, param);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Serialize list of events for storage in plugin settings.
     */
    public static List<String> serializeAll(List<AutomationEvent> events) {
        List<String> result = new ArrayList<>(events.size());
        for (AutomationEvent event : events) {
            result.add(event.serialize());
        }
        return result;
    }

    /**
     * Deserialize list of events from plugin settings.
     */
    public static List<AutomationEvent> deserializeAll(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return Collections.emptyList();
        }
        List<AutomationEvent> result = new ArrayList<>(strings.size());
        for (String s : strings) {
            AutomationEvent event = deserialize(s);
            if (event != null) {
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}

