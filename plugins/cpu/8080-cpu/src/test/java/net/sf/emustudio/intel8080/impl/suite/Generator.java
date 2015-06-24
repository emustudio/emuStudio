package net.sf.emustudio.intel8080.impl.suite;

import java.util.Random;
import java.util.function.Consumer;

public class Generator {
    public static final int RANDOM_TESTS = 20;

    public static void forAll8bitBinary(BinaryConsumer<Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (int j = i; j < 256; j++) {
                for (BinaryConsumer<Byte> runner : runners) {
                    runner.accept((byte) i, (byte) j);
                }
            }
        }
    }

    public static void forSome8bitBinary(BinaryConsumer<Byte>... runners) {
        Random random = new Random();
        for (int k = 0; k < RANDOM_TESTS; k++) {
            for (BinaryConsumer<Byte> runner : runners) {
                runner.accept((byte) random.nextInt(256), (byte) random.nextInt(256));
            }
        }
    }

    public static void forAll8bitBinaryWhichEqual(BinaryConsumer<Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (BinaryConsumer<Byte> runner : runners) {
                runner.accept((byte)i, (byte)i);
            }
        }
    }

    public static void forSome8bitBinaryWhichEqual(BinaryConsumer<Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BinaryConsumer<Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k, (byte)k);
            }
        }
    }

    public static void forSome16bitBinary(BinaryConsumer<Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BinaryConsumer<Integer> runner : runners) {
                runner.accept(random.nextInt(0xFFFF), random.nextInt(0xFFFF));
            }
        }
    }

    public static void forSome16bitBinaryWhichEqual(BinaryConsumer<Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (BinaryConsumer<Integer> runner : runners) {
                int k = random.nextInt(0xFFFF);
                runner.accept(k, k);
            }
        }
    }

    public static void forAll8bitUnary(Consumer<Byte>... runners) {
        for (int i = 0; i < 256; i++) {
            for (Consumer<Byte> runner : runners) {
                runner.accept((byte)i);
            }
        }
    }

    public static void forSome8bitUnary(Consumer<Byte>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (Consumer<Byte> runner : runners) {
                int k = random.nextInt(256);
                runner.accept((byte)k);
            }
        }
    }

    public static void forSome16bitUnary(Consumer<Integer>... runners) {
        Random random = new Random();
        for (int i = 0; i < RANDOM_TESTS; i++) {
            for (Consumer<Integer> runner : runners) {
                int k = random.nextInt(0xFFFF);
                runner.accept(k);
            }
        }
    }

}
