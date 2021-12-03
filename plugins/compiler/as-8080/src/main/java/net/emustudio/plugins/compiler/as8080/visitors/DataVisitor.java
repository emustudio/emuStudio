package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import net.emustudio.plugins.compiler.as8080.As8080ParserBaseVisitor;
import net.emustudio.plugins.compiler.as8080.CommonParsers;
import net.emustudio.plugins.compiler.as8080.ast.data.Data;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;

public class DataVisitor extends As8080ParserBaseVisitor<Data> {

    @Override
    public Data visitDataDB(DataDBContext ctx) {
        Data data = new Data();

        if (ctx.data.expr != null) {
            data.addChild(new DataDB(AllVisitors.expr.visit(ctx.data.expr)));
        } else if (ctx.data.instr != null) {
            data.addChild(new DataDB(AllVisitors.instr.visit(ctx.data.instr)));
        } else {
            data.addChild(new DataDB(CommonParsers.parseLitString(ctx.data.str)));
        }
        for (RDBdataContext next : ctx.rDBdata()) {
            if (next.expr != null) {
                data.addChild(new DataDB(AllVisitors.expr.visit(next.expr)));
            } else if (next.instr != null) {
                data.addChild(new DataDB(AllVisitors.instr.visit(next.instr)));
            } else {
                data.addChild(new DataDB(CommonParsers.parseLitString(next.str)));
            }
        }

        return data;
    }

    @Override
    public Data visitDataDW(DataDWContext ctx) {
        Data data = new Data();

        if (ctx.data.expr != null) {
            data.addChild(new DataDW(AllVisitors.expr.visit(ctx.data.expr)));
        }
        for (RDWdataContext next : ctx.rDWdata()) {
            if (next.expr != null) {
                data.addChild(new DataDW(AllVisitors.expr.visit(next.expr)));
            }
        }

        return data;
    }

    @Override
    public Data visitDataDS(DataDSContext ctx) {
        return new DataDS(AllVisitors.expr.visit(ctx.data));
    }
}
