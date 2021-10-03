package net.emustudio.plugins.compiler.as8080.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AbstractNode implements Node {
    protected final List<Node> children = new ArrayList<>();

    @Override
    public void addChild(Node node) {
        children.add(Objects.requireNonNull(node));
    }

    @Override
    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
