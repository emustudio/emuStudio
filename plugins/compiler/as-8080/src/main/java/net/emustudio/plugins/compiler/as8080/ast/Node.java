package net.emustudio.plugins.compiler.as8080.ast;

import java.util.*;

public abstract class Node {
    protected Node parent;
    protected final List<Node> children = new ArrayList<>();
    public final int line;
    public final int column;

    public Node(int line, int column) {
        this.line = line;
        this.column = column;
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
        if (!children.isEmpty()) {
            builder.append("\n");
        }
        for (Node child : children) {
            builder.append(child.toString(indent + 2));
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
}
