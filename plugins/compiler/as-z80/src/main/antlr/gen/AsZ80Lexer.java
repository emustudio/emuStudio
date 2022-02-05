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
			"PREP_ORG", "PREP_EQU", "PREP_SET", "PREP_VAR", "PREP_IF", "PREP_ENDIF", 
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
			return true;
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\u009a\u040b\b\1\b"+
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
		"\4\u00b2\t\u00b2\3\2\3\2\3\2\3\2\3\2\5\2\u016c\n\2\3\2\7\2\u016f\n\2\f"+
		"\2\16\2\u0172\13\2\3\2\3\2\3\3\3\3\3\3\3\3\7\3\u017a\n\3\f\3\16\3\u017d"+
		"\13\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t"+
		"\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3"+
		"\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3"+
		"\30\3\30\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35\3\35\3"+
		"\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3"+
		"!\3\"\3\"\3\"\3\"\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3\'"+
		"\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3+\3+\3+\3,\3,\3"+
		",\3,\3,\3-\3-\3-\3.\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\61\3"+
		"\61\3\61\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3"+
		"\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67\38\38"+
		"\38\38\38\39\39\39\39\39\3:\3:\3:\3;\3;\3;\3;\3<\3<\3<\3<\3<\3=\3=\3="+
		"\3=\3>\3>\3>\3>\3>\3?\3?\3?\3?\3@\3@\3@\3@\3A\3A\3A\3B\3B\3B\3B\3B\3C"+
		"\3C\3C\3C\3C\3D\3D\3D\3D\3E\3E\3E\3E\3E\3F\3F\3F\3F\3F\3G\3G\3G\3G\3H"+
		"\3H\3H\3H\3H\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3K\3K\3K\3K\3K\3L\3L\3L\3L"+
		"\3L\3M\3M\3M\3N\3N\3N\3N\3O\3O\3O\3O\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R"+
		"\3R\3S\3S\3S\3S\3T\3T\3T\3T\3U\3U\3U\3U\3U\3V\3V\3V\3V\3W\3W\3W\3W\3X"+
		"\3X\3X\3X\3Y\3Y\3Y\3Y\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3]\3]\3"+
		"]\3]\3^\3^\3^\3^\3_\3_\3_\3_\3`\3`\3`\3`\3a\3a\3a\3a\3b\3b\3b\3b\3b\3"+
		"c\3c\3c\3c\3d\3d\3d\3d\3d\3e\3e\3e\3e\3f\3f\3f\3f\3g\3g\3g\3g\3g\3h\3"+
		"h\3h\3h\3h\3i\6i\u02f9\ni\ri\16i\u02fa\3i\3i\3j\3j\3j\3j\3j\3k\3k\3k\3"+
		"k\3l\3l\3l\3l\3m\3m\3m\3m\3n\3n\3n\3n\3o\3o\3o\3p\3p\3p\3p\3p\3p\3q\3"+
		"q\3q\3q\3q\3q\3q\3q\3r\3r\3r\3r\3r\3r\3s\3s\3s\3s\3s\3t\3t\3t\3u\3u\3"+
		"u\3v\3v\3v\3w\3w\3x\3x\3y\3y\3z\3z\3{\3{\3|\3|\3}\3}\3~\3~\3\177\3\177"+
		"\3\177\3\u0080\3\u0080\3\u0080\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081"+
		"\3\u0082\3\u0082\3\u0082\3\u0083\3\u0083\3\u0083\3\u0083\3\u0084\3\u0084"+
		"\3\u0084\3\u0084\3\u0085\3\u0085\3\u0085\3\u0086\3\u0086\3\u0086\3\u0087"+
		"\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088\3\u0089\3\u0089\3\u0089\3\u008a"+
		"\3\u008a\3\u008a\3\u008a\3\u008b\3\u008b\3\u008c\3\u008c\3\u008d\3\u008d"+
		"\3\u008d\3\u008d\3\u008e\3\u008e\3\u008e\3\u008e\3\u008f\3\u008f\3\u008f"+
		"\3\u008f\3\u0090\3\u0090\3\u0090\3\u0090\3\u0091\3\u0091\3\u0091\3\u0091"+
		"\3\u0092\3\u0092\3\u0092\3\u0093\3\u0093\3\u0093\3\u0093\3\u0094\3\u0094"+
		"\3\u0094\6\u0094\u0394\n\u0094\r\u0094\16\u0094\u0395\3\u0095\6\u0095"+
		"\u0399\n\u0095\r\u0095\16\u0095\u039a\3\u0095\5\u0095\u039e\n\u0095\3"+
		"\u0096\6\u0096\u03a1\n\u0096\r\u0096\16\u0096\u03a2\3\u0096\3\u0096\3"+
		"\u0097\6\u0097\u03a8\n\u0097\r\u0097\16\u0097\u03a9\3\u0097\3\u0097\3"+
		"\u0098\6\u0098\u03af\n\u0098\r\u0098\16\u0098\u03b0\3\u0098\3\u0098\3"+
		"\u0099\3\u0099\7\u0099\u03b7\n\u0099\f\u0099\16\u0099\u03ba\13\u0099\3"+
		"\u0099\3\u0099\3\u009a\3\u009a\7\u009a\u03c0\n\u009a\f\u009a\16\u009a"+
		"\u03c3\13\u009a\3\u009a\3\u009a\3\u009b\3\u009b\7\u009b\u03c9\n\u009b"+
		"\f\u009b\16\u009b\u03cc\13\u009b\3\u009c\3\u009c\3\u009c\3\u009d\6\u009d"+
		"\u03d2\n\u009d\r\u009d\16\u009d\u03d3\3\u009e\3\u009e\3\u009f\3\u009f"+
		"\3\u00a0\3\u00a0\3\u00a1\3\u00a1\3\u00a2\3\u00a2\3\u00a3\3\u00a3\3\u00a4"+
		"\3\u00a4\3\u00a5\3\u00a5\3\u00a6\3\u00a6\3\u00a7\3\u00a7\3\u00a7\3\u00a8"+
		"\3\u00a8\3\u00a9\3\u00a9\3\u00a9\3\u00aa\3\u00aa\3\u00ab\3\u00ab\3\u00ab"+
		"\3\u00ac\3\u00ac\3\u00ac\3\u00ad\3\u00ad\3\u00ae\3\u00ae\3\u00af\3\u00af"+
		"\3\u00b0\3\u00b0\3\u00b1\6\u00b1\u0401\n\u00b1\r\u00b1\16\u00b1\u0402"+
		"\3\u00b1\3\u00b1\3\u00b2\5\u00b2\u0408\n\u00b2\3\u00b2\3\u00b2\3\u017b"+
		"\2\u00b3\4\3\6\4\b\2\n\2\f\2\16\2\20\2\22\2\24\2\26\2\30\2\32\2\34\2\36"+
		"\2 \2\"\2$\2&\2(\2*\2,\2.\2\60\2\62\2\64\2\66\28\2:\5<\6>\7@\bB\tD\nF"+
		"\13H\fJ\rL\16N\17P\20R\21T\22V\23X\24Z\25\\\26^\27`\30b\31d\32f\33h\34"+
		"j\35l\36n\37p r!t\"v#x$z%|&~\'\u0080(\u0082)\u0084*\u0086+\u0088,\u008a"+
		"-\u008c.\u008e/\u0090\60\u0092\61\u0094\62\u0096\63\u0098\64\u009a\65"+
		"\u009c\66\u009e\67\u00a08\u00a29\u00a4:\u00a6;\u00a8<\u00aa=\u00ac>\u00ae"+
		"?\u00b0@\u00b2A\u00b4B\u00b6C\u00b8D\u00baE\u00bcF\u00beG\u00c0H\u00c2"+
		"I\u00c4J\u00c6K\u00c8L\u00caM\u00ccN\u00ceO\u00d0P\u00d2Q\u00d4R\u00d6"+
		"S\u00d8T\u00daU\u00dcV\u00deW\u00e0X\u00e2Y\u00e4Z\u00e6[\u00e8\\\u00ea"+
		"]\u00ec^\u00ee_\u00f0`\u00f2a\u00f4b\u00f6c\u00f8d\u00fae\u00fcf\u00fe"+
		"g\u0100h\u0102i\u0104j\u0106k\u0108l\u010am\u010cn\u010eo\u0110p\u0112"+
		"q\u0114r\u0116s\u0118t\u011au\u011cv\u011ew\u0120x\u0122y\u0124z\u0126"+
		"{\u0128|\u012a}\u012c~\u012e\177\u0130\u0080\u0132\u0081\u0134\u0082\u0136"+
		"\u0083\u0138\u0084\u013a\u0085\u013c\u0086\u013e\u0087\u0140\u0088\u0142"+
		"\u0089\u0144\u008a\u0146\u008b\u0148\u008c\u014a\u008d\u014c\u008e\u014e"+
		"\u008f\u0150\u0090\u0152\u0091\u0154\u0092\u0156\u0093\u0158\u0094\u015a"+
		"\u0095\u015c\u0096\u015e\u0097\u0160\u0098\u0162\u0099\u0164\u009a\4\2"+
		"\3(\4\2%%==\4\2\f\f\17\17\4\2CCcc\4\2DDdd\4\2EEee\4\2FFff\4\2GGgg\4\2"+
		"HHhh\4\2IIii\4\2JJjj\4\2KKkk\4\2LLll\4\2NNnn\4\2OOoo\4\2PPpp\4\2QQqq\4"+
		"\2RRrr\4\2SSss\4\2TTtt\4\2UUuu\4\2VVvv\4\2WWww\4\2XXxx\4\2YYyy\4\2ZZz"+
		"z\4\2[[{{\4\2\\\\||\5\2\13\13\16\16\"\"\5\2\62;CHch\3\2\62;\3\2\629\6"+
		"\2QQSSqqss\3\2\62\63\3\2))\3\2$$\5\2A\\aac|\6\2\62;A\\aac|\b\2\13\f\16"+
		"\17\"\"*/\61\61??\2\u0401\2\4\3\2\2\2\2\6\3\2\2\2\2:\3\2\2\2\2<\3\2\2"+
		"\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2\2\2F\3\2\2\2\2H\3\2\2\2\2"+
		"J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2\2\2R\3\2\2\2\2T\3\2\2\2\2V\3"+
		"\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2\2\2\2^\3\2\2\2\2`\3\2\2\2\2b\3\2"+
		"\2\2\2d\3\2\2\2\2f\3\2\2\2\2h\3\2\2\2\2j\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2"+
		"\2p\3\2\2\2\2r\3\2\2\2\2t\3\2\2\2\2v\3\2\2\2\2x\3\2\2\2\2z\3\2\2\2\2|"+
		"\3\2\2\2\2~\3\2\2\2\2\u0080\3\2\2\2\2\u0082\3\2\2\2\2\u0084\3\2\2\2\2"+
		"\u0086\3\2\2\2\2\u0088\3\2\2\2\2\u008a\3\2\2\2\2\u008c\3\2\2\2\2\u008e"+
		"\3\2\2\2\2\u0090\3\2\2\2\2\u0092\3\2\2\2\2\u0094\3\2\2\2\2\u0096\3\2\2"+
		"\2\2\u0098\3\2\2\2\2\u009a\3\2\2\2\2\u009c\3\2\2\2\2\u009e\3\2\2\2\2\u00a0"+
		"\3\2\2\2\2\u00a2\3\2\2\2\2\u00a4\3\2\2\2\2\u00a6\3\2\2\2\2\u00a8\3\2\2"+
		"\2\2\u00aa\3\2\2\2\2\u00ac\3\2\2\2\2\u00ae\3\2\2\2\2\u00b0\3\2\2\2\2\u00b2"+
		"\3\2\2\2\2\u00b4\3\2\2\2\2\u00b6\3\2\2\2\2\u00b8\3\2\2\2\2\u00ba\3\2\2"+
		"\2\2\u00bc\3\2\2\2\2\u00be\3\2\2\2\2\u00c0\3\2\2\2\2\u00d6\3\2\2\2\2\u00d8"+
		"\3\2\2\2\2\u00da\3\2\2\2\2\u00dc\3\2\2\2\2\u00de\3\2\2\2\2\u00e0\3\2\2"+
		"\2\2\u00e2\3\2\2\2\2\u00e4\3\2\2\2\2\u00e6\3\2\2\2\2\u00e8\3\2\2\2\2\u00ea"+
		"\3\2\2\2\2\u00ec\3\2\2\2\2\u00ee\3\2\2\2\2\u00f0\3\2\2\2\2\u00f2\3\2\2"+
		"\2\2\u00f4\3\2\2\2\2\u00f6\3\2\2\2\2\u00f8\3\2\2\2\2\u00fa\3\2\2\2\2\u00fc"+
		"\3\2\2\2\2\u00fe\3\2\2\2\2\u0100\3\2\2\2\2\u0102\3\2\2\2\2\u0104\3\2\2"+
		"\2\2\u0106\3\2\2\2\2\u0108\3\2\2\2\2\u010a\3\2\2\2\2\u010c\3\2\2\2\2\u010e"+
		"\3\2\2\2\2\u0110\3\2\2\2\2\u0112\3\2\2\2\2\u0114\3\2\2\2\2\u0116\3\2\2"+
		"\2\2\u0118\3\2\2\2\2\u011a\3\2\2\2\2\u011c\3\2\2\2\2\u011e\3\2\2\2\2\u0120"+
		"\3\2\2\2\2\u0122\3\2\2\2\2\u0124\3\2\2\2\2\u0126\3\2\2\2\2\u0128\3\2\2"+
		"\2\2\u012a\3\2\2\2\2\u012c\3\2\2\2\2\u012e\3\2\2\2\2\u0130\3\2\2\2\2\u0132"+
		"\3\2\2\2\2\u0134\3\2\2\2\2\u0136\3\2\2\2\2\u0138\3\2\2\2\2\u013a\3\2\2"+
		"\2\2\u013c\3\2\2\2\2\u013e\3\2\2\2\2\u0140\3\2\2\2\2\u0142\3\2\2\2\2\u0144"+
		"\3\2\2\2\2\u0146\3\2\2\2\2\u0148\3\2\2\2\2\u014a\3\2\2\2\2\u014c\3\2\2"+
		"\2\2\u014e\3\2\2\2\2\u0150\3\2\2\2\2\u0152\3\2\2\2\2\u0154\3\2\2\2\2\u0156"+
		"\3\2\2\2\2\u0158\3\2\2\2\2\u015a\3\2\2\2\2\u015c\3\2\2\2\2\u015e\3\2\2"+
		"\2\2\u0160\3\2\2\2\2\u0162\3\2\2\2\2\u0164\3\2\2\2\3\u00c2\3\2\2\2\3\u00c4"+
		"\3\2\2\2\3\u00c6\3\2\2\2\3\u00c8\3\2\2\2\3\u00ca\3\2\2\2\3\u00cc\3\2\2"+
		"\2\3\u00ce\3\2\2\2\3\u00d0\3\2\2\2\3\u00d2\3\2\2\2\3\u00d4\3\2\2\2\4\u016b"+
		"\3\2\2\2\6\u0175\3\2\2\2\b\u0183\3\2\2\2\n\u0185\3\2\2\2\f\u0187\3\2\2"+
		"\2\16\u0189\3\2\2\2\20\u018b\3\2\2\2\22\u018d\3\2\2\2\24\u018f\3\2\2\2"+
		"\26\u0191\3\2\2\2\30\u0193\3\2\2\2\32\u0195\3\2\2\2\34\u0197\3\2\2\2\36"+
		"\u0199\3\2\2\2 \u019b\3\2\2\2\"\u019d\3\2\2\2$\u019f\3\2\2\2&\u01a1\3"+
		"\2\2\2(\u01a3\3\2\2\2*\u01a5\3\2\2\2,\u01a7\3\2\2\2.\u01a9\3\2\2\2\60"+
		"\u01ab\3\2\2\2\62\u01ad\3\2\2\2\64\u01af\3\2\2\2\66\u01b1\3\2\2\28\u01b3"+
		"\3\2\2\2:\u01b5\3\2\2\2<\u01b9\3\2\2\2>\u01bd\3\2\2\2@\u01c1\3\2\2\2B"+
		"\u01c5\3\2\2\2D\u01cc\3\2\2\2F\u01d0\3\2\2\2H\u01d3\3\2\2\2J\u01d7\3\2"+
		"\2\2L\u01dc\3\2\2\2N\u01e0\3\2\2\2P\u01e5\3\2\2\2R\u01e9\3\2\2\2T\u01ed"+
		"\3\2\2\2V\u01f1\3\2\2\2X\u01f4\3\2\2\2Z\u01f9\3\2\2\2\\\u01fc\3\2\2\2"+
		"^\u01ff\3\2\2\2`\u0203\3\2\2\2b\u0208\3\2\2\2d\u020b\3\2\2\2f\u020e\3"+
		"\2\2\2h\u0212\3\2\2\2j\u0216\3\2\2\2l\u021b\3\2\2\2n\u021f\3\2\2\2p\u0224"+
		"\3\2\2\2r\u0229\3\2\2\2t\u022e\3\2\2\2v\u0231\3\2\2\2x\u0235\3\2\2\2z"+
		"\u023a\3\2\2\2|\u023e\3\2\2\2~\u0243\3\2\2\2\u0080\u0247\3\2\2\2\u0082"+
		"\u024b\3\2\2\2\u0084\u024e\3\2\2\2\u0086\u0253\3\2\2\2\u0088\u0258\3\2"+
		"\2\2\u008a\u025c\3\2\2\2\u008c\u0261\3\2\2\2\u008e\u0266\3\2\2\2\u0090"+
		"\u026a\3\2\2\2\u0092\u026f\3\2\2\2\u0094\u0273\3\2\2\2\u0096\u0279\3\2"+
		"\2\2\u0098\u027e\3\2\2\2\u009a\u0283\3\2\2\2\u009c\u0286\3\2\2\2\u009e"+
		"\u028a\3\2\2\2\u00a0\u028e\3\2\2\2\u00a2\u0293\3\2\2\2\u00a4\u0297\3\2"+
		"\2\2\u00a6\u029a\3\2\2\2\u00a8\u029e\3\2\2\2\u00aa\u02a2\3\2\2\2\u00ac"+
		"\u02a7\3\2\2\2\u00ae\u02ab\3\2\2\2\u00b0\u02af\3\2\2\2\u00b2\u02b3\3\2"+
		"\2\2\u00b4\u02b7\3\2\2\2\u00b6\u02bb\3\2\2\2\u00b8\u02bf\3\2\2\2\u00ba"+
		"\u02c3\3\2\2\2\u00bc\u02c7\3\2\2\2\u00be\u02cb\3\2\2\2\u00c0\u02cf\3\2"+
		"\2\2\u00c2\u02d3\3\2\2\2\u00c4\u02d7\3\2\2\2\u00c6\u02dc\3\2\2\2\u00c8"+
		"\u02e0\3\2\2\2\u00ca\u02e5\3\2\2\2\u00cc\u02e9\3\2\2\2\u00ce\u02ed\3\2"+
		"\2\2\u00d0\u02f2\3\2\2\2\u00d2\u02f8\3\2\2\2\u00d4\u02fe\3\2\2\2\u00d6"+
		"\u0303\3\2\2\2\u00d8\u0307\3\2\2\2\u00da\u030b\3\2\2\2\u00dc\u030f\3\2"+
		"\2\2\u00de\u0313\3\2\2\2\u00e0\u0316\3\2\2\2\u00e2\u031c\3\2\2\2\u00e4"+
		"\u0324\3\2\2\2\u00e6\u032a\3\2\2\2\u00e8\u032f\3\2\2\2\u00ea\u0332\3\2"+
		"\2\2\u00ec\u0335\3\2\2\2\u00ee\u0338\3\2\2\2\u00f0\u033a\3\2\2\2\u00f2"+
		"\u033c\3\2\2\2\u00f4\u033e\3\2\2\2\u00f6\u0340\3\2\2\2\u00f8\u0342\3\2"+
		"\2\2\u00fa\u0344\3\2\2\2\u00fc\u0346\3\2\2\2\u00fe\u0348\3\2\2\2\u0100"+
		"\u034b\3\2\2\2\u0102\u034f\3\2\2\2\u0104\u0353\3\2\2\2\u0106\u0356\3\2"+
		"\2\2\u0108\u035a\3\2\2\2\u010a\u035e\3\2\2\2\u010c\u0361\3\2\2\2\u010e"+
		"\u0364\3\2\2\2\u0110\u0367\3\2\2\2\u0112\u036a\3\2\2\2\u0114\u036d\3\2"+
		"\2\2\u0116\u0371\3\2\2\2\u0118\u0373\3\2\2\2\u011a\u0375\3\2\2\2\u011c"+
		"\u0379\3\2\2\2\u011e\u037d\3\2\2\2\u0120\u0381\3\2\2\2\u0122\u0385\3\2"+
		"\2\2\u0124\u0389\3\2\2\2\u0126\u038c\3\2\2\2\u0128\u0390\3\2\2\2\u012a"+
		"\u0398\3\2\2\2\u012c\u03a0\3\2\2\2\u012e\u03a7\3\2\2\2\u0130\u03ae\3\2"+
		"\2\2\u0132\u03b4\3\2\2\2\u0134\u03bd\3\2\2\2\u0136\u03c6\3\2\2\2\u0138"+
		"\u03cd\3\2\2\2\u013a\u03d1\3\2\2\2\u013c\u03d5\3\2\2\2\u013e\u03d7\3\2"+
		"\2\2\u0140\u03d9\3\2\2\2\u0142\u03db\3\2\2\2\u0144\u03dd\3\2\2\2\u0146"+
		"\u03df\3\2\2\2\u0148\u03e1\3\2\2\2\u014a\u03e3\3\2\2\2\u014c\u03e5\3\2"+
		"\2\2\u014e\u03e7\3\2\2\2\u0150\u03ea\3\2\2\2\u0152\u03ec\3\2\2\2\u0154"+
		"\u03ef\3\2\2\2\u0156\u03f1\3\2\2\2\u0158\u03f4\3\2\2\2\u015a\u03f7\3\2"+
		"\2\2\u015c\u03f9\3\2\2\2\u015e\u03fb\3\2\2\2\u0160\u03fd\3\2\2\2\u0162"+
		"\u0400\3\2\2\2\u0164\u0407\3\2\2\2\u0166\u0167\7\61\2\2\u0167\u016c\7"+
		"\61\2\2\u0168\u0169\7/\2\2\u0169\u016c\7/\2\2\u016a\u016c\t\2\2\2\u016b"+
		"\u0166\3\2\2\2\u016b\u0168\3\2\2\2\u016b\u016a\3\2\2\2\u016c\u0170\3\2"+
		"\2\2\u016d\u016f\n\3\2\2\u016e\u016d\3\2\2\2\u016f\u0172\3\2\2\2\u0170"+
		"\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0173\3\2\2\2\u0172\u0170\3\2"+
		"\2\2\u0173\u0174\b\2\2\2\u0174\5\3\2\2\2\u0175\u0176\7\61\2\2\u0176\u0177"+
		"\7,\2\2\u0177\u017b\3\2\2\2\u0178\u017a\13\2\2\2\u0179\u0178\3\2\2\2\u017a"+
		"\u017d\3\2\2\2\u017b\u017c\3\2\2\2\u017b\u0179\3\2\2\2\u017c\u017e\3\2"+
		"\2\2\u017d\u017b\3\2\2\2\u017e\u017f\7,\2\2\u017f\u0180\7\61\2\2\u0180"+
		"\u0181\3\2\2\2\u0181\u0182\b\3\2\2\u0182\7\3\2\2\2\u0183\u0184\t\4\2\2"+
		"\u0184\t\3\2\2\2\u0185\u0186\t\5\2\2\u0186\13\3\2\2\2\u0187\u0188\t\6"+
		"\2\2\u0188\r\3\2\2\2\u0189\u018a\t\7\2\2\u018a\17\3\2\2\2\u018b\u018c"+
		"\t\b\2\2\u018c\21\3\2\2\2\u018d\u018e\t\t\2\2\u018e\23\3\2\2\2\u018f\u0190"+
		"\t\n\2\2\u0190\25\3\2\2\2\u0191\u0192\t\13\2\2\u0192\27\3\2\2\2\u0193"+
		"\u0194\t\f\2\2\u0194\31\3\2\2\2\u0195\u0196\t\r\2\2\u0196\33\3\2\2\2\u0197"+
		"\u0198\t\16\2\2\u0198\35\3\2\2\2\u0199\u019a\t\17\2\2\u019a\37\3\2\2\2"+
		"\u019b\u019c\t\20\2\2\u019c!\3\2\2\2\u019d\u019e\t\21\2\2\u019e#\3\2\2"+
		"\2\u019f\u01a0\t\22\2\2\u01a0%\3\2\2\2\u01a1\u01a2\t\23\2\2\u01a2\'\3"+
		"\2\2\2\u01a3\u01a4\t\24\2\2\u01a4)\3\2\2\2\u01a5\u01a6\t\25\2\2\u01a6"+
		"+\3\2\2\2\u01a7\u01a8\t\26\2\2\u01a8-\3\2\2\2\u01a9\u01aa\t\27\2\2\u01aa"+
		"/\3\2\2\2\u01ab\u01ac\t\30\2\2\u01ac\61\3\2\2\2\u01ad\u01ae\t\31\2\2\u01ae"+
		"\63\3\2\2\2\u01af\u01b0\t\32\2\2\u01b0\65\3\2\2\2\u01b1\u01b2\t\33\2\2"+
		"\u01b2\67\3\2\2\2\u01b3\u01b4\t\34\2\2\u01b49\3\2\2\2\u01b5\u01b6\5\b"+
		"\4\2\u01b6\u01b7\5\16\7\2\u01b7\u01b8\5\f\6\2\u01b8;\3\2\2\2\u01b9\u01ba"+
		"\5\b\4\2\u01ba\u01bb\5\16\7\2\u01bb\u01bc\5\16\7\2\u01bc=\3\2\2\2\u01bd"+
		"\u01be\5\b\4\2\u01be\u01bf\5 \20\2\u01bf\u01c0\5\16\7\2\u01c0?\3\2\2\2"+
		"\u01c1\u01c2\5\n\5\2\u01c2\u01c3\5\30\f\2\u01c3\u01c4\5,\26\2\u01c4A\3"+
		"\2\2\2\u01c5\u01c6\5\f\6\2\u01c6\u01c7\5\b\4\2\u01c7\u01c8\5\34\16\2\u01c8"+
		"\u01c9\5\34\16\2\u01c9\u01ca\3\2\2\2\u01ca\u01cb\b!\3\2\u01cbC\3\2\2\2"+
		"\u01cc\u01cd\5\f\6\2\u01cd\u01ce\5\f\6\2\u01ce\u01cf\5\22\t\2\u01cfE\3"+
		"\2\2\2\u01d0\u01d1\5\f\6\2\u01d1\u01d2\5$\22\2\u01d2G\3\2\2\2\u01d3\u01d4"+
		"\5\f\6\2\u01d4\u01d5\5$\22\2\u01d5\u01d6\5\16\7\2\u01d6I\3\2\2\2\u01d7"+
		"\u01d8\5\f\6\2\u01d8\u01d9\5$\22\2\u01d9\u01da\5\16\7\2\u01da\u01db\5"+
		"(\24\2\u01dbK\3\2\2\2\u01dc\u01dd\5\f\6\2\u01dd\u01de\5$\22\2\u01de\u01df"+
		"\5\30\f\2\u01dfM\3\2\2\2\u01e0\u01e1\5\f\6\2\u01e1\u01e2\5$\22\2\u01e2"+
		"\u01e3\5\30\f\2\u01e3\u01e4\5(\24\2\u01e4O\3\2\2\2\u01e5\u01e6\5\f\6\2"+
		"\u01e6\u01e7\5$\22\2\u01e7\u01e8\5\34\16\2\u01e8Q\3\2\2\2\u01e9\u01ea"+
		"\5\16\7\2\u01ea\u01eb\5\b\4\2\u01eb\u01ec\5\b\4\2\u01ecS\3\2\2\2\u01ed"+
		"\u01ee\5\16\7\2\u01ee\u01ef\5\20\b\2\u01ef\u01f0\5\f\6\2\u01f0U\3\2\2"+
		"\2\u01f1\u01f2\5\16\7\2\u01f2\u01f3\5\30\f\2\u01f3W\3\2\2\2\u01f4\u01f5"+
		"\5\16\7\2\u01f5\u01f6\5\32\r\2\u01f6\u01f7\5 \20\2\u01f7\u01f8\58\34\2"+
		"\u01f8Y\3\2\2\2\u01f9\u01fa\5\20\b\2\u01fa\u01fb\5\30\f\2\u01fb[\3\2\2"+
		"\2\u01fc\u01fd\5\20\b\2\u01fd\u01fe\5\64\32\2\u01fe]\3\2\2\2\u01ff\u0200"+
		"\5\20\b\2\u0200\u0201\5\64\32\2\u0201\u0202\5\64\32\2\u0202_\3\2\2\2\u0203"+
		"\u0204\5\26\13\2\u0204\u0205\5\b\4\2\u0205\u0206\5\34\16\2\u0206\u0207"+
		"\5,\26\2\u0207a\3\2\2\2\u0208\u0209\5\30\f\2\u0209\u020a\5\36\17\2\u020a"+
		"c\3\2\2\2\u020b\u020c\5\30\f\2\u020c\u020d\5 \20\2\u020de\3\2\2\2\u020e"+
		"\u020f\5\30\f\2\u020f\u0210\5 \20\2\u0210\u0211\5\f\6\2\u0211g\3\2\2\2"+
		"\u0212\u0213\5\30\f\2\u0213\u0214\5 \20\2\u0214\u0215\5\16\7\2\u0215i"+
		"\3\2\2\2\u0216\u0217\5\30\f\2\u0217\u0218\5 \20\2\u0218\u0219\5\16\7\2"+
		"\u0219\u021a\5(\24\2\u021ak\3\2\2\2\u021b\u021c\5\30\f\2\u021c\u021d\5"+
		" \20\2\u021d\u021e\5\30\f\2\u021em\3\2\2\2\u021f\u0220\5\30\f\2\u0220"+
		"\u0221\5 \20\2\u0221\u0222\5\30\f\2\u0222\u0223\5(\24\2\u0223o\3\2\2\2"+
		"\u0224\u0225\5\32\r\2\u0225\u0226\5$\22\2\u0226\u0227\3\2\2\2\u0227\u0228"+
		"\b8\3\2\u0228q\3\2\2\2\u0229\u022a\5\32\r\2\u022a\u022b\5(\24\2\u022b"+
		"\u022c\3\2\2\2\u022c\u022d\b9\3\2\u022ds\3\2\2\2\u022e\u022f\5\34\16\2"+
		"\u022f\u0230\5\16\7\2\u0230u\3\2\2\2\u0231\u0232\5\34\16\2\u0232\u0233"+
		"\5\16\7\2\u0233\u0234\5\16\7\2\u0234w\3\2\2\2\u0235\u0236\5\34\16\2\u0236"+
		"\u0237\5\16\7\2\u0237\u0238\5\16\7\2\u0238\u0239\5(\24\2\u0239y\3\2\2"+
		"\2\u023a\u023b\5\34\16\2\u023b\u023c\5\16\7\2\u023c\u023d\5\30\f\2\u023d"+
		"{\3\2\2\2\u023e\u023f\5\34\16\2\u023f\u0240\5\16\7\2\u0240\u0241\5\30"+
		"\f\2\u0241\u0242\5(\24\2\u0242}\3\2\2\2\u0243\u0244\5 \20\2\u0244\u0245"+
		"\5\20\b\2\u0245\u0246\5\24\n\2\u0246\177\3\2\2\2\u0247\u0248\5 \20\2\u0248"+
		"\u0249\5\"\21\2\u0249\u024a\5$\22\2\u024a\u0081\3\2\2\2\u024b\u024c\5"+
		"\"\21\2\u024c\u024d\5(\24\2\u024d\u0083\3\2\2\2\u024e\u024f\5\"\21\2\u024f"+
		"\u0250\5,\26\2\u0250\u0251\5\16\7\2\u0251\u0252\5(\24\2\u0252\u0085\3"+
		"\2\2\2\u0253\u0254\5\"\21\2\u0254\u0255\5,\26\2\u0255\u0256\5\30\f\2\u0256"+
		"\u0257\5(\24\2\u0257\u0087\3\2\2\2\u0258\u0259\5\"\21\2\u0259\u025a\5"+
		".\27\2\u025a\u025b\5,\26\2\u025b\u0089\3\2\2\2\u025c\u025d\5\"\21\2\u025d"+
		"\u025e\5.\27\2\u025e\u025f\5,\26\2\u025f\u0260\5\16\7\2\u0260\u008b\3"+
		"\2\2\2\u0261\u0262\5\"\21\2\u0262\u0263\5.\27\2\u0263\u0264\5,\26\2\u0264"+
		"\u0265\5\30\f\2\u0265\u008d\3\2\2\2\u0266\u0267\5$\22\2\u0267\u0268\5"+
		"\"\21\2\u0268\u0269\5$\22\2\u0269\u008f\3\2\2\2\u026a\u026b\5$\22\2\u026b"+
		"\u026c\5.\27\2\u026c\u026d\5*\25\2\u026d\u026e\5\26\13\2\u026e\u0091\3"+
		"\2\2\2\u026f\u0270\5(\24\2\u0270\u0271\5\20\b\2\u0271\u0272\5*\25\2\u0272"+
		"\u0093\3\2\2\2\u0273\u0274\5(\24\2\u0274\u0275\5\20\b\2\u0275\u0276\5"+
		",\26\2\u0276\u0277\3\2\2\2\u0277\u0278\bJ\3\2\u0278\u0095\3\2\2\2\u0279"+
		"\u027a\5(\24\2\u027a\u027b\5\20\b\2\u027b\u027c\5,\26\2\u027c\u027d\5"+
		"\30\f\2\u027d\u0097\3\2\2\2\u027e\u027f\5(\24\2\u027f\u0280\5\20\b\2\u0280"+
		"\u0281\5,\26\2\u0281\u0282\5 \20\2\u0282\u0099\3\2\2\2\u0283\u0284\5("+
		"\24\2\u0284\u0285\5\34\16\2\u0285\u009b\3\2\2\2\u0286\u0287\5(\24\2\u0287"+
		"\u0288\5\34\16\2\u0288\u0289\5\b\4\2\u0289\u009d\3\2\2\2\u028a\u028b\5"+
		"(\24\2\u028b\u028c\5\34\16\2\u028c\u028d\5\f\6\2\u028d\u009f\3\2\2\2\u028e"+
		"\u028f\5(\24\2\u028f\u0290\5\34\16\2\u0290\u0291\5\f\6\2\u0291\u0292\5"+
		"\b\4\2\u0292\u00a1\3\2\2\2\u0293\u0294\5(\24\2\u0294\u0295\5\34\16\2\u0295"+
		"\u0296\5\16\7\2\u0296\u00a3\3\2\2\2\u0297\u0298\5(\24\2\u0298\u0299\5"+
		"(\24\2\u0299\u00a5\3\2\2\2\u029a\u029b\5(\24\2\u029b\u029c\5(\24\2\u029c"+
		"\u029d\5\b\4\2\u029d\u00a7\3\2\2\2\u029e\u029f\5(\24\2\u029f\u02a0\5("+
		"\24\2\u02a0\u02a1\5\f\6\2\u02a1\u00a9\3\2\2\2\u02a2\u02a3\5(\24\2\u02a3"+
		"\u02a4\5(\24\2\u02a4\u02a5\5\f\6\2\u02a5\u02a6\5\b\4\2\u02a6\u00ab\3\2"+
		"\2\2\u02a7\u02a8\5(\24\2\u02a8\u02a9\5(\24\2\u02a9\u02aa\5\16\7\2\u02aa"+
		"\u00ad\3\2\2\2\u02ab\u02ac\5(\24\2\u02ac\u02ad\5*\25\2\u02ad\u02ae\5,"+
		"\26\2\u02ae\u00af\3\2\2\2\u02af\u02b0\5*\25\2\u02b0\u02b1\5\n\5\2\u02b1"+
		"\u02b2\5\f\6\2\u02b2\u00b1\3\2\2\2\u02b3\u02b4\5*\25\2\u02b4\u02b5\5\f"+
		"\6\2\u02b5\u02b6\5\22\t\2\u02b6\u00b3\3\2\2\2\u02b7\u02b8\5*\25\2\u02b8"+
		"\u02b9\5\20\b\2\u02b9\u02ba\5,\26\2\u02ba\u00b5\3\2\2\2\u02bb\u02bc\5"+
		"*\25\2\u02bc\u02bd\5\34\16\2\u02bd\u02be\5\b\4\2\u02be\u00b7\3\2\2\2\u02bf"+
		"\u02c0\5*\25\2\u02c0\u02c1\5(\24\2\u02c1\u02c2\5\b\4\2\u02c2\u00b9\3\2"+
		"\2\2\u02c3\u02c4\5*\25\2\u02c4\u02c5\5\34\16\2\u02c5\u02c6\5\34\16\2\u02c6"+
		"\u00bb\3\2\2\2\u02c7\u02c8\5*\25\2\u02c8\u02c9\5(\24\2\u02c9\u02ca\5\34"+
		"\16\2\u02ca\u00bd\3\2\2\2\u02cb\u02cc\5*\25\2\u02cc\u02cd\5.\27\2\u02cd"+
		"\u02ce\5\n\5\2\u02ce\u00bf\3\2\2\2\u02cf\u02d0\5\64\32\2\u02d0\u02d1\5"+
		"\"\21\2\u02d1\u02d2\5(\24\2\u02d2\u00c1\3\2\2\2\u02d3\u02d4\5\f\6\2\u02d4"+
		"\u02d5\3\2\2\2\u02d5\u02d6\ba\4\2\u02d6\u00c3\3\2\2\2\u02d7\u02d8\5 \20"+
		"\2\u02d8\u02d9\5\f\6\2\u02d9\u02da\3\2\2\2\u02da\u02db\bb\4\2\u02db\u00c5"+
		"\3\2\2\2\u02dc\u02dd\58\34\2\u02dd\u02de\3\2\2\2\u02de\u02df\bc\4\2\u02df"+
		"\u00c7\3\2\2\2\u02e0\u02e1\5 \20\2\u02e1\u02e2\58\34\2\u02e2\u02e3\3\2"+
		"\2\2\u02e3\u02e4\bd\4\2\u02e4\u00c9\3\2\2\2\u02e5\u02e6\5\36\17\2\u02e6"+
		"\u02e7\3\2\2\2\u02e7\u02e8\be\4\2\u02e8\u00cb\3\2\2\2\u02e9\u02ea\5$\22"+
		"\2\u02ea\u02eb\3\2\2\2\u02eb\u02ec\bf\4\2\u02ec\u00cd\3\2\2\2\u02ed\u02ee"+
		"\5$\22\2\u02ee\u02ef\5\20\b\2\u02ef\u02f0\3\2\2\2\u02f0\u02f1\bg\4\2\u02f1"+
		"\u00cf\3\2\2\2\u02f2\u02f3\5$\22\2\u02f3\u02f4\5\"\21\2\u02f4\u02f5\3"+
		"\2\2\2\u02f5\u02f6\bh\4\2\u02f6\u00d1\3\2\2\2\u02f7\u02f9\t\35\2\2\u02f8"+
		"\u02f7\3\2\2\2\u02f9\u02fa\3\2\2\2\u02fa\u02f8\3\2\2\2\u02fa\u02fb\3\2"+
		"\2\2\u02fb\u02fc\3\2\2\2\u02fc\u02fd\bi\2\2\u02fd\u00d3\3\2\2\2\u02fe"+
		"\u02ff\6j\2\2\u02ff\u0300\3\2\2\2\u0300\u0301\bj\4\2\u0301\u0302\bj\5"+
		"\2\u0302\u00d5\3\2\2\2\u0303\u0304\5\"\21\2\u0304\u0305\5(\24\2\u0305"+
		"\u0306\5\24\n\2\u0306\u00d7\3\2\2\2\u0307\u0308\5\20\b\2\u0308\u0309\5"+
		"&\23\2\u0309\u030a\5.\27\2\u030a\u00d9\3\2\2\2\u030b\u030c\5*\25\2\u030c"+
		"\u030d\5\20\b\2\u030d\u030e\5,\26\2\u030e\u00db\3\2\2\2\u030f\u0310\5"+
		"\60\30\2\u0310\u0311\5\b\4\2\u0311\u0312\5(\24\2\u0312\u00dd\3\2\2\2\u0313"+
		"\u0314\5\30\f\2\u0314\u0315\5\22\t\2\u0315\u00df\3\2\2\2\u0316\u0317\5"+
		"\20\b\2\u0317\u0318\5 \20\2\u0318\u0319\5\16\7\2\u0319\u031a\5\30\f\2"+
		"\u031a\u031b\5\22\t\2\u031b\u00e1\3\2\2\2\u031c\u031d\5\30\f\2\u031d\u031e"+
		"\5 \20\2\u031e\u031f\5\f\6\2\u031f\u0320\5\34\16\2\u0320\u0321\5.\27\2"+
		"\u0321\u0322\5\16\7\2\u0322\u0323\5\20\b\2\u0323\u00e3\3\2\2\2\u0324\u0325"+
		"\5\36\17\2\u0325\u0326\5\b\4\2\u0326\u0327\5\f\6\2\u0327\u0328\5(\24\2"+
		"\u0328\u0329\5\"\21\2\u0329\u00e5\3\2\2\2\u032a\u032b\5\20\b\2\u032b\u032c"+
		"\5 \20\2\u032c\u032d\5\16\7\2\u032d\u032e\5\36\17\2\u032e\u00e7\3\2\2"+
		"\2\u032f\u0330\5\16\7\2\u0330\u0331\5\n\5\2\u0331\u00e9\3\2\2\2\u0332"+
		"\u0333\5\16\7\2\u0333\u0334\5\62\31\2\u0334\u00eb\3\2\2\2\u0335\u0336"+
		"\5\16\7\2\u0336\u0337\5*\25\2\u0337\u00ed\3\2\2\2\u0338\u0339\7&\2\2\u0339"+
		"\u00ef\3\2\2\2\u033a\u033b\5\b\4\2\u033b\u00f1\3\2\2\2\u033c\u033d\5\n"+
		"\5\2\u033d\u00f3\3\2\2\2\u033e\u033f\5\f\6\2\u033f\u00f5\3\2\2\2\u0340"+
		"\u0341\5\16\7\2\u0341\u00f7\3\2\2\2\u0342\u0343\5\20\b\2\u0343\u00f9\3"+
		"\2\2\2\u0344\u0345\5\26\13\2\u0345\u00fb\3\2\2\2\u0346\u0347\5\34\16\2"+
		"\u0347\u00fd\3\2\2\2\u0348\u0349\5\30\f\2\u0349\u034a\5\64\32\2\u034a"+
		"\u00ff\3\2\2\2\u034b\u034c\5\30\f\2\u034c\u034d\5\64\32\2\u034d\u034e"+
		"\5\26\13\2\u034e\u0101\3\2\2\2\u034f\u0350\5\30\f\2\u0350\u0351\5\64\32"+
		"\2\u0351\u0352\5\34\16\2\u0352\u0103\3\2\2\2\u0353\u0354\5\30\f\2\u0354"+
		"\u0355\5\66\33\2\u0355\u0105\3\2\2\2\u0356\u0357\5\30\f\2\u0357\u0358"+
		"\5\66\33\2\u0358\u0359\5\26\13\2\u0359\u0107\3\2\2\2\u035a\u035b\5\30"+
		"\f\2\u035b\u035c\5\66\33\2\u035c\u035d\5\34\16\2\u035d\u0109\3\2\2\2\u035e"+
		"\u035f\5\n\5\2\u035f\u0360\5\f\6\2\u0360\u010b\3\2\2\2\u0361\u0362\5\16"+
		"\7\2\u0362\u0363\5\20\b\2\u0363\u010d\3\2\2\2\u0364\u0365\5\26\13\2\u0365"+
		"\u0366\5\34\16\2\u0366\u010f\3\2\2\2\u0367\u0368\5*\25\2\u0368\u0369\5"+
		"$\22\2\u0369\u0111\3\2\2\2\u036a\u036b\5\b\4\2\u036b\u036c\5\22\t\2\u036c"+
		"\u0113\3\2\2\2\u036d\u036e\5\b\4\2\u036e\u036f\5\22\t\2\u036f\u0370\7"+
		")\2\2\u0370\u0115\3\2\2\2\u0371\u0372\5\30\f\2\u0372\u0117\3\2\2\2\u0373"+
		"\u0374\5(\24\2\u0374\u0119\3\2\2\2\u0375\u0376\5\36\17\2\u0376\u0377\5"+
		"\"\21\2\u0377\u0378\5\16\7\2\u0378\u011b\3\2\2\2\u0379\u037a\5*\25\2\u037a"+
		"\u037b\5\26\13\2\u037b\u037c\5(\24\2\u037c\u011d\3\2\2\2\u037d\u037e\5"+
		"*\25\2\u037e\u037f\5\26\13\2\u037f\u0380\5\34\16\2\u0380\u011f\3\2\2\2"+
		"\u0381\u0382\5 \20\2\u0382\u0383\5\"\21\2\u0383\u0384\5,\26\2\u0384\u0121"+
		"\3\2\2\2\u0385\u0386\5\b\4\2\u0386\u0387\5 \20\2\u0387\u0388\5\16\7\2"+
		"\u0388\u0123\3\2\2\2\u0389\u038a\5\"\21\2\u038a\u038b\5(\24\2\u038b\u0125"+
		"\3\2\2\2\u038c\u038d\5\64\32\2\u038d\u038e\5\"\21\2\u038e\u038f\5(\24"+
		"\2\u038f\u0127\3\2\2\2\u0390\u0391\7\62\2\2\u0391\u0393\5\64\32\2\u0392"+
		"\u0394\t\36\2\2\u0393\u0392\3\2\2\2\u0394\u0395\3\2\2\2\u0395\u0393\3"+
		"\2\2\2\u0395\u0396\3\2\2\2\u0396\u0129\3\2\2\2\u0397\u0399\t\37\2\2\u0398"+
		"\u0397\3\2\2\2\u0399\u039a\3\2\2\2\u039a\u0398\3\2\2\2\u039a\u039b\3\2"+
		"\2\2\u039b\u039d\3\2\2\2\u039c\u039e\5\16\7\2\u039d\u039c\3\2\2\2\u039d"+
		"\u039e\3\2\2\2\u039e\u012b\3\2\2\2\u039f\u03a1\t\36\2\2\u03a0\u039f\3"+
		"\2\2\2\u03a1\u03a2\3\2\2\2\u03a2\u03a0\3\2\2\2\u03a2\u03a3\3\2\2\2\u03a3"+
		"\u03a4\3\2\2\2\u03a4\u03a5\5\26\13\2\u03a5\u012d\3\2\2\2\u03a6\u03a8\t"+
		" \2\2\u03a7\u03a6\3\2\2\2\u03a8\u03a9\3\2\2\2\u03a9\u03a7\3\2\2\2\u03a9"+
		"\u03aa\3\2\2\2\u03aa\u03ab\3\2\2\2\u03ab\u03ac\t!\2\2\u03ac\u012f\3\2"+
		"\2\2\u03ad\u03af\t\"\2\2\u03ae\u03ad\3\2\2\2\u03af\u03b0\3\2\2\2\u03b0"+
		"\u03ae\3\2\2\2\u03b0\u03b1\3\2\2\2\u03b1\u03b2\3\2\2\2\u03b2\u03b3\5\n"+
		"\5\2\u03b3\u0131\3\2\2\2\u03b4\u03b8\7)\2\2\u03b5\u03b7\n#\2\2\u03b6\u03b5"+
		"\3\2\2\2\u03b7\u03ba\3\2\2\2\u03b8\u03b6\3\2\2\2\u03b8\u03b9\3\2\2\2\u03b9"+
		"\u03bb\3\2\2\2\u03ba\u03b8\3\2\2\2\u03bb\u03bc\7)\2\2\u03bc\u0133\3\2"+
		"\2\2\u03bd\u03c1\7$\2\2\u03be\u03c0\n$\2\2\u03bf\u03be\3\2\2\2\u03c0\u03c3"+
		"\3\2\2\2\u03c1\u03bf\3\2\2\2\u03c1\u03c2\3\2\2\2\u03c2\u03c4\3\2\2\2\u03c3"+
		"\u03c1\3\2\2\2\u03c4\u03c5\7$\2\2\u03c5\u0135\3\2\2\2\u03c6\u03ca\t%\2"+
		"\2\u03c7\u03c9\t&\2\2\u03c8\u03c7\3\2\2\2\u03c9\u03cc\3\2\2\2\u03ca\u03c8"+
		"\3\2\2\2\u03ca\u03cb\3\2\2\2\u03cb\u0137\3\2\2\2\u03cc\u03ca\3\2\2\2\u03cd"+
		"\u03ce\5\u0136\u009b\2\u03ce\u03cf\7<\2\2\u03cf\u0139\3\2\2\2\u03d0\u03d2"+
		"\n\'\2\2\u03d1\u03d0\3\2\2\2\u03d2\u03d3\3\2\2\2\u03d3\u03d1\3\2\2\2\u03d3"+
		"\u03d4\3\2\2\2\u03d4\u013b\3\2\2\2\u03d5\u03d6\7*\2\2\u03d6\u013d\3\2"+
		"\2\2\u03d7\u03d8\7+\2\2\u03d8\u013f\3\2\2\2\u03d9\u03da\7.\2\2\u03da\u0141"+
		"\3\2\2\2\u03db\u03dc\7-\2\2\u03dc\u0143\3\2\2\2\u03dd\u03de\7/\2\2\u03de"+
		"\u0145\3\2\2\2\u03df\u03e0\7,\2\2\u03e0\u0147\3\2\2\2\u03e1\u03e2\7\61"+
		"\2\2\u03e2\u0149\3\2\2\2\u03e3\u03e4\7?\2\2\u03e4\u014b\3\2\2\2\u03e5"+
		"\u03e6\7@\2\2\u03e6\u014d\3\2\2\2\u03e7\u03e8\7@\2\2\u03e8\u03e9\7?\2"+
		"\2\u03e9\u014f\3\2\2\2\u03ea\u03eb\7>\2\2\u03eb\u0151\3\2\2\2\u03ec\u03ed"+
		"\7>\2\2\u03ed\u03ee\7?\2\2\u03ee\u0153\3\2\2\2\u03ef\u03f0\7\'\2\2\u03f0"+
		"\u0155\3\2\2\2\u03f1\u03f2\7@\2\2\u03f2\u03f3\7@\2\2\u03f3\u0157\3\2\2"+
		"\2\u03f4\u03f5\7>\2\2\u03f5\u03f6\7>\2\2\u03f6\u0159\3\2\2\2\u03f7\u03f8"+
		"\7#\2\2\u03f8\u015b\3\2\2\2\u03f9\u03fa\7(\2\2\u03fa\u015d\3\2\2\2\u03fb"+
		"\u03fc\7~\2\2\u03fc\u015f\3\2\2\2\u03fd\u03fe\7\u0080\2\2\u03fe\u0161"+
		"\3\2\2\2\u03ff\u0401\t\35\2\2\u0400\u03ff\3\2\2\2\u0401\u0402\3\2\2\2"+
		"\u0402\u0400\3\2\2\2\u0402\u0403\3\2\2\2\u0403\u0404\3\2\2\2\u0404\u0405"+
		"\b\u00b1\2\2\u0405\u0163\3\2\2\2\u0406\u0408\7\17\2\2\u0407\u0406\3\2"+
		"\2\2\u0407\u0408\3\2\2\2\u0408\u0409\3\2\2\2\u0409\u040a\7\f\2\2\u040a"+
		"\u0165\3\2\2\2\24\2\3\u016b\u0170\u017b\u02fa\u0395\u039a\u039d\u03a2"+
		"\u03a9\u03b0\u03b8\u03c1\u03ca\u03d3\u0402\u0407\6\b\2\2\7\3\2\6\2\2\2"+
		"\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}