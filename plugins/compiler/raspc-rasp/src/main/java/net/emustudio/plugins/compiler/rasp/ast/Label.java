package net.emustudio.plugins.compiler.rasp.ast;

import net.emustudio.plugins.memory.rasp.api.RASPLabel;

public class Label implements RASPLabel {
    public final int line;
    public final int column;

    private final String label;
    private final int address;

    public Label(int line, int column, String text, int address) {
        this.line = line;
        this.column = column;
        this.label = text.toUpperCase();
        this.address = address;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Label label1 = (Label) o;

        if (address != label1.address) return false;
        return label.equals(label1.label);
    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + address;
        return result;
    }

    @Override
    public String toString() {
        return "{" +
            "label='" + label + '\'' +
            ", address=" + address +
            '}';
    }
}
