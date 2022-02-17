package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Node {
    protected Node parent;
    protected final List<Node> children = new ArrayList<>();
    public final int line;
    public final int column;

    private int address;
    private Optional<Integer> maxValue;
    private Optional<Integer> sizeBytes;

    public Node(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public void addChildFirst(Node node) {
        node.parent = this;
        children.add(0, node);
    }

    public Node addChild(Node node) {
        node.parent = this;
        children.add(node);
        return this;
    }

    public void addChildAt(int index, Node node) {
        node.parent = this;
        children.add(index, node);
    }

    public void addChildren(List<Node> nodes) {
        for (Node node : nodes) {
            node.parent = this;
        }
        children.addAll(nodes);
    }

    public List<Node> getChildren() {
        return List.copyOf(children);
    }

    public <T extends Node> Optional<T> collectChild(Class<T> cl) {
        for (Node child : children) {
            if (cl.isInstance(child)) {
                return Optional.of((T) child);
            }
        }
        return Optional.empty();
    }

    public Optional<Node> getParent() {
        return Optional.ofNullable(parent);
    }

    public Node getChild(int index) {
        return children.get(index);
    }

    public void removeChild(Node node) {
        node.parent = null;
        children.remove(node);
    }

    public Optional<Node> remove() {
        Optional<Node> parent = getParent();
        parent.ifPresent(p -> p.removeChild(this));
        return parent;
    }

    public void exclude() {
        Optional<Node> parent = getParent();
        parent.ifPresent(p -> {
            int index = p.getChildren().indexOf(this);
            p.removeChild(this);
            for (Node child : getChildren()) {
                p.addChildAt(index++, child);
            }
        });
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return Optional.empty();
    }

    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        String spaces = new String(new char[indent]).replace("\0", " ");
        StringBuilder builder = new StringBuilder(spaces);
        builder.append(address).append("> ").append(toStringShallow());
        for (Node child : children) {
            builder.append("\n").append(child.toString(indent + 2));
        }
        return builder.toString();
    }

    protected String toStringShallow() {
        return getClass().getSimpleName();
    }

    protected abstract Node mkCopy();

    public Node copy() {
        Node copied = mkCopy();
        for (Node child : children) {
            copied.addChild(child.copy());
        }
        return copied;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return !(o == null || getClass() != o.getClass());
    }

    public Optional<Integer> getMaxValue() {
        return maxValue;
    }
    public Optional<Integer> getSizeBytes() {
        return sizeBytes;
    }

    public Node setMaxValue(int maxValue) {
        int wasBits = (int) Math.floor(Math.log10(Math.abs(maxValue)) / Math.log10(2)) + 1;
        this.sizeBytes = Optional.of((int) Math.ceil(wasBits / 8.0));
        this.maxValue = Optional.of(maxValue);
        return this;
    }

    public Node setSizeBytes(int bytes) {
        int value = 0;
        for (int i = 0; i < bytes; i++) {
            value <<= 8;
            value |= 0xFF;
        }

        this.sizeBytes = Optional.of(bytes);
        this.maxValue = Optional.of(value);
        return this;
    }
}
