package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.data.Data;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;

public class DataVisitor extends As8080ParserBaseVisitor<Data> {

    @Override
    public Data visitDataDB(As8080Parser.DataDBContext ctx) {

        return new DataDB();
    }

    @Override
    public Data visitDataDW(As8080Parser.DataDWContext ctx) {
        return new DataDW();
    }

    @Override
    public Data visitDataDS(As8080Parser.DataDSContext ctx) {
        return new DataDS(AllVisitors.expr.visit(ctx.data));
    }
}
