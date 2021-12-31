package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.Either;

import java.util.*;

public abstract class Node {
    protected Node parent;
    protected final List<Node> children = new ArrayList<>();
    public final int line;
    public final int column;

    private int address;

    public Node(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public Node addChildFirst(Node node) {
        node.parent = this;
        children.add(0, node);
        return this;
    }

    public Node addChild(Node node) {
        node.parent = this;
        children.add(node);
        return this;
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

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public Either<Node, Evaluated> eval(int currentAddress, NameSpace env) {
        return Either.ofLeft(this);
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
        builder.append(toStringShallow());
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
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return line == node.line && column == node.column && Objects.equals(parent, node.parent);
    }
}
