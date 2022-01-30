package net.emustudio.plugins.cpu.zilogZ80;

import org.junit.Test;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_C;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_Z;

public class TablesGenerator {
    int FLAG_S_SHIFT = 7;
    int FLAG_Z_SHIFT = 6;
    int FLAG_H_SHIFT = 4;
    int FLAG_PV_SHIFT = 2;
    int FLAG_N_SHIFT = 1;
    int FLAG_C_SHIFT = 0;

    @Test
    public void generateOverFlow() {
        // carryIns = *acc ^ a ^ b;
        // carryOut = 1 IFF (a + b > 0xFF) or,
        //   equivalently, but avoiding overflow in C: (a > 0xFF - b).
        // overflowOut = (carryIns >> 7) ^ carryOut;
        // halfCarryOut = (carryIns >> 4) & 1;
        // zeroOut = sum == 0
        // signOut = sum & 0x80

        System.out.print("    final static byte[] SZHPC_TABLE = {\n        ");

        for (int a = 0; a < 256; a++) {
            for (int b = 0; b < 256; b++) {
                int sum = (a + b) & 0xFF; // index to the array

                int carryOut = (a > 0xFF - b) ? FLAG_C : 0;
                int carryIns = sum ^ a ^ b;
                int halfCarryOut = (carryIns >> 4) & 1;
                int overflowOut = (carryIns >> 7) ^ carryOut;

                int result = carryOut | (halfCarryOut << FLAG_H_SHIFT) | (overflowOut << FLAG_PV_SHIFT) | (sum == 0 ? FLAG_Z : 0) | (sum & 0x80);
                System.out.print(result + ",");
            }
            if (((a + 1) & 0x03) == 0) {
                System.out.print("\n        ");
            }
        }
        System.out.println("};");
    }


    @Test
    public void testFlags() {
        int a = 0x65;
        int b = ((~0xE4) + 1) & 0xFF;

        int sum = (a + b) ; //& 0xFF; // index to the array

        System.out.println(Integer.toBinaryString(a));
        System.out.println(Integer.toBinaryString(b));
        System.out.println(Integer.toBinaryString(sum));

        int carryOut = (a > 0xFF - b) ? 1 : 0;
        int carryIns = sum ^ a ^ b;
        int halfCarryOut = (carryIns >> 4) & 1;
        int overflowOut = (((carryIns >> 7) ^ carryOut) != 0) ? 1 : 0;

        System.out.println("C=" + carryOut);
        System.out.println("H=" + halfCarryOut);
        System.out.println("P=" + overflowOut);
        System.out.println("S=" + (((sum & 0x80) != 0) ? 1 : 0));

        //System.out.println(intToFlags(SZHPC_TABLE[sum & 0x1FF]));
    }
}
