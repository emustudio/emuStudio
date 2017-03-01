# CPU Test Suite

CPU Test Suite is a general unit-testing framework, usable for testing emuStudio CPU plug-ins. It aims to automate as
much mechanical jobs as possible, and to allow declarative specification of tests. Some ideas were inspired from
project ["QuickCheck"](https://github.com/pholser/junit-quickcheck), like generating test cases.

# Features

- Generate test cases for instructions needed values (8-bit, 16-bit, unary, binary) 
- Using Builder pattern for declarative specification of tests
- Automatic set-up of the environment (fill memory with program, set up initial CPU flags or registers)

# Usage

If you are using Maven, the best would be to include this as test dependency in your CPU plug-in:

```
  <dependencies>
    <dependency>
      <groupId>net.sf.emustudio</groupId>
      <artifactId>cpu-testsuite</artifactId>
      <version>0.39-SNAPSHOT</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
```

# When to use CPU Test Suite

This project can be used for testing CPU emulators for emuStudio, which fulfill the following requirements:

- operating memory is a collection of linearly ordered cells
- operating memory cell type is `Short` or `Byte`
- CPU is using little endian
- CPU has a program counter register (or "instruction pointer") or similar
- Instruction operands are either `Byte` (8-bit) or `Integer` (16-bit)

# Getting started

Here are presented some code snippets, to let you imagine how CPU Test Suite works. 

NOTE: Realize that instructions in CPU always operate either on a state or with some data. The main idea of testing
      instructions is to verify the correctness of the instruction evaluation, by checking the output of the instruction,
      regardless it is stored as internal CPU state, or in a register, or in memory.

Imagine we have 8080 CPU, and we want to test instruction `SUB`. The test might look as follows:

```Java
import static net.sf.emustudio.cpu.testsuite.Generator.*;

public class CpuTest {
    
    @Test
    public void testSUB() throws Exception {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
                .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
                .keepCurrentInjectorsAfterRun();
        
        forSome8bitBinaryWhichEqual(
                test.run(0x97)
        );
        forSome8bitBinary(
                test.secondIsRegister(REG_B).run(0x90),
                test.secondIsRegister(REG_C).run(0x91),
                test.secondIsRegister(REG_D).run(0x92),
                test.secondIsRegister(REG_E).run(0x93),
                test.secondIsRegister(REG_H).run(0x94),
                test.secondIsRegister(REG_L).run(0x95),
                test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
        );
    }
}
```

At first, we need to know, if we will operate with bytes or integers (words). Therefore we create new `ByteTestBuilder`
for testing `SUB`. There exists also `IntegerTestBuilder` class for operating with 16-bit values.

Instruction `SUB` takes 1 argument - the register, e.g. `SUB B`, which substracts register `B` from register `A`.
In other words:

```
SUB B = A - B
```

Generally, instruction `SUB` will always be evaluated as `A - register`. Therefore we know, that first operand is always
register `A`:

```Java
   .firstIsRegister(REG_A)
```

NOTE: Constant `REG_A` is defined in our 8080 CPU.

That's it for preparing the environment. Now, we want to verify, that after performing the "subtract" operation,
we get result in register `A` with the correct value:

```Java
    .verifyRegister(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF))
```

We supply the computation based on the two values, which will be *generated* later. The values are accessible from
`context` object, as member values `context.first` and `context.second`. What you see above is a lambda (feature from
Java 8), taking the testing `context` object, and performing the subtract operation with given values.
 
NOTE: Here, you must be very careful; if you write the computation wrongly, the test will expect wrong results.
    
Also, the instruction is affecting flags in CPU. It is enough to specify that with the following statement:
 
```Java
    .verifyFlagsOfLastOp(new FlagsBuilderImpl().sign().zero().carry().auxCarry().parity())
```
 
Here, we are saying: verify flags of the last operation (taken from the previous line - the subtract), and we supply
the flags using `FlagsBuilderImpl` class - sign, zero, carry, auxiliary carry and parity. The class however must be
implemented manually, in order to preserve the generality of the Test Suite. Each CPU has different flags with
different semantics. But don't worry, it is not difficult.

And we're almost done with the test specification. Now, we must say that after we create a test, we want to keep
the environment we set up before (in our case setting that the first operand will be stored in register `A` - before
the operation). We do this with line:
 
```Java
    .keepCurrentInjectorsAfterRun();
```

And now, we can 'generate' tests for various random-generated combinations of operands. This is the strongest feature
of the suite, and frees us from creating manual examples of the instruction input and output data. It saves a lot of
time. We just say:

```Java
Generator.forSome8bitBinaryWhichEqual(
        test.run(0x97)
);
```

And the generator will generate some 8-bit pair of values, which equal. And we run the test for all the generated values
on a `SUB A` instruction (which has opcode `0x97`). Here, is the trick. In this statement, we test instruction `SUB A`,
which means:

```
SUB A = A - A
```

So in order to have valid test, and we have binary values from generator (we need to have both `context.first` and
`context.second`), we need to have them *equal*, because they represent the same value - in register `A`.

The final part of the test is much more obvious:

```
Generator.forSome8bitBinary(
        test.secondIsRegister(REG_B).run(0x90),
        test.secondIsRegister(REG_C).run(0x91),
        test.secondIsRegister(REG_D).run(0x92),
        test.secondIsRegister(REG_E).run(0x93),
        test.secondIsRegister(REG_H).run(0x94),
        test.secondIsRegister(REG_L).run(0x95),
        test.setPair(REG_PAIR_HL, 1).secondIsMemoryByteAt(1).run(0x96)
);
```

Here we want to run 7 tests, for each `SUB` variation - for registers `B`, `C`, `D`, etc. So for the specific test we
must say, that the second generated operand will be stored in the given register, before we actually 'run' the test.
Since we did not specify `keepCurrentInjectorsAfterRun()` after this step, the next step will not remember the previous
setting for the second operand. Only the first operand, for register `A` will be remembered for all tests.

The last line is interesting, with preparing register pair `HL=1` and second operand to the memory at address `1`, we
can safely run `SUB M` with opcode `0x96`, which actually does the following:

```
SUB M = A - [HL]
```

For more information, see Javadoc of the project, and real usage in available emuStudio CPU plug-ins.
    
# License

This project is released under GNU GPL v2 license.
