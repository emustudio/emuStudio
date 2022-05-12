package net.emustudio.plugins.compiler.rasp.ast;

import java.util.Objects;
import java.util.Optional;

public class Instruction {
    public final int line;
    public final int column;

    public final int address;
    public final int opcode;
    public final Optional<Integer> operand;
    public final Optional<String> id;

    public Instruction(int line, int column, int opcode, int address, Optional<Integer> operand) {
        this.opcode = opcode;
        this.address = address;
        this.operand = Objects.requireNonNull(operand);
        this.id = Optional.empty();
        this.line = line;
        this.column = column;
    }

    public Instruction(int line, int column, int opcode, int address, String id) {
        this.opcode = opcode;
        this.address = address;
        this.operand = Optional.empty();
        this.id = Optional.of(id);
        this.line = line;
        this.column = column;
    }

    public Instruction(int opcode, int address, Optional<Integer> operand) {
        this(0, 0, opcode, address, operand);
    }

    public Instruction(int opcode, int address, String id) {
        this(0, 0, opcode, address, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instruction that = (Instruction) o;
        return address == that.address && opcode == that.opcode && operand.equals(that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, opcode, operand);
    }

    @Override
    public String toString() {
        return "Instruction{" +
            "address=" + address +
            ", opcode=" + opcode +
            ", operand=" + operand +
            '}';
    }
}
