package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import org.antlr.v4.runtime.Token;

import static net.emustudio.plugins.compiler.as8080.ParsingUtils.parseLitString;

public class CreateDataVisitor extends As8080ParserBaseVisitor<Node> {

    @Override
    public Node visitDataDB(DataDBContext ctx) {
        Token start = ctx.getStart();
        DataDB db = new DataDB(start.getLine(), start.getCharPositionInLine());

        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                db.addChild(CreateVisitors.expr.visit(next.expr));
            } else if (next.instr != null) {
                db.addChild(CreateVisitors.instr.visit(next.instr));
            } else {
                Token token = next.str;
                String text = parseLitString(next.str);
                for (byte c : text.getBytes()) {
                    db.addChild(new ExprNumber(token.getLine(), token.getCharPositionInLine(), c));
                }
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
