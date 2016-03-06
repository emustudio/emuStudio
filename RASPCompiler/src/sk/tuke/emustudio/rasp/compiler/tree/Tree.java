/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler.tree;

import sk.tuke.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class Tree {

    private final int programStart;
    private final Program program;

    public Tree(int programStart, Program program) {
        this.programStart = programStart;
        this.program = program;
    }

    public void pass() {
        CompilerOutput.getInstance().setProgramStart(programStart);
        program.pass();

    }

}
