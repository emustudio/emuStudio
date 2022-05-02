package net.emustudio.plugins.compiler.as8080.visitors;

public class CreateVisitors {

    static CreatePseudoVisitor pseudo = new CreatePseudoVisitor();
    static CreateExprVisitor expr = new CreateExprVisitor();
    static CreateInstrVisitor instr = new CreateInstrVisitor();
    static CreateDataVisitor data = new CreateDataVisitor();
    static CreateLineVisitor line = new CreateLineVisitor();

}
