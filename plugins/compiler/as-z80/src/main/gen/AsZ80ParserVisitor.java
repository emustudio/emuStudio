// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-z80/src/main/antlr/AsZ80Parser.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AsZ80Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AsZ80ParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rStart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRStart(AsZ80Parser.RStartContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRLine(AsZ80Parser.RLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRStatement(AsZ80Parser.RStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instr8bit}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstr8bit(AsZ80Parser.Instr8bitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegPairExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegPairExpr(AsZ80Parser.InstrRegPairExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegExpr(AsZ80Parser.InstrRegExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrCondExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrCondExpr(AsZ80Parser.InstrCondExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrExprRegPair}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrExprRegPair(AsZ80Parser.InstrExprRegPairContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrExprReg}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrExprReg(AsZ80Parser.InstrExprRegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrExpr(AsZ80Parser.InstrExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrNoArgs}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrNoArgs(AsZ80Parser.InstrNoArgsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegPair(AsZ80Parser.InstrRegPairContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegPairReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegPairReg(AsZ80Parser.InstrRegPairRegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrReg(AsZ80Parser.InstrRegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegPairRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegPairRegPair(AsZ80Parser.InstrRegPairRegPairContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrRegReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrRegReg(AsZ80Parser.InstrRegRegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instrCond}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstrCond(AsZ80Parser.InstrCondContext ctx);
	/**
	 * Visit a parse tree produced by the {@code instr8bitExpr}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstr8bitExpr(AsZ80Parser.Instr8bitExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rRegister}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRRegister(AsZ80Parser.RRegisterContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#cCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCCondition(AsZ80Parser.CConditionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoOrg}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoOrg(AsZ80Parser.PseudoOrgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoEqu}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoEqu(AsZ80Parser.PseudoEquContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoSet}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoSet(AsZ80Parser.PseudoSetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoIf}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoIf(AsZ80Parser.PseudoIfContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoMacroDef}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoMacroDef(AsZ80Parser.PseudoMacroDefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoMacroCall}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoMacroCall(AsZ80Parser.PseudoMacroCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pseudoInclude}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoInclude(AsZ80Parser.PseudoIncludeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rMacroParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRMacroParameters(AsZ80Parser.RMacroParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rMacroArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRMacroArguments(AsZ80Parser.RMacroArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dataDB}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDB(AsZ80Parser.DataDBContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dataDW}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDW(AsZ80Parser.DataDWContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dataDS}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataDS(AsZ80Parser.DataDSContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rDBdata}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRDBdata(AsZ80Parser.RDBdataContext ctx);
	/**
	 * Visit a parse tree produced by {@link AsZ80Parser#rDWdata}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRDWdata(AsZ80Parser.RDWdataContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprOct}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprOct(AsZ80Parser.ExprOctContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprHex1}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprHex1(AsZ80Parser.ExprHex1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code exprHex2}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprHex2(AsZ80Parser.ExprHex2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code exprDec}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprDec(AsZ80Parser.ExprDecContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprString}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprString(AsZ80Parser.ExprStringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprBin}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprBin(AsZ80Parser.ExprBinContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprParens}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprParens(AsZ80Parser.ExprParensContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprId}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprId(AsZ80Parser.ExprIdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprUnary}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprUnary(AsZ80Parser.ExprUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprInfix}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprInfix(AsZ80Parser.ExprInfixContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprCurrentAddress}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprCurrentAddress(AsZ80Parser.ExprCurrentAddressContext ctx);
}