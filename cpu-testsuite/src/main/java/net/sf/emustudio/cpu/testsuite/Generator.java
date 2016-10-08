/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Generator {
    // Contract: Memory size is 16-bit at max
    private static final int MAX_OPERAND_SIZE = 0xFFFF;
    private static int randomTests = 25;

    public static void setRandomTestsCount(int randomTests) {
        Generator.randomTests = randomTests;
    }

    @SuppressWarnings("unused")
    public static void forAll8bitBinary(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (int j = i; j < 256; j++) {
                for (BiConsumer<Byte, Byte> runner : runners) {
                    runner.accept((byte) i, (byte) j);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome8bitBinary(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int k = 0; k < randomTests; k++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte) random.nextInt(256), (byte) random.nextInt(256));
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forAll8bitBinaryWhichEqual(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte)i, (byte)i);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome8bitBinaryWhichEqual(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k, (byte)k);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinary(int firstStartFrom, int secondStartFrom, BiConsumer<Integer, Integer>... runners) {
        if (firstStartFrom > MAX_OPERAND_SIZE) {
            throw new IllegalArgumentException("First start from must be <= " + MAX_OPERAND_SIZE);
        }
        if (secondStartFrom > MAX_OPERAND_SIZE) {
            throw new IllegalArgumentException("Second start from must be <= " + MAX_OPERAND_SIZE);
        }

        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(MAX_OPERAND_SIZE);
                if (first < firstStartFrom) {
                    first = firstStartFrom;
                }
                int second = random.nextInt(MAX_OPERAND_SIZE);
                if (second < secondStartFrom) {
                    second = secondStartFrom;
                }
                runner.accept(first, second);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        forSome16bitBinary(firstStartFrom, 0, runners);
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinaryFirstSatisfying(Predicate<Integer> predicate,
                                                         BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(MAX_OPERAND_SIZE);
                while(!predicate.test(first)) {
                    first = random.nextInt(MAX_OPERAND_SIZE);
                }
                runner.accept(first, random.nextInt(MAX_OPERAND_SIZE));
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinaryBothSatisfying(Predicate<Integer> firstP, Predicate<Integer> secondP,
                                                         BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(MAX_OPERAND_SIZE);
                while(!firstP.test(first)) {
                    first = random.nextInt(MAX_OPERAND_SIZE);
                }
                int second = random.nextInt(MAX_OPERAND_SIZE);
                while (!secondP.test(second)) {
                    second = random.nextInt(MAX_OPERAND_SIZE);
                }
                runner.accept(first, second);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinary(BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                runner.accept(random.nextInt(MAX_OPERAND_SIZE), random.nextInt(MAX_OPERAND_SIZE));
            }
        }
    }

    private static int adjustInteger(int value) {
        if (value > 100 && value < 32000) {
            return 32900;
        }
        if (value > 33000 && value < 65000) {
            return  65500;
        }
        return value;
    }

    /**
     * Select values: 0-100, 32900-33000, and 65500-65535
     */
    @SuppressWarnings("unused")
    public static void forAdjusted16bitBinary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        for (int first = firstStartFrom; first < MAX_OPERAND_SIZE; first++) {
            for (int second = 0; second < MAX_OPERAND_SIZE; second++) {
                for (BiConsumer<Integer, Integer> runner : runners) {
                    runner.accept(first, second);
                }
                second = adjustInteger(second);
            }
            first = adjustInteger(first);
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitBinaryWhichEqual(BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int k = random.nextInt(MAX_OPERAND_SIZE);
                runner.accept(k, k);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forAll8bitUnary(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte)i, (byte)0);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome8bitUnary(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k, (byte)0);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forSome16bitUnary(BiConsumer<Integer, Integer>... runners) {
        forSome16bitUnary(0, runners);
    }

    @SuppressWarnings("unused")
    public static void forSome16bitUnary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < randomTests; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(0xFFFF);
                if (first < firstStartFrom) {
                    first += firstStartFrom;
                }
                if (first > 0xFFFF) {
                    first = 0xFFFF;
                }
                runner.accept(first, 0);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void forAll16bitUnary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        if (firstStartFrom > MAX_OPERAND_SIZE) {
            throw new IllegalArgumentException("First start from must be <=" + MAX_OPERAND_SIZE);
        }

        for (int i = firstStartFrom; i < 0xffff; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                runner.accept(i, 0);
            }
        }
    }

    @SuppressWarnings("unused")
    public static <T extends Number> void forGivenOperandsAndSingleRun(T operand, BiConsumer<T, T>... runners) {
        for (BiConsumer<T,T> runner : runners) {
            runner.accept(operand, operand);
        }
    }

    @SuppressWarnings("unused")
    public static <T extends Number> void forGivenOperandsAndSingleRun(T first, T second, BiConsumer<T, T>... runners) {
        for (BiConsumer<T,T> runner : runners) {
            runner.accept(first, second);
        }
    }
}
