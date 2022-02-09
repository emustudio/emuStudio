// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-z80/src/main/antlr/AsZ80Lexer.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AsZ80Lexer extends Lexer {
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
		PREP_IF=84, PREP_ENDIF=85, PREP_INCLUDE=86, PREP_MACRO=87, PREP_ENDM=88, 
		PREP_DB=89, PREP_DW=90, PREP_DS=91, PREP_ADDR=92, REG_A=93, REG_B=94, 
		REG_C=95, REG_D=96, REG_E=97, REG_H=98, REG_L=99, REG_IX=100, REG_IXH=101, 
		REG_IXL=102, REG_IY=103, REG_IYH=104, REG_IYL=105, REG_BC=106, REG_DE=107, 
		REG_HL=108, REG_SP=109, REG_AF=110, REG_AFF=111, REG_I=112, REG_R=113, 
		OP_MOD=114, OP_SHR=115, OP_SHL=116, OP_NOT=117, OP_AND=118, OP_OR=119, 
		OP_XOR=120, LIT_HEXNUMBER_1=121, LIT_NUMBER=122, LIT_HEXNUMBER_2=123, 
		LIT_OCTNUMBER=124, LIT_BINNUMBER=125, LIT_STRING_1=126, LIT_STRING_2=127, 
		ID_IDENTIFIER=128, ID_LABEL=129, ERROR=130, SEP_LPAR=131, SEP_RPAR=132, 
		SEP_COMMA=133, OP_ADD=134, OP_SUBTRACT=135, OP_MULTIPLY=136, OP_DIVIDE=137, 
		OP_EQUAL=138, OP_GT=139, OP_GTE=140, OP_LT=141, OP_LTE=142, OP_MOD_2=143, 
		OP_SHR_2=144, OP_SHL_2=145, OP_NOT_2=146, OP_AND_2=147, OP_OR_2=148, OP_XOR_2=149, 
		WS=150, EOL=151;
	public static final int
		CONDITION=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "CONDITION"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"COMMENT", "COMMENT2", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", 
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", 
			"Z", "OPCODE_ADC", "OPCODE_ADD", "OPCODE_AND", "OPCODE_BIT", "OPCODE_CALL", 
			"OPCODE_CCF", "OPCODE_CP", "OPCODE_CPD", "OPCODE_CPDR", "OPCODE_CPI", 
			"OPCODE_CPIR", "OPCODE_CPL", "OPCODE_DAA", "OPCODE_DEC", "OPCODE_DI", 
			"OPCODE_DJNZ", "OPCODE_EI", "OPCODE_EX", "OPCODE_EXX", "OPCODE_HALT", 
			"OPCODE_IM", "OPCODE_IN", "OPCODE_INC", "OPCODE_IND", "OPCODE_INDR", 
			"OPCODE_INI", "OPCODE_INIR", "OPCODE_JP", "OPCODE_JR", "OPCODE_LD", "OPCODE_LDD", 
			"OPCODE_LDDR", "OPCODE_LDI", "OPCODE_LDIR", "OPCODE_NEG", "OPCODE_NOP", 
			"OPCODE_OR", "OPCODE_OTDR", "OPCODE_OTIR", "OPCODE_OUT", "OPCODE_OUTD", 
			"OPCODE_OUTI", "OPCODE_POP", "OPCODE_PUSH", "OPCODE_RES", "OPCODE_RET", 
			"OPCODE_RETI", "OPCODE_RETN", "OPCODE_RL", "OPCODE_RLA", "OPCODE_RLC", 
			"OPCODE_RLCA", "OPCODE_RLD", "OPCODE_RR", "OPCODE_RRA", "OPCODE_RRC", 
			"OPCODE_RRCA", "OPCODE_RRD", "OPCODE_RST", "OPCODE_SBC", "OPCODE_SCF", 
			"OPCODE_SET", "OPCODE_SLA", "OPCODE_SRA", "OPCODE_SLL", "OPCODE_SRL", 
			"OPCODE_SUB", "OPCODE_XOR", "COND_C", "COND_NC", "COND_Z", "COND_NZ", 
			"COND_M", "COND_P", "COND_PE", "COND_PO", "COND_WS", "COND_UNRECOGNIZED", 
			"PREP_ORG", "PREP_EQU", "PREP_SET", "PREP_IF", "PREP_ENDIF", "PREP_INCLUDE", 
			"PREP_MACRO", "PREP_ENDM", "PREP_DB", "PREP_DW", "PREP_DS", "PREP_ADDR", 
			"REG_A", "REG_B", "REG_C", "REG_D", "REG_E", "REG_H", "REG_L", "REG_IX", 
			"REG_IXH", "REG_IXL", "REG_IY", "REG_IYH", "REG_IYL", "REG_BC", "REG_DE", 
			"REG_HL", "REG_SP", "REG_AF", "REG_AFF", "REG_I", "REG_R", "OP_MOD", 
			"OP_SHR", "OP_SHL", "OP_NOT", "OP_AND", "OP_OR", "OP_XOR", "LIT_HEXNUMBER_1", 
			"LIT_NUMBER", "LIT_HEXNUMBER_2", "LIT_OCTNUMBER", "LIT_BINNUMBER", "LIT_STRING_1", 
			"LIT_STRING_2", "ID_IDENTIFIER", "ID_LABEL", "ERROR", "SEP_LPAR", "SEP_RPAR", 
			"SEP_COMMA", "OP_ADD", "OP_SUBTRACT", "OP_MULTIPLY", "OP_DIVIDE", "OP_EQUAL", 
			"OP_GT", "OP_GTE", "OP_LT", "OP_LTE", "OP_MOD_2", "OP_SHR_2", "OP_SHL_2", 
			"OP_NOT_2", "OP_AND_2", "OP_OR_2", "OP_XOR_2", "WS", "EOL"
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
			null, null, null, null, null, null, null, null, "'$'", null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, "'('", 
			"')'", "','", "'+'", "'-'", "'*'", "'/'", "'='", "'>'", "'>='", "'<'", 
			"'<='", "'%'", "'>>'", "'<<'", "'~'", "'&'", "'|'", "'^'"
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
			"COND_UNRECOGNIZED", "PREP_ORG", "PREP_EQU", "PREP_SET", "PREP_IF", "PREP_ENDIF", 
			"PREP_INCLUDE", "PREP_MACRO", "PREP_ENDM", "PREP_DB", "PREP_DW", "PREP_DS", 
			"PREP_ADDR", "REG_A", "REG_B", "REG_C", "REG_D", "REG_E", "REG_H", "REG_L", 
			"REG_IX", "REG_IXH", "REG_IXL", "REG_IY", "REG_IYH", "REG_IYL", "REG_BC", 
			"REG_DE", "REG_HL", "REG_SP", "REG_AF", "REG_AFF", "REG_I", "REG_R", 
			"OP_MOD", "OP_SHR", "OP_SHL", "OP_NOT", "OP_AND", "OP_OR", "OP_XOR", 
			"LIT_HEXNUMBER_1", "LIT_NUMBER", "LIT_HEXNUMBER_2", "LIT_OCTNUMBER", 
			"LIT_BINNUMBER", "LIT_STRING_1", "LIT_STRING_2", "ID_IDENTIFIER", "ID_LABEL", 
			"ERROR", "SEP_LPAR", "SEP_RPAR", "SEP_COMMA", "OP_ADD", "OP_SUBTRACT", 
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


	public AsZ80Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "AsZ80Lexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 104:
			return COND_UNRECOGNIZED_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean COND_UNRECOGNIZED_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return "cCnNzZmMpP".indexOf((char) _input.LA(1)) == -1;
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\u0099\u0404\b\1\b"+
		"\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n"+
		"\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21"+
		"\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30"+
		"\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37"+
		"\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t"+
		"*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63"+
		"\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t"+
		"<\4=\t=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4"+
		"H\tH\4I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\t"+
		"S\4T\tT\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^"+
		"\4_\t_\4`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j"+
		"\tj\4k\tk\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu"+
		"\4v\tv\4w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080"+
		"\t\u0080\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084"+
		"\4\u0085\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089"+
		"\t\u0089\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d"+
		"\4\u008e\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092"+
		"\t\u0092\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\4\u0096\t\u0096"+
		"\4\u0097\t\u0097\4\u0098\t\u0098\4\u0099\t\u0099\4\u009a\t\u009a\4\u009b"+
		"\t\u009b\4\u009c\t\u009c\4\u009d\t\u009d\4\u009e\t\u009e\4\u009f\t\u009f"+
		"\4\u00a0\t\u00a0\4\u00a1\t\u00a1\4\u00a2\t\u00a2\4\u00a3\t\u00a3\4\u00a4"+
		"\t\u00a4\4\u00a5\t\u00a5\4\u00a6\t\u00a6\4\u00a7\t\u00a7\4\u00a8\t\u00a8"+
		"\4\u00a9\t\u00a9\4\u00aa\t\u00aa\4\u00ab\t\u00ab\4\u00ac\t\u00ac\4\u00ad"+
		"\t\u00ad\4\u00ae\t\u00ae\4\u00af\t\u00af\4\u00b0\t\u00b0\4\u00b1\t\u00b1"+
		"\3\2\3\2\3\2\3\2\3\2\5\2\u016a\n\2\3\2\7\2\u016d\n\2\f\2\16\2\u0170\13"+
		"\2\3\2\3\2\3\3\3\3\3\3\3\3\7\3\u0178\n\3\f\3\16\3\u017b\13\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13"+
		"\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22"+
		"\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31"+
		"\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36"+
		"\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\""+
		"\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3"+
		"(\3(\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3+\3+\3+\3,\3,\3,\3,\3,\3-\3-\3-\3"+
		".\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\62\3\62"+
		"\3\62\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65"+
		"\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67\38\38\38\38\38\39\39\39"+
		"\39\39\3:\3:\3:\3;\3;\3;\3;\3<\3<\3<\3<\3<\3=\3=\3=\3=\3>\3>\3>\3>\3>"+
		"\3?\3?\3?\3?\3@\3@\3@\3@\3A\3A\3A\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C\3D\3D"+
		"\3D\3D\3E\3E\3E\3E\3E\3F\3F\3F\3F\3F\3G\3G\3G\3G\3H\3H\3H\3H\3H\3I\3I"+
		"\3I\3I\3J\3J\3J\3J\3J\3J\3K\3K\3K\3K\3K\3L\3L\3L\3L\3L\3M\3M\3M\3N\3N"+
		"\3N\3N\3O\3O\3O\3O\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R\3R\3S\3S\3S\3S\3T"+
		"\3T\3T\3T\3U\3U\3U\3U\3U\3V\3V\3V\3V\3W\3W\3W\3W\3X\3X\3X\3X\3Y\3Y\3Y"+
		"\3Y\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3]\3]\3]\3]\3^\3^\3^\3^\3"+
		"_\3_\3_\3_\3`\3`\3`\3`\3a\3a\3a\3a\3b\3b\3b\3b\3b\3c\3c\3c\3c\3d\3d\3"+
		"d\3d\3d\3e\3e\3e\3e\3f\3f\3f\3f\3g\3g\3g\3g\3g\3h\3h\3h\3h\3h\3i\6i\u02f7"+
		"\ni\ri\16i\u02f8\3i\3i\3j\3j\3j\3j\3k\3k\3k\3k\3l\3l\3l\3l\3m\3m\3m\3"+
		"m\3n\3n\3n\3o\3o\3o\3o\3o\3o\3p\3p\3p\3p\3p\3p\3p\3p\3q\3q\3q\3q\3q\3"+
		"q\3r\3r\3r\3r\3r\3s\3s\3s\3t\3t\3t\3u\3u\3u\3v\3v\3w\3w\3x\3x\3y\3y\3"+
		"z\3z\3{\3{\3|\3|\3}\3}\3~\3~\3~\3\177\3\177\3\177\3\177\3\u0080\3\u0080"+
		"\3\u0080\3\u0080\3\u0081\3\u0081\3\u0081\3\u0082\3\u0082\3\u0082\3\u0082"+
		"\3\u0083\3\u0083\3\u0083\3\u0083\3\u0084\3\u0084\3\u0084\3\u0085\3\u0085"+
		"\3\u0085\3\u0086\3\u0086\3\u0086\3\u0087\3\u0087\3\u0087\3\u0088\3\u0088"+
		"\3\u0088\3\u0089\3\u0089\3\u0089\3\u0089\3\u008a\3\u008a\3\u008b\3\u008b"+
		"\3\u008c\3\u008c\3\u008c\3\u008c\3\u008d\3\u008d\3\u008d\3\u008d\3\u008e"+
		"\3\u008e\3\u008e\3\u008e\3\u008f\3\u008f\3\u008f\3\u008f\3\u0090\3\u0090"+
		"\3\u0090\3\u0090\3\u0091\3\u0091\3\u0091\3\u0092\3\u0092\3\u0092\3\u0092"+
		"\3\u0093\3\u0093\3\u0093\6\u0093\u038d\n\u0093\r\u0093\16\u0093\u038e"+
		"\3\u0094\6\u0094\u0392\n\u0094\r\u0094\16\u0094\u0393\3\u0094\5\u0094"+
		"\u0397\n\u0094\3\u0095\6\u0095\u039a\n\u0095\r\u0095\16\u0095\u039b\3"+
		"\u0095\3\u0095\3\u0096\6\u0096\u03a1\n\u0096\r\u0096\16\u0096\u03a2\3"+
		"\u0096\3\u0096\3\u0097\6\u0097\u03a8\n\u0097\r\u0097\16\u0097\u03a9\3"+
		"\u0097\3\u0097\3\u0098\3\u0098\7\u0098\u03b0\n\u0098\f\u0098\16\u0098"+
		"\u03b3\13\u0098\3\u0098\3\u0098\3\u0099\3\u0099\7\u0099\u03b9\n\u0099"+
		"\f\u0099\16\u0099\u03bc\13\u0099\3\u0099\3\u0099\3\u009a\3\u009a\7\u009a"+
		"\u03c2\n\u009a\f\u009a\16\u009a\u03c5\13\u009a\3\u009b\3\u009b\3\u009b"+
		"\3\u009c\6\u009c\u03cb\n\u009c\r\u009c\16\u009c\u03cc\3\u009d\3\u009d"+
		"\3\u009e\3\u009e\3\u009f\3\u009f\3\u00a0\3\u00a0\3\u00a1\3\u00a1\3\u00a2"+
		"\3\u00a2\3\u00a3\3\u00a3\3\u00a4\3\u00a4\3\u00a5\3\u00a5\3\u00a6\3\u00a6"+
		"\3\u00a6\3\u00a7\3\u00a7\3\u00a8\3\u00a8\3\u00a8\3\u00a9\3\u00a9\3\u00aa"+
		"\3\u00aa\3\u00aa\3\u00ab\3\u00ab\3\u00ab\3\u00ac\3\u00ac\3\u00ad\3\u00ad"+
		"\3\u00ae\3\u00ae\3\u00af\3\u00af\3\u00b0\6\u00b0\u03fa\n\u00b0\r\u00b0"+
		"\16\u00b0\u03fb\3\u00b0\3\u00b0\3\u00b1\5\u00b1\u0401\n\u00b1\3\u00b1"+
		"\3\u00b1\3\u0179\2\u00b2\4\3\6\4\b\2\n\2\f\2\16\2\20\2\22\2\24\2\26\2"+
		"\30\2\32\2\34\2\36\2 \2\"\2$\2&\2(\2*\2,\2.\2\60\2\62\2\64\2\66\28\2:"+
		"\5<\6>\7@\bB\tD\nF\13H\fJ\rL\16N\17P\20R\21T\22V\23X\24Z\25\\\26^\27`"+
		"\30b\31d\32f\33h\34j\35l\36n\37p r!t\"v#x$z%|&~\'\u0080(\u0082)\u0084"+
		"*\u0086+\u0088,\u008a-\u008c.\u008e/\u0090\60\u0092\61\u0094\62\u0096"+
		"\63\u0098\64\u009a\65\u009c\66\u009e\67\u00a08\u00a29\u00a4:\u00a6;\u00a8"+
		"<\u00aa=\u00ac>\u00ae?\u00b0@\u00b2A\u00b4B\u00b6C\u00b8D\u00baE\u00bc"+
		"F\u00beG\u00c0H\u00c2I\u00c4J\u00c6K\u00c8L\u00caM\u00ccN\u00ceO\u00d0"+
		"P\u00d2Q\u00d4R\u00d6S\u00d8T\u00daU\u00dcV\u00deW\u00e0X\u00e2Y\u00e4"+
		"Z\u00e6[\u00e8\\\u00ea]\u00ec^\u00ee_\u00f0`\u00f2a\u00f4b\u00f6c\u00f8"+
		"d\u00fae\u00fcf\u00feg\u0100h\u0102i\u0104j\u0106k\u0108l\u010am\u010c"+
		"n\u010eo\u0110p\u0112q\u0114r\u0116s\u0118t\u011au\u011cv\u011ew\u0120"+
		"x\u0122y\u0124z\u0126{\u0128|\u012a}\u012c~\u012e\177\u0130\u0080\u0132"+
		"\u0081\u0134\u0082\u0136\u0083\u0138\u0084\u013a\u0085\u013c\u0086\u013e"+
		"\u0087\u0140\u0088\u0142\u0089\u0144\u008a\u0146\u008b\u0148\u008c\u014a"+
		"\u008d\u014c\u008e\u014e\u008f\u0150\u0090\u0152\u0091\u0154\u0092\u0156"+
		"\u0093\u0158\u0094\u015a\u0095\u015c\u0096\u015e\u0097\u0160\u0098\u0162"+
		"\u0099\4\2\3(\4\2%%==\4\2\f\f\17\17\4\2CCcc\4\2DDdd\4\2EEee\4\2FFff\4"+
		"\2GGgg\4\2HHhh\4\2IIii\4\2JJjj\4\2KKkk\4\2LLll\4\2NNnn\4\2OOoo\4\2PPp"+
		"p\4\2QQqq\4\2RRrr\4\2SSss\4\2TTtt\4\2UUuu\4\2VVvv\4\2WWww\4\2XXxx\4\2"+
		"YYyy\4\2ZZzz\4\2[[{{\4\2\\\\||\5\2\13\13\16\16\"\"\5\2\62;CHch\3\2\62"+
		";\3\2\629\6\2QQSSqqss\3\2\62\63\3\2))\3\2$$\5\2A\\aac|\6\2\62;A\\aac|"+
		"\f\2\13\f\16\17\"\"\'(*/\61\61>@``~~\u0080\u0080\2\u03fa\2\4\3\2\2\2\2"+
		"\6\3\2\2\2\2:\3\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3"+
		"\2\2\2\2F\3\2\2\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2"+
		"\2\2R\3\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2\2\2"+
		"\2^\3\2\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\2f\3\2\2\2\2h\3\2\2\2\2j"+
		"\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2\2p\3\2\2\2\2r\3\2\2\2\2t\3\2\2\2\2v\3\2"+
		"\2\2\2x\3\2\2\2\2z\3\2\2\2\2|\3\2\2\2\2~\3\2\2\2\2\u0080\3\2\2\2\2\u0082"+
		"\3\2\2\2\2\u0084\3\2\2\2\2\u0086\3\2\2\2\2\u0088\3\2\2\2\2\u008a\3\2\2"+
		"\2\2\u008c\3\2\2\2\2\u008e\3\2\2\2\2\u0090\3\2\2\2\2\u0092\3\2\2\2\2\u0094"+
		"\3\2\2\2\2\u0096\3\2\2\2\2\u0098\3\2\2\2\2\u009a\3\2\2\2\2\u009c\3\2\2"+
		"\2\2\u009e\3\2\2\2\2\u00a0\3\2\2\2\2\u00a2\3\2\2\2\2\u00a4\3\2\2\2\2\u00a6"+
		"\3\2\2\2\2\u00a8\3\2\2\2\2\u00aa\3\2\2\2\2\u00ac\3\2\2\2\2\u00ae\3\2\2"+
		"\2\2\u00b0\3\2\2\2\2\u00b2\3\2\2\2\2\u00b4\3\2\2\2\2\u00b6\3\2\2\2\2\u00b8"+
		"\3\2\2\2\2\u00ba\3\2\2\2\2\u00bc\3\2\2\2\2\u00be\3\2\2\2\2\u00c0\3\2\2"+
		"\2\2\u00d6\3\2\2\2\2\u00d8\3\2\2\2\2\u00da\3\2\2\2\2\u00dc\3\2\2\2\2\u00de"+
		"\3\2\2\2\2\u00e0\3\2\2\2\2\u00e2\3\2\2\2\2\u00e4\3\2\2\2\2\u00e6\3\2\2"+
		"\2\2\u00e8\3\2\2\2\2\u00ea\3\2\2\2\2\u00ec\3\2\2\2\2\u00ee\3\2\2\2\2\u00f0"+
		"\3\2\2\2\2\u00f2\3\2\2\2\2\u00f4\3\2\2\2\2\u00f6\3\2\2\2\2\u00f8\3\2\2"+
		"\2\2\u00fa\3\2\2\2\2\u00fc\3\2\2\2\2\u00fe\3\2\2\2\2\u0100\3\2\2\2\2\u0102"+
		"\3\2\2\2\2\u0104\3\2\2\2\2\u0106\3\2\2\2\2\u0108\3\2\2\2\2\u010a\3\2\2"+
		"\2\2\u010c\3\2\2\2\2\u010e\3\2\2\2\2\u0110\3\2\2\2\2\u0112\3\2\2\2\2\u0114"+
		"\3\2\2\2\2\u0116\3\2\2\2\2\u0118\3\2\2\2\2\u011a\3\2\2\2\2\u011c\3\2\2"+
		"\2\2\u011e\3\2\2\2\2\u0120\3\2\2\2\2\u0122\3\2\2\2\2\u0124\3\2\2\2\2\u0126"+
		"\3\2\2\2\2\u0128\3\2\2\2\2\u012a\3\2\2\2\2\u012c\3\2\2\2\2\u012e\3\2\2"+
		"\2\2\u0130\3\2\2\2\2\u0132\3\2\2\2\2\u0134\3\2\2\2\2\u0136\3\2\2\2\2\u0138"+
		"\3\2\2\2\2\u013a\3\2\2\2\2\u013c\3\2\2\2\2\u013e\3\2\2\2\2\u0140\3\2\2"+
		"\2\2\u0142\3\2\2\2\2\u0144\3\2\2\2\2\u0146\3\2\2\2\2\u0148\3\2\2\2\2\u014a"+
		"\3\2\2\2\2\u014c\3\2\2\2\2\u014e\3\2\2\2\2\u0150\3\2\2\2\2\u0152\3\2\2"+
		"\2\2\u0154\3\2\2\2\2\u0156\3\2\2\2\2\u0158\3\2\2\2\2\u015a\3\2\2\2\2\u015c"+
		"\3\2\2\2\2\u015e\3\2\2\2\2\u0160\3\2\2\2\2\u0162\3\2\2\2\3\u00c2\3\2\2"+
		"\2\3\u00c4\3\2\2\2\3\u00c6\3\2\2\2\3\u00c8\3\2\2\2\3\u00ca\3\2\2\2\3\u00cc"+
		"\3\2\2\2\3\u00ce\3\2\2\2\3\u00d0\3\2\2\2\3\u00d2\3\2\2\2\3\u00d4\3\2\2"+
		"\2\4\u0169\3\2\2\2\6\u0173\3\2\2\2\b\u0181\3\2\2\2\n\u0183\3\2\2\2\f\u0185"+
		"\3\2\2\2\16\u0187\3\2\2\2\20\u0189\3\2\2\2\22\u018b\3\2\2\2\24\u018d\3"+
		"\2\2\2\26\u018f\3\2\2\2\30\u0191\3\2\2\2\32\u0193\3\2\2\2\34\u0195\3\2"+
		"\2\2\36\u0197\3\2\2\2 \u0199\3\2\2\2\"\u019b\3\2\2\2$\u019d\3\2\2\2&\u019f"+
		"\3\2\2\2(\u01a1\3\2\2\2*\u01a3\3\2\2\2,\u01a5\3\2\2\2.\u01a7\3\2\2\2\60"+
		"\u01a9\3\2\2\2\62\u01ab\3\2\2\2\64\u01ad\3\2\2\2\66\u01af\3\2\2\28\u01b1"+
		"\3\2\2\2:\u01b3\3\2\2\2<\u01b7\3\2\2\2>\u01bb\3\2\2\2@\u01bf\3\2\2\2B"+
		"\u01c3\3\2\2\2D\u01ca\3\2\2\2F\u01ce\3\2\2\2H\u01d1\3\2\2\2J\u01d5\3\2"+
		"\2\2L\u01da\3\2\2\2N\u01de\3\2\2\2P\u01e3\3\2\2\2R\u01e7\3\2\2\2T\u01eb"+
		"\3\2\2\2V\u01ef\3\2\2\2X\u01f2\3\2\2\2Z\u01f7\3\2\2\2\\\u01fa\3\2\2\2"+
		"^\u01fd\3\2\2\2`\u0201\3\2\2\2b\u0206\3\2\2\2d\u0209\3\2\2\2f\u020c\3"+
		"\2\2\2h\u0210\3\2\2\2j\u0214\3\2\2\2l\u0219\3\2\2\2n\u021d\3\2\2\2p\u0222"+
		"\3\2\2\2r\u0227\3\2\2\2t\u022c\3\2\2\2v\u022f\3\2\2\2x\u0233\3\2\2\2z"+
		"\u0238\3\2\2\2|\u023c\3\2\2\2~\u0241\3\2\2\2\u0080\u0245\3\2\2\2\u0082"+
		"\u0249\3\2\2\2\u0084\u024c\3\2\2\2\u0086\u0251\3\2\2\2\u0088\u0256\3\2"+
		"\2\2\u008a\u025a\3\2\2\2\u008c\u025f\3\2\2\2\u008e\u0264\3\2\2\2\u0090"+
		"\u0268\3\2\2\2\u0092\u026d\3\2\2\2\u0094\u0271\3\2\2\2\u0096\u0277\3\2"+
		"\2\2\u0098\u027c\3\2\2\2\u009a\u0281\3\2\2\2\u009c\u0284\3\2\2\2\u009e"+
		"\u0288\3\2\2\2\u00a0\u028c\3\2\2\2\u00a2\u0291\3\2\2\2\u00a4\u0295\3\2"+
		"\2\2\u00a6\u0298\3\2\2\2\u00a8\u029c\3\2\2\2\u00aa\u02a0\3\2\2\2\u00ac"+
		"\u02a5\3\2\2\2\u00ae\u02a9\3\2\2\2\u00b0\u02ad\3\2\2\2\u00b2\u02b1\3\2"+
		"\2\2\u00b4\u02b5\3\2\2\2\u00b6\u02b9\3\2\2\2\u00b8\u02bd\3\2\2\2\u00ba"+
		"\u02c1\3\2\2\2\u00bc\u02c5\3\2\2\2\u00be\u02c9\3\2\2\2\u00c0\u02cd\3\2"+
		"\2\2\u00c2\u02d1\3\2\2\2\u00c4\u02d5\3\2\2\2\u00c6\u02da\3\2\2\2\u00c8"+
		"\u02de\3\2\2\2\u00ca\u02e3\3\2\2\2\u00cc\u02e7\3\2\2\2\u00ce\u02eb\3\2"+
		"\2\2\u00d0\u02f0\3\2\2\2\u00d2\u02f6\3\2\2\2\u00d4\u02fc\3\2\2\2\u00d6"+
		"\u0300\3\2\2\2\u00d8\u0304\3\2\2\2\u00da\u0308\3\2\2\2\u00dc\u030c\3\2"+
		"\2\2\u00de\u030f\3\2\2\2\u00e0\u0315\3\2\2\2\u00e2\u031d\3\2\2\2\u00e4"+
		"\u0323\3\2\2\2\u00e6\u0328\3\2\2\2\u00e8\u032b\3\2\2\2\u00ea\u032e\3\2"+
		"\2\2\u00ec\u0331\3\2\2\2\u00ee\u0333\3\2\2\2\u00f0\u0335\3\2\2\2\u00f2"+
		"\u0337\3\2\2\2\u00f4\u0339\3\2\2\2\u00f6\u033b\3\2\2\2\u00f8\u033d\3\2"+
		"\2\2\u00fa\u033f\3\2\2\2\u00fc\u0341\3\2\2\2\u00fe\u0344\3\2\2\2\u0100"+
		"\u0348\3\2\2\2\u0102\u034c\3\2\2\2\u0104\u034f\3\2\2\2\u0106\u0353\3\2"+
		"\2\2\u0108\u0357\3\2\2\2\u010a\u035a\3\2\2\2\u010c\u035d\3\2\2\2\u010e"+
		"\u0360\3\2\2\2\u0110\u0363\3\2\2\2\u0112\u0366\3\2\2\2\u0114\u036a\3\2"+
		"\2\2\u0116\u036c\3\2\2\2\u0118\u036e\3\2\2\2\u011a\u0372\3\2\2\2\u011c"+
		"\u0376\3\2\2\2\u011e\u037a\3\2\2\2\u0120\u037e\3\2\2\2\u0122\u0382\3\2"+
		"\2\2\u0124\u0385\3\2\2\2\u0126\u0389\3\2\2\2\u0128\u0391\3\2\2\2\u012a"+
		"\u0399\3\2\2\2\u012c\u03a0\3\2\2\2\u012e\u03a7\3\2\2\2\u0130\u03ad\3\2"+
		"\2\2\u0132\u03b6\3\2\2\2\u0134\u03bf\3\2\2\2\u0136\u03c6\3\2\2\2\u0138"+
		"\u03ca\3\2\2\2\u013a\u03ce\3\2\2\2\u013c\u03d0\3\2\2\2\u013e\u03d2\3\2"+
		"\2\2\u0140\u03d4\3\2\2\2\u0142\u03d6\3\2\2\2\u0144\u03d8\3\2\2\2\u0146"+
		"\u03da\3\2\2\2\u0148\u03dc\3\2\2\2\u014a\u03de\3\2\2\2\u014c\u03e0\3\2"+
		"\2\2\u014e\u03e3\3\2\2\2\u0150\u03e5\3\2\2\2\u0152\u03e8\3\2\2\2\u0154"+
		"\u03ea\3\2\2\2\u0156\u03ed\3\2\2\2\u0158\u03f0\3\2\2\2\u015a\u03f2\3\2"+
		"\2\2\u015c\u03f4\3\2\2\2\u015e\u03f6\3\2\2\2\u0160\u03f9\3\2\2\2\u0162"+
		"\u0400\3\2\2\2\u0164\u0165\7\61\2\2\u0165\u016a\7\61\2\2\u0166\u0167\7"+
		"/\2\2\u0167\u016a\7/\2\2\u0168\u016a\t\2\2\2\u0169\u0164\3\2\2\2\u0169"+
		"\u0166\3\2\2\2\u0169\u0168\3\2\2\2\u016a\u016e\3\2\2\2\u016b\u016d\n\3"+
		"\2\2\u016c\u016b\3\2\2\2\u016d\u0170\3\2\2\2\u016e\u016c\3\2\2\2\u016e"+
		"\u016f\3\2\2\2\u016f\u0171\3\2\2\2\u0170\u016e\3\2\2\2\u0171\u0172\b\2"+
		"\2\2\u0172\5\3\2\2\2\u0173\u0174\7\61\2\2\u0174\u0175\7,\2\2\u0175\u0179"+
		"\3\2\2\2\u0176\u0178\13\2\2\2\u0177\u0176\3\2\2\2\u0178\u017b\3\2\2\2"+
		"\u0179\u017a\3\2\2\2\u0179\u0177\3\2\2\2\u017a\u017c\3\2\2\2\u017b\u0179"+
		"\3\2\2\2\u017c\u017d\7,\2\2\u017d\u017e\7\61\2\2\u017e\u017f\3\2\2\2\u017f"+
		"\u0180\b\3\2\2\u0180\7\3\2\2\2\u0181\u0182\t\4\2\2\u0182\t\3\2\2\2\u0183"+
		"\u0184\t\5\2\2\u0184\13\3\2\2\2\u0185\u0186\t\6\2\2\u0186\r\3\2\2\2\u0187"+
		"\u0188\t\7\2\2\u0188\17\3\2\2\2\u0189\u018a\t\b\2\2\u018a\21\3\2\2\2\u018b"+
		"\u018c\t\t\2\2\u018c\23\3\2\2\2\u018d\u018e\t\n\2\2\u018e\25\3\2\2\2\u018f"+
		"\u0190\t\13\2\2\u0190\27\3\2\2\2\u0191\u0192\t\f\2\2\u0192\31\3\2\2\2"+
		"\u0193\u0194\t\r\2\2\u0194\33\3\2\2\2\u0195\u0196\t\16\2\2\u0196\35\3"+
		"\2\2\2\u0197\u0198\t\17\2\2\u0198\37\3\2\2\2\u0199\u019a\t\20\2\2\u019a"+
		"!\3\2\2\2\u019b\u019c\t\21\2\2\u019c#\3\2\2\2\u019d\u019e\t\22\2\2\u019e"+
		"%\3\2\2\2\u019f\u01a0\t\23\2\2\u01a0\'\3\2\2\2\u01a1\u01a2\t\24\2\2\u01a2"+
		")\3\2\2\2\u01a3\u01a4\t\25\2\2\u01a4+\3\2\2\2\u01a5\u01a6\t\26\2\2\u01a6"+
		"-\3\2\2\2\u01a7\u01a8\t\27\2\2\u01a8/\3\2\2\2\u01a9\u01aa\t\30\2\2\u01aa"+
		"\61\3\2\2\2\u01ab\u01ac\t\31\2\2\u01ac\63\3\2\2\2\u01ad\u01ae\t\32\2\2"+
		"\u01ae\65\3\2\2\2\u01af\u01b0\t\33\2\2\u01b0\67\3\2\2\2\u01b1\u01b2\t"+
		"\34\2\2\u01b29\3\2\2\2\u01b3\u01b4\5\b\4\2\u01b4\u01b5\5\16\7\2\u01b5"+
		"\u01b6\5\f\6\2\u01b6;\3\2\2\2\u01b7\u01b8\5\b\4\2\u01b8\u01b9\5\16\7\2"+
		"\u01b9\u01ba\5\16\7\2\u01ba=\3\2\2\2\u01bb\u01bc\5\b\4\2\u01bc\u01bd\5"+
		" \20\2\u01bd\u01be\5\16\7\2\u01be?\3\2\2\2\u01bf\u01c0\5\n\5\2\u01c0\u01c1"+
		"\5\30\f\2\u01c1\u01c2\5,\26\2\u01c2A\3\2\2\2\u01c3\u01c4\5\f\6\2\u01c4"+
		"\u01c5\5\b\4\2\u01c5\u01c6\5\34\16\2\u01c6\u01c7\5\34\16\2\u01c7\u01c8"+
		"\3\2\2\2\u01c8\u01c9\b!\3\2\u01c9C\3\2\2\2\u01ca\u01cb\5\f\6\2\u01cb\u01cc"+
		"\5\f\6\2\u01cc\u01cd\5\22\t\2\u01cdE\3\2\2\2\u01ce\u01cf\5\f\6\2\u01cf"+
		"\u01d0\5$\22\2\u01d0G\3\2\2\2\u01d1\u01d2\5\f\6\2\u01d2\u01d3\5$\22\2"+
		"\u01d3\u01d4\5\16\7\2\u01d4I\3\2\2\2\u01d5\u01d6\5\f\6\2\u01d6\u01d7\5"+
		"$\22\2\u01d7\u01d8\5\16\7\2\u01d8\u01d9\5(\24\2\u01d9K\3\2\2\2\u01da\u01db"+
		"\5\f\6\2\u01db\u01dc\5$\22\2\u01dc\u01dd\5\30\f\2\u01ddM\3\2\2\2\u01de"+
		"\u01df\5\f\6\2\u01df\u01e0\5$\22\2\u01e0\u01e1\5\30\f\2\u01e1\u01e2\5"+
		"(\24\2\u01e2O\3\2\2\2\u01e3\u01e4\5\f\6\2\u01e4\u01e5\5$\22\2\u01e5\u01e6"+
		"\5\34\16\2\u01e6Q\3\2\2\2\u01e7\u01e8\5\16\7\2\u01e8\u01e9\5\b\4\2\u01e9"+
		"\u01ea\5\b\4\2\u01eaS\3\2\2\2\u01eb\u01ec\5\16\7\2\u01ec\u01ed\5\20\b"+
		"\2\u01ed\u01ee\5\f\6\2\u01eeU\3\2\2\2\u01ef\u01f0\5\16\7\2\u01f0\u01f1"+
		"\5\30\f\2\u01f1W\3\2\2\2\u01f2\u01f3\5\16\7\2\u01f3\u01f4\5\32\r\2\u01f4"+
		"\u01f5\5 \20\2\u01f5\u01f6\58\34\2\u01f6Y\3\2\2\2\u01f7\u01f8\5\20\b\2"+
		"\u01f8\u01f9\5\30\f\2\u01f9[\3\2\2\2\u01fa\u01fb\5\20\b\2\u01fb\u01fc"+
		"\5\64\32\2\u01fc]\3\2\2\2\u01fd\u01fe\5\20\b\2\u01fe\u01ff\5\64\32\2\u01ff"+
		"\u0200\5\64\32\2\u0200_\3\2\2\2\u0201\u0202\5\26\13\2\u0202\u0203\5\b"+
		"\4\2\u0203\u0204\5\34\16\2\u0204\u0205\5,\26\2\u0205a\3\2\2\2\u0206\u0207"+
		"\5\30\f\2\u0207\u0208\5\36\17\2\u0208c\3\2\2\2\u0209\u020a\5\30\f\2\u020a"+
		"\u020b\5 \20\2\u020be\3\2\2\2\u020c\u020d\5\30\f\2\u020d\u020e\5 \20\2"+
		"\u020e\u020f\5\f\6\2\u020fg\3\2\2\2\u0210\u0211\5\30\f\2\u0211\u0212\5"+
		" \20\2\u0212\u0213\5\16\7\2\u0213i\3\2\2\2\u0214\u0215\5\30\f\2\u0215"+
		"\u0216\5 \20\2\u0216\u0217\5\16\7\2\u0217\u0218\5(\24\2\u0218k\3\2\2\2"+
		"\u0219\u021a\5\30\f\2\u021a\u021b\5 \20\2\u021b\u021c\5\30\f\2\u021cm"+
		"\3\2\2\2\u021d\u021e\5\30\f\2\u021e\u021f\5 \20\2\u021f\u0220\5\30\f\2"+
		"\u0220\u0221\5(\24\2\u0221o\3\2\2\2\u0222\u0223\5\32\r\2\u0223\u0224\5"+
		"$\22\2\u0224\u0225\3\2\2\2\u0225\u0226\b8\3\2\u0226q\3\2\2\2\u0227\u0228"+
		"\5\32\r\2\u0228\u0229\5(\24\2\u0229\u022a\3\2\2\2\u022a\u022b\b9\3\2\u022b"+
		"s\3\2\2\2\u022c\u022d\5\34\16\2\u022d\u022e\5\16\7\2\u022eu\3\2\2\2\u022f"+
		"\u0230\5\34\16\2\u0230\u0231\5\16\7\2\u0231\u0232\5\16\7\2\u0232w\3\2"+
		"\2\2\u0233\u0234\5\34\16\2\u0234\u0235\5\16\7\2\u0235\u0236\5\16\7\2\u0236"+
		"\u0237\5(\24\2\u0237y\3\2\2\2\u0238\u0239\5\34\16\2\u0239\u023a\5\16\7"+
		"\2\u023a\u023b\5\30\f\2\u023b{\3\2\2\2\u023c\u023d\5\34\16\2\u023d\u023e"+
		"\5\16\7\2\u023e\u023f\5\30\f\2\u023f\u0240\5(\24\2\u0240}\3\2\2\2\u0241"+
		"\u0242\5 \20\2\u0242\u0243\5\20\b\2\u0243\u0244\5\24\n\2\u0244\177\3\2"+
		"\2\2\u0245\u0246\5 \20\2\u0246\u0247\5\"\21\2\u0247\u0248\5$\22\2\u0248"+
		"\u0081\3\2\2\2\u0249\u024a\5\"\21\2\u024a\u024b\5(\24\2\u024b\u0083\3"+
		"\2\2\2\u024c\u024d\5\"\21\2\u024d\u024e\5,\26\2\u024e\u024f\5\16\7\2\u024f"+
		"\u0250\5(\24\2\u0250\u0085\3\2\2\2\u0251\u0252\5\"\21\2\u0252\u0253\5"+
		",\26\2\u0253\u0254\5\30\f\2\u0254\u0255\5(\24\2\u0255\u0087\3\2\2\2\u0256"+
		"\u0257\5\"\21\2\u0257\u0258\5.\27\2\u0258\u0259\5,\26\2\u0259\u0089\3"+
		"\2\2\2\u025a\u025b\5\"\21\2\u025b\u025c\5.\27\2\u025c\u025d\5,\26\2\u025d"+
		"\u025e\5\16\7\2\u025e\u008b\3\2\2\2\u025f\u0260\5\"\21\2\u0260\u0261\5"+
		".\27\2\u0261\u0262\5,\26\2\u0262\u0263\5\30\f\2\u0263\u008d\3\2\2\2\u0264"+
		"\u0265\5$\22\2\u0265\u0266\5\"\21\2\u0266\u0267\5$\22\2\u0267\u008f\3"+
		"\2\2\2\u0268\u0269\5$\22\2\u0269\u026a\5.\27\2\u026a\u026b\5*\25\2\u026b"+
		"\u026c\5\26\13\2\u026c\u0091\3\2\2\2\u026d\u026e\5(\24\2\u026e\u026f\5"+
		"\20\b\2\u026f\u0270\5*\25\2\u0270\u0093\3\2\2\2\u0271\u0272\5(\24\2\u0272"+
		"\u0273\5\20\b\2\u0273\u0274\5,\26\2\u0274\u0275\3\2\2\2\u0275\u0276\b"+
		"J\3\2\u0276\u0095\3\2\2\2\u0277\u0278\5(\24\2\u0278\u0279\5\20\b\2\u0279"+
		"\u027a\5,\26\2\u027a\u027b\5\30\f\2\u027b\u0097\3\2\2\2\u027c\u027d\5"+
		"(\24\2\u027d\u027e\5\20\b\2\u027e\u027f\5,\26\2\u027f\u0280\5 \20\2\u0280"+
		"\u0099\3\2\2\2\u0281\u0282\5(\24\2\u0282\u0283\5\34\16\2\u0283\u009b\3"+
		"\2\2\2\u0284\u0285\5(\24\2\u0285\u0286\5\34\16\2\u0286\u0287\5\b\4\2\u0287"+
		"\u009d\3\2\2\2\u0288\u0289\5(\24\2\u0289\u028a\5\34\16\2\u028a\u028b\5"+
		"\f\6\2\u028b\u009f\3\2\2\2\u028c\u028d\5(\24\2\u028d\u028e\5\34\16\2\u028e"+
		"\u028f\5\f\6\2\u028f\u0290\5\b\4\2\u0290\u00a1\3\2\2\2\u0291\u0292\5("+
		"\24\2\u0292\u0293\5\34\16\2\u0293\u0294\5\16\7\2\u0294\u00a3\3\2\2\2\u0295"+
		"\u0296\5(\24\2\u0296\u0297\5(\24\2\u0297\u00a5\3\2\2\2\u0298\u0299\5("+
		"\24\2\u0299\u029a\5(\24\2\u029a\u029b\5\b\4\2\u029b\u00a7\3\2\2\2\u029c"+
		"\u029d\5(\24\2\u029d\u029e\5(\24\2\u029e\u029f\5\f\6\2\u029f\u00a9\3\2"+
		"\2\2\u02a0\u02a1\5(\24\2\u02a1\u02a2\5(\24\2\u02a2\u02a3\5\f\6\2\u02a3"+
		"\u02a4\5\b\4\2\u02a4\u00ab\3\2\2\2\u02a5\u02a6\5(\24\2\u02a6\u02a7\5("+
		"\24\2\u02a7\u02a8\5\16\7\2\u02a8\u00ad\3\2\2\2\u02a9\u02aa\5(\24\2\u02aa"+
		"\u02ab\5*\25\2\u02ab\u02ac\5,\26\2\u02ac\u00af\3\2\2\2\u02ad\u02ae\5*"+
		"\25\2\u02ae\u02af\5\n\5\2\u02af\u02b0\5\f\6\2\u02b0\u00b1\3\2\2\2\u02b1"+
		"\u02b2\5*\25\2\u02b2\u02b3\5\f\6\2\u02b3\u02b4\5\22\t\2\u02b4\u00b3\3"+
		"\2\2\2\u02b5\u02b6\5*\25\2\u02b6\u02b7\5\20\b\2\u02b7\u02b8\5,\26\2\u02b8"+
		"\u00b5\3\2\2\2\u02b9\u02ba\5*\25\2\u02ba\u02bb\5\34\16\2\u02bb\u02bc\5"+
		"\b\4\2\u02bc\u00b7\3\2\2\2\u02bd\u02be\5*\25\2\u02be\u02bf\5(\24\2\u02bf"+
		"\u02c0\5\b\4\2\u02c0\u00b9\3\2\2\2\u02c1\u02c2\5*\25\2\u02c2\u02c3\5\34"+
		"\16\2\u02c3\u02c4\5\34\16\2\u02c4\u00bb\3\2\2\2\u02c5\u02c6\5*\25\2\u02c6"+
		"\u02c7\5(\24\2\u02c7\u02c8\5\34\16\2\u02c8\u00bd\3\2\2\2\u02c9\u02ca\5"+
		"*\25\2\u02ca\u02cb\5.\27\2\u02cb\u02cc\5\n\5\2\u02cc\u00bf\3\2\2\2\u02cd"+
		"\u02ce\5\64\32\2\u02ce\u02cf\5\"\21\2\u02cf\u02d0\5(\24\2\u02d0\u00c1"+
		"\3\2\2\2\u02d1\u02d2\5\f\6\2\u02d2\u02d3\3\2\2\2\u02d3\u02d4\ba\4\2\u02d4"+
		"\u00c3\3\2\2\2\u02d5\u02d6\5 \20\2\u02d6\u02d7\5\f\6\2\u02d7\u02d8\3\2"+
		"\2\2\u02d8\u02d9\bb\4\2\u02d9\u00c5\3\2\2\2\u02da\u02db\58\34\2\u02db"+
		"\u02dc\3\2\2\2\u02dc\u02dd\bc\4\2\u02dd\u00c7\3\2\2\2\u02de\u02df\5 \20"+
		"\2\u02df\u02e0\58\34\2\u02e0\u02e1\3\2\2\2\u02e1\u02e2\bd\4\2\u02e2\u00c9"+
		"\3\2\2\2\u02e3\u02e4\5\36\17\2\u02e4\u02e5\3\2\2\2\u02e5\u02e6\be\4\2"+
		"\u02e6\u00cb\3\2\2\2\u02e7\u02e8\5$\22\2\u02e8\u02e9\3\2\2\2\u02e9\u02ea"+
		"\bf\4\2\u02ea\u00cd\3\2\2\2\u02eb\u02ec\5$\22\2\u02ec\u02ed\5\20\b\2\u02ed"+
		"\u02ee\3\2\2\2\u02ee\u02ef\bg\4\2\u02ef\u00cf\3\2\2\2\u02f0\u02f1\5$\22"+
		"\2\u02f1\u02f2\5\"\21\2\u02f2\u02f3\3\2\2\2\u02f3\u02f4\bh\4\2\u02f4\u00d1"+
		"\3\2\2\2\u02f5\u02f7\t\35\2\2\u02f6\u02f5\3\2\2\2\u02f7\u02f8\3\2\2\2"+
		"\u02f8\u02f6\3\2\2\2\u02f8\u02f9\3\2\2\2\u02f9\u02fa\3\2\2\2\u02fa\u02fb"+
		"\bi\2\2\u02fb\u00d3\3\2\2\2\u02fc\u02fd\6j\2\2\u02fd\u02fe\3\2\2\2\u02fe"+
		"\u02ff\bj\4\2\u02ff\u00d5\3\2\2\2\u0300\u0301\5\"\21\2\u0301\u0302\5("+
		"\24\2\u0302\u0303\5\24\n\2\u0303\u00d7\3\2\2\2\u0304\u0305\5\20\b\2\u0305"+
		"\u0306\5&\23\2\u0306\u0307\5.\27\2\u0307\u00d9\3\2\2\2\u0308\u0309\5*"+
		"\25\2\u0309\u030a\5\20\b\2\u030a\u030b\5,\26\2\u030b\u00db\3\2\2\2\u030c"+
		"\u030d\5\30\f\2\u030d\u030e\5\22\t\2\u030e\u00dd\3\2\2\2\u030f\u0310\5"+
		"\20\b\2\u0310\u0311\5 \20\2\u0311\u0312\5\16\7\2\u0312\u0313\5\30\f\2"+
		"\u0313\u0314\5\22\t\2\u0314\u00df\3\2\2\2\u0315\u0316\5\30\f\2\u0316\u0317"+
		"\5 \20\2\u0317\u0318\5\f\6\2\u0318\u0319\5\34\16\2\u0319\u031a\5.\27\2"+
		"\u031a\u031b\5\16\7\2\u031b\u031c\5\20\b\2\u031c\u00e1\3\2\2\2\u031d\u031e"+
		"\5\36\17\2\u031e\u031f\5\b\4\2\u031f\u0320\5\f\6\2\u0320\u0321\5(\24\2"+
		"\u0321\u0322\5\"\21\2\u0322\u00e3\3\2\2\2\u0323\u0324\5\20\b\2\u0324\u0325"+
		"\5 \20\2\u0325\u0326\5\16\7\2\u0326\u0327\5\36\17\2\u0327\u00e5\3\2\2"+
		"\2\u0328\u0329\5\16\7\2\u0329\u032a\5\n\5\2\u032a\u00e7\3\2\2\2\u032b"+
		"\u032c\5\16\7\2\u032c\u032d\5\62\31\2\u032d\u00e9\3\2\2\2\u032e\u032f"+
		"\5\16\7\2\u032f\u0330\5*\25\2\u0330\u00eb\3\2\2\2\u0331\u0332\7&\2\2\u0332"+
		"\u00ed\3\2\2\2\u0333\u0334\5\b\4\2\u0334\u00ef\3\2\2\2\u0335\u0336\5\n"+
		"\5\2\u0336\u00f1\3\2\2\2\u0337\u0338\5\f\6\2\u0338\u00f3\3\2\2\2\u0339"+
		"\u033a\5\16\7\2\u033a\u00f5\3\2\2\2\u033b\u033c\5\20\b\2\u033c\u00f7\3"+
		"\2\2\2\u033d\u033e\5\26\13\2\u033e\u00f9\3\2\2\2\u033f\u0340\5\34\16\2"+
		"\u0340\u00fb\3\2\2\2\u0341\u0342\5\30\f\2\u0342\u0343\5\64\32\2\u0343"+
		"\u00fd\3\2\2\2\u0344\u0345\5\30\f\2\u0345\u0346\5\64\32\2\u0346\u0347"+
		"\5\26\13\2\u0347\u00ff\3\2\2\2\u0348\u0349\5\30\f\2\u0349\u034a\5\64\32"+
		"\2\u034a\u034b\5\34\16\2\u034b\u0101\3\2\2\2\u034c\u034d\5\30\f\2\u034d"+
		"\u034e\5\66\33\2\u034e\u0103\3\2\2\2\u034f\u0350\5\30\f\2\u0350\u0351"+
		"\5\66\33\2\u0351\u0352\5\26\13\2\u0352\u0105\3\2\2\2\u0353\u0354\5\30"+
		"\f\2\u0354\u0355\5\66\33\2\u0355\u0356\5\34\16\2\u0356\u0107\3\2\2\2\u0357"+
		"\u0358\5\n\5\2\u0358\u0359\5\f\6\2\u0359\u0109\3\2\2\2\u035a\u035b\5\16"+
		"\7\2\u035b\u035c\5\20\b\2\u035c\u010b\3\2\2\2\u035d\u035e\5\26\13\2\u035e"+
		"\u035f\5\34\16\2\u035f\u010d\3\2\2\2\u0360\u0361\5*\25\2\u0361\u0362\5"+
		"$\22\2\u0362\u010f\3\2\2\2\u0363\u0364\5\b\4\2\u0364\u0365\5\22\t\2\u0365"+
		"\u0111\3\2\2\2\u0366\u0367\5\b\4\2\u0367\u0368\5\22\t\2\u0368\u0369\7"+
		")\2\2\u0369\u0113\3\2\2\2\u036a\u036b\5\30\f\2\u036b\u0115\3\2\2\2\u036c"+
		"\u036d\5(\24\2\u036d\u0117\3\2\2\2\u036e\u036f\5\36\17\2\u036f\u0370\5"+
		"\"\21\2\u0370\u0371\5\16\7\2\u0371\u0119\3\2\2\2\u0372\u0373\5*\25\2\u0373"+
		"\u0374\5\26\13\2\u0374\u0375\5(\24\2\u0375\u011b\3\2\2\2\u0376\u0377\5"+
		"*\25\2\u0377\u0378\5\26\13\2\u0378\u0379\5\34\16\2\u0379\u011d\3\2\2\2"+
		"\u037a\u037b\5 \20\2\u037b\u037c\5\"\21\2\u037c\u037d\5,\26\2\u037d\u011f"+
		"\3\2\2\2\u037e\u037f\5\b\4\2\u037f\u0380\5 \20\2\u0380\u0381\5\16\7\2"+
		"\u0381\u0121\3\2\2\2\u0382\u0383\5\"\21\2\u0383\u0384\5(\24\2\u0384\u0123"+
		"\3\2\2\2\u0385\u0386\5\64\32\2\u0386\u0387\5\"\21\2\u0387\u0388\5(\24"+
		"\2\u0388\u0125\3\2\2\2\u0389\u038a\7\62\2\2\u038a\u038c\5\64\32\2\u038b"+
		"\u038d\t\36\2\2\u038c\u038b\3\2\2\2\u038d\u038e\3\2\2\2\u038e\u038c\3"+
		"\2\2\2\u038e\u038f\3\2\2\2\u038f\u0127\3\2\2\2\u0390\u0392\t\37\2\2\u0391"+
		"\u0390\3\2\2\2\u0392\u0393\3\2\2\2\u0393\u0391\3\2\2\2\u0393\u0394\3\2"+
		"\2\2\u0394\u0396\3\2\2\2\u0395\u0397\5\16\7\2\u0396\u0395\3\2\2\2\u0396"+
		"\u0397\3\2\2\2\u0397\u0129\3\2\2\2\u0398\u039a\t\36\2\2\u0399\u0398\3"+
		"\2\2\2\u039a\u039b\3\2\2\2\u039b\u0399\3\2\2\2\u039b\u039c\3\2\2\2\u039c"+
		"\u039d\3\2\2\2\u039d\u039e\5\26\13\2\u039e\u012b\3\2\2\2\u039f\u03a1\t"+
		" \2\2\u03a0\u039f\3\2\2\2\u03a1\u03a2\3\2\2\2\u03a2\u03a0\3\2\2\2\u03a2"+
		"\u03a3\3\2\2\2\u03a3\u03a4\3\2\2\2\u03a4\u03a5\t!\2\2\u03a5\u012d\3\2"+
		"\2\2\u03a6\u03a8\t\"\2\2\u03a7\u03a6\3\2\2\2\u03a8\u03a9\3\2\2\2\u03a9"+
		"\u03a7\3\2\2\2\u03a9\u03aa\3\2\2\2\u03aa\u03ab\3\2\2\2\u03ab\u03ac\5\n"+
		"\5\2\u03ac\u012f\3\2\2\2\u03ad\u03b1\7)\2\2\u03ae\u03b0\n#\2\2\u03af\u03ae"+
		"\3\2\2\2\u03b0\u03b3\3\2\2\2\u03b1\u03af\3\2\2\2\u03b1\u03b2\3\2\2\2\u03b2"+
		"\u03b4\3\2\2\2\u03b3\u03b1\3\2\2\2\u03b4\u03b5\7)\2\2\u03b5\u0131\3\2"+
		"\2\2\u03b6\u03ba\7$\2\2\u03b7\u03b9\n$\2\2\u03b8\u03b7\3\2\2\2\u03b9\u03bc"+
		"\3\2\2\2\u03ba\u03b8\3\2\2\2\u03ba\u03bb\3\2\2\2\u03bb\u03bd\3\2\2\2\u03bc"+
		"\u03ba\3\2\2\2\u03bd\u03be\7$\2\2\u03be\u0133\3\2\2\2\u03bf\u03c3\t%\2"+
		"\2\u03c0\u03c2\t&\2\2\u03c1\u03c0\3\2\2\2\u03c2\u03c5\3\2\2\2\u03c3\u03c1"+
		"\3\2\2\2\u03c3\u03c4\3\2\2\2\u03c4\u0135\3\2\2\2\u03c5\u03c3\3\2\2\2\u03c6"+
		"\u03c7\5\u0134\u009a\2\u03c7\u03c8\7<\2\2\u03c8\u0137\3\2\2\2\u03c9\u03cb"+
		"\n\'\2\2\u03ca\u03c9\3\2\2\2\u03cb\u03cc\3\2\2\2\u03cc\u03ca\3\2\2\2\u03cc"+
		"\u03cd\3\2\2\2\u03cd\u0139\3\2\2\2\u03ce\u03cf\7*\2\2\u03cf\u013b\3\2"+
		"\2\2\u03d0\u03d1\7+\2\2\u03d1\u013d\3\2\2\2\u03d2\u03d3\7.\2\2\u03d3\u013f"+
		"\3\2\2\2\u03d4\u03d5\7-\2\2\u03d5\u0141\3\2\2\2\u03d6\u03d7\7/\2\2\u03d7"+
		"\u0143\3\2\2\2\u03d8\u03d9\7,\2\2\u03d9\u0145\3\2\2\2\u03da\u03db\7\61"+
		"\2\2\u03db\u0147\3\2\2\2\u03dc\u03dd\7?\2\2\u03dd\u0149\3\2\2\2\u03de"+
		"\u03df\7@\2\2\u03df\u014b\3\2\2\2\u03e0\u03e1\7@\2\2\u03e1\u03e2\7?\2"+
		"\2\u03e2\u014d\3\2\2\2\u03e3\u03e4\7>\2\2\u03e4\u014f\3\2\2\2\u03e5\u03e6"+
		"\7>\2\2\u03e6\u03e7\7?\2\2\u03e7\u0151\3\2\2\2\u03e8\u03e9\7\'\2\2\u03e9"+
		"\u0153\3\2\2\2\u03ea\u03eb\7@\2\2\u03eb\u03ec\7@\2\2\u03ec\u0155\3\2\2"+
		"\2\u03ed\u03ee\7>\2\2\u03ee\u03ef\7>\2\2\u03ef\u0157\3\2\2\2\u03f0\u03f1"+
		"\7\u0080\2\2\u03f1\u0159\3\2\2\2\u03f2\u03f3\7(\2\2\u03f3\u015b\3\2\2"+
		"\2\u03f4\u03f5\7~\2\2\u03f5\u015d\3\2\2\2\u03f6\u03f7\7`\2\2\u03f7\u015f"+
		"\3\2\2\2\u03f8\u03fa\t\35\2\2\u03f9\u03f8\3\2\2\2\u03fa\u03fb\3\2\2\2"+
		"\u03fb\u03f9\3\2\2\2\u03fb\u03fc\3\2\2\2\u03fc\u03fd\3\2\2\2\u03fd\u03fe"+
		"\b\u00b0\2\2\u03fe\u0161\3\2\2\2\u03ff\u0401\7\17\2\2\u0400\u03ff\3\2"+
		"\2\2\u0400\u0401\3\2\2\2\u0401\u0402\3\2\2\2\u0402\u0403\7\f\2\2\u0403"+
		"\u0163\3\2\2\2\24\2\3\u0169\u016e\u0179\u02f8\u038e\u0393\u0396\u039b"+
		"\u03a2\u03a9\u03b1\u03ba\u03c3\u03cc\u03fb\u0400\5\b\2\2\7\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}