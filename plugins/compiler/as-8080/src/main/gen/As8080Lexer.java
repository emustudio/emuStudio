// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-8080/src/main/antlr/As8080Lexer.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class As8080Lexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, COMMENT=2, EOL=3, OPCODE_STC=4, OPCODE_CMC=5, OPCODE_INR=6, OPCODE_DCR=7, 
		OPCODE_CMA=8, OPCODE_DAA=9, OPCODE_NOP=10, OPCODE_MOV=11, OPCODE_STAX=12, 
		OPCODE_LDAX=13, OPCODE_ADD=14, OPCODE_ADC=15, OPCODE_SUB=16, OPCODE_SBB=17, 
		OPCODE_ANA=18, OPCODE_XRA=19, OPCODE_ORA=20, OPCODE_CMP=21, OPCODE_RLC=22, 
		OPCODE_RRC=23, OPCODE_RAL=24, OPCODE_RAR=25, OPCODE_PUSH=26, OPCODE_POP=27, 
		OPCODE_DAD=28, OPCODE_INX=29, OPCODE_DCX=30, OPCODE_XCHG=31, OPCODE_XTHL=32, 
		OPCODE_SPHL=33, OPCODE_LXI=34, OPCODE_MVI=35, OPCODE_ADI=36, OPCODE_ACI=37, 
		OPCODE_SUI=38, OPCODE_SBI=39, OPCODE_ANI=40, OPCODE_XRI=41, OPCODE_ORI=42, 
		OPCODE_CPI=43, OPCODE_STA=44, OPCODE_LDA=45, OPCODE_SHLD=46, OPCODE_LHLD=47, 
		OPCODE_PCHL=48, OPCODE_JMP=49, OPCODE_JC=50, OPCODE_JNC=51, OPCODE_JZ=52, 
		OPCODE_JNZ=53, OPCODE_JP=54, OPCODE_JM=55, OPCODE_JPE=56, OPCODE_JPO=57, 
		OPCODE_CALL=58, OPCODE_CC=59, OPCODE_CNC=60, OPCODE_CZ=61, OPCODE_CNZ=62, 
		OPCODE_CP=63, OPCODE_CM=64, OPCODE_CPE=65, OPCODE_CPO=66, OPCODE_RET=67, 
		OPCODE_RC=68, OPCODE_RNC=69, OPCODE_RZ=70, OPCODE_RNZ=71, OPCODE_RM=72, 
		OPCODE_RP=73, OPCODE_RPE=74, OPCODE_RPO=75, OPCODE_RST=76, OPCODE_EI=77, 
		OPCODE_DI=78, OPCODE_IN=79, OPCODE_OUT=80, OPCODE_HLT=81, PREP_ORG=82, 
		PREP_EQU=83, PREP_SET=84, PREP_INCLUDE=85, PREP_IF=86, PREP_ENDIF=87, 
		PREP_MACRO=88, PREP_ENDM=89, PREP_DB=90, PREP_DW=91, PREP_DS=92, PREP_ADDR=93, 
		REG_A=94, REG_B=95, REG_C=96, REG_D=97, REG_E=98, REG_H=99, REG_L=100, 
		REG_M=101, REG_PSW=102, REG_SP=103, SEP_LPAR=104, SEP_RPAR=105, SEP_COMMA=106, 
		OP_ADD=107, OP_SUBTRACT=108, OP_MULTIPLY=109, OP_DIVIDE=110, OP_EQUAL=111, 
		OP_MOD=112, OP_SHR=113, OP_SHL=114, OP_NOT=115, OP_AND=116, OP_OR=117, 
		OP_XOR=118, LIT_NUMBER=119, LIT_HEXNUMBER_1=120, LIT_HEXNUMBER_2=121, 
		LIT_OCTNUMBER=122, LIT_BINNUMBER=123, LIT_STRING_1=124, LIT_STRING_2=125, 
		ID_IDENTIFIER=126, ID_LABEL=127, ERROR=128;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "COMMENT", "EOL", "A", "B", "C", "D", "E", "F", "G", "H", "I", 
			"J", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", 
			"Z", "OPCODE_STC", "OPCODE_CMC", "OPCODE_INR", "OPCODE_DCR", "OPCODE_CMA", 
			"OPCODE_DAA", "OPCODE_NOP", "OPCODE_MOV", "OPCODE_STAX", "OPCODE_LDAX", 
			"OPCODE_ADD", "OPCODE_ADC", "OPCODE_SUB", "OPCODE_SBB", "OPCODE_ANA", 
			"OPCODE_XRA", "OPCODE_ORA", "OPCODE_CMP", "OPCODE_RLC", "OPCODE_RRC", 
			"OPCODE_RAL", "OPCODE_RAR", "OPCODE_PUSH", "OPCODE_POP", "OPCODE_DAD", 
			"OPCODE_INX", "OPCODE_DCX", "OPCODE_XCHG", "OPCODE_XTHL", "OPCODE_SPHL", 
			"OPCODE_LXI", "OPCODE_MVI", "OPCODE_ADI", "OPCODE_ACI", "OPCODE_SUI", 
			"OPCODE_SBI", "OPCODE_ANI", "OPCODE_XRI", "OPCODE_ORI", "OPCODE_CPI", 
			"OPCODE_STA", "OPCODE_LDA", "OPCODE_SHLD", "OPCODE_LHLD", "OPCODE_PCHL", 
			"OPCODE_JMP", "OPCODE_JC", "OPCODE_JNC", "OPCODE_JZ", "OPCODE_JNZ", "OPCODE_JP", 
			"OPCODE_JM", "OPCODE_JPE", "OPCODE_JPO", "OPCODE_CALL", "OPCODE_CC", 
			"OPCODE_CNC", "OPCODE_CZ", "OPCODE_CNZ", "OPCODE_CP", "OPCODE_CM", "OPCODE_CPE", 
			"OPCODE_CPO", "OPCODE_RET", "OPCODE_RC", "OPCODE_RNC", "OPCODE_RZ", "OPCODE_RNZ", 
			"OPCODE_RM", "OPCODE_RP", "OPCODE_RPE", "OPCODE_RPO", "OPCODE_RST", "OPCODE_EI", 
			"OPCODE_DI", "OPCODE_IN", "OPCODE_OUT", "OPCODE_HLT", "PREP_ORG", "PREP_EQU", 
			"PREP_SET", "PREP_INCLUDE", "PREP_IF", "PREP_ENDIF", "PREP_MACRO", "PREP_ENDM", 
			"PREP_DB", "PREP_DW", "PREP_DS", "PREP_ADDR", "REG_A", "REG_B", "REG_C", 
			"REG_D", "REG_E", "REG_H", "REG_L", "REG_M", "REG_PSW", "REG_SP", "SEP_LPAR", 
			"SEP_RPAR", "SEP_COMMA", "OP_ADD", "OP_SUBTRACT", "OP_MULTIPLY", "OP_DIVIDE", 
			"OP_EQUAL", "OP_MOD", "OP_SHR", "OP_SHL", "OP_NOT", "OP_AND", "OP_OR", 
			"OP_XOR", "LIT_NUMBER", "LIT_HEXNUMBER_1", "LIT_HEXNUMBER_2", "LIT_OCTNUMBER", 
			"LIT_BINNUMBER", "LIT_STRING_1", "LIT_STRING_2", "ID_IDENTIFIER", "ID_LABEL", 
			"ERROR"
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
			null, null, null, null, null, null, null, null, "'('", "')'", "','", 
			"'+'", "'-'", "'*'", "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "COMMENT", "EOL", "OPCODE_STC", "OPCODE_CMC", "OPCODE_INR", 
			"OPCODE_DCR", "OPCODE_CMA", "OPCODE_DAA", "OPCODE_NOP", "OPCODE_MOV", 
			"OPCODE_STAX", "OPCODE_LDAX", "OPCODE_ADD", "OPCODE_ADC", "OPCODE_SUB", 
			"OPCODE_SBB", "OPCODE_ANA", "OPCODE_XRA", "OPCODE_ORA", "OPCODE_CMP", 
			"OPCODE_RLC", "OPCODE_RRC", "OPCODE_RAL", "OPCODE_RAR", "OPCODE_PUSH", 
			"OPCODE_POP", "OPCODE_DAD", "OPCODE_INX", "OPCODE_DCX", "OPCODE_XCHG", 
			"OPCODE_XTHL", "OPCODE_SPHL", "OPCODE_LXI", "OPCODE_MVI", "OPCODE_ADI", 
			"OPCODE_ACI", "OPCODE_SUI", "OPCODE_SBI", "OPCODE_ANI", "OPCODE_XRI", 
			"OPCODE_ORI", "OPCODE_CPI", "OPCODE_STA", "OPCODE_LDA", "OPCODE_SHLD", 
			"OPCODE_LHLD", "OPCODE_PCHL", "OPCODE_JMP", "OPCODE_JC", "OPCODE_JNC", 
			"OPCODE_JZ", "OPCODE_JNZ", "OPCODE_JP", "OPCODE_JM", "OPCODE_JPE", "OPCODE_JPO", 
			"OPCODE_CALL", "OPCODE_CC", "OPCODE_CNC", "OPCODE_CZ", "OPCODE_CNZ", 
			"OPCODE_CP", "OPCODE_CM", "OPCODE_CPE", "OPCODE_CPO", "OPCODE_RET", "OPCODE_RC", 
			"OPCODE_RNC", "OPCODE_RZ", "OPCODE_RNZ", "OPCODE_RM", "OPCODE_RP", "OPCODE_RPE", 
			"OPCODE_RPO", "OPCODE_RST", "OPCODE_EI", "OPCODE_DI", "OPCODE_IN", "OPCODE_OUT", 
			"OPCODE_HLT", "PREP_ORG", "PREP_EQU", "PREP_SET", "PREP_INCLUDE", "PREP_IF", 
			"PREP_ENDIF", "PREP_MACRO", "PREP_ENDM", "PREP_DB", "PREP_DW", "PREP_DS", 
			"PREP_ADDR", "REG_A", "REG_B", "REG_C", "REG_D", "REG_E", "REG_H", "REG_L", 
			"REG_M", "REG_PSW", "REG_SP", "SEP_LPAR", "SEP_RPAR", "SEP_COMMA", "OP_ADD", 
			"OP_SUBTRACT", "OP_MULTIPLY", "OP_DIVIDE", "OP_EQUAL", "OP_MOD", "OP_SHR", 
			"OP_SHL", "OP_NOT", "OP_AND", "OP_OR", "OP_XOR", "LIT_NUMBER", "LIT_HEXNUMBER_1", 
			"LIT_HEXNUMBER_2", "LIT_OCTNUMBER", "LIT_BINNUMBER", "LIT_STRING_1", 
			"LIT_STRING_2", "ID_IDENTIFIER", "ID_LABEL", "ERROR"
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


	public As8080Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "As8080Lexer.g4"; }

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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\u0082\u0373\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\t"+
		"T\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_"+
		"\4`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k"+
		"\tk\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv"+
		"\4w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t"+
		"\u0080\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084"+
		"\4\u0085\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089"+
		"\t\u0089\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d"+
		"\4\u008e\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092"+
		"\t\u0092\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\4\u0096\t\u0096"+
		"\4\u0097\t\u0097\4\u0098\t\u0098\4\u0099\t\u0099\3\2\3\2\3\2\3\2\3\3\3"+
		"\3\3\3\3\3\3\3\5\3\u013d\n\3\3\3\7\3\u0140\n\3\f\3\16\3\u0143\13\3\3\4"+
		"\5\4\u0146\n\4\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n"+
		"\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22"+
		"\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31"+
		"\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36"+
		"\3\36\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3\"\3\"\3\"\3\"\3#\3"+
		"#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3(\3"+
		"(\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3+\3+\3+\3+\3,\3,\3,\3,\3-\3-\3-\3-\3"+
		".\3.\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\62\3"+
		"\62\3\62\3\62\3\63\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3"+
		"\65\3\65\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38\38\38\38\39\39"+
		"\39\39\39\3:\3:\3:\3:\3:\3;\3;\3;\3;\3<\3<\3<\3<\3=\3=\3=\3=\3>\3>\3>"+
		"\3>\3?\3?\3?\3?\3@\3@\3@\3@\3A\3A\3A\3A\3B\3B\3B\3B\3C\3C\3C\3C\3D\3D"+
		"\3D\3D\3E\3E\3E\3E\3F\3F\3F\3F\3G\3G\3G\3G\3G\3H\3H\3H\3H\3H\3I\3I\3I"+
		"\3I\3I\3J\3J\3J\3J\3K\3K\3K\3L\3L\3L\3L\3M\3M\3M\3N\3N\3N\3N\3O\3O\3O"+
		"\3P\3P\3P\3Q\3Q\3Q\3Q\3R\3R\3R\3R\3S\3S\3S\3S\3S\3T\3T\3T\3U\3U\3U\3U"+
		"\3V\3V\3V\3W\3W\3W\3W\3X\3X\3X\3Y\3Y\3Y\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3\\\3"+
		"\\\3\\\3\\\3]\3]\3]\3^\3^\3^\3^\3_\3_\3_\3`\3`\3`\3`\3a\3a\3a\3b\3b\3"+
		"b\3c\3c\3c\3c\3d\3d\3d\3d\3e\3e\3e\3e\3f\3f\3f\3g\3g\3g\3h\3h\3h\3i\3"+
		"i\3i\3i\3j\3j\3j\3j\3k\3k\3k\3k\3l\3l\3l\3l\3m\3m\3m\3m\3n\3n\3n\3n\3"+
		"n\3n\3n\3n\3o\3o\3o\3p\3p\3p\3p\3p\3p\3q\3q\3q\3q\3q\3q\3r\3r\3r\3r\3"+
		"r\3s\3s\3s\3t\3t\3t\3u\3u\3u\3v\3v\3w\3w\3x\3x\3y\3y\3z\3z\3{\3{\3|\3"+
		"|\3}\3}\3~\3~\3\177\3\177\3\177\3\177\3\u0080\3\u0080\3\u0080\3\u0081"+
		"\3\u0081\3\u0082\3\u0082\3\u0083\3\u0083\3\u0084\3\u0084\3\u0085\3\u0085"+
		"\3\u0086\3\u0086\3\u0087\3\u0087\3\u0088\3\u0088\3\u0089\3\u0089\3\u0089"+
		"\3\u0089\3\u008a\3\u008a\3\u008a\3\u008a\3\u008b\3\u008b\3\u008b\3\u008b"+
		"\3\u008c\3\u008c\3\u008c\3\u008c\3\u008d\3\u008d\3\u008d\3\u008d\3\u008e"+
		"\3\u008e\3\u008e\3\u008f\3\u008f\3\u008f\3\u008f\3\u0090\5\u0090\u0323"+
		"\n\u0090\3\u0090\6\u0090\u0326\n\u0090\r\u0090\16\u0090\u0327\3\u0090"+
		"\5\u0090\u032b\n\u0090\3\u0091\5\u0091\u032e\n\u0091\3\u0091\3\u0091\3"+
		"\u0091\6\u0091\u0333\n\u0091\r\u0091\16\u0091\u0334\3\u0092\5\u0092\u0338"+
		"\n\u0092\3\u0092\6\u0092\u033b\n\u0092\r\u0092\16\u0092\u033c\3\u0092"+
		"\3\u0092\3\u0093\5\u0093\u0342\n\u0093\3\u0093\6\u0093\u0345\n\u0093\r"+
		"\u0093\16\u0093\u0346\3\u0093\3\u0093\3\u0094\6\u0094\u034c\n\u0094\r"+
		"\u0094\16\u0094\u034d\3\u0094\3\u0094\3\u0095\3\u0095\6\u0095\u0354\n"+
		"\u0095\r\u0095\16\u0095\u0355\3\u0095\3\u0095\3\u0096\3\u0096\6\u0096"+
		"\u035c\n\u0096\r\u0096\16\u0096\u035d\3\u0096\3\u0096\3\u0097\3\u0097"+
		"\7\u0097\u0364\n\u0097\f\u0097\16\u0097\u0367\13\u0097\3\u0098\3\u0098"+
		"\7\u0098\u036b\n\u0098\f\u0098\16\u0098\u036e\13\u0098\3\u0098\3\u0098"+
		"\3\u0099\3\u0099\2\2\u009a\3\3\5\4\7\5\t\2\13\2\r\2\17\2\21\2\23\2\25"+
		"\2\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63\2\65\2\67"+
		"\29\6;\7=\b?\tA\nC\13E\fG\rI\16K\17M\20O\21Q\22S\23U\24W\25Y\26[\27]\30"+
		"_\31a\32c\33e\34g\35i\36k\37m o!q\"s#u$w%y&{\'}(\177)\u0081*\u0083+\u0085"+
		",\u0087-\u0089.\u008b/\u008d\60\u008f\61\u0091\62\u0093\63\u0095\64\u0097"+
		"\65\u0099\66\u009b\67\u009d8\u009f9\u00a1:\u00a3;\u00a5<\u00a7=\u00a9"+
		">\u00ab?\u00ad@\u00afA\u00b1B\u00b3C\u00b5D\u00b7E\u00b9F\u00bbG\u00bd"+
		"H\u00bfI\u00c1J\u00c3K\u00c5L\u00c7M\u00c9N\u00cbO\u00cdP\u00cfQ\u00d1"+
		"R\u00d3S\u00d5T\u00d7U\u00d9V\u00dbW\u00ddX\u00dfY\u00e1Z\u00e3[\u00e5"+
		"\\\u00e7]\u00e9^\u00eb_\u00ed`\u00efa\u00f1b\u00f3c\u00f5d\u00f7e\u00f9"+
		"f\u00fbg\u00fdh\u00ffi\u0101j\u0103k\u0105l\u0107m\u0109n\u010bo\u010d"+
		"p\u010fq\u0111r\u0113s\u0115t\u0117u\u0119v\u011bw\u011dx\u011fy\u0121"+
		"z\u0123{\u0125|\u0127}\u0129~\u012b\177\u012d\u0080\u012f\u0081\u0131"+
		"\u0082\3\2(\5\2\13\13\16\16\"\"\4\2%%==\4\2\f\f\17\17\4\2CCcc\4\2DDdd"+
		"\4\2EEee\4\2FFff\4\2GGgg\4\2HHhh\4\2IIii\4\2JJjj\4\2KKkk\4\2LLll\4\2N"+
		"Nnn\4\2OOoo\4\2PPpp\4\2QQqq\4\2RRrr\4\2SSss\4\2TTtt\4\2UUuu\4\2VVvv\4"+
		"\2WWww\4\2XXxx\4\2YYyy\4\2ZZzz\4\2\\\\||\3\2//\3\2\62;\5\2\62;CHch\3\2"+
		"\629\6\2QQSSqqss\3\2\62\63\3\2))\4\2))``\4\2$$``\5\2A\\aac|\6\2\62;A\\"+
		"aac|\2\u036c\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\29\3\2\2\2\2;\3\2\2\2"+
		"\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I"+
		"\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2"+
		"\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2"+
		"\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o"+
		"\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2"+
		"\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085"+
		"\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\2\u008b\3\2\2\2\2\u008d\3\2\2"+
		"\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\2\u0093\3\2\2\2\2\u0095\3\2\2\2\2\u0097"+
		"\3\2\2\2\2\u0099\3\2\2\2\2\u009b\3\2\2\2\2\u009d\3\2\2\2\2\u009f\3\2\2"+
		"\2\2\u00a1\3\2\2\2\2\u00a3\3\2\2\2\2\u00a5\3\2\2\2\2\u00a7\3\2\2\2\2\u00a9"+
		"\3\2\2\2\2\u00ab\3\2\2\2\2\u00ad\3\2\2\2\2\u00af\3\2\2\2\2\u00b1\3\2\2"+
		"\2\2\u00b3\3\2\2\2\2\u00b5\3\2\2\2\2\u00b7\3\2\2\2\2\u00b9\3\2\2\2\2\u00bb"+
		"\3\2\2\2\2\u00bd\3\2\2\2\2\u00bf\3\2\2\2\2\u00c1\3\2\2\2\2\u00c3\3\2\2"+
		"\2\2\u00c5\3\2\2\2\2\u00c7\3\2\2\2\2\u00c9\3\2\2\2\2\u00cb\3\2\2\2\2\u00cd"+
		"\3\2\2\2\2\u00cf\3\2\2\2\2\u00d1\3\2\2\2\2\u00d3\3\2\2\2\2\u00d5\3\2\2"+
		"\2\2\u00d7\3\2\2\2\2\u00d9\3\2\2\2\2\u00db\3\2\2\2\2\u00dd\3\2\2\2\2\u00df"+
		"\3\2\2\2\2\u00e1\3\2\2\2\2\u00e3\3\2\2\2\2\u00e5\3\2\2\2\2\u00e7\3\2\2"+
		"\2\2\u00e9\3\2\2\2\2\u00eb\3\2\2\2\2\u00ed\3\2\2\2\2\u00ef\3\2\2\2\2\u00f1"+
		"\3\2\2\2\2\u00f3\3\2\2\2\2\u00f5\3\2\2\2\2\u00f7\3\2\2\2\2\u00f9\3\2\2"+
		"\2\2\u00fb\3\2\2\2\2\u00fd\3\2\2\2\2\u00ff\3\2\2\2\2\u0101\3\2\2\2\2\u0103"+
		"\3\2\2\2\2\u0105\3\2\2\2\2\u0107\3\2\2\2\2\u0109\3\2\2\2\2\u010b\3\2\2"+
		"\2\2\u010d\3\2\2\2\2\u010f\3\2\2\2\2\u0111\3\2\2\2\2\u0113\3\2\2\2\2\u0115"+
		"\3\2\2\2\2\u0117\3\2\2\2\2\u0119\3\2\2\2\2\u011b\3\2\2\2\2\u011d\3\2\2"+
		"\2\2\u011f\3\2\2\2\2\u0121\3\2\2\2\2\u0123\3\2\2\2\2\u0125\3\2\2\2\2\u0127"+
		"\3\2\2\2\2\u0129\3\2\2\2\2\u012b\3\2\2\2\2\u012d\3\2\2\2\2\u012f\3\2\2"+
		"\2\2\u0131\3\2\2\2\3\u0133\3\2\2\2\5\u013c\3\2\2\2\7\u0145\3\2\2\2\t\u0149"+
		"\3\2\2\2\13\u014b\3\2\2\2\r\u014d\3\2\2\2\17\u014f\3\2\2\2\21\u0151\3"+
		"\2\2\2\23\u0153\3\2\2\2\25\u0155\3\2\2\2\27\u0157\3\2\2\2\31\u0159\3\2"+
		"\2\2\33\u015b\3\2\2\2\35\u015d\3\2\2\2\37\u015f\3\2\2\2!\u0161\3\2\2\2"+
		"#\u0163\3\2\2\2%\u0165\3\2\2\2\'\u0167\3\2\2\2)\u0169\3\2\2\2+\u016b\3"+
		"\2\2\2-\u016d\3\2\2\2/\u016f\3\2\2\2\61\u0171\3\2\2\2\63\u0173\3\2\2\2"+
		"\65\u0175\3\2\2\2\67\u0177\3\2\2\29\u0179\3\2\2\2;\u017d\3\2\2\2=\u0181"+
		"\3\2\2\2?\u0185\3\2\2\2A\u0189\3\2\2\2C\u018d\3\2\2\2E\u0191\3\2\2\2G"+
		"\u0195\3\2\2\2I\u0199\3\2\2\2K\u019e\3\2\2\2M\u01a3\3\2\2\2O\u01a7\3\2"+
		"\2\2Q\u01ab\3\2\2\2S\u01af\3\2\2\2U\u01b3\3\2\2\2W\u01b7\3\2\2\2Y\u01bb"+
		"\3\2\2\2[\u01bf\3\2\2\2]\u01c3\3\2\2\2_\u01c7\3\2\2\2a\u01cb\3\2\2\2c"+
		"\u01cf\3\2\2\2e\u01d3\3\2\2\2g\u01d8\3\2\2\2i\u01dc\3\2\2\2k\u01e0\3\2"+
		"\2\2m\u01e4\3\2\2\2o\u01e8\3\2\2\2q\u01ed\3\2\2\2s\u01f2\3\2\2\2u\u01f7"+
		"\3\2\2\2w\u01fb\3\2\2\2y\u01ff\3\2\2\2{\u0203\3\2\2\2}\u0207\3\2\2\2\177"+
		"\u020b\3\2\2\2\u0081\u020f\3\2\2\2\u0083\u0213\3\2\2\2\u0085\u0217\3\2"+
		"\2\2\u0087\u021b\3\2\2\2\u0089\u021f\3\2\2\2\u008b\u0223\3\2\2\2\u008d"+
		"\u0227\3\2\2\2\u008f\u022c\3\2\2\2\u0091\u0231\3\2\2\2\u0093\u0236\3\2"+
		"\2\2\u0095\u023a\3\2\2\2\u0097\u023d\3\2\2\2\u0099\u0241\3\2\2\2\u009b"+
		"\u0244\3\2\2\2\u009d\u0248\3\2\2\2\u009f\u024b\3\2\2\2\u00a1\u024e\3\2"+
		"\2\2\u00a3\u0252\3\2\2\2\u00a5\u0256\3\2\2\2\u00a7\u025b\3\2\2\2\u00a9"+
		"\u025e\3\2\2\2\u00ab\u0262\3\2\2\2\u00ad\u0265\3\2\2\2\u00af\u0269\3\2"+
		"\2\2\u00b1\u026c\3\2\2\2\u00b3\u026f\3\2\2\2\u00b5\u0273\3\2\2\2\u00b7"+
		"\u0277\3\2\2\2\u00b9\u027b\3\2\2\2\u00bb\u027e\3\2\2\2\u00bd\u0282\3\2"+
		"\2\2\u00bf\u0285\3\2\2\2\u00c1\u0289\3\2\2\2\u00c3\u028c\3\2\2\2\u00c5"+
		"\u028f\3\2\2\2\u00c7\u0293\3\2\2\2\u00c9\u0297\3\2\2\2\u00cb\u029b\3\2"+
		"\2\2\u00cd\u029e\3\2\2\2\u00cf\u02a1\3\2\2\2\u00d1\u02a4\3\2\2\2\u00d3"+
		"\u02a8\3\2\2\2\u00d5\u02ac\3\2\2\2\u00d7\u02b0\3\2\2\2\u00d9\u02b4\3\2"+
		"\2\2\u00db\u02b8\3\2\2\2\u00dd\u02c0\3\2\2\2\u00df\u02c3\3\2\2\2\u00e1"+
		"\u02c9\3\2\2\2\u00e3\u02cf\3\2\2\2\u00e5\u02d4\3\2\2\2\u00e7\u02d7\3\2"+
		"\2\2\u00e9\u02da\3\2\2\2\u00eb\u02dd\3\2\2\2\u00ed\u02df\3\2\2\2\u00ef"+
		"\u02e1\3\2\2\2\u00f1\u02e3\3\2\2\2\u00f3\u02e5\3\2\2\2\u00f5\u02e7\3\2"+
		"\2\2\u00f7\u02e9\3\2\2\2\u00f9\u02eb\3\2\2\2\u00fb\u02ed\3\2\2\2\u00fd"+
		"\u02ef\3\2\2\2\u00ff\u02f3\3\2\2\2\u0101\u02f6\3\2\2\2\u0103\u02f8\3\2"+
		"\2\2\u0105\u02fa\3\2\2\2\u0107\u02fc\3\2\2\2\u0109\u02fe\3\2\2\2\u010b"+
		"\u0300\3\2\2\2\u010d\u0302\3\2\2\2\u010f\u0304\3\2\2\2\u0111\u0306\3\2"+
		"\2\2\u0113\u030a\3\2\2\2\u0115\u030e\3\2\2\2\u0117\u0312\3\2\2\2\u0119"+
		"\u0316\3\2\2\2\u011b\u031a\3\2\2\2\u011d\u031d\3\2\2\2\u011f\u0322\3\2"+
		"\2\2\u0121\u032d\3\2\2\2\u0123\u0337\3\2\2\2\u0125\u0341\3\2\2\2\u0127"+
		"\u034b\3\2\2\2\u0129\u0351\3\2\2\2\u012b\u0359\3\2\2\2\u012d\u0361\3\2"+
		"\2\2\u012f\u0368\3\2\2\2\u0131\u0371\3\2\2\2\u0133\u0134\t\2\2\2\u0134"+
		"\u0135\3\2\2\2\u0135\u0136\b\2\2\2\u0136\4\3\2\2\2\u0137\u0138\7\61\2"+
		"\2\u0138\u013d\7\61\2\2\u0139\u013a\7/\2\2\u013a\u013d\7/\2\2\u013b\u013d"+
		"\t\3\2\2\u013c\u0137\3\2\2\2\u013c\u0139\3\2\2\2\u013c\u013b\3\2\2\2\u013d"+
		"\u0141\3\2\2\2\u013e\u0140\n\4\2\2\u013f\u013e\3\2\2\2\u0140\u0143\3\2"+
		"\2\2\u0141\u013f\3\2\2\2\u0141\u0142\3\2\2\2\u0142\6\3\2\2\2\u0143\u0141"+
		"\3\2\2\2\u0144\u0146\7\17\2\2\u0145\u0144\3\2\2\2\u0145\u0146\3\2\2\2"+
		"\u0146\u0147\3\2\2\2\u0147\u0148\7\f\2\2\u0148\b\3\2\2\2\u0149\u014a\t"+
		"\5\2\2\u014a\n\3\2\2\2\u014b\u014c\t\6\2\2\u014c\f\3\2\2\2\u014d\u014e"+
		"\t\7\2\2\u014e\16\3\2\2\2\u014f\u0150\t\b\2\2\u0150\20\3\2\2\2\u0151\u0152"+
		"\t\t\2\2\u0152\22\3\2\2\2\u0153\u0154\t\n\2\2\u0154\24\3\2\2\2\u0155\u0156"+
		"\t\13\2\2\u0156\26\3\2\2\2\u0157\u0158\t\f\2\2\u0158\30\3\2\2\2\u0159"+
		"\u015a\t\r\2\2\u015a\32\3\2\2\2\u015b\u015c\t\16\2\2\u015c\34\3\2\2\2"+
		"\u015d\u015e\t\17\2\2\u015e\36\3\2\2\2\u015f\u0160\t\20\2\2\u0160 \3\2"+
		"\2\2\u0161\u0162\t\21\2\2\u0162\"\3\2\2\2\u0163\u0164\t\22\2\2\u0164$"+
		"\3\2\2\2\u0165\u0166\t\23\2\2\u0166&\3\2\2\2\u0167\u0168\t\24\2\2\u0168"+
		"(\3\2\2\2\u0169\u016a\t\25\2\2\u016a*\3\2\2\2\u016b\u016c\t\26\2\2\u016c"+
		",\3\2\2\2\u016d\u016e\t\27\2\2\u016e.\3\2\2\2\u016f\u0170\t\30\2\2\u0170"+
		"\60\3\2\2\2\u0171\u0172\t\31\2\2\u0172\62\3\2\2\2\u0173\u0174\t\32\2\2"+
		"\u0174\64\3\2\2\2\u0175\u0176\t\33\2\2\u0176\66\3\2\2\2\u0177\u0178\t"+
		"\34\2\2\u01788\3\2\2\2\u0179\u017a\5+\26\2\u017a\u017b\5-\27\2\u017b\u017c"+
		"\5\r\7\2\u017c:\3\2\2\2\u017d\u017e\5\r\7\2\u017e\u017f\5\37\20\2\u017f"+
		"\u0180\5\r\7\2\u0180<\3\2\2\2\u0181\u0182\5\31\r\2\u0182\u0183\5!\21\2"+
		"\u0183\u0184\5)\25\2\u0184>\3\2\2\2\u0185\u0186\5\17\b\2\u0186\u0187\5"+
		"\r\7\2\u0187\u0188\5)\25\2\u0188@\3\2\2\2\u0189\u018a\5\r\7\2\u018a\u018b"+
		"\5\37\20\2\u018b\u018c\5\t\5\2\u018cB\3\2\2\2\u018d\u018e\5\17\b\2\u018e"+
		"\u018f\5\t\5\2\u018f\u0190\5\t\5\2\u0190D\3\2\2\2\u0191\u0192\5!\21\2"+
		"\u0192\u0193\5#\22\2\u0193\u0194\5%\23\2\u0194F\3\2\2\2\u0195\u0196\5"+
		"\37\20\2\u0196\u0197\5#\22\2\u0197\u0198\5\61\31\2\u0198H\3\2\2\2\u0199"+
		"\u019a\5+\26\2\u019a\u019b\5-\27\2\u019b\u019c\5\t\5\2\u019c\u019d\5\65"+
		"\33\2\u019dJ\3\2\2\2\u019e\u019f\5\35\17\2\u019f\u01a0\5\17\b\2\u01a0"+
		"\u01a1\5\t\5\2\u01a1\u01a2\5\65\33\2\u01a2L\3\2\2\2\u01a3\u01a4\5\t\5"+
		"\2\u01a4\u01a5\5\17\b\2\u01a5\u01a6\5\17\b\2\u01a6N\3\2\2\2\u01a7\u01a8"+
		"\5\t\5\2\u01a8\u01a9\5\17\b\2\u01a9\u01aa\5\r\7\2\u01aaP\3\2\2\2\u01ab"+
		"\u01ac\5+\26\2\u01ac\u01ad\5/\30\2\u01ad\u01ae\5\13\6\2\u01aeR\3\2\2\2"+
		"\u01af\u01b0\5+\26\2\u01b0\u01b1\5\13\6\2\u01b1\u01b2\5\13\6\2\u01b2T"+
		"\3\2\2\2\u01b3\u01b4\5\t\5\2\u01b4\u01b5\5!\21\2\u01b5\u01b6\5\t\5\2\u01b6"+
		"V\3\2\2\2\u01b7\u01b8\5\65\33\2\u01b8\u01b9\5)\25\2\u01b9\u01ba\5\t\5"+
		"\2\u01baX\3\2\2\2\u01bb\u01bc\5#\22\2\u01bc\u01bd\5)\25\2\u01bd\u01be"+
		"\5\t\5\2\u01beZ\3\2\2\2\u01bf\u01c0\5\r\7\2\u01c0\u01c1\5\37\20\2\u01c1"+
		"\u01c2\5%\23\2\u01c2\\\3\2\2\2\u01c3\u01c4\5)\25\2\u01c4\u01c5\5\35\17"+
		"\2\u01c5\u01c6\5\r\7\2\u01c6^\3\2\2\2\u01c7\u01c8\5)\25\2\u01c8\u01c9"+
		"\5)\25\2\u01c9\u01ca\5\r\7\2\u01ca`\3\2\2\2\u01cb\u01cc\5)\25\2\u01cc"+
		"\u01cd\5\t\5\2\u01cd\u01ce\5\35\17\2\u01ceb\3\2\2\2\u01cf\u01d0\5)\25"+
		"\2\u01d0\u01d1\5\t\5\2\u01d1\u01d2\5)\25\2\u01d2d\3\2\2\2\u01d3\u01d4"+
		"\5%\23\2\u01d4\u01d5\5/\30\2\u01d5\u01d6\5+\26\2\u01d6\u01d7\5\27\f\2"+
		"\u01d7f\3\2\2\2\u01d8\u01d9\5%\23\2\u01d9\u01da\5#\22\2\u01da\u01db\5"+
		"%\23\2\u01dbh\3\2\2\2\u01dc\u01dd\5\17\b\2\u01dd\u01de\5\t\5\2\u01de\u01df"+
		"\5\17\b\2\u01dfj\3\2\2\2\u01e0\u01e1\5\31\r\2\u01e1\u01e2\5!\21\2\u01e2"+
		"\u01e3\5\65\33\2\u01e3l\3\2\2\2\u01e4\u01e5\5\17\b\2\u01e5\u01e6\5\r\7"+
		"\2\u01e6\u01e7\5\65\33\2\u01e7n\3\2\2\2\u01e8\u01e9\5\65\33\2\u01e9\u01ea"+
		"\5\r\7\2\u01ea\u01eb\5\27\f\2\u01eb\u01ec\5\25\13\2\u01ecp\3\2\2\2\u01ed"+
		"\u01ee\5\65\33\2\u01ee\u01ef\5-\27\2\u01ef\u01f0\5\27\f\2\u01f0\u01f1"+
		"\5\35\17\2\u01f1r\3\2\2\2\u01f2\u01f3\5+\26\2\u01f3\u01f4\5%\23\2\u01f4"+
		"\u01f5\5\27\f\2\u01f5\u01f6\5\35\17\2\u01f6t\3\2\2\2\u01f7\u01f8\5\35"+
		"\17\2\u01f8\u01f9\5\65\33\2\u01f9\u01fa\5\31\r\2\u01fav\3\2\2\2\u01fb"+
		"\u01fc\5\37\20\2\u01fc\u01fd\5\61\31\2\u01fd\u01fe\5\31\r\2\u01fex\3\2"+
		"\2\2\u01ff\u0200\5\t\5\2\u0200\u0201\5\17\b\2\u0201\u0202\5\31\r\2\u0202"+
		"z\3\2\2\2\u0203\u0204\5\t\5\2\u0204\u0205\5\r\7\2\u0205\u0206\5\31\r\2"+
		"\u0206|\3\2\2\2\u0207\u0208\5+\26\2\u0208\u0209\5/\30\2\u0209\u020a\5"+
		"\31\r\2\u020a~\3\2\2\2\u020b\u020c\5+\26\2\u020c\u020d\5\13\6\2\u020d"+
		"\u020e\5\31\r\2\u020e\u0080\3\2\2\2\u020f\u0210\5\t\5\2\u0210\u0211\5"+
		"!\21\2\u0211\u0212\5\31\r\2\u0212\u0082\3\2\2\2\u0213\u0214\5\65\33\2"+
		"\u0214\u0215\5)\25\2\u0215\u0216\5\31\r\2\u0216\u0084\3\2\2\2\u0217\u0218"+
		"\5#\22\2\u0218\u0219\5)\25\2\u0219\u021a\5\31\r\2\u021a\u0086\3\2\2\2"+
		"\u021b\u021c\5\r\7\2\u021c\u021d\5%\23\2\u021d\u021e\5\31\r\2\u021e\u0088"+
		"\3\2\2\2\u021f\u0220\5+\26\2\u0220\u0221\5-\27\2\u0221\u0222\5\t\5\2\u0222"+
		"\u008a\3\2\2\2\u0223\u0224\5\35\17\2\u0224\u0225\5\17\b\2\u0225\u0226"+
		"\5\t\5\2\u0226\u008c\3\2\2\2\u0227\u0228\5+\26\2\u0228\u0229\5\27\f\2"+
		"\u0229\u022a\5\35\17\2\u022a\u022b\5\17\b\2\u022b\u008e\3\2\2\2\u022c"+
		"\u022d\5\35\17\2\u022d\u022e\5\27\f\2\u022e\u022f\5\35\17\2\u022f\u0230"+
		"\5\17\b\2\u0230\u0090\3\2\2\2\u0231\u0232\5%\23\2\u0232\u0233\5\r\7\2"+
		"\u0233\u0234\5\27\f\2\u0234\u0235\5\35\17\2\u0235\u0092\3\2\2\2\u0236"+
		"\u0237\5\33\16\2\u0237\u0238\5\37\20\2\u0238\u0239\5%\23\2\u0239\u0094"+
		"\3\2\2\2\u023a\u023b\5\33\16\2\u023b\u023c\5\r\7\2\u023c\u0096\3\2\2\2"+
		"\u023d\u023e\5\33\16\2\u023e\u023f\5!\21\2\u023f\u0240\5\r\7\2\u0240\u0098"+
		"\3\2\2\2\u0241\u0242\5\33\16\2\u0242\u0243\5\67\34\2\u0243\u009a\3\2\2"+
		"\2\u0244\u0245\5\33\16\2\u0245\u0246\5!\21\2\u0246\u0247\5\67\34\2\u0247"+
		"\u009c\3\2\2\2\u0248\u0249\5\33\16\2\u0249\u024a\5%\23\2\u024a\u009e\3"+
		"\2\2\2\u024b\u024c\5\33\16\2\u024c\u024d\5\37\20\2\u024d\u00a0\3\2\2\2"+
		"\u024e\u024f\5\33\16\2\u024f\u0250\5%\23\2\u0250\u0251\5\21\t\2\u0251"+
		"\u00a2\3\2\2\2\u0252\u0253\5\33\16\2\u0253\u0254\5%\23\2\u0254\u0255\5"+
		"#\22\2\u0255\u00a4\3\2\2\2\u0256\u0257\5\r\7\2\u0257\u0258\5\t\5\2\u0258"+
		"\u0259\5\35\17\2\u0259\u025a\5\35\17\2\u025a\u00a6\3\2\2\2\u025b\u025c"+
		"\5\r\7\2\u025c\u025d\5\r\7\2\u025d\u00a8\3\2\2\2\u025e\u025f\5\r\7\2\u025f"+
		"\u0260\5!\21\2\u0260\u0261\5\r\7\2\u0261\u00aa\3\2\2\2\u0262\u0263\5\r"+
		"\7\2\u0263\u0264\5\67\34\2\u0264\u00ac\3\2\2\2\u0265\u0266\5\r\7\2\u0266"+
		"\u0267\5!\21\2\u0267\u0268\5\67\34\2\u0268\u00ae\3\2\2\2\u0269\u026a\5"+
		"\r\7\2\u026a\u026b\5%\23\2\u026b\u00b0\3\2\2\2\u026c\u026d\5\r\7\2\u026d"+
		"\u026e\5\37\20\2\u026e\u00b2\3\2\2\2\u026f\u0270\5\r\7\2\u0270\u0271\5"+
		"%\23\2\u0271\u0272\5\21\t\2\u0272\u00b4\3\2\2\2\u0273\u0274\5\r\7\2\u0274"+
		"\u0275\5%\23\2\u0275\u0276\5#\22\2\u0276\u00b6\3\2\2\2\u0277\u0278\5)"+
		"\25\2\u0278\u0279\5\21\t\2\u0279\u027a\5-\27\2\u027a\u00b8\3\2\2\2\u027b"+
		"\u027c\5)\25\2\u027c\u027d\5\r\7\2\u027d\u00ba\3\2\2\2\u027e\u027f\5)"+
		"\25\2\u027f\u0280\5!\21\2\u0280\u0281\5\r\7\2\u0281\u00bc\3\2\2\2\u0282"+
		"\u0283\5)\25\2\u0283\u0284\5\67\34\2\u0284\u00be\3\2\2\2\u0285\u0286\5"+
		")\25\2\u0286\u0287\5!\21\2\u0287\u0288\5\67\34\2\u0288\u00c0\3\2\2\2\u0289"+
		"\u028a\5)\25\2\u028a\u028b\5\37\20\2\u028b\u00c2\3\2\2\2\u028c\u028d\5"+
		")\25\2\u028d\u028e\5%\23\2\u028e\u00c4\3\2\2\2\u028f\u0290\5)\25\2\u0290"+
		"\u0291\5%\23\2\u0291\u0292\5\21\t\2\u0292\u00c6\3\2\2\2\u0293\u0294\5"+
		")\25\2\u0294\u0295\5%\23\2\u0295\u0296\5#\22\2\u0296\u00c8\3\2\2\2\u0297"+
		"\u0298\5)\25\2\u0298\u0299\5+\26\2\u0299\u029a\5-\27\2\u029a\u00ca\3\2"+
		"\2\2\u029b\u029c\5\21\t\2\u029c\u029d\5\31\r\2\u029d\u00cc\3\2\2\2\u029e"+
		"\u029f\5\17\b\2\u029f\u02a0\5\31\r\2\u02a0\u00ce\3\2\2\2\u02a1\u02a2\5"+
		"\31\r\2\u02a2\u02a3\5!\21\2\u02a3\u00d0\3\2\2\2\u02a4\u02a5\5#\22\2\u02a5"+
		"\u02a6\5/\30\2\u02a6\u02a7\5-\27\2\u02a7\u00d2\3\2\2\2\u02a8\u02a9\5\27"+
		"\f\2\u02a9\u02aa\5\35\17\2\u02aa\u02ab\5-\27\2\u02ab\u00d4\3\2\2\2\u02ac"+
		"\u02ad\5#\22\2\u02ad\u02ae\5)\25\2\u02ae\u02af\5\25\13\2\u02af\u00d6\3"+
		"\2\2\2\u02b0\u02b1\5\21\t\2\u02b1\u02b2\5\'\24\2\u02b2\u02b3\5/\30\2\u02b3"+
		"\u00d8\3\2\2\2\u02b4\u02b5\5+\26\2\u02b5\u02b6\5\21\t\2\u02b6\u02b7\5"+
		"-\27\2\u02b7\u00da\3\2\2\2\u02b8\u02b9\5\31\r\2\u02b9\u02ba\5!\21\2\u02ba"+
		"\u02bb\5\r\7\2\u02bb\u02bc\5\35\17\2\u02bc\u02bd\5/\30\2\u02bd\u02be\5"+
		"\17\b\2\u02be\u02bf\5\21\t\2\u02bf\u00dc\3\2\2\2\u02c0\u02c1\5\31\r\2"+
		"\u02c1\u02c2\5\23\n\2\u02c2\u00de\3\2\2\2\u02c3\u02c4\5\21\t\2\u02c4\u02c5"+
		"\5!\21\2\u02c5\u02c6\5\17\b\2\u02c6\u02c7\5\31\r\2\u02c7\u02c8\5\23\n"+
		"\2\u02c8\u00e0\3\2\2\2\u02c9\u02ca\5\37\20\2\u02ca\u02cb\5\t\5\2\u02cb"+
		"\u02cc\5\r\7\2\u02cc\u02cd\5)\25\2\u02cd\u02ce\5#\22\2\u02ce\u00e2\3\2"+
		"\2\2\u02cf\u02d0\5\21\t\2\u02d0\u02d1\5!\21\2\u02d1\u02d2\5\17\b\2\u02d2"+
		"\u02d3\5\37\20\2\u02d3\u00e4\3\2\2\2\u02d4\u02d5\5\17\b\2\u02d5\u02d6"+
		"\5\13\6\2\u02d6\u00e6\3\2\2\2\u02d7\u02d8\5\17\b\2\u02d8\u02d9\5\63\32"+
		"\2\u02d9\u00e8\3\2\2\2\u02da\u02db\5\17\b\2\u02db\u02dc\5+\26\2\u02dc"+
		"\u00ea\3\2\2\2\u02dd\u02de\7&\2\2\u02de\u00ec\3\2\2\2\u02df\u02e0\5\t"+
		"\5\2\u02e0\u00ee\3\2\2\2\u02e1\u02e2\5\13\6\2\u02e2\u00f0\3\2\2\2\u02e3"+
		"\u02e4\5\r\7\2\u02e4\u00f2\3\2\2\2\u02e5\u02e6\5\17\b\2\u02e6\u00f4\3"+
		"\2\2\2\u02e7\u02e8\5\21\t\2\u02e8\u00f6\3\2\2\2\u02e9\u02ea\5\27\f\2\u02ea"+
		"\u00f8\3\2\2\2\u02eb\u02ec\5\35\17\2\u02ec\u00fa\3\2\2\2\u02ed\u02ee\5"+
		"\37\20\2\u02ee\u00fc\3\2\2\2\u02ef\u02f0\5%\23\2\u02f0\u02f1\5+\26\2\u02f1"+
		"\u02f2\5\63\32\2\u02f2\u00fe\3\2\2\2\u02f3\u02f4\5+\26\2\u02f4\u02f5\5"+
		"%\23\2\u02f5\u0100\3\2\2\2\u02f6\u02f7\7*\2\2\u02f7\u0102\3\2\2\2\u02f8"+
		"\u02f9\7+\2\2\u02f9\u0104\3\2\2\2\u02fa\u02fb\7.\2\2\u02fb\u0106\3\2\2"+
		"\2\u02fc\u02fd\7-\2\2\u02fd\u0108\3\2\2\2\u02fe\u02ff\7/\2\2\u02ff\u010a"+
		"\3\2\2\2\u0300\u0301\7,\2\2\u0301\u010c\3\2\2\2\u0302\u0303\7\61\2\2\u0303"+
		"\u010e\3\2\2\2\u0304\u0305\7?\2\2\u0305\u0110\3\2\2\2\u0306\u0307\5\37"+
		"\20\2\u0307\u0308\5#\22\2\u0308\u0309\5\17\b\2\u0309\u0112\3\2\2\2\u030a"+
		"\u030b\5+\26\2\u030b\u030c\5\27\f\2\u030c\u030d\5)\25\2\u030d\u0114\3"+
		"\2\2\2\u030e\u030f\5+\26\2\u030f\u0310\5\27\f\2\u0310\u0311\5\35\17\2"+
		"\u0311\u0116\3\2\2\2\u0312\u0313\5!\21\2\u0313\u0314\5#\22\2\u0314\u0315"+
		"\5-\27\2\u0315\u0118\3\2\2\2\u0316\u0317\5\t\5\2\u0317\u0318\5!\21\2\u0318"+
		"\u0319\5\17\b\2\u0319\u011a\3\2\2\2\u031a\u031b\5#\22\2\u031b\u031c\5"+
		")\25\2\u031c\u011c\3\2\2\2\u031d\u031e\5\65\33\2\u031e\u031f\5#\22\2\u031f"+
		"\u0320\5)\25\2\u0320\u011e\3\2\2\2\u0321\u0323\t\35\2\2\u0322\u0321\3"+
		"\2\2\2\u0322\u0323\3\2\2\2\u0323\u0325\3\2\2\2\u0324\u0326\t\36\2\2\u0325"+
		"\u0324\3\2\2\2\u0326\u0327\3\2\2\2\u0327\u0325\3\2\2\2\u0327\u0328\3\2"+
		"\2\2\u0328\u032a\3\2\2\2\u0329\u032b\5\17\b\2\u032a\u0329\3\2\2\2\u032a"+
		"\u032b\3\2\2\2\u032b\u0120\3\2\2\2\u032c\u032e\t\35\2\2\u032d\u032c\3"+
		"\2\2\2\u032d\u032e\3\2\2\2\u032e\u032f\3\2\2\2\u032f\u0330\7\62\2\2\u0330"+
		"\u0332\5\65\33\2\u0331\u0333\t\37\2\2\u0332\u0331\3\2\2\2\u0333\u0334"+
		"\3\2\2\2\u0334\u0332\3\2\2\2\u0334\u0335\3\2\2\2\u0335\u0122\3\2\2\2\u0336"+
		"\u0338\t\35\2\2\u0337\u0336\3\2\2\2\u0337\u0338\3\2\2\2\u0338\u033a\3"+
		"\2\2\2\u0339\u033b\t\37\2\2\u033a\u0339\3\2\2\2\u033b\u033c\3\2\2\2\u033c"+
		"\u033a\3\2\2\2\u033c\u033d\3\2\2\2\u033d\u033e\3\2\2\2\u033e\u033f\5\27"+
		"\f\2\u033f\u0124\3\2\2\2\u0340\u0342\t\35\2\2\u0341\u0340\3\2\2\2\u0341"+
		"\u0342\3\2\2\2\u0342\u0344\3\2\2\2\u0343\u0345\t \2\2\u0344\u0343\3\2"+
		"\2\2\u0345\u0346\3\2\2\2\u0346\u0344\3\2\2\2\u0346\u0347\3\2\2\2\u0347"+
		"\u0348\3\2\2\2\u0348\u0349\t!\2\2\u0349\u0126\3\2\2\2\u034a\u034c\t\""+
		"\2\2\u034b\u034a\3\2\2\2\u034c\u034d\3\2\2\2\u034d\u034b\3\2\2\2\u034d"+
		"\u034e\3\2\2\2\u034e\u034f\3\2\2\2\u034f\u0350\5\13\6\2\u0350\u0128\3"+
		"\2\2\2\u0351\u0353\t#\2\2\u0352\u0354\t$\2\2\u0353\u0352\3\2\2\2\u0354"+
		"\u0355\3\2\2\2\u0355\u0353\3\2\2\2\u0355\u0356\3\2\2\2\u0356\u0357\3\2"+
		"\2\2\u0357\u0358\t#\2\2\u0358\u012a\3\2\2\2\u0359\u035b\7$\2\2\u035a\u035c"+
		"\t%\2\2\u035b\u035a\3\2\2\2\u035c\u035d\3\2\2\2\u035d\u035b\3\2\2\2\u035d"+
		"\u035e\3\2\2\2\u035e\u035f\3\2\2\2\u035f\u0360\7$\2\2\u0360\u012c\3\2"+
		"\2\2\u0361\u0365\t&\2\2\u0362\u0364\t\'\2\2\u0363\u0362\3\2\2\2\u0364"+
		"\u0367\3\2\2\2\u0365\u0363\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u012e\3\2"+
		"\2\2\u0367\u0365\3\2\2\2\u0368\u036c\t&\2\2\u0369\u036b\t\'\2\2\u036a"+
		"\u0369\3\2\2\2\u036b\u036e\3\2\2\2\u036c\u036a\3\2\2\2\u036c\u036d\3\2"+
		"\2\2\u036d\u036f\3\2\2\2\u036e\u036c\3\2\2\2\u036f\u0370\7<\2\2\u0370"+
		"\u0130\3\2\2\2\u0371\u0372\13\2\2\2\u0372\u0132\3\2\2\2\24\2\u013c\u0141"+
		"\u0145\u0322\u0327\u032a\u032d\u0334\u0337\u033c\u0341\u0346\u034d\u0355"+
		"\u035d\u0365\u036c\3\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}