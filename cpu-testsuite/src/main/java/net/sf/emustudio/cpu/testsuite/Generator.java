package net.sf.emustudio.cpu.testsuite;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Generator {
    public static final int RANDOM_TESTS = 25;

    public static void forAll8bitBinary(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (int j = i; j < 256; j++) {
                for (BiConsumer<Byte, Byte> runner : runners) {
                    runner.accept((byte) i, (byte) j);
                }
            }
        }
    }

    public static void forSome8bitBinary(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int k = 0; k < RANDOM_TESTS; k++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte) random.nextInt(256), (byte) random.nextInt(256));
            }
        }
    }

    public static void forAll8bitBinaryWhichEqual(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte)i, (byte)i);
            }
        }
    }

    public static void forSome8bitBinaryWhichEqual(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k, (byte)k);
            }
        }
    }

    public static void forSome16bitBinary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(0xFFFF);
                if (first < firstStartFrom) {
                    first += firstStartFrom;
                }
                runner.accept(first, random.nextInt(0xFFFF));
            }
        }
    }

    public static void forSome16bitBinaryFirstSatisfying(Predicate<Integer> predicate,
                                                         BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(0xFFFF);
                while(!predicate.test(first)) {
                    first = random.nextInt(0xFFFF);
                }
                runner.accept(first, random.nextInt(0xFFFF));
            }
        }
    }

    public static void forSome16bitBinary(BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                runner.accept(random.nextInt(0xFFFF), random.nextInt(0xFFFF));
            }
        }
    }

    public static void forSome16bitBinaryWhichEqual(BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int k = random.nextInt(0xFFFF);
                runner.accept(k, k);
            }
        }
    }

    public static void forAll8bitUnary(BiConsumer<Byte, Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                runner.accept((byte)i, (byte)0);
            }
        }
    }

    public static void forSome8bitUnary(BiConsumer<Byte, Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Byte, Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k, (byte)0);
            }
        }
    }

    public static void forSome16bitUnary(BiConsumer<Integer, Integer>... runners) {
        forSome16bitUnary(0, runners);
    }

    public static void forSome16bitUnary(int firstStartFrom, BiConsumer<Integer, Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BiConsumer<Integer, Integer> runner : runners) {
                int first = random.nextInt(0xFFFF);
                if (first < firstStartFrom) {
                    first += firstStartFrom;
                }
                runner.accept(first, 0);
            }
        }
    }

}
