package net.sf.emustudio.rasp.compiler.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miso on 24.9.2017.
 */
public class Input {
    private final List<Integer> numbers = new ArrayList<>();

    public Input() {
    }

    public Input(int n) {
        numbers.add(n);
    }

    public void addNumber(int n) {
        numbers.add(n);
    }

    public List<Integer> getAll() {
        return numbers;
    }
}
