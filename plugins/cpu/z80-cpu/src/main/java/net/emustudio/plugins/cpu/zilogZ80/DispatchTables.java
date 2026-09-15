/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Z80 opcode dispatch tables.
 * <p>
 * The opcode &rarr; {@link EmulatorEngine} method mappings live in the
 * {@code dispatch-tables.txt} classpath resource and are bound to {@link MethodHandle}s
 * at class initialization. See ADR-0010.
 */
public class DispatchTables {
    public final static MethodHandle[] DISPATCH_TABLE = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_ED = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_CB = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_DD = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_DD_CB = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_FD = new MethodHandle[256];
    public final static MethodHandle[] DISPATCH_TABLE_FD_CB = new MethodHandle[256];

    private final static Logger LOGGER = LoggerFactory.getLogger(DispatchTables.class);
    private final static String RESOURCE = "dispatch-tables.txt";

    static {
        Map<String, MethodHandle[]> tables = new HashMap<>();
        tables.put("DISPATCH_TABLE", DISPATCH_TABLE);
        tables.put("DISPATCH_TABLE_ED", DISPATCH_TABLE_ED);
        tables.put("DISPATCH_TABLE_CB", DISPATCH_TABLE_CB);
        tables.put("DISPATCH_TABLE_DD", DISPATCH_TABLE_DD);
        tables.put("DISPATCH_TABLE_DD_CB", DISPATCH_TABLE_DD_CB);
        tables.put("DISPATCH_TABLE_FD", DISPATCH_TABLE_FD);
        tables.put("DISPATCH_TABLE_FD_CB", DISPATCH_TABLE_FD_CB);

        try {
            load(tables);
        } catch (IllegalAccessException | NoSuchMethodException | IOException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    private static void load(Map<String, MethodHandle[]> tables)
            throws IOException, NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        InputStream in = DispatchTables.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IOException("Missing dispatch table resource: " + RESOURCE);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            MethodHandle[] current = null;
            MethodType type = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                if (line.charAt(0) == '[') {
                    int end = line.indexOf(']');
                    String tableName = line.substring(1, end);
                    current = tables.get(tableName);
                    if (current == null) {
                        throw new NoSuchMethodException("Unknown dispatch table: " + tableName);
                    }
                    type = methodType(line.substring(end + 1).trim());
                } else {
                    int space = line.indexOf(' ');
                    int opcode = Integer.parseInt(line.substring(0, space), 16);
                    String method = line.substring(space + 1).trim();
                    current[opcode] = lookup.findVirtual(EmulatorEngine.class, method, type);
                }
            }
        }
    }

    private static MethodType methodType(String ret) {
        switch (ret) {
            case "retVoid":
                return MethodType.methodType(void.class);
            case "retVoidByte":
                return MethodType.methodType(void.class, byte.class);
            default:
                throw new IllegalArgumentException("Unknown return signature: " + ret);
        }
    }
}
