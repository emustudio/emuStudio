package net.emustudio.plugins.compiler.as8080.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Node {

    protected final List<Node> children = new ArrayList<>();

    public Node addChild(Node node) {
        children.add(Objects.requireNonNull(node));
        return this;
    }

    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Node getChild(int index) {
        return children.get(index);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        String spaces = new String(new char[indent]).replace("\0", " ");
        StringBuilder builder = new StringBuilder(spaces);
        builder.append(getClass().getSimpleName()).append("\n");
        for (Node child : children) {
            builder.append(child.toString(indent + 2));
        }
        return builder.toString();
    }
}
