/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Node {
    public final SourceCodePosition position;
    protected final List<Node> children = new ArrayList<>();
    protected Node parent;
    private int address;
    private Optional<Integer> maxValue = Optional.empty();
    private Optional<Integer> sizeBytes = Optional.empty();

    public Node(SourceCodePosition position) {
        this.position = Objects.requireNonNull(position);
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
        builder
                .append(Integer.toHexString(address))
                .append("> ")
                .append(toStringShallow())
                .append(sizeBytes.map(s -> "(size=" + s + ")").orElse(""));

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
        maxValue.ifPresent(copied::setMaxValue);
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

    public Node setMaxValue(int maxValue) {
        int wasBits = (int) Math.floor(Math.log10(Math.abs(maxValue)) / Math.log10(2)) + 1;
        this.sizeBytes = Optional.of((int) Math.ceil(wasBits / 8.0));
        this.maxValue = Optional.of(maxValue);
        return this;
    }

    public Optional<Integer> getSizeBytes() {
        return sizeBytes;
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

    public static SourceCodePosition positionFromToken(String fileName, Token token) {
        return new SourceCodePosition(token.getLine(), token.getCharPositionInLine(), fileName);
    }
}
