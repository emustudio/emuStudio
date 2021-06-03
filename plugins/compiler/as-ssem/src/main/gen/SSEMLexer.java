// Generated from /home/vbmacher/projects/emustudio/emuStudio/plugins/compiler/as-ssem/src/main/antlr/SSEMLexer.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SSEMLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, COMMENT=2, EOL=3, JMP=4, JPR=5, LDN=6, STO=7, SUB=8, CMP=9, STP=10, 
		START=11, NUM=12, BNUM=13, NUMBER=14, HEXNUMBER=15, ERROR=16, BWS=17, 
		BinaryNumber=18, BERROR=19;
	public static final int
		BIN=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "BIN"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "COMMENT", "EOL", "J", "M", "P", "R", "L", "D", "N", "S", "T", 
			"O", "U", "B", "C", "K", "H", "A", "I", "JMP", "JPR", "LDN", "STO", "SUB", 
			"CMP", "STP", "START", "NUM", "BNUM", "NUMBER", "HEXNUMBER", "ERROR", 
			"BWS", "BinaryNumber", "BERROR"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "COMMENT", "EOL", "JMP", "JPR", "LDN", "STO", "SUB", "CMP", 
			"STP", "START", "NUM", "BNUM", "NUMBER", "HEXNUMBER", "ERROR", "BWS", 
			"BinaryNumber", "BERROR"
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


	public SSEMLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "SSEMLexer.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\25\u00f3\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4"+
		" \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3"+
		"\3\5\3V\n\3\3\3\7\3Y\n\3\f\3\16\3\\\13\3\3\4\5\4_\n\4\3\4\3\4\3\5\3\5"+
		"\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16"+
		"\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25"+
		"\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\5\27\u0095\n\27\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31"+
		"\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\5\33\u00ab"+
		"\n\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\5\34\u00b5\n\34\3\35\3\35"+
		"\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\5\37\u00cb\n\37\3\37\3\37\3 \5 \u00d0\n \3 \6 \u00d3"+
		"\n \r \16 \u00d4\3!\5!\u00d8\n!\3!\3!\3!\3!\5!\u00de\n!\3!\6!\u00e1\n"+
		"!\r!\16!\u00e2\3\"\3\"\3#\3#\3#\3#\3$\6$\u00ec\n$\r$\16$\u00ed\3$\3$\3"+
		"%\3%\2\2&\4\3\6\4\b\5\n\2\f\2\16\2\20\2\22\2\24\2\26\2\30\2\32\2\34\2"+
		"\36\2 \2\"\2$\2&\2(\2*\2,\6.\7\60\b\62\t\64\n\66\138\f:\r<\16>\17@\20"+
		"B\21D\22F\23H\24J\25\4\2\3\32\4\2\13\13\"\"\4\2%%==\4\2\f\f\17\17\4\2"+
		"LLll\4\2OOoo\4\2RRrr\4\2TTtt\4\2NNnn\4\2FFff\4\2PPpp\4\2UUuu\4\2VVvv\4"+
		"\2QQqq\4\2WWww\4\2DDdd\4\2EEee\4\2MMmm\4\2JJjj\4\2CCcc\4\2KKkk\3\2//\3"+
		"\2\62;\5\2\62;CHch\3\2\62\63\2\u00ef\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2"+
		"\2\2,\3\2\2\2\2.\3\2\2\2\2\60\3\2\2\2\2\62\3\2\2\2\2\64\3\2\2\2\2\66\3"+
		"\2\2\2\28\3\2\2\2\2:\3\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2"+
		"\2\2D\3\2\2\2\3F\3\2\2\2\3H\3\2\2\2\3J\3\2\2\2\4L\3\2\2\2\6U\3\2\2\2\b"+
		"^\3\2\2\2\nb\3\2\2\2\fd\3\2\2\2\16f\3\2\2\2\20h\3\2\2\2\22j\3\2\2\2\24"+
		"l\3\2\2\2\26n\3\2\2\2\30p\3\2\2\2\32r\3\2\2\2\34t\3\2\2\2\36v\3\2\2\2"+
		" x\3\2\2\2\"z\3\2\2\2$|\3\2\2\2&~\3\2\2\2(\u0080\3\2\2\2*\u0082\3\2\2"+
		"\2,\u0084\3\2\2\2.\u0094\3\2\2\2\60\u0096\3\2\2\2\62\u009a\3\2\2\2\64"+
		"\u009e\3\2\2\2\66\u00aa\3\2\2\28\u00b4\3\2\2\2:\u00b6\3\2\2\2<\u00bc\3"+
		"\2\2\2>\u00ca\3\2\2\2@\u00cf\3\2\2\2B\u00d7\3\2\2\2D\u00e4\3\2\2\2F\u00e6"+
		"\3\2\2\2H\u00eb\3\2\2\2J\u00f1\3\2\2\2LM\t\2\2\2MN\3\2\2\2NO\b\2\2\2O"+
		"\5\3\2\2\2PQ\7\61\2\2QV\7\61\2\2RS\7/\2\2SV\7/\2\2TV\t\3\2\2UP\3\2\2\2"+
		"UR\3\2\2\2UT\3\2\2\2VZ\3\2\2\2WY\n\4\2\2XW\3\2\2\2Y\\\3\2\2\2ZX\3\2\2"+
		"\2Z[\3\2\2\2[\7\3\2\2\2\\Z\3\2\2\2]_\7\17\2\2^]\3\2\2\2^_\3\2\2\2_`\3"+
		"\2\2\2`a\7\f\2\2a\t\3\2\2\2bc\t\5\2\2c\13\3\2\2\2de\t\6\2\2e\r\3\2\2\2"+
		"fg\t\7\2\2g\17\3\2\2\2hi\t\b\2\2i\21\3\2\2\2jk\t\t\2\2k\23\3\2\2\2lm\t"+
		"\n\2\2m\25\3\2\2\2no\t\13\2\2o\27\3\2\2\2pq\t\f\2\2q\31\3\2\2\2rs\t\r"+
		"\2\2s\33\3\2\2\2tu\t\16\2\2u\35\3\2\2\2vw\t\17\2\2w\37\3\2\2\2xy\t\20"+
		"\2\2y!\3\2\2\2z{\t\21\2\2{#\3\2\2\2|}\t\22\2\2}%\3\2\2\2~\177\t\23\2\2"+
		"\177\'\3\2\2\2\u0080\u0081\t\24\2\2\u0081)\3\2\2\2\u0082\u0083\t\25\2"+
		"\2\u0083+\3\2\2\2\u0084\u0085\5\n\5\2\u0085\u0086\5\f\6\2\u0086\u0087"+
		"\5\16\7\2\u0087-\3\2\2\2\u0088\u0089\5\n\5\2\u0089\u008a\5\16\7\2\u008a"+
		"\u008b\5\20\b\2\u008b\u0095\3\2\2\2\u008c\u008d\5\n\5\2\u008d\u008e\5"+
		"\20\b\2\u008e\u008f\5\16\7\2\u008f\u0095\3\2\2\2\u0090\u0091\5\n\5\2\u0091"+
		"\u0092\5\f\6\2\u0092\u0093\5\20\b\2\u0093\u0095\3\2\2\2\u0094\u0088\3"+
		"\2\2\2\u0094\u008c\3\2\2\2\u0094\u0090\3\2\2\2\u0095/\3\2\2\2\u0096\u0097"+
		"\5\22\t\2\u0097\u0098\5\24\n\2\u0098\u0099\5\26\13\2\u0099\61\3\2\2\2"+
		"\u009a\u009b\5\30\f\2\u009b\u009c\5\32\r\2\u009c\u009d\5\34\16\2\u009d"+
		"\63\3\2\2\2\u009e\u009f\5\30\f\2\u009f\u00a0\5\36\17\2\u00a0\u00a1\5 "+
		"\20\2\u00a1\65\3\2\2\2\u00a2\u00a3\5\"\21\2\u00a3\u00a4\5\f\6\2\u00a4"+
		"\u00a5\5\16\7\2\u00a5\u00ab\3\2\2\2\u00a6\u00a7\5\30\f\2\u00a7\u00a8\5"+
		"$\22\2\u00a8\u00a9\5\26\13\2\u00a9\u00ab\3\2\2\2\u00aa\u00a2\3\2\2\2\u00aa"+
		"\u00a6\3\2\2\2\u00ab\67\3\2\2\2\u00ac\u00ad\5\30\f\2\u00ad\u00ae\5\32"+
		"\r\2\u00ae\u00af\5\16\7\2\u00af\u00b5\3\2\2\2\u00b0\u00b1\5&\23\2\u00b1"+
		"\u00b2\5\22\t\2\u00b2\u00b3\5\32\r\2\u00b3\u00b5\3\2\2\2\u00b4\u00ac\3"+
		"\2\2\2\u00b4\u00b0\3\2\2\2\u00b59\3\2\2\2\u00b6\u00b7\5\30\f\2\u00b7\u00b8"+
		"\5\32\r\2\u00b8\u00b9\5(\24\2\u00b9\u00ba\5\20\b\2\u00ba\u00bb\5\32\r"+
		"\2\u00bb;\3\2\2\2\u00bc\u00bd\5\26\13\2\u00bd\u00be\5\36\17\2\u00be\u00bf"+
		"\5\f\6\2\u00bf=\3\2\2\2\u00c0\u00c1\5 \20\2\u00c1\u00c2\5\26\13\2\u00c2"+
		"\u00c3\5\36\17\2\u00c3\u00c4\5\f\6\2\u00c4\u00cb\3\2\2\2\u00c5\u00c6\5"+
		" \20\2\u00c6\u00c7\5*\25\2\u00c7\u00c8\5\26\13\2\u00c8\u00c9\5\30\f\2"+
		"\u00c9\u00cb\3\2\2\2\u00ca\u00c0\3\2\2\2\u00ca\u00c5\3\2\2\2\u00cb\u00cc"+
		"\3\2\2\2\u00cc\u00cd\b\37\3\2\u00cd?\3\2\2\2\u00ce\u00d0\t\26\2\2\u00cf"+
		"\u00ce\3\2\2\2\u00cf\u00d0\3\2\2\2\u00d0\u00d2\3\2\2\2\u00d1\u00d3\t\27"+
		"\2\2\u00d2\u00d1\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00d2\3\2\2\2\u00d4"+
		"\u00d5\3\2\2\2\u00d5A\3\2\2\2\u00d6\u00d8\t\26\2\2\u00d7\u00d6\3\2\2\2"+
		"\u00d7\u00d8\3\2\2\2\u00d8\u00dd\3\2\2\2\u00d9\u00da\7\62\2\2\u00da\u00de"+
		"\7z\2\2\u00db\u00dc\7\62\2\2\u00dc\u00de\7Z\2\2\u00dd\u00d9\3\2\2\2\u00dd"+
		"\u00db\3\2\2\2\u00de\u00e0\3\2\2\2\u00df\u00e1\t\30\2\2\u00e0\u00df\3"+
		"\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e0\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3"+
		"C\3\2\2\2\u00e4\u00e5\13\2\2\2\u00e5E\3\2\2\2\u00e6\u00e7\t\2\2\2\u00e7"+
		"\u00e8\3\2\2\2\u00e8\u00e9\b#\2\2\u00e9G\3\2\2\2\u00ea\u00ec\t\31\2\2"+
		"\u00eb\u00ea\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00eb\3\2\2\2\u00ed\u00ee"+
		"\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0\b$\4\2\u00f0I\3\2\2\2\u00f1\u00f2"+
		"\13\2\2\2\u00f2K\3\2\2\2\21\2\3UZ^\u0094\u00aa\u00b4\u00ca\u00cf\u00d4"+
		"\u00d7\u00dd\u00e2\u00ed\5\2\3\2\7\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}