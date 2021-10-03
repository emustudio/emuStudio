package net.emustudio.plugins.compiler.as8080.visitors;

public class AllVisitors {

    static PseudoVisitor pseudo = new PseudoVisitor();
    static ExprVisitor expr = new ExprVisitor();
    static InstrVisitor instr = new InstrVisitor();
    static DataVisitor data = new DataVisitor();
    static StatementVisitor statement = new StatementVisitor();
}
