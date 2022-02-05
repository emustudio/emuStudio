package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import net.emustudio.plugins.compiler.asZ80.AsZ80ParserBaseVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import org.antlr.v4.runtime.Token;

public class CreateDataVisitor extends AsZ80ParserBaseVisitor<Node> {

    @Override
    public Node visitDataDB(DataDBContext ctx) {
        Token start = ctx.getStart();
        DataDB db = new DataDB(start.getLine(), start.getCharPositionInLine());

        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                db.addChild(CreateVisitors.expr.visit(next.expr));
            } else if (next.instr != null) {
                db.addChild(CreateVisitors.instr.visit(next.instr));
            }
        }
        return db;
    }

    @Override
    public Node visitDataDW(DataDWContext ctx) {
        Token start = ctx.getStart();
        DataDW dw = new DataDW(start.getLine(), start.getCharPositionInLine());

        for (RDWdataContext next : ctx.rDWdata()) {
            if (next.expr != null) {
                dw.addChild(CreateVisitors.expr.visit(next.expr));
            }
        }

        return dw;
    }

    @Override
    public Node visitDataDS(DataDSContext ctx) {
        Token start = ctx.getStart();
        DataDS ds = new DataDS(start.getLine(), start.getCharPositionInLine());
        ds.addChild(CreateVisitors.expr.visit(ctx.data));
        return ds;
    }
}
