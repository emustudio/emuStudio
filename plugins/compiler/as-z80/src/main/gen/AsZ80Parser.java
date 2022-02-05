// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-z80/src/main/antlr/AsZ80Parser.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AsZ80Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, COMMENT2=2, OPCODE_ADC=3, OPCODE_ADD=4, OPCODE_AND=5, OPCODE_BIT=6, 
		OPCODE_CALL=7, OPCODE_CCF=8, OPCODE_CP=9, OPCODE_CPD=10, OPCODE_CPDR=11, 
		OPCODE_CPI=12, OPCODE_CPIR=13, OPCODE_CPL=14, OPCODE_DAA=15, OPCODE_DEC=16, 
		OPCODE_DI=17, OPCODE_DJNZ=18, OPCODE_EI=19, OPCODE_EX=20, OPCODE_EXX=21, 
		OPCODE_HALT=22, OPCODE_IM=23, OPCODE_IN=24, OPCODE_INC=25, OPCODE_IND=26, 
		OPCODE_INDR=27, OPCODE_INI=28, OPCODE_INIR=29, OPCODE_JP=30, OPCODE_JR=31, 
		OPCODE_LD=32, OPCODE_LDD=33, OPCODE_LDDR=34, OPCODE_LDI=35, OPCODE_LDIR=36, 
		OPCODE_NEG=37, OPCODE_NOP=38, OPCODE_OR=39, OPCODE_OTDR=40, OPCODE_OTIR=41, 
		OPCODE_OUT=42, OPCODE_OUTD=43, OPCODE_OUTI=44, OPCODE_POP=45, OPCODE_PUSH=46, 
		OPCODE_RES=47, OPCODE_RET=48, OPCODE_RETI=49, OPCODE_RETN=50, OPCODE_RL=51, 
		OPCODE_RLA=52, OPCODE_RLC=53, OPCODE_RLCA=54, OPCODE_RLD=55, OPCODE_RR=56, 
		OPCODE_RRA=57, OPCODE_RRC=58, OPCODE_RRCA=59, OPCODE_RRD=60, OPCODE_RST=61, 
		OPCODE_SBC=62, OPCODE_SCF=63, OPCODE_SET=64, OPCODE_SLA=65, OPCODE_SRA=66, 
		OPCODE_SLL=67, OPCODE_SRL=68, OPCODE_SUB=69, OPCODE_XOR=70, COND_C=71, 
		COND_NC=72, COND_Z=73, COND_NZ=74, COND_M=75, COND_P=76, COND_PE=77, COND_PO=78, 
		COND_WS=79, COND_UNRECOGNIZED=80, PREP_ORG=81, PREP_EQU=82, PREP_SET=83, 
		PREP_VAR=84, PREP_IF=85, PREP_ENDIF=86, PREP_INCLUDE=87, PREP_MACRO=88, 
		PREP_ENDM=89, PREP_DB=90, PREP_DW=91, PREP_DS=92, PREP_ADDR=93, REG_A=94, 
		REG_B=95, REG_C=96, REG_D=97, REG_E=98, REG_H=99, REG_L=100, REG_IX=101, 
		REG_IXH=102, REG_IXL=103, REG_IY=104, REG_IYH=105, REG_IYL=106, REG_BC=107, 
		REG_DE=108, REG_HL=109, REG_SP=110, REG_AF=111, REG_AFF=112, REG_I=113, 
		REG_R=114, OP_MOD=115, OP_SHR=116, OP_SHL=117, OP_NOT=118, OP_AND=119, 
		OP_OR=120, OP_XOR=121, LIT_HEXNUMBER_1=122, LIT_NUMBER=123, LIT_HEXNUMBER_2=124, 
		LIT_OCTNUMBER=125, LIT_BINNUMBER=126, LIT_STRING_1=127, LIT_STRING_2=128, 
		ID_IDENTIFIER=129, ID_LABEL=130, ERROR=131, SEP_LPAR=132, SEP_RPAR=133, 
		SEP_COMMA=134, OP_ADD=135, OP_SUBTRACT=136, OP_MULTIPLY=137, OP_DIVIDE=138, 
		OP_EQUAL=139, OP_GT=140, OP_GTE=141, OP_LT=142, OP_LTE=143, OP_MOD_2=144, 
		OP_SHR_2=145, OP_SHL_2=146, OP_NOT_2=147, OP_AND_2=148, OP_OR_2=149, OP_XOR_2=150, 
		WS=151, EOL=152;
	public static final int
		RULE_rStart = 0, RULE_rLine = 1, RULE_rStatement = 2, RULE_rInstruction = 3, 
		RULE_r8bitInstruction = 4, RULE_rRegister = 5, RULE_cCondition = 6, RULE_rPseudoCode = 7, 
		RULE_rMacroParameters = 8, RULE_rMacroArguments = 9, RULE_rData = 10, 
		RULE_rDBdata = 11, RULE_rDWdata = 12, RULE_rExpression = 13;
	private static String[] makeRuleNames() {
		return new String[] {
			"rStart", "rLine", "rStatement", "rInstruction", "r8bitInstruction", 
			"rRegister", "cCondition", "rPseudoCode", "rMacroParameters", "rMacroArguments", 
			"rData", "rDBdata", "rDWdata", "rExpression"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, "'$'", null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"'('", "')'", "','", "'+'", "'-'", "'*'", "'/'", "'='", "'>'", "'>='", 
			"'<'", "'<='", "'%'", "'>>'", "'<<'", "'!'", "'&'", "'|'", "'~'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "COMMENT2", "OPCODE_ADC", "OPCODE_ADD", "OPCODE_AND", 
			"OPCODE_BIT", "OPCODE_CALL", "OPCODE_CCF", "OPCODE_CP", "OPCODE_CPD", 
			"OPCODE_CPDR", "OPCODE_CPI", "OPCODE_CPIR", "OPCODE_CPL", "OPCODE_DAA", 
			"OPCODE_DEC", "OPCODE_DI", "OPCODE_DJNZ", "OPCODE_EI", "OPCODE_EX", "OPCODE_EXX", 
			"OPCODE_HALT", "OPCODE_IM", "OPCODE_IN", "OPCODE_INC", "OPCODE_IND", 
			"OPCODE_INDR", "OPCODE_INI", "OPCODE_INIR", "OPCODE_JP", "OPCODE_JR", 
			"OPCODE_LD", "OPCODE_LDD", "OPCODE_LDDR", "OPCODE_LDI", "OPCODE_LDIR", 
			"OPCODE_NEG", "OPCODE_NOP", "OPCODE_OR", "OPCODE_OTDR", "OPCODE_OTIR", 
			"OPCODE_OUT", "OPCODE_OUTD", "OPCODE_OUTI", "OPCODE_POP", "OPCODE_PUSH", 
			"OPCODE_RES", "OPCODE_RET", "OPCODE_RETI", "OPCODE_RETN", "OPCODE_RL", 
			"OPCODE_RLA", "OPCODE_RLC", "OPCODE_RLCA", "OPCODE_RLD", "OPCODE_RR", 
			"OPCODE_RRA", "OPCODE_RRC", "OPCODE_RRCA", "OPCODE_RRD", "OPCODE_RST", 
			"OPCODE_SBC", "OPCODE_SCF", "OPCODE_SET", "OPCODE_SLA", "OPCODE_SRA", 
			"OPCODE_SLL", "OPCODE_SRL", "OPCODE_SUB", "OPCODE_XOR", "COND_C", "COND_NC", 
			"COND_Z", "COND_NZ", "COND_M", "COND_P", "COND_PE", "COND_PO", "COND_WS", 
			"COND_UNRECOGNIZED", "PREP_ORG", "PREP_EQU", "PREP_SET", "PREP_VAR", 
			"PREP_IF", "PREP_ENDIF", "PREP_INCLUDE", "PREP_MACRO", "PREP_ENDM", "PREP_DB", 
			"PREP_DW", "PREP_DS", "PREP_ADDR", "REG_A", "REG_B", "REG_C", "REG_D", 
			"REG_E", "REG_H", "REG_L", "REG_IX", "REG_IXH", "REG_IXL", "REG_IY", 
			"REG_IYH", "REG_IYL", "REG_BC", "REG_DE", "REG_HL", "REG_SP", "REG_AF", 
			"REG_AFF", "REG_I", "REG_R", "OP_MOD", "OP_SHR", "OP_SHL", "OP_NOT", 
			"OP_AND", "OP_OR", "OP_XOR", "LIT_HEXNUMBER_1", "LIT_NUMBER", "LIT_HEXNUMBER_2", 
			"LIT_OCTNUMBER", "LIT_BINNUMBER", "LIT_STRING_1", "LIT_STRING_2", "ID_IDENTIFIER", 
			"ID_LABEL", "ERROR", "SEP_LPAR", "SEP_RPAR", "SEP_COMMA", "OP_ADD", "OP_SUBTRACT", 
			"OP_MULTIPLY", "OP_DIVIDE", "OP_EQUAL", "OP_GT", "OP_GTE", "OP_LT", "OP_LTE", 
			"OP_MOD_2", "OP_SHR_2", "OP_SHL_2", "OP_NOT_2", "OP_AND_2", "OP_OR_2", 
			"OP_XOR_2", "WS", "EOL"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "AsZ80Parser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public AsZ80Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class RStartContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(AsZ80Parser.EOF, 0); }
		public List<TerminalNode> EOL() { return getTokens(AsZ80Parser.EOL); }
		public TerminalNode EOL(int i) {
			return getToken(AsZ80Parser.EOL, i);
		}
		public List<RLineContext> rLine() {
			return getRuleContexts(RLineContext.class);
		}
		public RLineContext rLine(int i) {
			return getRuleContext(RLineContext.class,i);
		}
		public RStartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rStart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRStart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRStart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RStartContext rStart() throws RecognitionException {
		RStartContext _localctx = new RStartContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_rStart);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(31);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(28);
					match(EOL);
					}
					} 
				}
				setState(33);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(35);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(34);
				rLine();
				}
				break;
			}
			setState(45);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(38); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(37);
							match(EOL);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(40); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					setState(42);
					rLine();
					}
					} 
				}
				setState(47);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			}
			setState(51);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EOL) {
				{
				{
				setState(48);
				match(EOL);
				}
				}
				setState(53);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(54);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RLineContext extends ParserRuleContext {
		public Token label;
		public RStatementContext statement;
		public RStatementContext rStatement() {
			return getRuleContext(RStatementContext.class,0);
		}
		public List<TerminalNode> EOL() { return getTokens(AsZ80Parser.EOL); }
		public TerminalNode EOL(int i) {
			return getToken(AsZ80Parser.EOL, i);
		}
		public TerminalNode ID_LABEL() { return getToken(AsZ80Parser.ID_LABEL, 0); }
		public RLineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rLine; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RLineContext rLine() throws RecognitionException {
		RLineContext _localctx = new RLineContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_rLine);
		int _la;
		try {
			setState(67);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(57);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID_LABEL) {
					{
					setState(56);
					((RLineContext)_localctx).label = match(ID_LABEL);
					}
				}

				setState(62);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==EOL) {
					{
					{
					setState(59);
					match(EOL);
					}
					}
					setState(64);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(65);
				((RLineContext)_localctx).statement = rStatement();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				((RLineContext)_localctx).label = match(ID_LABEL);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RStatementContext extends ParserRuleContext {
		public RInstructionContext instr;
		public RPseudoCodeContext pseudo;
		public RDataContext data;
		public RInstructionContext rInstruction() {
			return getRuleContext(RInstructionContext.class,0);
		}
		public RPseudoCodeContext rPseudoCode() {
			return getRuleContext(RPseudoCodeContext.class,0);
		}
		public RDataContext rData() {
			return getRuleContext(RDataContext.class,0);
		}
		public RStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RStatementContext rStatement() throws RecognitionException {
		RStatementContext _localctx = new RStatementContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_rStatement);
		try {
			setState(72);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPCODE_ADC:
			case OPCODE_ADD:
			case OPCODE_AND:
			case OPCODE_CALL:
			case OPCODE_CCF:
			case OPCODE_CP:
			case OPCODE_CPL:
			case OPCODE_DAA:
			case OPCODE_DEC:
			case OPCODE_DI:
			case OPCODE_EI:
			case OPCODE_EX:
			case OPCODE_EXX:
			case OPCODE_HALT:
			case OPCODE_IN:
			case OPCODE_INC:
			case OPCODE_JP:
			case OPCODE_JR:
			case OPCODE_LD:
			case OPCODE_NOP:
			case OPCODE_OR:
			case OPCODE_OUT:
			case OPCODE_POP:
			case OPCODE_PUSH:
			case OPCODE_RET:
			case OPCODE_RLA:
			case OPCODE_RLCA:
			case OPCODE_RRA:
			case OPCODE_RRCA:
			case OPCODE_RST:
			case OPCODE_SBC:
			case OPCODE_SCF:
			case OPCODE_SUB:
			case OPCODE_XOR:
				enterOuterAlt(_localctx, 1);
				{
				setState(69);
				((RStatementContext)_localctx).instr = rInstruction();
				}
				break;
			case PREP_ORG:
			case PREP_IF:
			case PREP_INCLUDE:
			case ID_IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(70);
				((RStatementContext)_localctx).pseudo = rPseudoCode();
				}
				break;
			case PREP_DB:
			case PREP_DW:
			case PREP_DS:
				enterOuterAlt(_localctx, 3);
				{
				setState(71);
				((RStatementContext)_localctx).data = rData();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RInstructionContext extends ParserRuleContext {
		public RInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rInstruction; }
	 
		public RInstructionContext() { }
		public void copyFrom(RInstructionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class InstrRegExprContext extends RInstructionContext {
		public Token opcode;
		public RRegisterContext reg;
		public RExpressionContext expr;
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public RRegisterContext rRegister() {
			return getRuleContext(RRegisterContext.class,0);
		}
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public InstrRegExprContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Instr8bitContext extends RInstructionContext {
		public R8bitInstructionContext r8bitInstruction() {
			return getRuleContext(R8bitInstructionContext.class,0);
		}
		public Instr8bitContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstr8bit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstr8bit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstr8bit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrExprRegPairContext extends RInstructionContext {
		public Token opcode;
		public RExpressionContext expr;
		public Token regpair;
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode REG_HL() { return getToken(AsZ80Parser.REG_HL, 0); }
		public InstrExprRegPairContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrExprRegPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrExprRegPair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrExprRegPair(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrExprContext extends RInstructionContext {
		public Token opcode;
		public RExpressionContext expr;
		public TerminalNode REG_A() { return getToken(AsZ80Parser.REG_A, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode OPCODE_CALL() { return getToken(AsZ80Parser.OPCODE_CALL, 0); }
		public TerminalNode OPCODE_ADD() { return getToken(AsZ80Parser.OPCODE_ADD, 0); }
		public TerminalNode OPCODE_ADC() { return getToken(AsZ80Parser.OPCODE_ADC, 0); }
		public TerminalNode OPCODE_OUT() { return getToken(AsZ80Parser.OPCODE_OUT, 0); }
		public TerminalNode OPCODE_SUB() { return getToken(AsZ80Parser.OPCODE_SUB, 0); }
		public TerminalNode OPCODE_IN() { return getToken(AsZ80Parser.OPCODE_IN, 0); }
		public TerminalNode OPCODE_SBC() { return getToken(AsZ80Parser.OPCODE_SBC, 0); }
		public TerminalNode OPCODE_AND() { return getToken(AsZ80Parser.OPCODE_AND, 0); }
		public TerminalNode OPCODE_XOR() { return getToken(AsZ80Parser.OPCODE_XOR, 0); }
		public TerminalNode OPCODE_OR() { return getToken(AsZ80Parser.OPCODE_OR, 0); }
		public TerminalNode OPCODE_CP() { return getToken(AsZ80Parser.OPCODE_CP, 0); }
		public InstrExprContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrExprRegContext extends RInstructionContext {
		public Token opcode;
		public RExpressionContext expr;
		public Token reg;
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode REG_A() { return getToken(AsZ80Parser.REG_A, 0); }
		public InstrExprRegContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrExprReg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrExprReg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrExprReg(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrCondExprContext extends RInstructionContext {
		public Token opcode;
		public Token cond;
		public RExpressionContext expr;
		public TerminalNode OPCODE_JR() { return getToken(AsZ80Parser.OPCODE_JR, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode COND_NZ() { return getToken(AsZ80Parser.COND_NZ, 0); }
		public TerminalNode COND_Z() { return getToken(AsZ80Parser.COND_Z, 0); }
		public TerminalNode COND_NC() { return getToken(AsZ80Parser.COND_NC, 0); }
		public TerminalNode COND_C() { return getToken(AsZ80Parser.COND_C, 0); }
		public TerminalNode OPCODE_JP() { return getToken(AsZ80Parser.OPCODE_JP, 0); }
		public CConditionContext cCondition() {
			return getRuleContext(CConditionContext.class,0);
		}
		public TerminalNode OPCODE_CALL() { return getToken(AsZ80Parser.OPCODE_CALL, 0); }
		public InstrCondExprContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrCondExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrCondExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrCondExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrRegPairExprContext extends RInstructionContext {
		public Token opcode;
		public Token regpair;
		public RExpressionContext expr;
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode REG_BC() { return getToken(AsZ80Parser.REG_BC, 0); }
		public TerminalNode REG_DE() { return getToken(AsZ80Parser.REG_DE, 0); }
		public TerminalNode REG_HL() { return getToken(AsZ80Parser.REG_HL, 0); }
		public TerminalNode REG_SP() { return getToken(AsZ80Parser.REG_SP, 0); }
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public InstrRegPairExprContext(RInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegPairExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegPairExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegPairExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RInstructionContext rInstruction() throws RecognitionException {
		RInstructionContext _localctx = new RInstructionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_rInstruction);
		int _la;
		try {
			setState(168);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				_localctx = new Instr8bitContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(74);
				r8bitInstruction();
				}
				break;
			case 2:
				_localctx = new InstrRegPairExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(75);
				((InstrRegPairExprContext)_localctx).opcode = match(OPCODE_LD);
				setState(76);
				((InstrRegPairExprContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_SP - 107)))) != 0)) ) {
					((InstrRegPairExprContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(77);
				match(SEP_COMMA);
				setState(78);
				((InstrRegPairExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 3:
				_localctx = new InstrRegExprContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(79);
				((InstrRegExprContext)_localctx).opcode = match(OPCODE_LD);
				setState(80);
				((InstrRegExprContext)_localctx).reg = rRegister();
				setState(81);
				match(SEP_COMMA);
				setState(82);
				((InstrRegExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 4:
				_localctx = new InstrCondExprContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(84);
				((InstrCondExprContext)_localctx).opcode = match(OPCODE_JR);
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (COND_C - 71)) | (1L << (COND_NC - 71)) | (1L << (COND_Z - 71)) | (1L << (COND_NZ - 71)))) != 0)) {
					{
					setState(85);
					((InstrCondExprContext)_localctx).cond = _input.LT(1);
					_la = _input.LA(1);
					if ( !(((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (COND_C - 71)) | (1L << (COND_NC - 71)) | (1L << (COND_Z - 71)) | (1L << (COND_NZ - 71)))) != 0)) ) {
						((InstrCondExprContext)_localctx).cond = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(86);
					match(SEP_COMMA);
					}
				}

				setState(89);
				((InstrCondExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 5:
				_localctx = new InstrExprRegPairContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(90);
				((InstrExprRegPairContext)_localctx).opcode = match(OPCODE_LD);
				setState(91);
				match(SEP_LPAR);
				setState(92);
				((InstrExprRegPairContext)_localctx).expr = rExpression(0);
				setState(93);
				match(SEP_RPAR);
				setState(94);
				match(SEP_COMMA);
				setState(95);
				((InstrExprRegPairContext)_localctx).regpair = match(REG_HL);
				}
				break;
			case 6:
				_localctx = new InstrRegPairExprContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(97);
				((InstrRegPairExprContext)_localctx).opcode = match(OPCODE_LD);
				setState(98);
				((InstrRegPairExprContext)_localctx).regpair = match(REG_HL);
				setState(99);
				match(SEP_COMMA);
				setState(100);
				match(SEP_LPAR);
				setState(101);
				((InstrRegPairExprContext)_localctx).expr = rExpression(0);
				setState(102);
				match(SEP_RPAR);
				}
				break;
			case 7:
				_localctx = new InstrExprRegContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(104);
				((InstrExprRegContext)_localctx).opcode = match(OPCODE_LD);
				setState(105);
				match(SEP_LPAR);
				setState(106);
				((InstrExprRegContext)_localctx).expr = rExpression(0);
				setState(107);
				match(SEP_RPAR);
				setState(108);
				match(SEP_COMMA);
				setState(109);
				((InstrExprRegContext)_localctx).reg = match(REG_A);
				}
				break;
			case 8:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(111);
				((InstrExprContext)_localctx).opcode = match(OPCODE_LD);
				setState(112);
				match(REG_A);
				setState(113);
				match(SEP_COMMA);
				setState(114);
				match(SEP_LPAR);
				setState(115);
				((InstrExprContext)_localctx).expr = rExpression(0);
				setState(116);
				match(SEP_RPAR);
				}
				break;
			case 9:
				_localctx = new InstrCondExprContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(118);
				((InstrCondExprContext)_localctx).opcode = match(OPCODE_JP);
				setState(122);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (COND_C - 71)) | (1L << (COND_NC - 71)) | (1L << (COND_Z - 71)) | (1L << (COND_NZ - 71)) | (1L << (COND_M - 71)) | (1L << (COND_P - 71)) | (1L << (COND_PE - 71)) | (1L << (COND_PO - 71)))) != 0)) {
					{
					setState(119);
					((InstrCondExprContext)_localctx).cond = cCondition();
					setState(120);
					match(SEP_COMMA);
					}
				}

				setState(124);
				((InstrCondExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 10:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(125);
				((InstrExprContext)_localctx).opcode = match(OPCODE_CALL);
				setState(126);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 11:
				_localctx = new InstrCondExprContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(127);
				((InstrCondExprContext)_localctx).opcode = match(OPCODE_CALL);
				setState(128);
				((InstrCondExprContext)_localctx).cond = cCondition();
				setState(129);
				match(SEP_COMMA);
				setState(130);
				((InstrCondExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 12:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(132);
				((InstrExprContext)_localctx).opcode = match(OPCODE_ADD);
				setState(133);
				match(REG_A);
				setState(134);
				match(SEP_COMMA);
				setState(135);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 13:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(136);
				((InstrExprContext)_localctx).opcode = match(OPCODE_ADC);
				setState(137);
				match(REG_A);
				setState(138);
				match(SEP_COMMA);
				setState(139);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 14:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(140);
				((InstrExprContext)_localctx).opcode = match(OPCODE_OUT);
				setState(141);
				match(SEP_LPAR);
				setState(142);
				((InstrExprContext)_localctx).expr = rExpression(0);
				setState(143);
				match(SEP_RPAR);
				setState(144);
				match(SEP_COMMA);
				setState(145);
				match(REG_A);
				}
				break;
			case 15:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(147);
				((InstrExprContext)_localctx).opcode = match(OPCODE_SUB);
				setState(148);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 16:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(149);
				((InstrExprContext)_localctx).opcode = match(OPCODE_IN);
				setState(150);
				match(REG_A);
				setState(151);
				match(SEP_COMMA);
				setState(152);
				match(SEP_LPAR);
				setState(153);
				((InstrExprContext)_localctx).expr = rExpression(0);
				setState(154);
				match(SEP_RPAR);
				}
				break;
			case 17:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(156);
				((InstrExprContext)_localctx).opcode = match(OPCODE_SBC);
				setState(157);
				match(REG_A);
				setState(158);
				match(SEP_COMMA);
				setState(159);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 18:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(160);
				((InstrExprContext)_localctx).opcode = match(OPCODE_AND);
				setState(161);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 19:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(162);
				((InstrExprContext)_localctx).opcode = match(OPCODE_XOR);
				setState(163);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 20:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(164);
				((InstrExprContext)_localctx).opcode = match(OPCODE_OR);
				setState(165);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 21:
				_localctx = new InstrExprContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(166);
				((InstrExprContext)_localctx).opcode = match(OPCODE_CP);
				setState(167);
				((InstrExprContext)_localctx).expr = rExpression(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class R8bitInstructionContext extends ParserRuleContext {
		public R8bitInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_r8bitInstruction; }
	 
		public R8bitInstructionContext() { }
		public void copyFrom(R8bitInstructionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class InstrRegPairRegContext extends R8bitInstructionContext {
		public Token opcode;
		public Token regpair;
		public RRegisterContext reg;
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public TerminalNode REG_HL() { return getToken(AsZ80Parser.REG_HL, 0); }
		public RRegisterContext rRegister() {
			return getRuleContext(RRegisterContext.class,0);
		}
		public InstrRegPairRegContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegPairReg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegPairReg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegPairReg(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Instr8bitExprContext extends R8bitInstructionContext {
		public Token opcode;
		public RExpressionContext expr;
		public TerminalNode OPCODE_RST() { return getToken(AsZ80Parser.OPCODE_RST, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public Instr8bitExprContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstr8bitExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstr8bitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstr8bitExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrRegPairContext extends R8bitInstructionContext {
		public Token opcode;
		public Token regpair;
		public Token regpairM;
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode REG_A() { return getToken(AsZ80Parser.REG_A, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public TerminalNode REG_BC() { return getToken(AsZ80Parser.REG_BC, 0); }
		public TerminalNode REG_DE() { return getToken(AsZ80Parser.REG_DE, 0); }
		public TerminalNode OPCODE_INC() { return getToken(AsZ80Parser.OPCODE_INC, 0); }
		public List<TerminalNode> REG_HL() { return getTokens(AsZ80Parser.REG_HL); }
		public TerminalNode REG_HL(int i) {
			return getToken(AsZ80Parser.REG_HL, i);
		}
		public TerminalNode REG_SP() { return getToken(AsZ80Parser.REG_SP, 0); }
		public TerminalNode OPCODE_ADD() { return getToken(AsZ80Parser.OPCODE_ADD, 0); }
		public TerminalNode OPCODE_DEC() { return getToken(AsZ80Parser.OPCODE_DEC, 0); }
		public TerminalNode OPCODE_POP() { return getToken(AsZ80Parser.OPCODE_POP, 0); }
		public TerminalNode REG_AF() { return getToken(AsZ80Parser.REG_AF, 0); }
		public TerminalNode OPCODE_PUSH() { return getToken(AsZ80Parser.OPCODE_PUSH, 0); }
		public TerminalNode OPCODE_JP() { return getToken(AsZ80Parser.OPCODE_JP, 0); }
		public InstrRegPairContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegPair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegPair(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrRegContext extends R8bitInstructionContext {
		public Token opcode;
		public RRegisterContext reg;
		public TerminalNode OPCODE_INC() { return getToken(AsZ80Parser.OPCODE_INC, 0); }
		public RRegisterContext rRegister() {
			return getRuleContext(RRegisterContext.class,0);
		}
		public TerminalNode OPCODE_DEC() { return getToken(AsZ80Parser.OPCODE_DEC, 0); }
		public TerminalNode REG_A() { return getToken(AsZ80Parser.REG_A, 0); }
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_ADD() { return getToken(AsZ80Parser.OPCODE_ADD, 0); }
		public TerminalNode OPCODE_ADC() { return getToken(AsZ80Parser.OPCODE_ADC, 0); }
		public TerminalNode OPCODE_SUB() { return getToken(AsZ80Parser.OPCODE_SUB, 0); }
		public TerminalNode OPCODE_SBC() { return getToken(AsZ80Parser.OPCODE_SBC, 0); }
		public TerminalNode OPCODE_AND() { return getToken(AsZ80Parser.OPCODE_AND, 0); }
		public TerminalNode OPCODE_XOR() { return getToken(AsZ80Parser.OPCODE_XOR, 0); }
		public TerminalNode OPCODE_OR() { return getToken(AsZ80Parser.OPCODE_OR, 0); }
		public TerminalNode OPCODE_CP() { return getToken(AsZ80Parser.OPCODE_CP, 0); }
		public InstrRegContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrReg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrReg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrReg(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrCondContext extends R8bitInstructionContext {
		public Token opcode;
		public CConditionContext cond;
		public TerminalNode OPCODE_RET() { return getToken(AsZ80Parser.OPCODE_RET, 0); }
		public CConditionContext cCondition() {
			return getRuleContext(CConditionContext.class,0);
		}
		public InstrCondContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrCond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrCond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrCond(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrNoArgsContext extends R8bitInstructionContext {
		public Token opcode;
		public TerminalNode OPCODE_NOP() { return getToken(AsZ80Parser.OPCODE_NOP, 0); }
		public TerminalNode OPCODE_RLCA() { return getToken(AsZ80Parser.OPCODE_RLCA, 0); }
		public TerminalNode OPCODE_RRCA() { return getToken(AsZ80Parser.OPCODE_RRCA, 0); }
		public TerminalNode OPCODE_RLA() { return getToken(AsZ80Parser.OPCODE_RLA, 0); }
		public TerminalNode OPCODE_RRA() { return getToken(AsZ80Parser.OPCODE_RRA, 0); }
		public TerminalNode OPCODE_DAA() { return getToken(AsZ80Parser.OPCODE_DAA, 0); }
		public TerminalNode OPCODE_CPL() { return getToken(AsZ80Parser.OPCODE_CPL, 0); }
		public TerminalNode OPCODE_SCF() { return getToken(AsZ80Parser.OPCODE_SCF, 0); }
		public TerminalNode OPCODE_CCF() { return getToken(AsZ80Parser.OPCODE_CCF, 0); }
		public TerminalNode OPCODE_HALT() { return getToken(AsZ80Parser.OPCODE_HALT, 0); }
		public TerminalNode OPCODE_EXX() { return getToken(AsZ80Parser.OPCODE_EXX, 0); }
		public TerminalNode OPCODE_DI() { return getToken(AsZ80Parser.OPCODE_DI, 0); }
		public TerminalNode OPCODE_EI() { return getToken(AsZ80Parser.OPCODE_EI, 0); }
		public InstrNoArgsContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrNoArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrNoArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrNoArgs(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrRegRegContext extends R8bitInstructionContext {
		public Token opcode;
		public RRegisterContext dst;
		public RRegisterContext src;
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public List<RRegisterContext> rRegister() {
			return getRuleContexts(RRegisterContext.class);
		}
		public RRegisterContext rRegister(int i) {
			return getRuleContext(RRegisterContext.class,i);
		}
		public InstrRegRegContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegReg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegReg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegReg(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InstrRegPairRegPairContext extends R8bitInstructionContext {
		public Token opcode;
		public Token src;
		public Token dst;
		public TerminalNode SEP_COMMA() { return getToken(AsZ80Parser.SEP_COMMA, 0); }
		public TerminalNode OPCODE_EX() { return getToken(AsZ80Parser.OPCODE_EX, 0); }
		public TerminalNode REG_AF() { return getToken(AsZ80Parser.REG_AF, 0); }
		public TerminalNode REG_AFF() { return getToken(AsZ80Parser.REG_AFF, 0); }
		public TerminalNode REG_DE() { return getToken(AsZ80Parser.REG_DE, 0); }
		public TerminalNode REG_HL() { return getToken(AsZ80Parser.REG_HL, 0); }
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode REG_SP() { return getToken(AsZ80Parser.REG_SP, 0); }
		public TerminalNode OPCODE_LD() { return getToken(AsZ80Parser.OPCODE_LD, 0); }
		public InstrRegPairRegPairContext(R8bitInstructionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterInstrRegPairRegPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitInstrRegPairRegPair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitInstrRegPairRegPair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final R8bitInstructionContext r8bitInstruction() throws RecognitionException {
		R8bitInstructionContext _localctx = new R8bitInstructionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_r8bitInstruction);
		int _la;
		try {
			setState(280);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(170);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_NOP);
				}
				break;
			case 2:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(171);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_LD);
				setState(172);
				match(SEP_LPAR);
				setState(173);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==REG_BC || _la==REG_DE) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(174);
				match(SEP_RPAR);
				setState(175);
				match(SEP_COMMA);
				setState(176);
				match(REG_A);
				}
				break;
			case 3:
				_localctx = new InstrRegPairRegContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(177);
				((InstrRegPairRegContext)_localctx).opcode = match(OPCODE_LD);
				setState(178);
				match(SEP_LPAR);
				setState(179);
				((InstrRegPairRegContext)_localctx).regpair = match(REG_HL);
				setState(180);
				match(SEP_RPAR);
				setState(181);
				match(SEP_COMMA);
				setState(182);
				((InstrRegPairRegContext)_localctx).reg = rRegister();
				}
				break;
			case 4:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(183);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_INC);
				setState(184);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_SP - 107)))) != 0)) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 5:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(185);
				((InstrRegContext)_localctx).opcode = match(OPCODE_INC);
				setState(186);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 6:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(187);
				((InstrRegContext)_localctx).opcode = match(OPCODE_DEC);
				setState(188);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 7:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(189);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_RLCA);
				}
				break;
			case 8:
				_localctx = new InstrRegPairRegPairContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(190);
				((InstrRegPairRegPairContext)_localctx).opcode = match(OPCODE_EX);
				setState(191);
				((InstrRegPairRegPairContext)_localctx).src = match(REG_AF);
				setState(192);
				match(SEP_COMMA);
				setState(193);
				((InstrRegPairRegPairContext)_localctx).dst = match(REG_AFF);
				}
				break;
			case 9:
				_localctx = new InstrRegPairRegPairContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(194);
				((InstrRegPairRegPairContext)_localctx).opcode = match(OPCODE_EX);
				setState(195);
				((InstrRegPairRegPairContext)_localctx).src = match(REG_DE);
				setState(196);
				match(SEP_COMMA);
				setState(197);
				((InstrRegPairRegPairContext)_localctx).dst = match(REG_HL);
				}
				break;
			case 10:
				_localctx = new InstrRegPairRegPairContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(198);
				((InstrRegPairRegPairContext)_localctx).opcode = match(OPCODE_EX);
				setState(199);
				match(SEP_LPAR);
				setState(200);
				((InstrRegPairRegPairContext)_localctx).src = match(REG_SP);
				setState(201);
				match(SEP_RPAR);
				setState(202);
				match(SEP_COMMA);
				setState(203);
				((InstrRegPairRegPairContext)_localctx).dst = match(REG_HL);
				}
				break;
			case 11:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(204);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_ADD);
				setState(205);
				match(REG_HL);
				setState(206);
				match(SEP_COMMA);
				setState(207);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_SP - 107)))) != 0)) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 12:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(208);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_LD);
				setState(209);
				match(REG_A);
				setState(210);
				match(SEP_COMMA);
				setState(211);
				match(SEP_LPAR);
				setState(212);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==REG_BC || _la==REG_DE) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(213);
				match(SEP_RPAR);
				}
				break;
			case 13:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(214);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_DEC);
				setState(215);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_SP - 107)))) != 0)) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 14:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(216);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_RRCA);
				}
				break;
			case 15:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(217);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_RLA);
				}
				break;
			case 16:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(218);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_RRA);
				}
				break;
			case 17:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(219);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_DAA);
				}
				break;
			case 18:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(220);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_CPL);
				}
				break;
			case 19:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(221);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_INC);
				setState(222);
				match(SEP_LPAR);
				setState(223);
				((InstrRegPairContext)_localctx).regpairM = match(REG_HL);
				setState(224);
				match(SEP_RPAR);
				}
				break;
			case 20:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(225);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_DEC);
				setState(226);
				match(SEP_LPAR);
				setState(227);
				((InstrRegPairContext)_localctx).regpairM = match(REG_HL);
				setState(228);
				match(SEP_RPAR);
				}
				break;
			case 21:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(229);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_SCF);
				}
				break;
			case 22:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(230);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_CCF);
				}
				break;
			case 23:
				_localctx = new InstrRegRegContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(231);
				((InstrRegRegContext)_localctx).opcode = match(OPCODE_LD);
				setState(232);
				((InstrRegRegContext)_localctx).dst = rRegister();
				setState(233);
				match(SEP_COMMA);
				setState(234);
				((InstrRegRegContext)_localctx).src = rRegister();
				}
				break;
			case 24:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(236);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_HALT);
				}
				break;
			case 25:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(237);
				((InstrRegContext)_localctx).opcode = match(OPCODE_ADD);
				setState(238);
				match(REG_A);
				setState(239);
				match(SEP_COMMA);
				setState(240);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 26:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(241);
				((InstrRegContext)_localctx).opcode = match(OPCODE_ADC);
				setState(242);
				match(REG_A);
				setState(243);
				match(SEP_COMMA);
				setState(244);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 27:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(245);
				((InstrRegContext)_localctx).opcode = match(OPCODE_SUB);
				setState(246);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 28:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(247);
				((InstrRegContext)_localctx).opcode = match(OPCODE_SBC);
				setState(248);
				match(REG_A);
				setState(249);
				match(SEP_COMMA);
				setState(250);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 29:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(251);
				((InstrRegContext)_localctx).opcode = match(OPCODE_AND);
				setState(252);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 30:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(253);
				((InstrRegContext)_localctx).opcode = match(OPCODE_XOR);
				setState(254);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 31:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(255);
				((InstrRegContext)_localctx).opcode = match(OPCODE_OR);
				setState(256);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 32:
				_localctx = new InstrRegContext(_localctx);
				enterOuterAlt(_localctx, 32);
				{
				setState(257);
				((InstrRegContext)_localctx).opcode = match(OPCODE_CP);
				setState(258);
				((InstrRegContext)_localctx).reg = rRegister();
				}
				break;
			case 33:
				_localctx = new InstrCondContext(_localctx);
				enterOuterAlt(_localctx, 33);
				{
				setState(259);
				((InstrCondContext)_localctx).opcode = match(OPCODE_RET);
				setState(261);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (COND_C - 71)) | (1L << (COND_NC - 71)) | (1L << (COND_Z - 71)) | (1L << (COND_NZ - 71)) | (1L << (COND_M - 71)) | (1L << (COND_P - 71)) | (1L << (COND_PE - 71)) | (1L << (COND_PO - 71)))) != 0)) {
					{
					setState(260);
					((InstrCondContext)_localctx).cond = cCondition();
					}
				}

				}
				break;
			case 34:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 34);
				{
				setState(263);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_POP);
				setState(264);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_AF - 107)))) != 0)) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 35:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 35);
				{
				setState(265);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_PUSH);
				setState(266);
				((InstrRegPairContext)_localctx).regpair = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 107)) & ~0x3f) == 0 && ((1L << (_la - 107)) & ((1L << (REG_BC - 107)) | (1L << (REG_DE - 107)) | (1L << (REG_HL - 107)) | (1L << (REG_AF - 107)))) != 0)) ) {
					((InstrRegPairContext)_localctx).regpair = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 36:
				_localctx = new Instr8bitExprContext(_localctx);
				enterOuterAlt(_localctx, 36);
				{
				setState(267);
				((Instr8bitExprContext)_localctx).opcode = match(OPCODE_RST);
				setState(268);
				((Instr8bitExprContext)_localctx).expr = rExpression(0);
				}
				break;
			case 37:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 37);
				{
				setState(269);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_EXX);
				}
				break;
			case 38:
				_localctx = new InstrRegPairContext(_localctx);
				enterOuterAlt(_localctx, 38);
				{
				setState(270);
				((InstrRegPairContext)_localctx).opcode = match(OPCODE_JP);
				setState(271);
				match(SEP_LPAR);
				setState(272);
				((InstrRegPairContext)_localctx).regpairM = match(REG_HL);
				setState(273);
				match(SEP_RPAR);
				}
				break;
			case 39:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 39);
				{
				setState(274);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_DI);
				}
				break;
			case 40:
				_localctx = new InstrRegPairRegPairContext(_localctx);
				enterOuterAlt(_localctx, 40);
				{
				setState(275);
				((InstrRegPairRegPairContext)_localctx).opcode = match(OPCODE_LD);
				setState(276);
				((InstrRegPairRegPairContext)_localctx).dst = match(REG_SP);
				setState(277);
				match(SEP_COMMA);
				setState(278);
				((InstrRegPairRegPairContext)_localctx).src = match(REG_HL);
				}
				break;
			case 41:
				_localctx = new InstrNoArgsContext(_localctx);
				enterOuterAlt(_localctx, 41);
				{
				setState(279);
				((InstrNoArgsContext)_localctx).opcode = match(OPCODE_EI);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RRegisterContext extends ParserRuleContext {
		public Token r;
		public TerminalNode REG_A() { return getToken(AsZ80Parser.REG_A, 0); }
		public TerminalNode REG_B() { return getToken(AsZ80Parser.REG_B, 0); }
		public TerminalNode REG_C() { return getToken(AsZ80Parser.REG_C, 0); }
		public TerminalNode REG_D() { return getToken(AsZ80Parser.REG_D, 0); }
		public TerminalNode REG_E() { return getToken(AsZ80Parser.REG_E, 0); }
		public TerminalNode REG_H() { return getToken(AsZ80Parser.REG_H, 0); }
		public TerminalNode REG_L() { return getToken(AsZ80Parser.REG_L, 0); }
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public TerminalNode REG_HL() { return getToken(AsZ80Parser.REG_HL, 0); }
		public RRegisterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rRegister; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRRegister(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRRegister(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRRegister(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RRegisterContext rRegister() throws RecognitionException {
		RRegisterContext _localctx = new RRegisterContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_rRegister);
		try {
			setState(292);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REG_A:
				enterOuterAlt(_localctx, 1);
				{
				setState(282);
				((RRegisterContext)_localctx).r = match(REG_A);
				}
				break;
			case REG_B:
				enterOuterAlt(_localctx, 2);
				{
				setState(283);
				((RRegisterContext)_localctx).r = match(REG_B);
				}
				break;
			case REG_C:
				enterOuterAlt(_localctx, 3);
				{
				setState(284);
				((RRegisterContext)_localctx).r = match(REG_C);
				}
				break;
			case REG_D:
				enterOuterAlt(_localctx, 4);
				{
				setState(285);
				((RRegisterContext)_localctx).r = match(REG_D);
				}
				break;
			case REG_E:
				enterOuterAlt(_localctx, 5);
				{
				setState(286);
				((RRegisterContext)_localctx).r = match(REG_E);
				}
				break;
			case REG_H:
				enterOuterAlt(_localctx, 6);
				{
				setState(287);
				((RRegisterContext)_localctx).r = match(REG_H);
				}
				break;
			case REG_L:
				enterOuterAlt(_localctx, 7);
				{
				setState(288);
				((RRegisterContext)_localctx).r = match(REG_L);
				}
				break;
			case SEP_LPAR:
				enterOuterAlt(_localctx, 8);
				{
				setState(289);
				match(SEP_LPAR);
				setState(290);
				((RRegisterContext)_localctx).r = match(REG_HL);
				setState(291);
				match(SEP_RPAR);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CConditionContext extends ParserRuleContext {
		public TerminalNode COND_C() { return getToken(AsZ80Parser.COND_C, 0); }
		public TerminalNode COND_NC() { return getToken(AsZ80Parser.COND_NC, 0); }
		public TerminalNode COND_Z() { return getToken(AsZ80Parser.COND_Z, 0); }
		public TerminalNode COND_NZ() { return getToken(AsZ80Parser.COND_NZ, 0); }
		public TerminalNode COND_M() { return getToken(AsZ80Parser.COND_M, 0); }
		public TerminalNode COND_P() { return getToken(AsZ80Parser.COND_P, 0); }
		public TerminalNode COND_PE() { return getToken(AsZ80Parser.COND_PE, 0); }
		public TerminalNode COND_PO() { return getToken(AsZ80Parser.COND_PO, 0); }
		public CConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterCCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitCCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitCCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CConditionContext cCondition() throws RecognitionException {
		CConditionContext _localctx = new CConditionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_cCondition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(294);
			_la = _input.LA(1);
			if ( !(((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (COND_C - 71)) | (1L << (COND_NC - 71)) | (1L << (COND_Z - 71)) | (1L << (COND_NZ - 71)) | (1L << (COND_M - 71)) | (1L << (COND_P - 71)) | (1L << (COND_PE - 71)) | (1L << (COND_PO - 71)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RPseudoCodeContext extends ParserRuleContext {
		public RPseudoCodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rPseudoCode; }
	 
		public RPseudoCodeContext() { }
		public void copyFrom(RPseudoCodeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PseudoIncludeContext extends RPseudoCodeContext {
		public Token filename;
		public TerminalNode PREP_INCLUDE() { return getToken(AsZ80Parser.PREP_INCLUDE, 0); }
		public TerminalNode LIT_STRING_1() { return getToken(AsZ80Parser.LIT_STRING_1, 0); }
		public TerminalNode LIT_STRING_2() { return getToken(AsZ80Parser.LIT_STRING_2, 0); }
		public PseudoIncludeContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoInclude(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoInclude(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoInclude(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoIfContext extends RPseudoCodeContext {
		public RExpressionContext expr;
		public TerminalNode PREP_IF() { return getToken(AsZ80Parser.PREP_IF, 0); }
		public List<TerminalNode> EOL() { return getTokens(AsZ80Parser.EOL); }
		public TerminalNode EOL(int i) {
			return getToken(AsZ80Parser.EOL, i);
		}
		public TerminalNode PREP_ENDIF() { return getToken(AsZ80Parser.PREP_ENDIF, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public List<RLineContext> rLine() {
			return getRuleContexts(RLineContext.class);
		}
		public RLineContext rLine(int i) {
			return getRuleContext(RLineContext.class,i);
		}
		public PseudoIfContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoIf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoIf(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoIf(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoEquContext extends RPseudoCodeContext {
		public Token id;
		public RExpressionContext expr;
		public TerminalNode PREP_EQU() { return getToken(AsZ80Parser.PREP_EQU, 0); }
		public TerminalNode ID_IDENTIFIER() { return getToken(AsZ80Parser.ID_IDENTIFIER, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public PseudoEquContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoEqu(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoEqu(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoEqu(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoMacroCallContext extends RPseudoCodeContext {
		public Token id;
		public RMacroArgumentsContext args;
		public TerminalNode ID_IDENTIFIER() { return getToken(AsZ80Parser.ID_IDENTIFIER, 0); }
		public RMacroArgumentsContext rMacroArguments() {
			return getRuleContext(RMacroArgumentsContext.class,0);
		}
		public PseudoMacroCallContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoMacroCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoMacroCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoMacroCall(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoMacroDefContext extends RPseudoCodeContext {
		public Token id;
		public RMacroParametersContext params;
		public TerminalNode PREP_MACRO() { return getToken(AsZ80Parser.PREP_MACRO, 0); }
		public List<TerminalNode> EOL() { return getTokens(AsZ80Parser.EOL); }
		public TerminalNode EOL(int i) {
			return getToken(AsZ80Parser.EOL, i);
		}
		public TerminalNode PREP_ENDM() { return getToken(AsZ80Parser.PREP_ENDM, 0); }
		public TerminalNode ID_IDENTIFIER() { return getToken(AsZ80Parser.ID_IDENTIFIER, 0); }
		public List<RLineContext> rLine() {
			return getRuleContexts(RLineContext.class);
		}
		public RLineContext rLine(int i) {
			return getRuleContext(RLineContext.class,i);
		}
		public RMacroParametersContext rMacroParameters() {
			return getRuleContext(RMacroParametersContext.class,0);
		}
		public PseudoMacroDefContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoMacroDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoMacroDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoMacroDef(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoSetContext extends RPseudoCodeContext {
		public Token id;
		public RExpressionContext expr;
		public TerminalNode PREP_SET() { return getToken(AsZ80Parser.PREP_SET, 0); }
		public TerminalNode ID_IDENTIFIER() { return getToken(AsZ80Parser.ID_IDENTIFIER, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public PseudoSetContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoSet(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoOrgContext extends RPseudoCodeContext {
		public RExpressionContext expr;
		public TerminalNode PREP_ORG() { return getToken(AsZ80Parser.PREP_ORG, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public PseudoOrgContext(RPseudoCodeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterPseudoOrg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitPseudoOrg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitPseudoOrg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RPseudoCodeContext rPseudoCode() throws RecognitionException {
		RPseudoCodeContext _localctx = new RPseudoCodeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_rPseudoCode);
		int _la;
		try {
			int _alt;
			setState(350);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				_localctx = new PseudoOrgContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(296);
				match(PREP_ORG);
				setState(297);
				((PseudoOrgContext)_localctx).expr = rExpression(0);
				}
				break;
			case 2:
				_localctx = new PseudoEquContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(298);
				((PseudoEquContext)_localctx).id = match(ID_IDENTIFIER);
				setState(299);
				match(PREP_EQU);
				setState(300);
				((PseudoEquContext)_localctx).expr = rExpression(0);
				}
				break;
			case 3:
				_localctx = new PseudoSetContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(301);
				((PseudoSetContext)_localctx).id = match(ID_IDENTIFIER);
				setState(302);
				match(PREP_SET);
				setState(303);
				((PseudoSetContext)_localctx).expr = rExpression(0);
				}
				break;
			case 4:
				_localctx = new PseudoIfContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(304);
				match(PREP_IF);
				setState(305);
				((PseudoIfContext)_localctx).expr = rExpression(0);
				setState(306);
				match(EOL);
				setState(312);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(307);
						rLine();
						setState(308);
						match(EOL);
						}
						} 
					}
					setState(314);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
				}
				setState(318);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==EOL) {
					{
					{
					setState(315);
					match(EOL);
					}
					}
					setState(320);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(321);
				match(PREP_ENDIF);
				}
				break;
			case 5:
				_localctx = new PseudoMacroDefContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(323);
				((PseudoMacroDefContext)_localctx).id = match(ID_IDENTIFIER);
				setState(324);
				match(PREP_MACRO);
				setState(326);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID_IDENTIFIER) {
					{
					setState(325);
					((PseudoMacroDefContext)_localctx).params = rMacroParameters();
					}
				}

				setState(328);
				match(EOL);
				setState(334);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(329);
						rLine();
						setState(330);
						match(EOL);
						}
						} 
					}
					setState(336);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
				}
				setState(340);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==EOL) {
					{
					{
					setState(337);
					match(EOL);
					}
					}
					setState(342);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(343);
				match(PREP_ENDM);
				}
				break;
			case 6:
				_localctx = new PseudoMacroCallContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(344);
				((PseudoMacroCallContext)_localctx).id = match(ID_IDENTIFIER);
				setState(346);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 93)) & ~0x3f) == 0 && ((1L << (_la - 93)) & ((1L << (PREP_ADDR - 93)) | (1L << (OP_NOT - 93)) | (1L << (LIT_HEXNUMBER_1 - 93)) | (1L << (LIT_NUMBER - 93)) | (1L << (LIT_HEXNUMBER_2 - 93)) | (1L << (LIT_OCTNUMBER - 93)) | (1L << (LIT_BINNUMBER - 93)) | (1L << (LIT_STRING_1 - 93)) | (1L << (LIT_STRING_2 - 93)) | (1L << (ID_IDENTIFIER - 93)) | (1L << (SEP_LPAR - 93)) | (1L << (OP_ADD - 93)) | (1L << (OP_SUBTRACT - 93)) | (1L << (OP_NOT_2 - 93)))) != 0)) {
					{
					setState(345);
					((PseudoMacroCallContext)_localctx).args = rMacroArguments();
					}
				}

				}
				break;
			case 7:
				_localctx = new PseudoIncludeContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(348);
				match(PREP_INCLUDE);
				setState(349);
				((PseudoIncludeContext)_localctx).filename = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LIT_STRING_1 || _la==LIT_STRING_2) ) {
					((PseudoIncludeContext)_localctx).filename = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RMacroParametersContext extends ParserRuleContext {
		public List<TerminalNode> ID_IDENTIFIER() { return getTokens(AsZ80Parser.ID_IDENTIFIER); }
		public TerminalNode ID_IDENTIFIER(int i) {
			return getToken(AsZ80Parser.ID_IDENTIFIER, i);
		}
		public List<TerminalNode> SEP_COMMA() { return getTokens(AsZ80Parser.SEP_COMMA); }
		public TerminalNode SEP_COMMA(int i) {
			return getToken(AsZ80Parser.SEP_COMMA, i);
		}
		public RMacroParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rMacroParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRMacroParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRMacroParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRMacroParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RMacroParametersContext rMacroParameters() throws RecognitionException {
		RMacroParametersContext _localctx = new RMacroParametersContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_rMacroParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
			match(ID_IDENTIFIER);
			setState(357);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEP_COMMA) {
				{
				{
				setState(353);
				match(SEP_COMMA);
				setState(354);
				match(ID_IDENTIFIER);
				}
				}
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RMacroArgumentsContext extends ParserRuleContext {
		public List<RExpressionContext> rExpression() {
			return getRuleContexts(RExpressionContext.class);
		}
		public RExpressionContext rExpression(int i) {
			return getRuleContext(RExpressionContext.class,i);
		}
		public List<TerminalNode> SEP_COMMA() { return getTokens(AsZ80Parser.SEP_COMMA); }
		public TerminalNode SEP_COMMA(int i) {
			return getToken(AsZ80Parser.SEP_COMMA, i);
		}
		public RMacroArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rMacroArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRMacroArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRMacroArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRMacroArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RMacroArgumentsContext rMacroArguments() throws RecognitionException {
		RMacroArgumentsContext _localctx = new RMacroArgumentsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_rMacroArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(360);
			rExpression(0);
			setState(365);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEP_COMMA) {
				{
				{
				setState(361);
				match(SEP_COMMA);
				setState(362);
				rExpression(0);
				}
				}
				setState(367);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RDataContext extends ParserRuleContext {
		public RDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rData; }
	 
		public RDataContext() { }
		public void copyFrom(RDataContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DataDSContext extends RDataContext {
		public RExpressionContext data;
		public TerminalNode PREP_DS() { return getToken(AsZ80Parser.PREP_DS, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public DataDSContext(RDataContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterDataDS(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitDataDS(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitDataDS(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DataDBContext extends RDataContext {
		public TerminalNode PREP_DB() { return getToken(AsZ80Parser.PREP_DB, 0); }
		public List<RDBdataContext> rDBdata() {
			return getRuleContexts(RDBdataContext.class);
		}
		public RDBdataContext rDBdata(int i) {
			return getRuleContext(RDBdataContext.class,i);
		}
		public List<TerminalNode> SEP_COMMA() { return getTokens(AsZ80Parser.SEP_COMMA); }
		public TerminalNode SEP_COMMA(int i) {
			return getToken(AsZ80Parser.SEP_COMMA, i);
		}
		public DataDBContext(RDataContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterDataDB(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitDataDB(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitDataDB(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DataDWContext extends RDataContext {
		public TerminalNode PREP_DW() { return getToken(AsZ80Parser.PREP_DW, 0); }
		public List<RDWdataContext> rDWdata() {
			return getRuleContexts(RDWdataContext.class);
		}
		public RDWdataContext rDWdata(int i) {
			return getRuleContext(RDWdataContext.class,i);
		}
		public List<TerminalNode> SEP_COMMA() { return getTokens(AsZ80Parser.SEP_COMMA); }
		public TerminalNode SEP_COMMA(int i) {
			return getToken(AsZ80Parser.SEP_COMMA, i);
		}
		public DataDWContext(RDataContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterDataDW(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitDataDW(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitDataDW(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RDataContext rData() throws RecognitionException {
		RDataContext _localctx = new RDataContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_rData);
		int _la;
		try {
			setState(388);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PREP_DB:
				_localctx = new DataDBContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(368);
				match(PREP_DB);
				setState(369);
				rDBdata();
				setState(374);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SEP_COMMA) {
					{
					{
					setState(370);
					match(SEP_COMMA);
					setState(371);
					rDBdata();
					}
					}
					setState(376);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case PREP_DW:
				_localctx = new DataDWContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(377);
				match(PREP_DW);
				setState(378);
				rDWdata();
				setState(383);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SEP_COMMA) {
					{
					{
					setState(379);
					match(SEP_COMMA);
					setState(380);
					rDWdata();
					}
					}
					setState(385);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case PREP_DS:
				_localctx = new DataDSContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(386);
				match(PREP_DS);
				setState(387);
				((DataDSContext)_localctx).data = rExpression(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RDBdataContext extends ParserRuleContext {
		public RExpressionContext expr;
		public R8bitInstructionContext instr;
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public R8bitInstructionContext r8bitInstruction() {
			return getRuleContext(R8bitInstructionContext.class,0);
		}
		public RDBdataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rDBdata; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRDBdata(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRDBdata(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRDBdata(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RDBdataContext rDBdata() throws RecognitionException {
		RDBdataContext _localctx = new RDBdataContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_rDBdata);
		try {
			setState(392);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PREP_ADDR:
			case OP_NOT:
			case LIT_HEXNUMBER_1:
			case LIT_NUMBER:
			case LIT_HEXNUMBER_2:
			case LIT_OCTNUMBER:
			case LIT_BINNUMBER:
			case LIT_STRING_1:
			case LIT_STRING_2:
			case ID_IDENTIFIER:
			case SEP_LPAR:
			case OP_ADD:
			case OP_SUBTRACT:
			case OP_NOT_2:
				enterOuterAlt(_localctx, 1);
				{
				setState(390);
				((RDBdataContext)_localctx).expr = rExpression(0);
				}
				break;
			case OPCODE_ADC:
			case OPCODE_ADD:
			case OPCODE_AND:
			case OPCODE_CCF:
			case OPCODE_CP:
			case OPCODE_CPL:
			case OPCODE_DAA:
			case OPCODE_DEC:
			case OPCODE_DI:
			case OPCODE_EI:
			case OPCODE_EX:
			case OPCODE_EXX:
			case OPCODE_HALT:
			case OPCODE_INC:
			case OPCODE_JP:
			case OPCODE_LD:
			case OPCODE_NOP:
			case OPCODE_OR:
			case OPCODE_POP:
			case OPCODE_PUSH:
			case OPCODE_RET:
			case OPCODE_RLA:
			case OPCODE_RLCA:
			case OPCODE_RRA:
			case OPCODE_RRCA:
			case OPCODE_RST:
			case OPCODE_SBC:
			case OPCODE_SCF:
			case OPCODE_SUB:
			case OPCODE_XOR:
				enterOuterAlt(_localctx, 2);
				{
				setState(391);
				((RDBdataContext)_localctx).instr = r8bitInstruction();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RDWdataContext extends ParserRuleContext {
		public RExpressionContext expr;
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public RDWdataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rDWdata; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterRDWdata(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitRDWdata(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitRDWdata(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RDWdataContext rDWdata() throws RecognitionException {
		RDWdataContext _localctx = new RDWdataContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_rDWdata);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(394);
			((RDWdataContext)_localctx).expr = rExpression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RExpressionContext extends ParserRuleContext {
		public RExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rExpression; }
	 
		public RExpressionContext() { }
		public void copyFrom(RExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ExprOctContext extends RExpressionContext {
		public Token num;
		public TerminalNode LIT_OCTNUMBER() { return getToken(AsZ80Parser.LIT_OCTNUMBER, 0); }
		public ExprOctContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprOct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprOct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprOct(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprHex1Context extends RExpressionContext {
		public Token num;
		public TerminalNode LIT_HEXNUMBER_1() { return getToken(AsZ80Parser.LIT_HEXNUMBER_1, 0); }
		public ExprHex1Context(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprHex1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprHex1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprHex1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprHex2Context extends RExpressionContext {
		public Token num;
		public TerminalNode LIT_HEXNUMBER_2() { return getToken(AsZ80Parser.LIT_HEXNUMBER_2, 0); }
		public ExprHex2Context(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprHex2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprHex2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprHex2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprDecContext extends RExpressionContext {
		public Token num;
		public TerminalNode LIT_NUMBER() { return getToken(AsZ80Parser.LIT_NUMBER, 0); }
		public ExprDecContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprDec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprDec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprDec(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprStringContext extends RExpressionContext {
		public Token str;
		public TerminalNode LIT_STRING_1() { return getToken(AsZ80Parser.LIT_STRING_1, 0); }
		public TerminalNode LIT_STRING_2() { return getToken(AsZ80Parser.LIT_STRING_2, 0); }
		public ExprStringContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprString(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprBinContext extends RExpressionContext {
		public Token num;
		public TerminalNode LIT_BINNUMBER() { return getToken(AsZ80Parser.LIT_BINNUMBER, 0); }
		public ExprBinContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprBin(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprBin(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprBin(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprParensContext extends RExpressionContext {
		public RExpressionContext expr;
		public TerminalNode SEP_LPAR() { return getToken(AsZ80Parser.SEP_LPAR, 0); }
		public TerminalNode SEP_RPAR() { return getToken(AsZ80Parser.SEP_RPAR, 0); }
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public ExprParensContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprParens(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprParens(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprIdContext extends RExpressionContext {
		public Token id;
		public TerminalNode ID_IDENTIFIER() { return getToken(AsZ80Parser.ID_IDENTIFIER, 0); }
		public ExprIdContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprId(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprUnaryContext extends RExpressionContext {
		public Token unaryop;
		public RExpressionContext expr;
		public RExpressionContext rExpression() {
			return getRuleContext(RExpressionContext.class,0);
		}
		public TerminalNode OP_ADD() { return getToken(AsZ80Parser.OP_ADD, 0); }
		public TerminalNode OP_SUBTRACT() { return getToken(AsZ80Parser.OP_SUBTRACT, 0); }
		public TerminalNode OP_NOT() { return getToken(AsZ80Parser.OP_NOT, 0); }
		public TerminalNode OP_NOT_2() { return getToken(AsZ80Parser.OP_NOT_2, 0); }
		public ExprUnaryContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprUnary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprUnary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprUnary(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprInfixContext extends RExpressionContext {
		public RExpressionContext expr1;
		public Token op;
		public RExpressionContext expr2;
		public List<RExpressionContext> rExpression() {
			return getRuleContexts(RExpressionContext.class);
		}
		public RExpressionContext rExpression(int i) {
			return getRuleContext(RExpressionContext.class,i);
		}
		public TerminalNode OP_MULTIPLY() { return getToken(AsZ80Parser.OP_MULTIPLY, 0); }
		public TerminalNode OP_DIVIDE() { return getToken(AsZ80Parser.OP_DIVIDE, 0); }
		public TerminalNode OP_MOD() { return getToken(AsZ80Parser.OP_MOD, 0); }
		public TerminalNode OP_MOD_2() { return getToken(AsZ80Parser.OP_MOD_2, 0); }
		public TerminalNode OP_ADD() { return getToken(AsZ80Parser.OP_ADD, 0); }
		public TerminalNode OP_SUBTRACT() { return getToken(AsZ80Parser.OP_SUBTRACT, 0); }
		public TerminalNode OP_SHL() { return getToken(AsZ80Parser.OP_SHL, 0); }
		public TerminalNode OP_SHR() { return getToken(AsZ80Parser.OP_SHR, 0); }
		public TerminalNode OP_SHR_2() { return getToken(AsZ80Parser.OP_SHR_2, 0); }
		public TerminalNode OP_SHL_2() { return getToken(AsZ80Parser.OP_SHL_2, 0); }
		public TerminalNode OP_GT() { return getToken(AsZ80Parser.OP_GT, 0); }
		public TerminalNode OP_GTE() { return getToken(AsZ80Parser.OP_GTE, 0); }
		public TerminalNode OP_LT() { return getToken(AsZ80Parser.OP_LT, 0); }
		public TerminalNode OP_LTE() { return getToken(AsZ80Parser.OP_LTE, 0); }
		public TerminalNode OP_EQUAL() { return getToken(AsZ80Parser.OP_EQUAL, 0); }
		public TerminalNode OP_AND() { return getToken(AsZ80Parser.OP_AND, 0); }
		public TerminalNode OP_AND_2() { return getToken(AsZ80Parser.OP_AND_2, 0); }
		public TerminalNode OP_XOR() { return getToken(AsZ80Parser.OP_XOR, 0); }
		public TerminalNode OP_XOR_2() { return getToken(AsZ80Parser.OP_XOR_2, 0); }
		public TerminalNode OP_OR() { return getToken(AsZ80Parser.OP_OR, 0); }
		public TerminalNode OP_OR_2() { return getToken(AsZ80Parser.OP_OR_2, 0); }
		public ExprInfixContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprInfix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprInfix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprInfix(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExprCurrentAddressContext extends RExpressionContext {
		public TerminalNode PREP_ADDR() { return getToken(AsZ80Parser.PREP_ADDR, 0); }
		public ExprCurrentAddressContext(RExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).enterExprCurrentAddress(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AsZ80ParserListener ) ((AsZ80ParserListener)listener).exitExprCurrentAddress(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AsZ80ParserVisitor ) return ((AsZ80ParserVisitor<? extends T>)visitor).visitExprCurrentAddress(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RExpressionContext rExpression() throws RecognitionException {
		return rExpression(0);
	}

	private RExpressionContext rExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		RExpressionContext _localctx = new RExpressionContext(_ctx, _parentState);
		RExpressionContext _prevctx = _localctx;
		int _startState = 26;
		enterRecursionRule(_localctx, 26, RULE_rExpression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(411);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OP_NOT:
			case OP_ADD:
			case OP_SUBTRACT:
			case OP_NOT_2:
				{
				_localctx = new ExprUnaryContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(397);
				((ExprUnaryContext)_localctx).unaryop = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((((_la - 118)) & ~0x3f) == 0 && ((1L << (_la - 118)) & ((1L << (OP_NOT - 118)) | (1L << (OP_ADD - 118)) | (1L << (OP_SUBTRACT - 118)) | (1L << (OP_NOT_2 - 118)))) != 0)) ) {
					((ExprUnaryContext)_localctx).unaryop = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(398);
				((ExprUnaryContext)_localctx).expr = rExpression(18);
				}
				break;
			case SEP_LPAR:
				{
				_localctx = new ExprParensContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(399);
				match(SEP_LPAR);
				setState(400);
				((ExprParensContext)_localctx).expr = rExpression(0);
				setState(401);
				match(SEP_RPAR);
				}
				break;
			case LIT_NUMBER:
				{
				_localctx = new ExprDecContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(403);
				((ExprDecContext)_localctx).num = match(LIT_NUMBER);
				}
				break;
			case LIT_HEXNUMBER_1:
				{
				_localctx = new ExprHex1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(404);
				((ExprHex1Context)_localctx).num = match(LIT_HEXNUMBER_1);
				}
				break;
			case LIT_HEXNUMBER_2:
				{
				_localctx = new ExprHex2Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(405);
				((ExprHex2Context)_localctx).num = match(LIT_HEXNUMBER_2);
				}
				break;
			case LIT_OCTNUMBER:
				{
				_localctx = new ExprOctContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(406);
				((ExprOctContext)_localctx).num = match(LIT_OCTNUMBER);
				}
				break;
			case LIT_BINNUMBER:
				{
				_localctx = new ExprBinContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(407);
				((ExprBinContext)_localctx).num = match(LIT_BINNUMBER);
				}
				break;
			case PREP_ADDR:
				{
				_localctx = new ExprCurrentAddressContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(408);
				match(PREP_ADDR);
				}
				break;
			case ID_IDENTIFIER:
				{
				_localctx = new ExprIdContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(409);
				((ExprIdContext)_localctx).id = match(ID_IDENTIFIER);
				}
				break;
			case LIT_STRING_1:
			case LIT_STRING_2:
				{
				_localctx = new ExprStringContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(410);
				((ExprStringContext)_localctx).str = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==LIT_STRING_1 || _la==LIT_STRING_2) ) {
					((ExprStringContext)_localctx).str = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(439);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(437);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
					case 1:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(413);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(414);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 115)) & ~0x3f) == 0 && ((1L << (_la - 115)) & ((1L << (OP_MOD - 115)) | (1L << (OP_MULTIPLY - 115)) | (1L << (OP_DIVIDE - 115)) | (1L << (OP_MOD_2 - 115)))) != 0)) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(415);
						((ExprInfixContext)_localctx).expr2 = rExpression(18);
						}
						break;
					case 2:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(416);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(417);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==OP_ADD || _la==OP_SUBTRACT) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(418);
						((ExprInfixContext)_localctx).expr2 = rExpression(17);
						}
						break;
					case 3:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(419);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(420);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 116)) & ~0x3f) == 0 && ((1L << (_la - 116)) & ((1L << (OP_SHR - 116)) | (1L << (OP_SHL - 116)) | (1L << (OP_SHR_2 - 116)) | (1L << (OP_SHL_2 - 116)))) != 0)) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(421);
						((ExprInfixContext)_localctx).expr2 = rExpression(16);
						}
						break;
					case 4:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(422);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(423);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(((((_la - 140)) & ~0x3f) == 0 && ((1L << (_la - 140)) & ((1L << (OP_GT - 140)) | (1L << (OP_GTE - 140)) | (1L << (OP_LT - 140)) | (1L << (OP_LTE - 140)))) != 0)) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(424);
						((ExprInfixContext)_localctx).expr2 = rExpression(14);
						}
						break;
					case 5:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(425);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(426);
						((ExprInfixContext)_localctx).op = match(OP_EQUAL);
						setState(427);
						((ExprInfixContext)_localctx).expr2 = rExpression(13);
						}
						break;
					case 6:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(428);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(429);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==OP_AND || _la==OP_AND_2) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(430);
						((ExprInfixContext)_localctx).expr2 = rExpression(13);
						}
						break;
					case 7:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(431);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(432);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==OP_XOR || _la==OP_XOR_2) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(433);
						((ExprInfixContext)_localctx).expr2 = rExpression(12);
						}
						break;
					case 8:
						{
						_localctx = new ExprInfixContext(new RExpressionContext(_parentctx, _parentState));
						((ExprInfixContext)_localctx).expr1 = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_rExpression);
						setState(434);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(435);
						((ExprInfixContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==OP_OR || _la==OP_OR_2) ) {
							((ExprInfixContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(436);
						((ExprInfixContext)_localctx).expr2 = rExpression(11);
						}
						break;
					}
					} 
				}
				setState(441);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 13:
			return rExpression_sempred((RExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean rExpression_sempred(RExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 17);
		case 1:
			return precpred(_ctx, 16);
		case 2:
			return precpred(_ctx, 15);
		case 3:
			return precpred(_ctx, 14);
		case 4:
			return precpred(_ctx, 13);
		case 5:
			return precpred(_ctx, 12);
		case 6:
			return precpred(_ctx, 11);
		case 7:
			return precpred(_ctx, 10);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\u009a\u01bd\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\3\2\7\2 \n\2\f\2\16\2#\13\2"+
		"\3\2\5\2&\n\2\3\2\6\2)\n\2\r\2\16\2*\3\2\7\2.\n\2\f\2\16\2\61\13\2\3\2"+
		"\7\2\64\n\2\f\2\16\2\67\13\2\3\2\3\2\3\3\5\3<\n\3\3\3\7\3?\n\3\f\3\16"+
		"\3B\13\3\3\3\3\3\5\3F\n\3\3\4\3\4\3\4\5\4K\n\4\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5Z\n\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5}\n\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3"+
		"\5\3\5\5\5\u00ab\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u0108\n\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u011b\n\6\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u0127\n\7\3\b\3\b\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\7\t\u0139\n\t\f\t\16\t\u013c\13"+
		"\t\3\t\7\t\u013f\n\t\f\t\16\t\u0142\13\t\3\t\3\t\3\t\3\t\3\t\5\t\u0149"+
		"\n\t\3\t\3\t\3\t\3\t\7\t\u014f\n\t\f\t\16\t\u0152\13\t\3\t\7\t\u0155\n"+
		"\t\f\t\16\t\u0158\13\t\3\t\3\t\3\t\5\t\u015d\n\t\3\t\3\t\5\t\u0161\n\t"+
		"\3\n\3\n\3\n\7\n\u0166\n\n\f\n\16\n\u0169\13\n\3\13\3\13\3\13\7\13\u016e"+
		"\n\13\f\13\16\13\u0171\13\13\3\f\3\f\3\f\3\f\7\f\u0177\n\f\f\f\16\f\u017a"+
		"\13\f\3\f\3\f\3\f\3\f\7\f\u0180\n\f\f\f\16\f\u0183\13\f\3\f\3\f\5\f\u0187"+
		"\n\f\3\r\3\r\5\r\u018b\n\r\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\5\17\u019e\n\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\7\17\u01b8\n\17\f\17\16\17\u01bb\13"+
		"\17\3\17\2\3\34\20\2\4\6\b\n\f\16\20\22\24\26\30\32\34\2\20\3\2mp\3\2"+
		"IL\3\2mn\4\2moqq\3\2IP\3\2\u0081\u0082\5\2xx\u0089\u008a\u0095\u0095\5"+
		"\2uu\u008b\u008c\u0092\u0092\3\2\u0089\u008a\4\2vw\u0093\u0094\3\2\u008e"+
		"\u0091\4\2yy\u0096\u0096\4\2{{\u0098\u0098\4\2zz\u0097\u0097\2\u0222\2"+
		"!\3\2\2\2\4E\3\2\2\2\6J\3\2\2\2\b\u00aa\3\2\2\2\n\u011a\3\2\2\2\f\u0126"+
		"\3\2\2\2\16\u0128\3\2\2\2\20\u0160\3\2\2\2\22\u0162\3\2\2\2\24\u016a\3"+
		"\2\2\2\26\u0186\3\2\2\2\30\u018a\3\2\2\2\32\u018c\3\2\2\2\34\u019d\3\2"+
		"\2\2\36 \7\u009a\2\2\37\36\3\2\2\2 #\3\2\2\2!\37\3\2\2\2!\"\3\2\2\2\""+
		"%\3\2\2\2#!\3\2\2\2$&\5\4\3\2%$\3\2\2\2%&\3\2\2\2&/\3\2\2\2\')\7\u009a"+
		"\2\2(\'\3\2\2\2)*\3\2\2\2*(\3\2\2\2*+\3\2\2\2+,\3\2\2\2,.\5\4\3\2-(\3"+
		"\2\2\2.\61\3\2\2\2/-\3\2\2\2/\60\3\2\2\2\60\65\3\2\2\2\61/\3\2\2\2\62"+
		"\64\7\u009a\2\2\63\62\3\2\2\2\64\67\3\2\2\2\65\63\3\2\2\2\65\66\3\2\2"+
		"\2\668\3\2\2\2\67\65\3\2\2\289\7\2\2\39\3\3\2\2\2:<\7\u0084\2\2;:\3\2"+
		"\2\2;<\3\2\2\2<@\3\2\2\2=?\7\u009a\2\2>=\3\2\2\2?B\3\2\2\2@>\3\2\2\2@"+
		"A\3\2\2\2AC\3\2\2\2B@\3\2\2\2CF\5\6\4\2DF\7\u0084\2\2E;\3\2\2\2ED\3\2"+
		"\2\2F\5\3\2\2\2GK\5\b\5\2HK\5\20\t\2IK\5\26\f\2JG\3\2\2\2JH\3\2\2\2JI"+
		"\3\2\2\2K\7\3\2\2\2L\u00ab\5\n\6\2MN\7\"\2\2NO\t\2\2\2OP\7\u0088\2\2P"+
		"\u00ab\5\34\17\2QR\7\"\2\2RS\5\f\7\2ST\7\u0088\2\2TU\5\34\17\2U\u00ab"+
		"\3\2\2\2VY\7!\2\2WX\t\3\2\2XZ\7\u0088\2\2YW\3\2\2\2YZ\3\2\2\2Z[\3\2\2"+
		"\2[\u00ab\5\34\17\2\\]\7\"\2\2]^\7\u0086\2\2^_\5\34\17\2_`\7\u0087\2\2"+
		"`a\7\u0088\2\2ab\7o\2\2b\u00ab\3\2\2\2cd\7\"\2\2de\7o\2\2ef\7\u0088\2"+
		"\2fg\7\u0086\2\2gh\5\34\17\2hi\7\u0087\2\2i\u00ab\3\2\2\2jk\7\"\2\2kl"+
		"\7\u0086\2\2lm\5\34\17\2mn\7\u0087\2\2no\7\u0088\2\2op\7`\2\2p\u00ab\3"+
		"\2\2\2qr\7\"\2\2rs\7`\2\2st\7\u0088\2\2tu\7\u0086\2\2uv\5\34\17\2vw\7"+
		"\u0087\2\2w\u00ab\3\2\2\2x|\7 \2\2yz\5\16\b\2z{\7\u0088\2\2{}\3\2\2\2"+
		"|y\3\2\2\2|}\3\2\2\2}~\3\2\2\2~\u00ab\5\34\17\2\177\u0080\7\t\2\2\u0080"+
		"\u00ab\5\34\17\2\u0081\u0082\7\t\2\2\u0082\u0083\5\16\b\2\u0083\u0084"+
		"\7\u0088\2\2\u0084\u0085\5\34\17\2\u0085\u00ab\3\2\2\2\u0086\u0087\7\6"+
		"\2\2\u0087\u0088\7`\2\2\u0088\u0089\7\u0088\2\2\u0089\u00ab\5\34\17\2"+
		"\u008a\u008b\7\5\2\2\u008b\u008c\7`\2\2\u008c\u008d\7\u0088\2\2\u008d"+
		"\u00ab\5\34\17\2\u008e\u008f\7,\2\2\u008f\u0090\7\u0086\2\2\u0090\u0091"+
		"\5\34\17\2\u0091\u0092\7\u0087\2\2\u0092\u0093\7\u0088\2\2\u0093\u0094"+
		"\7`\2\2\u0094\u00ab\3\2\2\2\u0095\u0096\7G\2\2\u0096\u00ab\5\34\17\2\u0097"+
		"\u0098\7\32\2\2\u0098\u0099\7`\2\2\u0099\u009a\7\u0088\2\2\u009a\u009b"+
		"\7\u0086\2\2\u009b\u009c\5\34\17\2\u009c\u009d\7\u0087\2\2\u009d\u00ab"+
		"\3\2\2\2\u009e\u009f\7@\2\2\u009f\u00a0\7`\2\2\u00a0\u00a1\7\u0088\2\2"+
		"\u00a1\u00ab\5\34\17\2\u00a2\u00a3\7\7\2\2\u00a3\u00ab\5\34\17\2\u00a4"+
		"\u00a5\7H\2\2\u00a5\u00ab\5\34\17\2\u00a6\u00a7\7)\2\2\u00a7\u00ab\5\34"+
		"\17\2\u00a8\u00a9\7\13\2\2\u00a9\u00ab\5\34\17\2\u00aaL\3\2\2\2\u00aa"+
		"M\3\2\2\2\u00aaQ\3\2\2\2\u00aaV\3\2\2\2\u00aa\\\3\2\2\2\u00aac\3\2\2\2"+
		"\u00aaj\3\2\2\2\u00aaq\3\2\2\2\u00aax\3\2\2\2\u00aa\177\3\2\2\2\u00aa"+
		"\u0081\3\2\2\2\u00aa\u0086\3\2\2\2\u00aa\u008a\3\2\2\2\u00aa\u008e\3\2"+
		"\2\2\u00aa\u0095\3\2\2\2\u00aa\u0097\3\2\2\2\u00aa\u009e\3\2\2\2\u00aa"+
		"\u00a2\3\2\2\2\u00aa\u00a4\3\2\2\2\u00aa\u00a6\3\2\2\2\u00aa\u00a8\3\2"+
		"\2\2\u00ab\t\3\2\2\2\u00ac\u011b\7(\2\2\u00ad\u00ae\7\"\2\2\u00ae\u00af"+
		"\7\u0086\2\2\u00af\u00b0\t\4\2\2\u00b0\u00b1\7\u0087\2\2\u00b1\u00b2\7"+
		"\u0088\2\2\u00b2\u011b\7`\2\2\u00b3\u00b4\7\"\2\2\u00b4\u00b5\7\u0086"+
		"\2\2\u00b5\u00b6\7o\2\2\u00b6\u00b7\7\u0087\2\2\u00b7\u00b8\7\u0088\2"+
		"\2\u00b8\u011b\5\f\7\2\u00b9\u00ba\7\33\2\2\u00ba\u011b\t\2\2\2\u00bb"+
		"\u00bc\7\33\2\2\u00bc\u011b\5\f\7\2\u00bd\u00be\7\22\2\2\u00be\u011b\5"+
		"\f\7\2\u00bf\u011b\78\2\2\u00c0\u00c1\7\26\2\2\u00c1\u00c2\7q\2\2\u00c2"+
		"\u00c3\7\u0088\2\2\u00c3\u011b\7r\2\2\u00c4\u00c5\7\26\2\2\u00c5\u00c6"+
		"\7n\2\2\u00c6\u00c7\7\u0088\2\2\u00c7\u011b\7o\2\2\u00c8\u00c9\7\26\2"+
		"\2\u00c9\u00ca\7\u0086\2\2\u00ca\u00cb\7p\2\2\u00cb\u00cc\7\u0087\2\2"+
		"\u00cc\u00cd\7\u0088\2\2\u00cd\u011b\7o\2\2\u00ce\u00cf\7\6\2\2\u00cf"+
		"\u00d0\7o\2\2\u00d0\u00d1\7\u0088\2\2\u00d1\u011b\t\2\2\2\u00d2\u00d3"+
		"\7\"\2\2\u00d3\u00d4\7`\2\2\u00d4\u00d5\7\u0088\2\2\u00d5\u00d6\7\u0086"+
		"\2\2\u00d6\u00d7\t\4\2\2\u00d7\u011b\7\u0087\2\2\u00d8\u00d9\7\22\2\2"+
		"\u00d9\u011b\t\2\2\2\u00da\u011b\7=\2\2\u00db\u011b\7\66\2\2\u00dc\u011b"+
		"\7;\2\2\u00dd\u011b\7\21\2\2\u00de\u011b\7\20\2\2\u00df\u00e0\7\33\2\2"+
		"\u00e0\u00e1\7\u0086\2\2\u00e1\u00e2\7o\2\2\u00e2\u011b\7\u0087\2\2\u00e3"+
		"\u00e4\7\22\2\2\u00e4\u00e5\7\u0086\2\2\u00e5\u00e6\7o\2\2\u00e6\u011b"+
		"\7\u0087\2\2\u00e7\u011b\7A\2\2\u00e8\u011b\7\n\2\2\u00e9\u00ea\7\"\2"+
		"\2\u00ea\u00eb\5\f\7\2\u00eb\u00ec\7\u0088\2\2\u00ec\u00ed\5\f\7\2\u00ed"+
		"\u011b\3\2\2\2\u00ee\u011b\7\30\2\2\u00ef\u00f0\7\6\2\2\u00f0\u00f1\7"+
		"`\2\2\u00f1\u00f2\7\u0088\2\2\u00f2\u011b\5\f\7\2\u00f3\u00f4\7\5\2\2"+
		"\u00f4\u00f5\7`\2\2\u00f5\u00f6\7\u0088\2\2\u00f6\u011b\5\f\7\2\u00f7"+
		"\u00f8\7G\2\2\u00f8\u011b\5\f\7\2\u00f9\u00fa\7@\2\2\u00fa\u00fb\7`\2"+
		"\2\u00fb\u00fc\7\u0088\2\2\u00fc\u011b\5\f\7\2\u00fd\u00fe\7\7\2\2\u00fe"+
		"\u011b\5\f\7\2\u00ff\u0100\7H\2\2\u0100\u011b\5\f\7\2\u0101\u0102\7)\2"+
		"\2\u0102\u011b\5\f\7\2\u0103\u0104\7\13\2\2\u0104\u011b\5\f\7\2\u0105"+
		"\u0107\7\62\2\2\u0106\u0108\5\16\b\2\u0107\u0106\3\2\2\2\u0107\u0108\3"+
		"\2\2\2\u0108\u011b\3\2\2\2\u0109\u010a\7/\2\2\u010a\u011b\t\5\2\2\u010b"+
		"\u010c\7\60\2\2\u010c\u011b\t\5\2\2\u010d\u010e\7?\2\2\u010e\u011b\5\34"+
		"\17\2\u010f\u011b\7\27\2\2\u0110\u0111\7 \2\2\u0111\u0112\7\u0086\2\2"+
		"\u0112\u0113\7o\2\2\u0113\u011b\7\u0087\2\2\u0114\u011b\7\23\2\2\u0115"+
		"\u0116\7\"\2\2\u0116\u0117\7p\2\2\u0117\u0118\7\u0088\2\2\u0118\u011b"+
		"\7o\2\2\u0119\u011b\7\25\2\2\u011a\u00ac\3\2\2\2\u011a\u00ad\3\2\2\2\u011a"+
		"\u00b3\3\2\2\2\u011a\u00b9\3\2\2\2\u011a\u00bb\3\2\2\2\u011a\u00bd\3\2"+
		"\2\2\u011a\u00bf\3\2\2\2\u011a\u00c0\3\2\2\2\u011a\u00c4\3\2\2\2\u011a"+
		"\u00c8\3\2\2\2\u011a\u00ce\3\2\2\2\u011a\u00d2\3\2\2\2\u011a\u00d8\3\2"+
		"\2\2\u011a\u00da\3\2\2\2\u011a\u00db\3\2\2\2\u011a\u00dc\3\2\2\2\u011a"+
		"\u00dd\3\2\2\2\u011a\u00de\3\2\2\2\u011a\u00df\3\2\2\2\u011a\u00e3\3\2"+
		"\2\2\u011a\u00e7\3\2\2\2\u011a\u00e8\3\2\2\2\u011a\u00e9\3\2\2\2\u011a"+
		"\u00ee\3\2\2\2\u011a\u00ef\3\2\2\2\u011a\u00f3\3\2\2\2\u011a\u00f7\3\2"+
		"\2\2\u011a\u00f9\3\2\2\2\u011a\u00fd\3\2\2\2\u011a\u00ff\3\2\2\2\u011a"+
		"\u0101\3\2\2\2\u011a\u0103\3\2\2\2\u011a\u0105\3\2\2\2\u011a\u0109\3\2"+
		"\2\2\u011a\u010b\3\2\2\2\u011a\u010d\3\2\2\2\u011a\u010f\3\2\2\2\u011a"+
		"\u0110\3\2\2\2\u011a\u0114\3\2\2\2\u011a\u0115\3\2\2\2\u011a\u0119\3\2"+
		"\2\2\u011b\13\3\2\2\2\u011c\u0127\7`\2\2\u011d\u0127\7a\2\2\u011e\u0127"+
		"\7b\2\2\u011f\u0127\7c\2\2\u0120\u0127\7d\2\2\u0121\u0127\7e\2\2\u0122"+
		"\u0127\7f\2\2\u0123\u0124\7\u0086\2\2\u0124\u0125\7o\2\2\u0125\u0127\7"+
		"\u0087\2\2\u0126\u011c\3\2\2\2\u0126\u011d\3\2\2\2\u0126\u011e\3\2\2\2"+
		"\u0126\u011f\3\2\2\2\u0126\u0120\3\2\2\2\u0126\u0121\3\2\2\2\u0126\u0122"+
		"\3\2\2\2\u0126\u0123\3\2\2\2\u0127\r\3\2\2\2\u0128\u0129\t\6\2\2\u0129"+
		"\17\3\2\2\2\u012a\u012b\7S\2\2\u012b\u0161\5\34\17\2\u012c\u012d\7\u0083"+
		"\2\2\u012d\u012e\7T\2\2\u012e\u0161\5\34\17\2\u012f\u0130\7\u0083\2\2"+
		"\u0130\u0131\7U\2\2\u0131\u0161\5\34\17\2\u0132\u0133\7W\2\2\u0133\u0134"+
		"\5\34\17\2\u0134\u013a\7\u009a\2\2\u0135\u0136\5\4\3\2\u0136\u0137\7\u009a"+
		"\2\2\u0137\u0139\3\2\2\2\u0138\u0135\3\2\2\2\u0139\u013c\3\2\2\2\u013a"+
		"\u0138\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u0140\3\2\2\2\u013c\u013a\3\2"+
		"\2\2\u013d\u013f\7\u009a\2\2\u013e\u013d\3\2\2\2\u013f\u0142\3\2\2\2\u0140"+
		"\u013e\3\2\2\2\u0140\u0141\3\2\2\2\u0141\u0143\3\2\2\2\u0142\u0140\3\2"+
		"\2\2\u0143\u0144\7X\2\2\u0144\u0161\3\2\2\2\u0145\u0146\7\u0083\2\2\u0146"+
		"\u0148\7Z\2\2\u0147\u0149\5\22\n\2\u0148\u0147\3\2\2\2\u0148\u0149\3\2"+
		"\2\2\u0149\u014a\3\2\2\2\u014a\u0150\7\u009a\2\2\u014b\u014c\5\4\3\2\u014c"+
		"\u014d\7\u009a\2\2\u014d\u014f\3\2\2\2\u014e\u014b\3\2\2\2\u014f\u0152"+
		"\3\2\2\2\u0150\u014e\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u0156\3\2\2\2\u0152"+
		"\u0150\3\2\2\2\u0153\u0155\7\u009a\2\2\u0154\u0153\3\2\2\2\u0155\u0158"+
		"\3\2\2\2\u0156\u0154\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0159\3\2\2\2\u0158"+
		"\u0156\3\2\2\2\u0159\u0161\7[\2\2\u015a\u015c\7\u0083\2\2\u015b\u015d"+
		"\5\24\13\2\u015c\u015b\3\2\2\2\u015c\u015d\3\2\2\2\u015d\u0161\3\2\2\2"+
		"\u015e\u015f\7Y\2\2\u015f\u0161\t\7\2\2\u0160\u012a\3\2\2\2\u0160\u012c"+
		"\3\2\2\2\u0160\u012f\3\2\2\2\u0160\u0132\3\2\2\2\u0160\u0145\3\2\2\2\u0160"+
		"\u015a\3\2\2\2\u0160\u015e\3\2\2\2\u0161\21\3\2\2\2\u0162\u0167\7\u0083"+
		"\2\2\u0163\u0164\7\u0088\2\2\u0164\u0166\7\u0083\2\2\u0165\u0163\3\2\2"+
		"\2\u0166\u0169\3\2\2\2\u0167\u0165\3\2\2\2\u0167\u0168\3\2\2\2\u0168\23"+
		"\3\2\2\2\u0169\u0167\3\2\2\2\u016a\u016f\5\34\17\2\u016b\u016c\7\u0088"+
		"\2\2\u016c\u016e\5\34\17\2\u016d\u016b\3\2\2\2\u016e\u0171\3\2\2\2\u016f"+
		"\u016d\3\2\2\2\u016f\u0170\3\2\2\2\u0170\25\3\2\2\2\u0171\u016f\3\2\2"+
		"\2\u0172\u0173\7\\\2\2\u0173\u0178\5\30\r\2\u0174\u0175\7\u0088\2\2\u0175"+
		"\u0177\5\30\r\2\u0176\u0174\3\2\2\2\u0177\u017a\3\2\2\2\u0178\u0176\3"+
		"\2\2\2\u0178\u0179\3\2\2\2\u0179\u0187\3\2\2\2\u017a\u0178\3\2\2\2\u017b"+
		"\u017c\7]\2\2\u017c\u0181\5\32\16\2\u017d\u017e\7\u0088\2\2\u017e\u0180"+
		"\5\32\16\2\u017f\u017d\3\2\2\2\u0180\u0183\3\2\2\2\u0181\u017f\3\2\2\2"+
		"\u0181\u0182\3\2\2\2\u0182\u0187\3\2\2\2\u0183\u0181\3\2\2\2\u0184\u0185"+
		"\7^\2\2\u0185\u0187\5\34\17\2\u0186\u0172\3\2\2\2\u0186\u017b\3\2\2\2"+
		"\u0186\u0184\3\2\2\2\u0187\27\3\2\2\2\u0188\u018b\5\34\17\2\u0189\u018b"+
		"\5\n\6\2\u018a\u0188\3\2\2\2\u018a\u0189\3\2\2\2\u018b\31\3\2\2\2\u018c"+
		"\u018d\5\34\17\2\u018d\33\3\2\2\2\u018e\u018f\b\17\1\2\u018f\u0190\t\b"+
		"\2\2\u0190\u019e\5\34\17\24\u0191\u0192\7\u0086\2\2\u0192\u0193\5\34\17"+
		"\2\u0193\u0194\7\u0087\2\2\u0194\u019e\3\2\2\2\u0195\u019e\7}\2\2\u0196"+
		"\u019e\7|\2\2\u0197\u019e\7~\2\2\u0198\u019e\7\177\2\2\u0199\u019e\7\u0080"+
		"\2\2\u019a\u019e\7_\2\2\u019b\u019e\7\u0083\2\2\u019c\u019e\t\7\2\2\u019d"+
		"\u018e\3\2\2\2\u019d\u0191\3\2\2\2\u019d\u0195\3\2\2\2\u019d\u0196\3\2"+
		"\2\2\u019d\u0197\3\2\2\2\u019d\u0198\3\2\2\2\u019d\u0199\3\2\2\2\u019d"+
		"\u019a\3\2\2\2\u019d\u019b\3\2\2\2\u019d\u019c\3\2\2\2\u019e\u01b9\3\2"+
		"\2\2\u019f\u01a0\f\23\2\2\u01a0\u01a1\t\t\2\2\u01a1\u01b8\5\34\17\24\u01a2"+
		"\u01a3\f\22\2\2\u01a3\u01a4\t\n\2\2\u01a4\u01b8\5\34\17\23\u01a5\u01a6"+
		"\f\21\2\2\u01a6\u01a7\t\13\2\2\u01a7\u01b8\5\34\17\22\u01a8\u01a9\f\20"+
		"\2\2\u01a9\u01aa\t\f\2\2\u01aa\u01b8\5\34\17\20\u01ab\u01ac\f\17\2\2\u01ac"+
		"\u01ad\7\u008d\2\2\u01ad\u01b8\5\34\17\17\u01ae\u01af\f\16\2\2\u01af\u01b0"+
		"\t\r\2\2\u01b0\u01b8\5\34\17\17\u01b1\u01b2\f\r\2\2\u01b2\u01b3\t\16\2"+
		"\2\u01b3\u01b8\5\34\17\16\u01b4\u01b5\f\f\2\2\u01b5\u01b6\t\17\2\2\u01b6"+
		"\u01b8\5\34\17\r\u01b7\u019f\3\2\2\2\u01b7\u01a2\3\2\2\2\u01b7\u01a5\3"+
		"\2\2\2\u01b7\u01a8\3\2\2\2\u01b7\u01ab\3\2\2\2\u01b7\u01ae\3\2\2\2\u01b7"+
		"\u01b1\3\2\2\2\u01b7\u01b4\3\2\2\2\u01b8\u01bb\3\2\2\2\u01b9\u01b7\3\2"+
		"\2\2\u01b9\u01ba\3\2\2\2\u01ba\35\3\2\2\2\u01bb\u01b9\3\2\2\2!!%*/\65"+
		";@EJY|\u00aa\u0107\u011a\u0126\u013a\u0140\u0148\u0150\u0156\u015c\u0160"+
		"\u0167\u016f\u0178\u0181\u0186\u018a\u019d\u01b7\u01b9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}