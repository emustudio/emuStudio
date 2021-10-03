package net.emustudio.plugins.compiler.as8080.ast;

import java.util.List;

public interface Node {

    void addChild(Node node);

    List<Node> getChildren();
}
