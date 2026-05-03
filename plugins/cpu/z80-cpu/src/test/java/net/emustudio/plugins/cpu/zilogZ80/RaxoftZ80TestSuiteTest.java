/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_A;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_B;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_C;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_D;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_E;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_H;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_L;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RaxoftZ80TestSuiteTest {
    private static final long PLUGIN_ID = 0L;
    private static final String TESTS_RESOURCE =
            "/net/emustudio/plugins/cpu/zilogZ80/raxoft-z80test/tests.asm";
    private static final int MEMORY_SIZE = 0x10000;

    // Extracted from upstream v1.2a TAP binaries so local CRCs use exact workspace addresses.
    private static final int DATA_ADDRESS = 0x8800;
    private static final int MEMORY_WORD_ADDRESS = DATA_ADDRESS + 0x0C;
    private static final int JUMP_ADDRESS = DATA_ADDRESS + 0x10;
    private static final int BASE_OPCODE_ADDRESS = 0x832F;
    private static final int POST_CCF_OPCODE_ADDRESS = 0x8335;

    private static final int ALT_AF_PRIME_A = 0xA9;
    private static final int ALT_AF_PRIME_F = 0xAC;
    private static final int INPUT_PORT_FE_VALUE = 0xBF;
    private static final int MAX_STEPS = 20_000;
    private static final byte[] SETUP_PREAMBLE = new byte[]{
            (byte) 0xF1, (byte) 0xC1, (byte) 0xD1, (byte) 0xE1,
            (byte) 0xDD, (byte) 0xE1, (byte) 0xFD, (byte) 0xE1,
            (byte) 0xED, (byte) 0x7B, (byte) 0x0E, (byte) 0x88
    };

    private static final int[] CRC_TABLE = createCrcTable();
    private static final List<SourceTest> TESTS = loadTests();

    private CpuImpl cpu;
    private ByteMemoryStub memory;
    private List<FakeByteDevice> devices;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new byte[MEMORY_SIZE]);

        Capture<Context8080> cpuContext = Capture.newInstance();
        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        contextPool.register(anyLong(), capture(cpuContext), same(Context8080.class));
        expectLastCall().anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = EasyMock.createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        cpu = new CpuImpl(PLUGIN_ID, applicationApi, PluginSettings.UNAVAILABLE);
        cpu.initialize();

        assertTrue(cpuContext.hasCaptured());

        devices = new ArrayList<>(256);
        for (int port = 0; port < 256; port++) {
            FakeByteDevice device = (port == 0xFE) ? new SpectrumPortFeDevice() : new FakeByteDevice();
            devices.add(device);
            cpuContext.getValue().attachDevice(port, device);
        }
    }

    @After
    public void tearDown() {
        if (cpu != null) {
            cpu.destroy();
        }
    }

    @Test
    public void testFullVariant() throws Exception {
        assertVariant(Variant.FULL);
    }

    @Test
    public void testDocumentedVariant() throws Exception {
        assertVariant(Variant.DOC);
    }

    @Test
    public void testFlagsVariant() throws Exception {
        assertVariant(Variant.FLAGS);
    }

    @Test
    public void testDocumentedFlagsVariant() throws Exception {
        assertVariant(Variant.DOC_FLAGS);
    }

    @Test
    public void testPostCcfVariant() throws Exception {
        assertVariant(Variant.CCF);
    }

    @Test
    public void testMemptrVariant() throws Exception {
        assertVariant(Variant.MEMPTR);
    }

    private void assertVariant(Variant variant) throws Exception {
        for (SourceTest test : TESTS) {
            if (test.preCheck == PreCheck.FAIL_CHECK) {
                continue;
            }

            int actual = runTest(test, variant);
            assertEquals(
                    variant.displayName + " CRC mismatch in " + test.name,
                    test.expectedCrc(variant),
                    actual
            );
        }
    }

    private int runTest(SourceTest test, Variant variant) throws Exception {
        CompiledTest compiled = test.compile(variant);
        int crc = -1;

        for (byte[] shifter : shifterStates(compiled.shifterMask)) {
            byte[] counter = Arrays.copyOf(compiled.counterMask, compiled.counterMask.length);
            while (true) {
                byte[] snapshot = executeVector(compiled, variant, counter, shifter, crc);
                if (snapshot != null) {
                    crc = updateCrc(crc, snapshot);
                }
                if (!advanceCounter(counter, compiled.counterMask)) {
                    break;
                }
            }
        }

        return crc;
    }

    private byte[] executeVector(CompiledTest compiled, Variant variant, byte[] counter,
                                 byte[] shifter, int harnessCrc) throws Exception {
        EmulatorEngine engine = cpu.getEngine();
        memory.clear();
        resetDevices();

        byte[] combined = xorVectors(compiled.baseVector, counter, shifter);
        if (shouldSkipHalt(combined)) {
            return null;
        }
        int dataOffset = variant.opcodeSize;

        for (int i = 0; i < 16; i++) {
            memory.write(DATA_ADDRESS + i, combined[dataOffset + i]);
        }

        for (int i = 0; i < variant.opcodeSize; i++) {
            memory.write(variant.opcodeAddress + i, combined[i]);
        }
        for (int i = 0; i < SETUP_PREAMBLE.length; i++) {
            memory.write(variant.setupAddress + i, SETUP_PREAMBLE[i]);
        }

        writeJumpTrampoline(variant);

        engine.reset(variant.setupAddress);
        initializeHarnessState(engine, harnessCrc);
        engine.SP = DATA_ADDRESS;

        int continueAddress = variant.opcodeAddress + variant.opcodeSize;
        int steps = 0;
        while (engine.PC != continueAddress) {
            if (steps++ >= MAX_STEPS) {
                fail("z80test did not reach continue trampoline for " + compiled.name
                        + " (" + variant.displayName + "), PC=" + Integer.toHexString(engine.PC));
            }
            engine.step();
        }

        if (variant.memptr) {
            memory.write(continueAddress, (byte) 0xCB);
            memory.write((continueAddress + 1) & 0xFFFF, (byte) 0x46);
            engine.regs[REG_H] = (DATA_ADDRESS >>> 8) & 0xFF;
            engine.regs[REG_L] = DATA_ADDRESS & 0xFF;
            engine.PC = continueAddress;
            engine.step();
        }

        return snapshotState(engine, compiled.flagMask, variant);
    }

    private static boolean shouldSkipHalt(byte[] combined) {
        int opcode0 = unsignedByte(combined[0]);
        if (opcode0 == 0x76) {
            return true;
        }

        int opcode1 = unsignedByte(combined[1]);
        return opcode1 == 0x76 && ((opcode0 & 0xDF) == 0xDD);
    }

    private void initializeHarnessState(EmulatorEngine engine, int harnessCrc) {
        engine.regs2[REG_A] = ALT_AF_PRIME_A;
        engine.regs2[REG_B] = (harnessCrc >>> 24) & 0xFF;
        engine.regs2[REG_C] = (harnessCrc >>> 16) & 0xFF;
        engine.regs2[REG_D] = (harnessCrc >>> 8) & 0xFF;
        engine.regs2[REG_E] = harnessCrc & 0xFF;
        engine.flags2 = ALT_AF_PRIME_F;
        engine.I = ALT_AF_PRIME_A;
        engine.R = ALT_AF_PRIME_A;
    }

    private void writeJumpTrampoline(Variant variant) {
        int continueAddress = variant.opcodeAddress + variant.opcodeSize;
        if (variant.postCcf) {
            memory.write(JUMP_ADDRESS, (byte) 0x3F);
            memory.write(JUMP_ADDRESS + 1, (byte) 0xC3);
            memory.write(JUMP_ADDRESS + 2, (byte) (continueAddress & 0xFF));
            memory.write(JUMP_ADDRESS + 3, (byte) ((continueAddress >>> 8) & 0xFF));
        } else {
            memory.write(JUMP_ADDRESS, (byte) 0x03);
            memory.write(JUMP_ADDRESS + 1, (byte) 0xC3);
            memory.write(JUMP_ADDRESS + 2, (byte) (continueAddress & 0xFF));
            memory.write(JUMP_ADDRESS + 3, (byte) ((continueAddress >>> 8) & 0xFF));
        }
    }

    private void resetDevices() {
        for (int port = 0; port < devices.size(); port++) {
            if (port != 0xFE) {
                devices.get(port).setValue((byte) 0);
            }
        }
    }

    private byte[] snapshotState(EmulatorEngine engine, int flagMask, Variant variant) {
        int flags = engine.flags & 0xFF;
        if (variant.maskFlags) {
            flags &= flagMask;
        }

        if (variant.onlyFlags) {
            return new byte[]{(byte) flags};
        }

        byte[] snapshot = new byte[16];
        snapshot[0] = (byte) flags;
        snapshot[1] = (byte) engine.regs[REG_A];
        snapshot[2] = (byte) engine.regs[REG_C];
        snapshot[3] = (byte) engine.regs[REG_B];
        snapshot[4] = (byte) engine.regs[REG_E];
        snapshot[5] = (byte) engine.regs[REG_D];
        snapshot[6] = (byte) engine.regs[REG_L];
        snapshot[7] = (byte) engine.regs[REG_H];
        snapshot[8] = (byte) engine.IX;
        snapshot[9] = (byte) (engine.IX >>> 8);
        snapshot[10] = (byte) engine.IY;
        snapshot[11] = (byte) (engine.IY >>> 8);
        snapshot[12] = memory.read(MEMORY_WORD_ADDRESS);
        snapshot[13] = memory.read((MEMORY_WORD_ADDRESS + 1) & 0xFFFF);
        snapshot[14] = (byte) engine.SP;
        snapshot[15] = (byte) (engine.SP >>> 8);
        return snapshot;
    }

    private static int updateCrc(int crc, byte[] data) {
        int result = crc;
        for (byte value : data) {
            result = (result >>> 8) ^ CRC_TABLE[(result ^ unsignedByte(value)) & 0xFF];
        }
        return result;
    }

    private static byte[] xorVectors(byte[] base, byte[] counter, byte[] shifter) {
        byte[] result = new byte[base.length];
        for (int i = 0; i < base.length; i++) {
            result[i] = (byte) (base[i] ^ counter[i] ^ shifter[i]);
        }
        return result;
    }

    private static boolean advanceCounter(byte[] counter, byte[] mask) {
        for (int i = 0; i < counter.length; i++) {
            int value = unsignedByte(counter[i]);
            if (value != 0) {
                counter[i] = (byte) (((value - 1) & 0xFF) & unsignedByte(mask[i]));
                return true;
            }
            counter[i] = mask[i];
        }
        return false;
    }

    private static List<byte[]> shifterStates(byte[] mask) {
        List<byte[]> states = new ArrayList<>();
        states.add(new byte[mask.length]);

        for (int index = 0; index < mask.length; index++) {
            int byteMask = unsignedByte(mask[index]);
            for (int bit = 1; bit <= 0x80; bit <<= 1) {
                if ((byteMask & bit) == 0) {
                    continue;
                }
                byte[] state = new byte[mask.length];
                state[index] = (byte) bit;
                states.add(state);
            }
        }

        return states;
    }

    private static List<SourceTest> loadTests() {
        List<SourceTest> tests = TestParser.parse(readResource(TESTS_RESOURCE));
        long failChecks = tests.stream().filter(test -> test.preCheck == PreCheck.FAIL_CHECK).count();
        if (tests.size() != 160 || failChecks != 4) {
            throw new IllegalStateException("Unexpected z80test shape: tests=" + tests.size()
                    + ", failcheck=" + failChecks);
        }
        return tests;
    }

    private static String readResource(String path) {
        try (InputStream input = RaxoftZ80TestSuiteTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read resource: " + path, e);
        }
    }

    private static int littleEndianWord(byte[] bytes, int offset) {
        return unsignedByte(bytes[offset]) | (unsignedByte(bytes[offset + 1]) << 8);
    }

    private static int unsignedByte(byte value) {
        return value & 0xFF;
    }

    private static int[] createCrcTable() {
        int[] table = new int[256];
        for (int value = 0; value < table.length; value++) {
            int crc = value;
            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xEDB88320;
                } else {
                    crc >>>= 1;
                }
            }
            table[value] = crc;
        }
        return table;
    }

    private enum Variant {
        FULL("z80full", false, false, false, false, BASE_OPCODE_ADDRESS),
        DOC("z80doc", true, false, false, false, BASE_OPCODE_ADDRESS),
        FLAGS("z80flags", false, true, false, false, BASE_OPCODE_ADDRESS),
        DOC_FLAGS("z80docflags", true, true, false, false, BASE_OPCODE_ADDRESS),
        CCF("z80ccf", false, true, true, false, POST_CCF_OPCODE_ADDRESS),
        MEMPTR("z80memptr", false, true, false, true, BASE_OPCODE_ADDRESS);

        private final String displayName;
        private final boolean maskFlags;
        private final boolean onlyFlags;
        private final boolean postCcf;
        private final boolean memptr;
        private final int opcodeAddress;
        private final int setupAddress;
        private final int opcodeSize;
        private final int tailByte;

        Variant(String displayName, boolean maskFlags, boolean onlyFlags,
                boolean postCcf, boolean memptr, int opcodeAddress) {
            this.displayName = displayName;
            this.maskFlags = maskFlags;
            this.onlyFlags = onlyFlags;
            this.postCcf = postCcf;
            this.memptr = memptr;
            this.opcodeAddress = opcodeAddress;
            this.setupAddress = opcodeAddress - SETUP_PREAMBLE.length;
            this.opcodeSize = postCcf ? 5 : 4;
            this.tailByte = postCcf ? 0x3F : 0x00;
        }
    }

    private enum PreCheck {
        NONE,
        FAIL_CHECK,
        IN_CHECK
    }

    private static final class SourceTest {
        private final String label;
        private final String name;
        private final int flagMask;
        private final SourceVector baseVector;
        private final SourceVector counterVector;
        private final SourceVector shifterVector;
        private final EnumMap<Variant, Integer> expectedCrcs;
        private final PreCheck preCheck;

        private SourceTest(String label, String name, int flagMask, SourceVector baseVector,
                           SourceVector counterVector, SourceVector shifterVector,
                           EnumMap<Variant, Integer> expectedCrcs, PreCheck preCheck) {
            this.label = Objects.requireNonNull(label);
            this.name = Objects.requireNonNull(name);
            this.flagMask = flagMask;
            this.baseVector = Objects.requireNonNull(baseVector);
            this.counterVector = Objects.requireNonNull(counterVector);
            this.shifterVector = Objects.requireNonNull(shifterVector);
            this.expectedCrcs = Objects.requireNonNull(expectedCrcs);
            this.preCheck = Objects.requireNonNull(preCheck);
        }

        private int expectedCrc(Variant variant) {
            Integer crc = expectedCrcs.get(variant);
            if (crc == null) {
                throw new IllegalStateException("Missing CRC for " + variant + " in " + name);
            }
            return crc;
        }

        private CompiledTest compile(Variant variant) {
            if (variant.postCcf) {
                int counterA = resolve(counterVector.a, variant);
                int shifterA = resolve(shifterVector.a, variant);
                int postCcfA = shifterA | (~(counterA | shifterA) & 0x28);
                return new CompiledTest(
                        name,
                        flagMask,
                        baseVector.compile(variant, true),
                        counterVector.compile(variant, false),
                        shifterVector.compile(variant, false, postCcfA)
                );
            }

            return new CompiledTest(
                    name,
                    flagMask,
                    baseVector.compile(variant, true),
                    counterVector.compile(variant, false),
                    shifterVector.compile(variant, false)
            );
        }
    }

    private static final class CompiledTest {
        private final String name;
        private final int flagMask;
        private final byte[] baseVector;
        private final byte[] counterMask;
        private final byte[] shifterMask;

        private CompiledTest(String name, int flagMask, byte[] baseVector, byte[] counterMask, byte[] shifterMask) {
            this.name = name;
            this.flagMask = flagMask;
            this.baseVector = baseVector;
            this.counterMask = counterMask;
            this.shifterMask = shifterMask;
        }
    }

    private static final class SourceVector {
        private final String[] opcodeTokens;
        private final String flags;
        private final String a;
        private final String bc;
        private final String de;
        private final String hl;
        private final String ix;
        private final String iy;
        private final String mem;
        private final String sp;

        private SourceVector(String[] opcodeTokens, Map<String, String> values) {
            this.opcodeTokens = opcodeTokens;
            this.flags = require(values, "f");
            this.a = require(values, "a");
            this.bc = require(values, "bc");
            this.de = require(values, "de");
            this.hl = require(values, "hl");
            this.ix = require(values, "ix");
            this.iy = require(values, "iy");
            this.mem = require(values, "mem");
            this.sp = require(values, "sp");
        }

        private byte[] compile(Variant variant, boolean baseVector) {
            return compile(variant, baseVector, resolve(a, variant));
        }

        private byte[] compile(Variant variant, boolean baseVector, int aValue) {
            byte[] result = new byte[variant.opcodeSize + 16];
            byte[] opcodes = compileOpcodes(variant, baseVector);
            System.arraycopy(opcodes, 0, result, 0, opcodes.length);

            int offset = variant.opcodeSize;
            result[offset] = (byte) resolve(flags, variant);
            result[offset + 1] = (byte) aValue;
            writeWord(result, offset + 2, resolve(bc, variant));
            writeWord(result, offset + 4, resolve(de, variant));
            writeWord(result, offset + 6, resolve(hl, variant));
            writeWord(result, offset + 8, resolve(ix, variant));
            writeWord(result, offset + 10, resolve(iy, variant));
            writeWord(result, offset + 12, resolve(mem, variant));
            writeWord(result, offset + 14, resolve(sp, variant));
            return result;
        }

        private byte[] compileOpcodes(Variant variant, boolean baseVector) {
            if (!variant.postCcf) {
                return new byte[]{
                        (byte) resolve(opcodeTokens[0], variant),
                        (byte) resolve(opcodeTokens[1], variant),
                        (byte) resolve(opcodeTokens[2], variant),
                        (byte) resolve(opcodeTokens[3], variant)
                };
            }

            if (!baseVector) {
                return new byte[]{
                        (byte) resolve(opcodeTokens[0], variant),
                        (byte) resolve(opcodeTokens[1], variant),
                        (byte) resolve(opcodeTokens[2], variant),
                        (byte) resolve(opcodeTokens[3], variant),
                        0
                };
            }

            int op1 = resolve(opcodeTokens[0], variant);
            int op2 = resolve(opcodeTokens[1], variant);
            int op3 = resolve(opcodeTokens[2], variant);
            int op4 = resolve(opcodeTokens[3], variant);

            if (isStop(opcodeTokens[3])) {
                return new byte[]{(byte) op1, (byte) op2, (byte) op3, (byte) variant.tailByte, 0};
            }
            if (isStop(opcodeTokens[2])) {
                return new byte[]{(byte) op1, (byte) op2, (byte) variant.tailByte, (byte) op4, 0};
            }
            if (isStop(opcodeTokens[1])) {
                return new byte[]{(byte) op1, (byte) variant.tailByte, (byte) op3, (byte) op4, 0};
            }
            return new byte[]{(byte) op1, (byte) op2, (byte) op3, (byte) op4, (byte) variant.tailByte};
        }

        private static boolean isStop(String token) {
            return "stop".equals(token);
        }

        private static String require(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null) {
                throw new IllegalStateException("Missing vector value: " + key);
            }
            return value;
        }
    }

    private static final class TestParser {
        private static final List<String> SKIPPED_SELFTEST_LABELS = List.of("crc", "counter", "shifter");

        private static List<SourceTest> parse(String source) {
            List<String> lines = source.lines().collect(Collectors.toList());
            List<SourceTest> tests = new ArrayList<>();

            for (int index = 0; index < lines.size(); index++) {
                String trimmed = stripComment(lines.get(index)).trim();
                if (!trimmed.startsWith(".") || !trimmed.contains(" flags ")) {
                    continue;
                }

                int flagsPos = trimmed.indexOf(" flags ");
                String label = trimmed.substring(1, flagsPos).trim();
                if (SKIPPED_SELFTEST_LABELS.contains(label)) {
                    continue;
                }

                int flagMask = parseFlagMask(trimmed.substring(flagsPos + " flags ".length()).trim());
                SourceVector base = parseVector(nextRelevant(lines, ++index));
                SourceVector counter = parseVector(nextRelevant(lines, ++index));
                SourceVector shifter = parseVector(nextRelevant(lines, ++index));
                EnumMap<Variant, Integer> crcMap = parseCrcs(nextRelevant(lines, ++index));
                String name = parseName(nextRelevant(lines, ++index));

                PreCheck preCheck = PreCheck.NONE;
                if (index + 1 < lines.size()) {
                    String maybeCheck = stripComment(lines.get(index + 1)).trim();
                    if (maybeCheck.startsWith("db ")) {
                        preCheck = parsePreCheck(maybeCheck);
                        index++;
                    }
                }

                tests.add(new SourceTest(label, name, flagMask, base, counter, shifter, crcMap, preCheck));
            }

            return tests;
        }

        private static String nextRelevant(List<String> lines, int start) {
            for (int index = start; index < lines.size(); index++) {
                String stripped = stripComment(lines.get(index)).trim();
                if (!stripped.isEmpty()) {
                    return stripped;
                }
            }
            throw new IllegalStateException("Unexpected end of z80test source");
        }

        private static int parseFlagMask(String line) {
            String[] parts = splitCsv(line);
            if (parts.length != 16) {
                throw new IllegalStateException("Unexpected flags line: " + line);
            }

            int mask = 0;
            int[] bits = {0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01};
            for (int pair = 0; pair < bits.length; pair++) {
                if ("1".equals(parts[pair * 2 + 1])) {
                    mask |= bits[pair];
                }
            }
            return mask;
        }

        private static SourceVector parseVector(String line) {
            if (!line.startsWith("vec ")) {
                throw new IllegalStateException("Expected vec line, got: " + line);
            }

            String[] parts = splitCsv(line.substring(4));
            if (parts.length != 22) {
                throw new IllegalStateException("Unexpected vec line: " + line);
            }

            Map<String, String> values = new HashMap<>();
            for (int index = 4; index < parts.length; index += 2) {
                values.put(parts[index], parts[index + 1]);
            }
            return new SourceVector(Arrays.copyOf(parts, 4), values);
        }

        private static EnumMap<Variant, Integer> parseCrcs(String line) {
            if (!line.startsWith("crcs ")) {
                throw new IllegalStateException("Expected crcs line, got: " + line);
            }

            String[] parts = splitCsv(line.substring(5));
            if (parts.length != 12) {
                throw new IllegalStateException("Unexpected crcs line: " + line);
            }

            Map<String, Integer> values = new HashMap<>();
            for (int index = 0; index < parts.length; index += 2) {
                values.put(parts[index], parseInt(parts[index + 1]));
            }

            EnumMap<Variant, Integer> crcs = new EnumMap<>(Variant.class);
            crcs.put(Variant.FULL, values.get("all"));
            crcs.put(Variant.DOC, values.get("doc"));
            crcs.put(Variant.FLAGS, values.get("allflags"));
            crcs.put(Variant.DOC_FLAGS, values.get("docflags"));
            crcs.put(Variant.CCF, values.get("ccf"));
            crcs.put(Variant.MEMPTR, values.get("mptr"));
            return crcs;
        }

        private static String parseName(String line) {
            if (!line.startsWith("name ")) {
                throw new IllegalStateException("Expected name line, got: " + line);
            }

            int firstQuote = line.indexOf('"');
            int lastQuote = line.lastIndexOf('"');
            if (firstQuote < 0 || lastQuote <= firstQuote) {
                throw new IllegalStateException("Unexpected name line: " + line);
            }
            return line.substring(firstQuote + 1, lastQuote);
        }

        private static PreCheck parsePreCheck(String line) {
            String value = line.substring(3).trim();
            switch (value) {
                case "failcheck":
                    return PreCheck.FAIL_CHECK;
                case "incheck":
                    return PreCheck.IN_CHECK;
                case "nocheck":
                    return PreCheck.NONE;
                default:
                    throw new IllegalStateException("Unexpected pre-check: " + line);
            }
        }

        private static String[] splitCsv(String line) {
            return Arrays.stream(line.split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
        }

        private static String stripComment(String line) {
            int comment = line.indexOf(';');
            return (comment >= 0) ? line.substring(0, comment) : line;
        }
    }

    private static int resolve(String expression, Variant variant) {
        String trimmed = expression.replace(" ", "");
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }

        int split = findAddSub(trimmed);
        if (split > 0) {
            int left = resolve(trimmed.substring(0, split), variant);
            int right = resolve(trimmed.substring(split + 1), variant);
            return (trimmed.charAt(split) == '+') ? (left + right) : (left - right);
        }

        switch (trimmed) {
            case "mem":
                return MEMORY_WORD_ADDRESS;
            case "memsp":
                return MEMORY_WORD_ADDRESS + 2;
            case "meml":
                return MEMORY_WORD_ADDRESS & 0xFF;
            case "memh":
                return (MEMORY_WORD_ADDRESS >>> 8) & 0xFF;
            case "jmp":
                return JUMP_ADDRESS;
            case "jmpl":
                return JUMP_ADDRESS & 0xFF;
            case "jmph":
                return (JUMP_ADDRESS >>> 8) & 0xFF;
            case "self":
                return variant.opcodeAddress;
            case "tail":
                return variant.tailByte;
            case "stop":
                return 0;
            default:
                return parseInt(trimmed);
        }
    }

    private static int findAddSub(String expression) {
        for (int index = 1; index < expression.length(); index++) {
            char c = expression.charAt(index);
            if (c == '+' || c == '-') {
                return index;
            }
        }
        return -1;
    }

    private static int parseInt(String value) {
        return (int) Long.decode(value).longValue();
    }

    private static void writeWord(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
    }

    private static final class SpectrumPortFeDevice extends FakeByteDevice {
        @Override
        public byte read(int portAddress) {
            return (byte) INPUT_PORT_FE_VALUE;
        }

        @Override
        public void write(int portAddress, byte value) {
            // Keep Spectrum FE reads stable like upstream suite expects.
        }
    }
}
