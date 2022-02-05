// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-z80/src/main/antlr/AsZ80Parser.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AsZ80Parser}.
 */
public interface AsZ80ParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rStart}.
	 * @param ctx the parse tree
	 */
	void enterRStart(AsZ80Parser.RStartContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rStart}.
	 * @param ctx the parse tree
	 */
	void exitRStart(AsZ80Parser.RStartContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rLine}.
	 * @param ctx the parse tree
	 */
	void enterRLine(AsZ80Parser.RLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rLine}.
	 * @param ctx the parse tree
	 */
	void exitRLine(AsZ80Parser.RLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rStatement}.
	 * @param ctx the parse tree
	 */
	void enterRStatement(AsZ80Parser.RStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rStatement}.
	 * @param ctx the parse tree
	 */
	void exitRStatement(AsZ80Parser.RStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instr8bit}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstr8bit(AsZ80Parser.Instr8bitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instr8bit}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstr8bit(AsZ80Parser.Instr8bitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegPairExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegPairExpr(AsZ80Parser.InstrRegPairExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegPairExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegPairExpr(AsZ80Parser.InstrRegPairExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegExpr(AsZ80Parser.InstrRegExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegExpr(AsZ80Parser.InstrRegExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrCondExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrCondExpr(AsZ80Parser.InstrCondExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrCondExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrCondExpr(AsZ80Parser.InstrCondExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrExprRegPair}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrExprRegPair(AsZ80Parser.InstrExprRegPairContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrExprRegPair}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrExprRegPair(AsZ80Parser.InstrExprRegPairContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrExprReg}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrExprReg(AsZ80Parser.InstrExprRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrExprReg}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrExprReg(AsZ80Parser.InstrExprRegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrExpr(AsZ80Parser.InstrExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrExpr}
	 * labeled alternative in {@link AsZ80Parser#rInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrExpr(AsZ80Parser.InstrExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrNoArgs}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrNoArgs(AsZ80Parser.InstrNoArgsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrNoArgs}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrNoArgs(AsZ80Parser.InstrNoArgsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegPair(AsZ80Parser.InstrRegPairContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegPair(AsZ80Parser.InstrRegPairContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegPairReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegPairReg(AsZ80Parser.InstrRegPairRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegPairReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegPairReg(AsZ80Parser.InstrRegPairRegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrReg(AsZ80Parser.InstrRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrReg(AsZ80Parser.InstrRegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegPairRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegPairRegPair(AsZ80Parser.InstrRegPairRegPairContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegPairRegPair}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegPairRegPair(AsZ80Parser.InstrRegPairRegPairContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrRegReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrRegReg(AsZ80Parser.InstrRegRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrRegReg}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrRegReg(AsZ80Parser.InstrRegRegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instrCond}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstrCond(AsZ80Parser.InstrCondContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instrCond}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstrCond(AsZ80Parser.InstrCondContext ctx);
	/**
	 * Enter a parse tree produced by the {@code instr8bitExpr}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void enterInstr8bitExpr(AsZ80Parser.Instr8bitExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code instr8bitExpr}
	 * labeled alternative in {@link AsZ80Parser#r8bitInstruction}.
	 * @param ctx the parse tree
	 */
	void exitInstr8bitExpr(AsZ80Parser.Instr8bitExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rRegister}.
	 * @param ctx the parse tree
	 */
	void enterRRegister(AsZ80Parser.RRegisterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rRegister}.
	 * @param ctx the parse tree
	 */
	void exitRRegister(AsZ80Parser.RRegisterContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#cCondition}.
	 * @param ctx the parse tree
	 */
	void enterCCondition(AsZ80Parser.CConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#cCondition}.
	 * @param ctx the parse tree
	 */
	void exitCCondition(AsZ80Parser.CConditionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoOrg}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoOrg(AsZ80Parser.PseudoOrgContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoOrg}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoOrg(AsZ80Parser.PseudoOrgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoEqu}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoEqu(AsZ80Parser.PseudoEquContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoEqu}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoEqu(AsZ80Parser.PseudoEquContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoSet}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoSet(AsZ80Parser.PseudoSetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoSet}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoSet(AsZ80Parser.PseudoSetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoIf}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoIf(AsZ80Parser.PseudoIfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoIf}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoIf(AsZ80Parser.PseudoIfContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoMacroDef}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoMacroDef(AsZ80Parser.PseudoMacroDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoMacroDef}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoMacroDef(AsZ80Parser.PseudoMacroDefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoMacroCall}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoMacroCall(AsZ80Parser.PseudoMacroCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoMacroCall}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoMacroCall(AsZ80Parser.PseudoMacroCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pseudoInclude}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void enterPseudoInclude(AsZ80Parser.PseudoIncludeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pseudoInclude}
	 * labeled alternative in {@link AsZ80Parser#rPseudoCode}.
	 * @param ctx the parse tree
	 */
	void exitPseudoInclude(AsZ80Parser.PseudoIncludeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rMacroParameters}.
	 * @param ctx the parse tree
	 */
	void enterRMacroParameters(AsZ80Parser.RMacroParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rMacroParameters}.
	 * @param ctx the parse tree
	 */
	void exitRMacroParameters(AsZ80Parser.RMacroParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rMacroArguments}.
	 * @param ctx the parse tree
	 */
	void enterRMacroArguments(AsZ80Parser.RMacroArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rMacroArguments}.
	 * @param ctx the parse tree
	 */
	void exitRMacroArguments(AsZ80Parser.RMacroArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dataDB}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void enterDataDB(AsZ80Parser.DataDBContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dataDB}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void exitDataDB(AsZ80Parser.DataDBContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dataDW}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void enterDataDW(AsZ80Parser.DataDWContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dataDW}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void exitDataDW(AsZ80Parser.DataDWContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dataDS}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void enterDataDS(AsZ80Parser.DataDSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dataDS}
	 * labeled alternative in {@link AsZ80Parser#rData}.
	 * @param ctx the parse tree
	 */
	void exitDataDS(AsZ80Parser.DataDSContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rDBdata}.
	 * @param ctx the parse tree
	 */
	void enterRDBdata(AsZ80Parser.RDBdataContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rDBdata}.
	 * @param ctx the parse tree
	 */
	void exitRDBdata(AsZ80Parser.RDBdataContext ctx);
	/**
	 * Enter a parse tree produced by {@link AsZ80Parser#rDWdata}.
	 * @param ctx the parse tree
	 */
	void enterRDWdata(AsZ80Parser.RDWdataContext ctx);
	/**
	 * Exit a parse tree produced by {@link AsZ80Parser#rDWdata}.
	 * @param ctx the parse tree
	 */
	void exitRDWdata(AsZ80Parser.RDWdataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprOct}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprOct(AsZ80Parser.ExprOctContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprOct}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprOct(AsZ80Parser.ExprOctContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprHex1}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprHex1(AsZ80Parser.ExprHex1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code exprHex1}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprHex1(AsZ80Parser.ExprHex1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code exprHex2}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprHex2(AsZ80Parser.ExprHex2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code exprHex2}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprHex2(AsZ80Parser.ExprHex2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code exprDec}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprDec(AsZ80Parser.ExprDecContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprDec}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprDec(AsZ80Parser.ExprDecContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprString}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprString(AsZ80Parser.ExprStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprString}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprString(AsZ80Parser.ExprStringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprBin}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprBin(AsZ80Parser.ExprBinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprBin}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprBin(AsZ80Parser.ExprBinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprParens}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprParens(AsZ80Parser.ExprParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprParens}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprParens(AsZ80Parser.ExprParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprId}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprId(AsZ80Parser.ExprIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprId}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprId(AsZ80Parser.ExprIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprUnary}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprUnary(AsZ80Parser.ExprUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprUnary}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprUnary(AsZ80Parser.ExprUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprInfix}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprInfix(AsZ80Parser.ExprInfixContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprInfix}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprInfix(AsZ80Parser.ExprInfixContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprCurrentAddress}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void enterExprCurrentAddress(AsZ80Parser.ExprCurrentAddressContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprCurrentAddress}
	 * labeled alternative in {@link AsZ80Parser#rExpression}.
	 * @param ctx the parse tree
	 */
	void exitExprCurrentAddress(AsZ80Parser.ExprCurrentAddressContext ctx);
}