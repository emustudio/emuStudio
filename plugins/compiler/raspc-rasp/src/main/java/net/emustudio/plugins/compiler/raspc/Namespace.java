package net.emustudio.plugins.compiler.raspc;

import net.emustudio.plugins.compiler.raspc.tree.Input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Namespace {
    private final List<Integer> inputs = new ArrayList<>();
    private int programStart = -1;

    public void addInput(Input input) {
        inputs.addAll(input.getAll());
    }

    public List<Integer> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public void setProgramStart(int programStart) {
        this.programStart = programStart;
    }

    public int getProgramStart() {
        return programStart;
    }
}
