/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.emustudio.rasp.compiler.tree;

import net.sf.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class SourceCode extends AbstractTreeNode{

    private final int programStart;
    private final Program program;

    public SourceCode(int programStart, Program program) {
        this.programStart = programStart;
        this.program = program;
    }

    @Override
    public void pass() throws Exception {
        CompilerOutput.getInstance().setProgramStart(programStart);
        program.pass();
    }

}
