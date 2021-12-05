package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.CommonParsers;
import net.emustudio.plugins.compiler.as8080.ast.data.*;

public class DataVisitor extends As8080ParserBaseVisitor<Data> {

    @Override
    public Data visitDataDB(DataDBContext ctx) {
        Data data = new Data();
        DataDB db = new DataDB();
        data.addChild(db);

        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                db.addChild(Visitors.expr.visit(next.expr));
            } else if (next.instr != null) {
                db.addChild(Visitors.instr.visit(next.instr));
            } else {
                db.addChild(new DataPlainString(CommonParsers.parseLitString(next.str)));
            }
        }

        return data;
    }

    @Override
    public Data visitDataDW(DataDWContext ctx) {
        Data data = new Data();
        DataDW dw = new DataDW();
        data.addChild(dw);

        for (RDWdataContext next : ctx.rDWdata()) {
            if (next.expr != null) {
                dw.addChild(Visitors.expr.visit(next.expr));
            }
        }

        return data;
    }

    @Override
    public Data visitDataDS(DataDSContext ctx) {
        DataDS ds = new DataDS();
        ds.addChild(Visitors.expr.visit(ctx.data));

        return ds;
    }
}
