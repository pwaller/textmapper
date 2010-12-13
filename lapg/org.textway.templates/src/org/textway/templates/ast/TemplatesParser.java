package org.textway.templates.ast;


import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.textway.templates.ast.TemplatesLexer.ErrorReporter;
import org.textway.templates.ast.TemplatesLexer.LapgSymbol;
import org.textway.templates.ast.TemplatesLexer.Lexems;
import org.textway.templates.ast.TemplatesTree.TextSource;
import org.textway.templates.bundle.IBundleEntity;

public class TemplatesParser {

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 1L;

		public ParseException() {
		}
	}

	private final ErrorReporter reporter;

	public TemplatesParser(ErrorReporter reporter) {
		this.reporter = reporter;
	}

	
	private static final boolean DEBUG_SYNTAX = false;
	TextSource source;
	String templatePackage;
	
	private int killEnds = -1;
	
	private int rawText(int start, final int end) {
		char[] buff = source.getContents();
		if( killEnds == start ) {
			while( start < end && (buff[start] == '\t' || buff[start] == ' ') )
				start++;
	
			if( start < end && buff[start] == '\r' )
				start++;
	
			if( start < end && buff[start] == '\n' )
				start++;
		}
		return start;
	}
	
	private void checkIsSpace(int start, int end, int line) {
		String val = source.getText(rawText(start,end),end).trim();
		if( val.length() > 0 )
			reporter.error(start, end, line, "Unknown text ignored: `"+val+"`");
	}
	
	private void applyElse(CompoundNode node, ElseIfNode elseNode, int offset, int endoffset, int line) {
		if (elseNode == null ) {
			return;
		}
		if (node instanceof IfNode) {
			((IfNode)node).applyElse(elseNode);
		} else {
			reporter.error(offset, endoffset, line, "Unknown else node, instructions skipped");
		}
	}
	
	private ExpressionNode createMapCollect(ExpressionNode context, String instruction, String varName, ExpressionNode key, ExpressionNode value, TextSource source, int offset, int endoffset, int line) {
		if(!instruction.equals("collect")) {
			reporter.error(offset, endoffset, line, "unknown collection processing instruction: " + instruction);
			return new ErrorNode(source, offset, endoffset);
		}
		return new CollectMapNode(context, varName, key, value, source, offset, endoffset);
	}
	
	private ExpressionNode createCollectionProcessor(ExpressionNode context, String instruction, String varName, ExpressionNode foreachExpr, TextSource source, int offset, int endoffset, int line) {
		char first = instruction.charAt(0);
		int kind = 0;
		switch(first) {
		case 'c':
			if(instruction.equals("collect")) {
				kind = CollectionProcessorNode.COLLECT;
			} else if(instruction.equals("collectUnique")) {
				kind = CollectionProcessorNode.COLLECTUNIQUE;
			}
			break;
		case 'r':
			if(instruction.equals("reject")) {
				kind = CollectionProcessorNode.REJECT;
			}
			break;
		case 's':
			if(instruction.equals("select")) {
				kind = CollectionProcessorNode.SELECT;
			} else if(instruction.equals("sort")) {
				kind = CollectionProcessorNode.SORT;
			}
			break;
		case 'f':
			if(instruction.equals("forAll")) {
				kind = CollectionProcessorNode.FORALL;
			}
			break;
		case 'e':
			if(instruction.equals("exists")) {
				kind = CollectionProcessorNode.EXISTS;
			}
			break;
		case 'g':
			if(instruction.equals("groupBy")) {
				kind = CollectionProcessorNode.GROUPBY;
			}
			break;
		}
		if(kind == 0) {
			reporter.error(offset, endoffset, line, "unknown collection processing instruction: " + instruction);
			return new ErrorNode(source, offset, endoffset);
		}
		return new CollectionProcessorNode(context, kind, varName, foreachExpr, source, offset, endoffset);
	}
	
	private Node createEscapedId(String escid, int offset, int endoffset) {
		int sharp = escid.indexOf('#');
		if( sharp >= 0 ) {
			Integer index = new Integer(escid.substring(sharp+1));
			escid = escid.substring(0, sharp);
			return new IndexNode(new SelectNode(null,escid,source,offset,endoffset), new LiteralNode(index,source,offset,endoffset),source,offset,endoffset);
		
		} else {
			return new SelectNode(null,escid,source,offset,endoffset);
		}
	}
	
	private void skipSpaces(int offset) {
		killEnds = offset+1;
	}
	
	private void checkFqn(String templateName, int offset, int endoffset, int line) {
		if( templateName.indexOf('.') >= 0 && templatePackage != null) {
			reporter.error(offset, endoffset, line, "template name should be simple identifier");
		}
	}
	private static final int lapg_action[] = {
		-3, -1, 8, -11, -19, 3, 7, -1, 2, 37, 36, 34, 35, -1, -27, 28,
		33, 31, 32, -1, 16, -1, 10, -1, 4, -1, 6, -1, -41, 80, 82, -1,
		-1, 105, -1, -1, -1, -1, -1, 84, -1, 104, 83, -1, -1, -1, -97, -1,
		-1, -1, -125, 93, 81, 109, -177, -223, -251, -277, -299, 131, 133, -319, 27, -1,
		50, -327, -1, -1, 5, -339, -1, -367, -379, -433, -1, -441, -1, -449, -1, -1,
		-457, 108, 107, -465, -1, 135, -515, -1, -1, 30, 29, 38, 68, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, 77, 56, 59, -523, -1, 12, -529, -537, 26, -1, 132, -547, 41, -575, -1,
		46, 47, -1, -1, -583, -589, 102, 103, 101, -1, 95, -1, -1, 94, 79, -1,
		-595, -1, -1, -649, -695, -741, -787, -833, -879, -925, -971, -1017, 121, -1063, -1093, -1121,
		-1149, -1175, -1, 134, -1, -1, -1197, -1207, -1, -1, 51, -1, 14, -1, -1213, 85,
		-1, -1, 43, 44, 49, -1, -1219, -1, -1229, -1, 61, -1, 99, -1, 136, 92,
		-1235, -1, -1263, -1, 76, -1, -1, -1, 22, 20, -1291, 17, -1, 55, -1301, -1,
		-1, 70, 71, 98, -1, -1, 64, -1, -1309, -1, -1, -1, 130, -1, -1, -1355,
		-1, -1, -1, -1365, -1, -1, 66, 65, 62, 100, -1, 87, -1373, 90, -1, -1,
		58, 24, -1, -1, -1, -1, -1, -1, -1, 15, -1401, 67, -1, 88, -1, 91,
		57, 72, -1, -1, 63, 89, -1, -1, -2, -2
	};

	private static final short lapg_lalr[] = {
		1, -1, 5, -1, 0, 0, -1, -2, 11, -1, 31, -1, 28, 9, -1, -2,
		1, -1, 5, -1, 0, 1, -1, -2, 1, -1, 2, -1, 3, -1, 4, -1,
		5, -1, 0, 137, -1, -2, 46, -1, 53, -1, 24, 78, 30, 78, 35, 78,
		36, 78, 37, 78, 38, 78, 39, 78, 40, 78, 41, 78, 44, 78, 45, 78,
		47, 78, 48, 78, 49, 78, 50, 78, 51, 78, 52, 78, 54, 78, 55, 78,
		57, 78, 58, 78, 59, 78, 60, 78, 61, 78, 62, 78, -1, -2, 7, -1,
		8, -1, 9, -1, 16, -1, 26, -1, 27, -1, 32, -1, 33, -1, 38, -1,
		42, -1, 44, -1, 46, -1, 45, 53, -1, -2, 44, -1, 48, -1, 55, -1,
		24, 106, 30, 106, 35, 106, 36, 106, 37, 106, 38, 106, 39, 106, 40, 106,
		41, 106, 45, 106, 47, 106, 49, 106, 50, 106, 51, 106, 52, 106, 54, 106,
		57, 106, 58, 106, 59, 106, 60, 106, 61, 106, 62, 106, -1, -2, 37, -1,
		38, -1, 39, -1, 40, -1, 41, -1, 57, -1, 58, -1, 59, -1, 60, -1,
		24, 119, 30, 119, 35, 119, 36, 119, 45, 119, 47, 119, 49, 119, 50, 119,
		51, 119, 52, 119, 54, 119, 61, 119, 62, 119, -1, -2, 24, -1, 30, 122,
		35, 122, 36, 122, 45, 122, 47, 122, 49, 122, 50, 122, 51, 122, 52, 122,
		54, 122, 61, 122, 62, 122, -1, -2, 52, -1, 54, -1, 30, 125, 35, 125,
		36, 125, 45, 125, 47, 125, 49, 125, 50, 125, 51, 125, 61, 125, 62, 125,
		-1, -2, 50, -1, 30, 127, 35, 127, 36, 127, 45, 127, 47, 127, 49, 127,
		51, 127, 61, 127, 62, 127, -1, -2, 51, -1, 62, -1, 30, 129, 35, 129,
		36, 129, 45, 129, 47, 129, 49, 129, 61, 129, -1, -2, 49, -1, 35, 39,
		36, 39, -1, -2, 46, -1, 48, -1, 17, 11, 35, 11, 36, 11, -1, -2,
		7, -1, 8, -1, 9, -1, 16, -1, 26, -1, 27, -1, 32, -1, 33, -1,
		38, -1, 42, -1, 44, -1, 46, -1, 47, 53, -1, -2, 46, -1, 48, -1,
		17, 40, 35, 40, 36, 40, -1, -2, 46, -1, 24, 78, 30, 78, 35, 78,
		36, 78, 37, 78, 38, 78, 39, 78, 40, 78, 41, 78, 44, 78, 45, 78,
		47, 78, 48, 78, 49, 78, 50, 78, 51, 78, 52, 78, 54, 78, 55, 78,
		57, 78, 58, 78, 59, 78, 60, 78, 61, 78, 62, 78, -1, -2, 49, -1,
		35, 45, 36, 45, -1, -2, 49, -1, 35, 74, 36, 74, -1, -2, 49, -1,
		35, 73, 36, 73, -1, -2, 49, -1, 35, 48, 36, 48, -1, -2, 46, -1,
		53, -1, 56, -1, 61, -1, 24, 78, 37, 78, 38, 78, 39, 78, 40, 78,
		41, 78, 44, 78, 45, 78, 48, 78, 49, 78, 50, 78, 51, 78, 52, 78,
		54, 78, 55, 78, 57, 78, 58, 78, 59, 78, 60, 78, 62, 78, -1, -2,
		49, -1, 45, 54, 47, 54, -1, -2, 7, -1, 47, 18, -1, -2, 17, -1,
		35, 13, 36, 13, -1, -2, 46, -1, 48, -1, 17, 11, 53, 11, -1, -2,
		7, -1, 8, -1, 9, -1, 16, -1, 26, -1, 27, -1, 32, -1, 33, -1,
		38, -1, 42, -1, 44, -1, 46, -1, 47, 53, -1, -2, 17, -1, 35, 42,
		36, 42, -1, -2, 7, -1, 47, 96, -1, -2, 1, -1, 5, 60, -1, -2,
		46, -1, 24, 86, 30, 86, 35, 86, 36, 86, 37, 86, 38, 86, 39, 86,
		40, 86, 41, 86, 44, 86, 45, 86, 47, 86, 48, 86, 49, 86, 50, 86,
		51, 86, 52, 86, 54, 86, 55, 86, 57, 86, 58, 86, 59, 86, 60, 86,
		61, 86, 62, 86, -1, -2, 37, 113, 38, 113, 39, -1, 40, -1, 41, -1,
		57, 113, 58, 113, 59, 113, 60, 113, 24, 113, 30, 113, 35, 113, 36, 113,
		45, 113, 47, 113, 49, 113, 50, 113, 51, 113, 52, 113, 54, 113, 61, 113,
		62, 113, -1, -2, 37, 114, 38, 114, 39, -1, 40, -1, 41, -1, 57, 114,
		58, 114, 59, 114, 60, 114, 24, 114, 30, 114, 35, 114, 36, 114, 45, 114,
		47, 114, 49, 114, 50, 114, 51, 114, 52, 114, 54, 114, 61, 114, 62, 114,
		-1, -2, 37, 110, 38, 110, 39, 110, 40, 110, 41, 110, 57, 110, 58, 110,
		59, 110, 60, 110, 24, 110, 30, 110, 35, 110, 36, 110, 45, 110, 47, 110,
		49, 110, 50, 110, 51, 110, 52, 110, 54, 110, 61, 110, 62, 110, -1, -2,
		37, 111, 38, 111, 39, 111, 40, 111, 41, 111, 57, 111, 58, 111, 59, 111,
		60, 111, 24, 111, 30, 111, 35, 111, 36, 111, 45, 111, 47, 111, 49, 111,
		50, 111, 51, 111, 52, 111, 54, 111, 61, 111, 62, 111, -1, -2, 37, 112,
		38, 112, 39, 112, 40, 112, 41, 112, 57, 112, 58, 112, 59, 112, 60, 112,
		24, 112, 30, 112, 35, 112, 36, 112, 45, 112, 47, 112, 49, 112, 50, 112,
		51, 112, 52, 112, 54, 112, 61, 112, 62, 112, -1, -2, 37, -1, 38, -1,
		39, -1, 40, -1, 41, -1, 57, 117, 58, 117, 59, 117, 60, 117, 24, 117,
		30, 117, 35, 117, 36, 117, 45, 117, 47, 117, 49, 117, 50, 117, 51, 117,
		52, 117, 54, 117, 61, 117, 62, 117, -1, -2, 37, -1, 38, -1, 39, -1,
		40, -1, 41, -1, 57, 118, 58, 118, 59, 118, 60, 118, 24, 118, 30, 118,
		35, 118, 36, 118, 45, 118, 47, 118, 49, 118, 50, 118, 51, 118, 52, 118,
		54, 118, 61, 118, 62, 118, -1, -2, 37, -1, 38, -1, 39, -1, 40, -1,
		41, -1, 57, 115, 58, 115, 59, 115, 60, 115, 24, 115, 30, 115, 35, 115,
		36, 115, 45, 115, 47, 115, 49, 115, 50, 115, 51, 115, 52, 115, 54, 115,
		61, 115, 62, 115, -1, -2, 37, -1, 38, -1, 39, -1, 40, -1, 41, -1,
		57, 116, 58, 116, 59, 116, 60, 116, 24, 116, 30, 116, 35, 116, 36, 116,
		45, 116, 47, 116, 49, 116, 50, 116, 51, 116, 52, 116, 54, 116, 61, 116,
		62, 116, -1, -2, 48, -1, 24, 120, 30, 120, 35, 120, 36, 120, 45, 120,
		47, 120, 49, 120, 50, 120, 51, 120, 52, 120, 54, 120, 61, 120, 62, 120,
		-1, -2, 24, -1, 30, 123, 35, 123, 36, 123, 45, 123, 47, 123, 49, 123,
		50, 123, 51, 123, 52, 123, 54, 123, 61, 123, 62, 123, -1, -2, 24, -1,
		30, 124, 35, 124, 36, 124, 45, 124, 47, 124, 49, 124, 50, 124, 51, 124,
		52, 124, 54, 124, 61, 124, 62, 124, -1, -2, 52, -1, 54, -1, 30, 126,
		35, 126, 36, 126, 45, 126, 47, 126, 49, 126, 50, 126, 51, 126, 61, 126,
		62, 126, -1, -2, 50, -1, 30, 128, 35, 128, 36, 128, 45, 128, 47, 128,
		49, 128, 51, 128, 61, 128, 62, 128, -1, -2, 47, 21, 49, 21, 7, 50,
		48, 50, -1, -2, 49, -1, 47, 19, -1, -2, 17, -1, 53, 13, -1, -2,
		30, -1, 49, -1, 35, 69, 36, 69, -1, -2, 49, -1, 47, 97, -1, -2,
		7, -1, 8, -1, 9, -1, 16, -1, 26, -1, 27, -1, 32, -1, 33, -1,
		38, -1, 42, -1, 44, -1, 46, -1, 47, 53, -1, -2, 7, -1, 8, -1,
		9, -1, 16, -1, 26, -1, 27, -1, 32, -1, 33, -1, 38, -1, 42, -1,
		44, -1, 46, -1, 47, 53, -1, -2, 48, -1, 35, 25, 36, 25, 53, 25,
		-1, -2, 49, -1, 35, 52, 36, 52, -1, -2, 43, -1, 46, -1, 24, 78,
		37, 78, 38, 78, 39, 78, 40, 78, 41, 78, 44, 78, 47, 78, 48, 78,
		49, 78, 50, 78, 51, 78, 52, 78, 54, 78, 55, 78, 57, 78, 58, 78,
		59, 78, 60, 78, 62, 78, -1, -2, 47, 23, 49, 23, 7, 50, 48, 50,
		-1, -2, 49, -1, 35, 75, 36, 75, -1, -2, 7, -1, 8, -1, 9, -1,
		16, -1, 26, -1, 27, -1, 32, -1, 33, -1, 38, -1, 42, -1, 44, -1,
		46, -1, 47, 53, -1, -2, 30, -1, 35, 69, 36, 69, -1, -2
	};

	private static final short lapg_sym_goto[] = {
		0, 2, 18, 31, 44, 57, 73, 78, 147, 199, 252, 257, 258, 260, 264, 266,
		271, 323, 331, 336, 341, 341, 347, 349, 349, 352, 352, 404, 456, 457, 462, 464,
		465, 517, 569, 574, 585, 594, 604, 666, 676, 686, 696, 748, 749, 803, 807, 871,
		882, 892, 914, 916, 917, 919, 924, 926, 927, 930, 940, 950, 960, 970, 975, 976,
		976, 977, 978, 980, 982, 983, 985, 987, 988, 990, 992, 998, 1007, 1020, 1033, 1038,
		1039, 1048, 1049, 1050, 1063, 1065, 1078, 1079, 1081, 1094, 1099, 1101, 1106, 1158, 1210, 1212,
		1215, 1267, 1319, 1369, 1410, 1449, 1487, 1524, 1561, 1582, 1602, 1608, 1609, 1610, 1611, 1613,
		1615, 1616, 1617, 1618, 1619, 1625, 1626, 1628, 1629
	};

	private static final short lapg_sym_from[] = {
		262, 263, 0, 1, 4, 7, 14, 19, 27, 63, 133, 198, 213, 222, 238, 248,
		252, 258, 1, 7, 14, 19, 27, 63, 198, 213, 222, 238, 248, 252, 258, 1,
		7, 14, 19, 27, 63, 198, 213, 222, 238, 248, 252, 258, 1, 7, 14, 19,
		27, 63, 198, 213, 222, 238, 248, 252, 258, 0, 1, 4, 7, 14, 19, 27,
		63, 187, 198, 213, 222, 238, 248, 252, 258, 63, 213, 222, 248, 258, 13, 21,
		25, 31, 32, 34, 35, 36, 37, 38, 40, 43, 44, 45, 46, 47, 66, 69,
		70, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
		108, 109, 110, 111, 112, 116, 117, 124, 127, 131, 132, 137, 139, 140, 145, 168,
		171, 177, 181, 192, 194, 195, 197, 199, 208, 215, 224, 225, 226, 228, 229, 234,
		236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70,
		93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111,
		112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225,
		226, 228, 229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45,
		46, 47, 69, 70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106,
		107, 108, 109, 110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194,
		195, 197, 208, 215, 225, 226, 228, 229, 234, 236, 239, 254, 13, 25, 112, 229,
		239, 3, 212, 229, 25, 112, 229, 239, 112, 229, 13, 25, 112, 229, 239, 13,
		25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70, 93, 96, 97, 98,
		99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124, 127, 131,
		137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229, 234,
		236, 239, 254, 13, 25, 112, 119, 126, 174, 229, 239, 13, 25, 112, 229, 239,
		13, 25, 112, 229, 239, 13, 25, 112, 165, 229, 239, 74, 76, 55, 158, 159,
		13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70, 93, 96, 97,
		98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124, 127,
		131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229,
		234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69,
		70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110,
		111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215,
		225, 226, 228, 229, 234, 236, 239, 254, 23, 13, 25, 112, 229, 239, 182, 250,
		3, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70, 93, 96,
		97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124,
		127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228,
		229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47,
		69, 70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109,
		110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208,
		215, 225, 226, 228, 229, 234, 236, 239, 254, 13, 25, 112, 229, 239, 48, 49,
		67, 79, 164, 165, 173, 221, 242, 244, 245, 48, 49, 79, 164, 165, 173, 221,
		244, 245, 54, 147, 148, 149, 150, 151, 152, 153, 154, 155, 13, 25, 32, 35,
		37, 40, 43, 44, 45, 46, 47, 54, 69, 70, 93, 96, 97, 98, 99, 100,
		101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124, 127, 131, 137, 140,
		145, 147, 148, 149, 150, 151, 152, 153, 154, 155, 177, 181, 192, 194, 195, 197,
		208, 215, 225, 226, 228, 229, 234, 236, 239, 254, 54, 147, 148, 149, 150, 151,
		152, 153, 154, 155, 54, 147, 148, 149, 150, 151, 152, 153, 154, 155, 54, 147,
		148, 149, 150, 151, 152, 153, 154, 155, 13, 25, 32, 35, 37, 40, 43, 44,
		45, 46, 47, 69, 70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106,
		107, 108, 109, 110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194,
		195, 197, 208, 215, 225, 226, 228, 229, 234, 236, 239, 254, 216, 13, 25, 32,
		35, 37, 40, 43, 44, 45, 46, 47, 50, 69, 70, 93, 96, 97, 98, 99,
		100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124, 127, 130, 131,
		137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229, 234,
		236, 239, 254, 84, 87, 143, 243, 13, 25, 28, 32, 35, 37, 40, 43, 44,
		45, 46, 47, 65, 69, 70, 71, 72, 78, 83, 93, 95, 96, 97, 98, 99,
		100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 120, 124, 127, 131,
		137, 140, 144, 145, 146, 177, 181, 192, 194, 195, 197, 208, 215, 216, 218, 225,
		226, 228, 229, 234, 236, 239, 254, 88, 122, 169, 176, 185, 193, 217, 219, 246,
		247, 259, 50, 65, 71, 78, 120, 146, 157, 168, 202, 224, 61, 73, 75, 77,
		79, 80, 84, 86, 88, 143, 167, 182, 184, 193, 206, 207, 221, 227, 242, 244,
		246, 259, 57, 161, 58, 56, 160, 28, 83, 183, 189, 204, 56, 160, 50, 83,
		183, 189, 54, 147, 148, 149, 150, 151, 152, 153, 154, 155, 54, 147, 148, 149,
		150, 151, 152, 153, 154, 155, 54, 147, 148, 149, 150, 151, 152, 153, 154, 155,
		54, 147, 148, 149, 150, 151, 152, 153, 154, 155, 83, 162, 183, 189, 246, 58,
		0, 0, 0, 4, 0, 4, 3, 0, 4, 65, 120, 116, 119, 174, 7, 27,
		1, 7, 19, 198, 238, 252, 48, 49, 79, 164, 165, 173, 221, 244, 245, 1,
		7, 14, 19, 27, 63, 198, 213, 222, 238, 248, 252, 258, 1, 7, 14, 19,
		27, 63, 198, 213, 222, 238, 248, 252, 258, 13, 25, 112, 229, 239, 73, 21,
		31, 38, 66, 95, 105, 116, 171, 199, 126, 71, 1, 7, 14, 19, 27, 63,
		198, 213, 222, 238, 248, 252, 258, 63, 248, 1, 7, 14, 19, 27, 63, 198,
		213, 222, 238, 248, 252, 258, 187, 187, 213, 1, 7, 14, 19, 27, 63, 198,
		213, 222, 238, 248, 252, 258, 13, 25, 112, 229, 239, 182, 250, 63, 213, 222,
		248, 258, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70, 93,
		96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112,
		124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226,
		228, 229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46,
		47, 69, 70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108,
		109, 110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197,
		208, 215, 225, 226, 228, 229, 234, 236, 239, 254, 46, 132, 83, 183, 189, 13,
		25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70, 93, 96, 97, 98,
		99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 124, 127, 131,
		137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229, 234,
		236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 44, 45, 46, 47, 69, 70,
		93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111,
		112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225,
		226, 228, 229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 46, 47,
		69, 70, 93, 96, 97, 98, 99, 100, 101, 102, 103, 104, 106, 107, 108, 109,
		110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208,
		215, 225, 226, 228, 229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43,
		46, 47, 69, 70, 93, 106, 107, 108, 109, 110, 111, 112, 124, 127, 131, 137,
		140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229, 234, 236,
		239, 254, 13, 25, 32, 35, 37, 40, 43, 46, 47, 69, 70, 93, 108, 109,
		110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208,
		215, 225, 226, 228, 229, 234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43,
		46, 47, 69, 70, 93, 109, 110, 111, 112, 124, 127, 131, 137, 140, 145, 177,
		181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229, 234, 236, 239, 254, 13,
		25, 32, 35, 37, 40, 43, 46, 47, 69, 70, 93, 110, 111, 112, 124, 127,
		131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208, 215, 225, 226, 228, 229,
		234, 236, 239, 254, 13, 25, 32, 35, 37, 40, 43, 46, 47, 69, 70, 93,
		110, 111, 112, 124, 127, 131, 137, 140, 145, 177, 181, 192, 194, 195, 197, 208,
		215, 225, 226, 228, 229, 234, 236, 239, 254, 13, 25, 35, 37, 40, 43, 47,
		93, 111, 112, 131, 145, 177, 197, 208, 225, 228, 229, 234, 239, 254, 13, 25,
		35, 37, 40, 43, 47, 93, 112, 131, 145, 177, 197, 208, 225, 228, 229, 234,
		239, 254, 46, 69, 124, 192, 194, 236, 1, 0, 3, 65, 120, 119, 174, 116,
		71, 126, 73, 46, 69, 124, 192, 194, 236, 133, 182, 250, 132
	};

	private static final short lapg_sym_to[] = {
		264, 265, 2, 9, 2, 9, 9, 9, 9, 9, 186, 9, 9, 9, 9, 9,
		9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11,
		11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12,
		12, 12, 12, 12, 12, 12, 12, 12, 12, 3, 13, 3, 25, 13, 13, 25,
		112, 212, 13, 229, 239, 13, 112, 13, 239, 113, 113, 113, 113, 113, 28, 64,
		28, 64, 72, 74, 28, 76, 28, 64, 28, 28, 72, 72, 83, 28, 64, 72,
		72, 28, 144, 64, 72, 72, 72, 72, 72, 72, 72, 72, 72, 64, 72, 72,
		72, 72, 72, 28, 28, 166, 170, 72, 72, 28, 183, 72, 189, 72, 28, 200,
		64, 28, 72, 216, 72, 72, 28, 223, 28, 72, 241, 28, 72, 28, 28, 28,
		72, 28, 28, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
		29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
		29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
		29, 29, 29, 29, 29, 29, 29, 30, 30, 30, 30, 30, 30, 30, 30, 30,
		30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 156, 30,
		30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
		30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 31, 31, 31, 31,
		31, 20, 228, 228, 67, 164, 164, 164, 165, 245, 32, 32, 32, 32, 32, 33,
		33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
		33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
		33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33,
		33, 33, 33, 34, 34, 34, 171, 177, 171, 34, 34, 35, 35, 35, 35, 35,
		36, 36, 36, 36, 36, 37, 37, 37, 197, 37, 37, 130, 131, 105, 105, 105,
		38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
		38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
		38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38, 38,
		38, 38, 38, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
		39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
		39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39,
		39, 39, 39, 39, 39, 39, 39, 39, 66, 40, 40, 40, 40, 40, 208, 208,
		21, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
		41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
		41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
		41, 41, 41, 41, 41, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
		42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
		42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
		42, 42, 42, 42, 42, 42, 42, 42, 42, 43, 43, 43, 43, 43, 89, 89,
		121, 89, 89, 89, 89, 89, 249, 89, 89, 90, 90, 90, 90, 90, 90, 90,
		90, 90, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 44, 44, 44, 44,
		44, 44, 44, 44, 44, 44, 44, 97, 44, 44, 44, 44, 44, 44, 44, 44,
		44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44,
		44, 97, 97, 97, 97, 97, 97, 97, 97, 97, 44, 44, 44, 44, 44, 44,
		44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 98, 98, 98, 98, 98, 98,
		98, 98, 98, 98, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 100, 100,
		100, 100, 100, 100, 100, 100, 100, 100, 45, 45, 45, 45, 45, 45, 45, 45,
		45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
		45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45,
		45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 45, 234, 46, 46, 46,
		46, 46, 46, 46, 46, 46, 46, 46, 93, 46, 46, 46, 46, 46, 46, 46,
		46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 181, 46,
		46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46,
		46, 46, 46, 138, 141, 191, 250, 47, 47, 69, 47, 47, 47, 47, 47, 47,
		47, 47, 47, 116, 47, 47, 124, 69, 132, 69, 47, 145, 47, 47, 47, 47,
		47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 116, 47, 47, 47,
		47, 47, 192, 47, 194, 47, 47, 47, 47, 47, 47, 47, 47, 69, 236, 47,
		47, 47, 47, 47, 47, 47, 47, 142, 175, 201, 205, 211, 218, 235, 237, 253,
		255, 261, 94, 117, 117, 117, 117, 117, 117, 117, 117, 117, 111, 127, 111, 111,
		111, 111, 139, 140, 111, 111, 199, 111, 139, 111, 111, 226, 111, 111, 111, 111,
		111, 111, 108, 108, 109, 106, 106, 70, 134, 134, 134, 225, 107, 107, 95, 135,
		135, 135, 101, 101, 101, 101, 101, 101, 101, 101, 101, 101, 102, 102, 102, 102,
		102, 102, 102, 102, 102, 102, 103, 103, 103, 103, 103, 103, 103, 103, 103, 103,
		104, 104, 104, 104, 104, 104, 104, 104, 104, 104, 136, 195, 136, 136, 254, 110,
		262, 4, 5, 24, 6, 6, 22, 7, 7, 118, 118, 167, 172, 172, 26, 68,
		14, 27, 63, 222, 248, 258, 91, 92, 133, 196, 198, 203, 238, 251, 252, 15,
		15, 62, 15, 62, 62, 15, 230, 62, 15, 62, 15, 62, 16, 16, 16, 16,
		16, 16, 16, 16, 16, 16, 16, 16, 16, 48, 48, 48, 48, 48, 128, 65,
		71, 78, 120, 146, 157, 168, 202, 224, 178, 125, 17, 17, 17, 17, 17, 17,
		17, 17, 17, 17, 17, 17, 17, 114, 256, 18, 18, 18, 18, 18, 18, 18,
		18, 18, 18, 18, 18, 18, 213, 214, 231, 19, 19, 19, 19, 19, 19, 19,
		19, 19, 19, 19, 19, 19, 49, 49, 49, 49, 49, 209, 209, 115, 232, 240,
		115, 260, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
		50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
		50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
		50, 50, 50, 50, 50, 50, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
		51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
		51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
		51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 84, 184, 137, 137, 215, 52,
		52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
		52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
		52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
		52, 52, 52, 53, 53, 53, 53, 53, 53, 53, 81, 82, 53, 53, 53, 53,
		53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
		53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
		53, 53, 53, 53, 53, 53, 53, 54, 54, 54, 54, 54, 54, 54, 54, 54,
		54, 54, 54, 147, 148, 149, 150, 151, 152, 153, 154, 155, 54, 54, 54, 54,
		54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
		54, 54, 54, 54, 54, 54, 54, 54, 54, 55, 55, 55, 55, 55, 55, 55,
		55, 55, 55, 55, 55, 158, 159, 55, 55, 55, 55, 55, 55, 55, 55, 55,
		55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
		55, 55, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 160, 56,
		56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
		56, 56, 56, 56, 56, 56, 56, 56, 56, 57, 57, 57, 57, 57, 57, 57,
		57, 57, 57, 57, 57, 161, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57,
		57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 58,
		58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
		58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
		58, 58, 58, 58, 59, 59, 73, 59, 59, 59, 59, 85, 59, 85, 123, 59,
		162, 59, 59, 85, 180, 59, 188, 190, 59, 59, 207, 85, 85, 220, 59, 59,
		233, 59, 243, 59, 59, 59, 85, 59, 59, 60, 60, 60, 60, 60, 60, 60,
		60, 163, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 61, 61,
		75, 77, 79, 80, 88, 143, 61, 182, 193, 206, 221, 227, 242, 244, 61, 246,
		61, 259, 86, 86, 86, 86, 86, 86, 263, 8, 23, 119, 174, 173, 204, 169,
		126, 179, 129, 87, 122, 176, 217, 219, 247, 187, 210, 257, 185
	};

	private static final short lapg_rlen[] = {
		0, 1, 1, 1, 2, 3, 2, 1, 1, 0, 1, 0, 1, 0, 1, 9,
		1, 6, 0, 1, 3, 1, 2, 3, 4, 2, 3, 2, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 3, 1, 0, 1, 0, 1, 4, 0, 1, 3,
		2, 2, 1, 3, 2, 0, 1, 3, 3, 7, 5, 1, 0, 1, 7, 11,
		1, 2, 2, 4, 3, 0, 1, 5, 9, 2, 2, 2, 3, 1, 1, 3,
		1, 1, 1, 1, 1, 4, 3, 6, 8, 10, 6, 8, 4, 1, 3, 3,
		0, 1, 5, 3, 5, 1, 1, 1, 1, 1, 1, 2, 2, 1, 3, 3,
		3, 3, 3, 3, 3, 3, 3, 1, 3, 3, 1, 3, 3, 1, 3, 1,
		3, 1, 5, 1, 3, 1, 3, 1, 3, 1
	};

	private static final short lapg_rlex[] = {
		108, 108, 64, 65, 65, 66, 66, 66, 66, 109, 109, 110, 110, 111, 111, 67,
		68, 69, 112, 112, 70, 71, 71, 71, 71, 72, 73, 74, 74, 75, 75, 76,
		76, 76, 76, 76, 76, 76, 77, 78, 113, 113, 114, 114, 78, 115, 115, 78,
		78, 79, 80, 80, 81, 116, 116, 82, 83, 84, 84, 84, 117, 117, 85, 85,
		86, 86, 86, 87, 88, 118, 118, 89, 89, 89, 89, 90, 91, 91, 92, 92,
		92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 93, 93,
		119, 119, 93, 94, 94, 95, 95, 95, 96, 96, 97, 97, 97, 98, 98, 98,
		98, 98, 98, 98, 98, 98, 98, 99, 99, 99, 100, 100, 100, 101, 101, 102,
		102, 103, 103, 104, 104, 105, 105, 106, 106, 107
	};

	private static final String[] lapg_syms = new String[] {
		"eoi",
		"any",
		"escdollar",
		"escid",
		"escint",
		"'${'",
		"'$/'",
		"identifier",
		"icon",
		"ccon",
		"Lcall",
		"Lcached",
		"Lcase",
		"Lend",
		"Lelse",
		"Leval",
		"Lfalse",
		"Lfor",
		"Lfile",
		"Lforeach",
		"Lgrep",
		"Lif",
		"Lin",
		"Limport",
		"Lis",
		"Lmap",
		"Lnew",
		"Lnull",
		"Lquery",
		"Lswitch",
		"Lseparator",
		"Ltemplate",
		"Ltrue",
		"Lself",
		"Lassert",
		"'}'",
		"'-}'",
		"'+'",
		"'-'",
		"'*'",
		"'/'",
		"'%'",
		"'!'",
		"'|'",
		"'['",
		"']'",
		"'('",
		"')'",
		"'.'",
		"','",
		"'&&'",
		"'||'",
		"'=='",
		"'='",
		"'!='",
		"'->'",
		"'=>'",
		"'<='",
		"'>='",
		"'<'",
		"'>'",
		"':'",
		"'?'",
		"_skip",
		"input",
		"definitions",
		"definition",
		"query_def",
		"cached_flag",
		"template_start",
		"parameters",
		"parameter_list",
		"context_type",
		"template_end",
		"instructions",
		"'[-]}'",
		"instruction",
		"simple_instruction",
		"sentence",
		"comma_expr",
		"qualified_id",
		"template_for_expr",
		"template_arguments",
		"control_instruction",
		"else_clause",
		"switch_instruction",
		"case_list",
		"one_case",
		"control_start",
		"control_sentence",
		"separator_expr",
		"control_end",
		"primary_expression",
		"complex_data",
		"map_entries",
		"map_separator",
		"bcon",
		"unary_expression",
		"binary_op",
		"instanceof_expression",
		"equality_expression",
		"conditional_and_expression",
		"conditional_or_expression",
		"conditional_expression",
		"assignment_expression",
		"expression",
		"expression_list",
		"body",
		"definitionsopt",
		"cached_flagopt",
		"parametersopt",
		"context_typeopt",
		"parameter_listopt",
		"template_argumentsopt",
		"template_for_expropt",
		"comma_expropt",
		"expression_listopt",
		"anyopt",
		"separator_expropt",
		"map_entriesopt",
	};

	public interface Tokens extends Lexems {
		// non-terminals
		public static final int input = 64;
		public static final int definitions = 65;
		public static final int definition = 66;
		public static final int query_def = 67;
		public static final int cached_flag = 68;
		public static final int template_start = 69;
		public static final int parameters = 70;
		public static final int parameter_list = 71;
		public static final int context_type = 72;
		public static final int template_end = 73;
		public static final int instructions = 74;
		public static final int LSQUAREMINUSRSQUARERCURLY = 75;
		public static final int instruction = 76;
		public static final int simple_instruction = 77;
		public static final int sentence = 78;
		public static final int comma_expr = 79;
		public static final int qualified_id = 80;
		public static final int template_for_expr = 81;
		public static final int template_arguments = 82;
		public static final int control_instruction = 83;
		public static final int else_clause = 84;
		public static final int switch_instruction = 85;
		public static final int case_list = 86;
		public static final int one_case = 87;
		public static final int control_start = 88;
		public static final int control_sentence = 89;
		public static final int separator_expr = 90;
		public static final int control_end = 91;
		public static final int primary_expression = 92;
		public static final int complex_data = 93;
		public static final int map_entries = 94;
		public static final int map_separator = 95;
		public static final int bcon = 96;
		public static final int unary_expression = 97;
		public static final int binary_op = 98;
		public static final int instanceof_expression = 99;
		public static final int equality_expression = 100;
		public static final int conditional_and_expression = 101;
		public static final int conditional_or_expression = 102;
		public static final int conditional_expression = 103;
		public static final int assignment_expression = 104;
		public static final int expression = 105;
		public static final int expression_list = 106;
		public static final int body = 107;
		public static final int definitionsopt = 108;
		public static final int cached_flagopt = 109;
		public static final int parametersopt = 110;
		public static final int context_typeopt = 111;
		public static final int parameter_listopt = 112;
		public static final int template_argumentsopt = 113;
		public static final int template_for_expropt = 114;
		public static final int comma_expropt = 115;
		public static final int expression_listopt = 116;
		public static final int anyopt = 117;
		public static final int separator_expropt = 118;
		public static final int map_entriesopt = 119;
	}

	private static int lapg_next(int state, int symbol) {
		int p;
		if (lapg_action[state] < -2) {
			for (p = -lapg_action[state] - 3; lapg_lalr[p] >= 0; p += 2) {
				if (lapg_lalr[p] == symbol) {
					break;
				}
			}
			return lapg_lalr[p + 1];
		}
		return lapg_action[state];
	}

	private static int lapg_state_sym(int state, int symbol) {
		int min = lapg_sym_goto[symbol], max = lapg_sym_goto[symbol + 1] - 1;
		int i, e;

		while (min <= max) {
			e = (min + max) >> 1;
			i = lapg_sym_from[e];
			if (i == state) {
				return lapg_sym_to[e];
			} else if (i < state) {
				min = e + 1;
			} else {
				max = e - 1;
			}
		}
		return -1;
	}

	private int lapg_head;
	private LapgSymbol[] lapg_m;
	private LapgSymbol lapg_n;

	private Object parse(TemplatesLexer lexer, int state) throws IOException, ParseException {

		lapg_m = new LapgSymbol[1024];
		lapg_head = 0;

		lapg_m[0] = new LapgSymbol();
		lapg_m[0].state = state;
		lapg_n = lexer.next();

		while (lapg_m[lapg_head].state != 264+state) {
			int lapg_i = lapg_next(lapg_m[lapg_head].state, lapg_n.lexem);

			if (lapg_i >= 0) {
				reduce(lapg_i);
			} else if (lapg_i == -1) {
				shift(lexer);
			}

			if (lapg_i == -2 || lapg_m[lapg_head].state == -1) {
				break;
			}
		}

		if (lapg_m[lapg_head].state != 264+state) {
			reporter.error(lapg_n.offset, lapg_n.endoffset, lexer.getTokenLine(), MessageFormat.format("syntax error before line {0}", lexer.getTokenLine()));
			throw new ParseException();
		}
		return lapg_m[lapg_head - 1].sym;
	}

	private void shift(TemplatesLexer lexer) throws IOException {
		lapg_m[++lapg_head] = lapg_n;
		lapg_m[lapg_head].state = lapg_state_sym(lapg_m[lapg_head - 1].state, lapg_n.lexem);
		if (DEBUG_SYNTAX) {
			System.out.println(MessageFormat.format("shift: {0} ({1})", lapg_syms[lapg_n.lexem], lexer.current()));
		}
		if (lapg_m[lapg_head].state != -1 && lapg_n.lexem != 0) {
			lapg_n = lexer.next();
		}
	}

	@SuppressWarnings("unchecked")
	private void reduce(int rule) {
		LapgSymbol lapg_gg = new LapgSymbol();
		lapg_gg.sym = (lapg_rlen[rule] != 0) ? lapg_m[lapg_head + 1 - lapg_rlen[rule]].sym : null;
		lapg_gg.lexem = lapg_rlex[rule];
		lapg_gg.state = 0;
		if (DEBUG_SYNTAX) {
			System.out.println("reduce to " + lapg_syms[lapg_rlex[rule]]);
		}
		LapgSymbol startsym = (lapg_rlen[rule] != 0) ? lapg_m[lapg_head + 1 - lapg_rlen[rule]] : lapg_n;
		lapg_gg.line = startsym.line;
		lapg_gg.offset = startsym.offset;
		lapg_gg.endoffset = (lapg_rlen[rule] != 0) ? lapg_m[lapg_head].endoffset : lapg_n.offset;
		switch (rule) {
			case 3:  // definitions ::= definition
				 lapg_gg.sym = new ArrayList(); if (((IBundleEntity)lapg_m[lapg_head-0].sym) != null) ((List<IBundleEntity>)lapg_gg.sym).add(((IBundleEntity)lapg_m[lapg_head-0].sym)); 
				break;
			case 4:  // definitions ::= definitions definition
				 if (((IBundleEntity)lapg_m[lapg_head-0].sym) != null) ((List<IBundleEntity>)lapg_gg.sym).add(((IBundleEntity)lapg_m[lapg_head-0].sym)); 
				break;
			case 5:  // definition ::= template_start instructions template_end
				 ((TemplateNode)lapg_m[lapg_head-2].sym).setInstructions(((ArrayList<Node>)lapg_m[lapg_head-1].sym)); 
				break;
			case 8:  // definition ::= any
				 lapg_gg.sym = null; 
				break;
			case 15:  // query_def ::= '${' cached_flagopt Lquery qualified_id parametersopt context_typeopt '=' expression '}'
				 lapg_gg.sym = new QueryNode(((String)lapg_m[lapg_head-5].sym), ((List<ParameterNode>)lapg_m[lapg_head-4].sym), ((String)lapg_m[lapg_head-3].sym), templatePackage, ((ExpressionNode)lapg_m[lapg_head-1].sym), ((Boolean)lapg_m[lapg_head-7].sym) != null, source, lapg_gg.offset, lapg_gg.endoffset); checkFqn(((String)lapg_m[lapg_head-5].sym), lapg_gg.offset, lapg_gg.endoffset, lapg_m[lapg_head-8].line); 
				break;
			case 16:  // cached_flag ::= Lcached
				 lapg_gg.sym = Boolean.TRUE; 
				break;
			case 17:  // template_start ::= '${' Ltemplate qualified_id parametersopt context_typeopt '[-]}'
				 lapg_gg.sym = new TemplateNode(((String)lapg_m[lapg_head-3].sym), ((List<ParameterNode>)lapg_m[lapg_head-2].sym), ((String)lapg_m[lapg_head-1].sym), templatePackage, source, lapg_gg.offset, lapg_gg.endoffset); checkFqn(((String)lapg_m[lapg_head-3].sym), lapg_gg.offset, lapg_gg.endoffset, lapg_m[lapg_head-5].line); 
				break;
			case 20:  // parameters ::= '(' parameter_listopt ')'
				 lapg_gg.sym = ((List<ParameterNode>)lapg_m[lapg_head-1].sym); 
				break;
			case 21:  // parameter_list ::= identifier
				 lapg_gg.sym = new ArrayList(); ((List<ParameterNode>)lapg_gg.sym).add(new ParameterNode(null, ((String)lapg_m[lapg_head-0].sym), source, lapg_m[lapg_head-0].offset, lapg_gg.endoffset)); 
				break;
			case 22:  // parameter_list ::= qualified_id identifier
				 lapg_gg.sym = new ArrayList(); ((List<ParameterNode>)lapg_gg.sym).add(new ParameterNode(((String)lapg_m[lapg_head-1].sym), ((String)lapg_m[lapg_head-0].sym), source, lapg_m[lapg_head-1].offset, lapg_gg.endoffset)); 
				break;
			case 23:  // parameter_list ::= parameter_list ',' identifier
				 ((List<ParameterNode>)lapg_gg.sym).add(new ParameterNode(null, ((String)lapg_m[lapg_head-0].sym), source, lapg_m[lapg_head-0].offset, lapg_gg.endoffset)); 
				break;
			case 24:  // parameter_list ::= parameter_list ',' qualified_id identifier
				 ((List<ParameterNode>)lapg_gg.sym).add(new ParameterNode(((String)lapg_m[lapg_head-1].sym), ((String)lapg_m[lapg_head-0].sym), source, lapg_m[lapg_head-1].offset, lapg_gg.endoffset)); 
				break;
			case 25:  // context_type ::= Lfor qualified_id
				 lapg_gg.sym = ((String)lapg_m[lapg_head-0].sym); 
				break;
			case 27:  // instructions ::= instructions instruction
				 ((ArrayList<Node>)lapg_gg.sym).add(((Node)lapg_m[lapg_head-0].sym)); 
				break;
			case 28:  // instructions ::= instruction
				 lapg_gg.sym = new ArrayList<Node>(); ((ArrayList<Node>)lapg_gg.sym).add(((Node)lapg_m[lapg_head-0].sym)); 
				break;
			case 29:  // '[-]}' ::= '-}'
				 skipSpaces(lapg_m[lapg_head-0].offset+1); 
				break;
			case 34:  // instruction ::= escid
				 lapg_gg.sym = createEscapedId(((String)lapg_m[lapg_head-0].sym), lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 35:  // instruction ::= escint
				 lapg_gg.sym = new IndexNode(null, new LiteralNode(((Integer)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 36:  // instruction ::= escdollar
				 lapg_gg.sym = new DollarNode(source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 37:  // instruction ::= any
				 lapg_gg.sym = new TextNode(source, rawText(lapg_gg.offset, lapg_gg.endoffset), lapg_gg.endoffset); 
				break;
			case 38:  // simple_instruction ::= '${' sentence '[-]}'
				 lapg_gg.sym = ((Node)lapg_m[lapg_head-1].sym); 
				break;
			case 44:  // sentence ::= Lcall qualified_id template_argumentsopt template_for_expropt
				 lapg_gg.sym = new CallTemplateNode(((String)lapg_m[lapg_head-2].sym), ((ArrayList)lapg_m[lapg_head-1].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), templatePackage, true, source, lapg_gg.offset,lapg_gg.endoffset); 
				break;
			case 47:  // sentence ::= Leval conditional_expression comma_expropt
				 lapg_gg.sym = new EvalNode(((ExpressionNode)lapg_m[lapg_head-1].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset,lapg_gg.endoffset); 
				break;
			case 48:  // sentence ::= Lassert expression
				 lapg_gg.sym = new AssertNode(((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset,lapg_gg.endoffset); 
				break;
			case 49:  // comma_expr ::= ',' conditional_expression
				 lapg_gg.sym = ((ExpressionNode)lapg_m[lapg_head-0].sym); 
				break;
			case 51:  // qualified_id ::= qualified_id '.' identifier
				 lapg_gg.sym = ((String)lapg_gg.sym) + "." + ((String)lapg_m[lapg_head-0].sym); 
				break;
			case 52:  // template_for_expr ::= Lfor expression
				 lapg_gg.sym = ((ExpressionNode)lapg_m[lapg_head-0].sym); 
				break;
			case 55:  // template_arguments ::= '(' expression_listopt ')'
				 lapg_gg.sym = ((ArrayList)lapg_m[lapg_head-1].sym); 
				break;
			case 56:  // control_instruction ::= control_start instructions else_clause
				 ((CompoundNode)lapg_gg.sym).setInstructions(((ArrayList<Node>)lapg_m[lapg_head-1].sym)); applyElse(((CompoundNode)lapg_m[lapg_head-2].sym),((ElseIfNode)lapg_m[lapg_head-0].sym), lapg_gg.offset, lapg_gg.endoffset, lapg_gg.line); 
				break;
			case 57:  // else_clause ::= '${' Lelse Lif expression '[-]}' instructions else_clause
				 lapg_gg.sym = new ElseIfNode(((ExpressionNode)lapg_m[lapg_head-3].sym), ((ArrayList<Node>)lapg_m[lapg_head-1].sym), ((ElseIfNode)lapg_m[lapg_head-0].sym), source, lapg_m[lapg_head-6].offset, lapg_m[lapg_head-1].endoffset); 
				break;
			case 58:  // else_clause ::= '${' Lelse '[-]}' instructions control_end
				 lapg_gg.sym = new ElseIfNode(null, ((ArrayList<Node>)lapg_m[lapg_head-1].sym), null, source, lapg_m[lapg_head-4].offset, lapg_m[lapg_head-1].endoffset); 
				break;
			case 59:  // else_clause ::= control_end
				 lapg_gg.sym = null; 
				break;
			case 62:  // switch_instruction ::= '${' Lswitch expression '[-]}' anyopt case_list control_end
				 lapg_gg.sym = new SwitchNode(((ExpressionNode)lapg_m[lapg_head-4].sym), ((ArrayList)lapg_m[lapg_head-1].sym), null, source, lapg_gg.offset,lapg_gg.endoffset); checkIsSpace(lapg_m[lapg_head-2].offset,lapg_m[lapg_head-2].endoffset, lapg_m[lapg_head-2].line); 
				break;
			case 63:  // switch_instruction ::= '${' Lswitch expression '[-]}' anyopt case_list '${' Lelse '[-]}' instructions control_end
				 lapg_gg.sym = new SwitchNode(((ExpressionNode)lapg_m[lapg_head-8].sym), ((ArrayList)lapg_m[lapg_head-5].sym), ((ArrayList<Node>)lapg_m[lapg_head-1].sym), source, lapg_gg.offset,lapg_gg.endoffset); checkIsSpace(lapg_m[lapg_head-6].offset,lapg_m[lapg_head-6].endoffset, lapg_m[lapg_head-6].line); 
				break;
			case 64:  // case_list ::= one_case
				 lapg_gg.sym = new ArrayList(); ((ArrayList)lapg_gg.sym).add(((CaseNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 65:  // case_list ::= case_list one_case
				 ((ArrayList)lapg_gg.sym).add(((CaseNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 66:  // case_list ::= case_list instruction
				 CaseNode.add(((ArrayList)lapg_gg.sym), ((Node)lapg_m[lapg_head-0].sym)); 
				break;
			case 67:  // one_case ::= '${' Lcase expression '[-]}'
				 lapg_gg.sym = new CaseNode(((ExpressionNode)lapg_m[lapg_head-1].sym), source, lapg_gg.offset,lapg_gg.endoffset); 
				break;
			case 68:  // control_start ::= '${' control_sentence '[-]}'
				 lapg_gg.sym = ((CompoundNode)lapg_m[lapg_head-1].sym); 
				break;
			case 71:  // control_sentence ::= Lforeach identifier Lin expression separator_expropt
				 lapg_gg.sym = new ForeachNode(((String)lapg_m[lapg_head-3].sym), ((ExpressionNode)lapg_m[lapg_head-1].sym), null, ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 72:  // control_sentence ::= Lfor identifier Lin '[' conditional_expression ',' conditional_expression ']' separator_expropt
				 lapg_gg.sym = new ForeachNode(((String)lapg_m[lapg_head-7].sym), ((ExpressionNode)lapg_m[lapg_head-4].sym), ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 73:  // control_sentence ::= Lif expression
				 lapg_gg.sym = new IfNode(((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 74:  // control_sentence ::= Lfile expression
				 lapg_gg.sym = new FileNode(((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 75:  // separator_expr ::= Lseparator expression
				 lapg_gg.sym = ((ExpressionNode)lapg_m[lapg_head-0].sym); 
				break;
			case 78:  // primary_expression ::= identifier
				 lapg_gg.sym = new SelectNode(null, ((String)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 79:  // primary_expression ::= '(' expression ')'
				 lapg_gg.sym = new ParenthesesNode(((ExpressionNode)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 80:  // primary_expression ::= icon
				 lapg_gg.sym = new LiteralNode(((Integer)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 81:  // primary_expression ::= bcon
				 lapg_gg.sym = new LiteralNode(((Boolean)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 82:  // primary_expression ::= ccon
				 lapg_gg.sym = new LiteralNode(((String)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 83:  // primary_expression ::= Lself
				 lapg_gg.sym = new ThisNode(source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 84:  // primary_expression ::= Lnull
				 lapg_gg.sym = new LiteralNode(null, source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 85:  // primary_expression ::= identifier '(' expression_listopt ')'
				 lapg_gg.sym = new MethodCallNode(null, ((String)lapg_m[lapg_head-3].sym), ((ArrayList)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 86:  // primary_expression ::= primary_expression '.' identifier
				 lapg_gg.sym = new SelectNode(((ExpressionNode)lapg_m[lapg_head-2].sym), ((String)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 87:  // primary_expression ::= primary_expression '.' identifier '(' expression_listopt ')'
				 lapg_gg.sym = new MethodCallNode(((ExpressionNode)lapg_m[lapg_head-5].sym), ((String)lapg_m[lapg_head-3].sym), ((ArrayList)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 88:  // primary_expression ::= primary_expression '.' identifier '(' identifier '|' expression ')'
				 lapg_gg.sym = createCollectionProcessor(((ExpressionNode)lapg_m[lapg_head-7].sym), ((String)lapg_m[lapg_head-5].sym), ((String)lapg_m[lapg_head-3].sym), ((ExpressionNode)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset, lapg_gg.line); 
				break;
			case 89:  // primary_expression ::= primary_expression '.' identifier '(' identifier '|' expression ':' expression ')'
				 lapg_gg.sym = createMapCollect(((ExpressionNode)lapg_m[lapg_head-9].sym), ((String)lapg_m[lapg_head-7].sym), ((String)lapg_m[lapg_head-5].sym), ((ExpressionNode)lapg_m[lapg_head-3].sym), ((ExpressionNode)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset, lapg_gg.line); 
				break;
			case 90:  // primary_expression ::= primary_expression '->' qualified_id '(' expression_listopt ')'
				 lapg_gg.sym = new CallTemplateNode(((String)lapg_m[lapg_head-3].sym), ((ArrayList)lapg_m[lapg_head-1].sym), ((ExpressionNode)lapg_m[lapg_head-5].sym), templatePackage, false, source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 91:  // primary_expression ::= primary_expression '->' '(' expression ')' '(' expression_listopt ')'
				 lapg_gg.sym = new CallTemplateNode(((ExpressionNode)lapg_m[lapg_head-4].sym),((ArrayList)lapg_m[lapg_head-1].sym),((ExpressionNode)lapg_m[lapg_head-7].sym),templatePackage, source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 92:  // primary_expression ::= primary_expression '[' expression ']'
				 lapg_gg.sym = new IndexNode(((ExpressionNode)lapg_m[lapg_head-3].sym), ((ExpressionNode)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 94:  // complex_data ::= '[' expression_listopt ']'
				 lapg_gg.sym = new ListNode(((ArrayList)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 95:  // complex_data ::= '[' map_entries ']'
				 lapg_gg.sym = new ConcreteMapNode(((Map<String,ExpressionNode>)lapg_m[lapg_head-1].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 98:  // complex_data ::= Lnew qualified_id '(' map_entriesopt ')'
				 lapg_gg.sym = null; /* todo */ 
				break;
			case 99:  // map_entries ::= identifier map_separator conditional_expression
				 lapg_gg.sym = new HashMap(); ((Map<String,ExpressionNode>)lapg_gg.sym).put(((String)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 100:  // map_entries ::= map_entries ',' identifier map_separator conditional_expression
				 ((Map<String,ExpressionNode>)lapg_gg.sym).put(((String)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 104:  // bcon ::= Ltrue
				 lapg_gg.sym = Boolean.TRUE; 
				break;
			case 105:  // bcon ::= Lfalse
				 lapg_gg.sym = Boolean.FALSE; 
				break;
			case 107:  // unary_expression ::= '!' unary_expression
				 lapg_gg.sym = new UnaryExpression(UnaryExpression.NOT, ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 108:  // unary_expression ::= '-' unary_expression
				 lapg_gg.sym = new UnaryExpression(UnaryExpression.MINUS, ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 110:  // binary_op ::= binary_op '*' binary_op
				 lapg_gg.sym = new ArithmeticNode(ArithmeticNode.MULT, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 111:  // binary_op ::= binary_op '/' binary_op
				 lapg_gg.sym = new ArithmeticNode(ArithmeticNode.DIV, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 112:  // binary_op ::= binary_op '%' binary_op
				 lapg_gg.sym = new ArithmeticNode(ArithmeticNode.REM, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 113:  // binary_op ::= binary_op '+' binary_op
				 lapg_gg.sym = new ArithmeticNode(ArithmeticNode.PLUS, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 114:  // binary_op ::= binary_op '-' binary_op
				 lapg_gg.sym = new ArithmeticNode(ArithmeticNode.MINUS, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 115:  // binary_op ::= binary_op '<' binary_op
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.LT, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 116:  // binary_op ::= binary_op '>' binary_op
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.GT, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 117:  // binary_op ::= binary_op '<=' binary_op
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.LE, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 118:  // binary_op ::= binary_op '>=' binary_op
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.GE, ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 120:  // instanceof_expression ::= instanceof_expression Lis qualified_id
				 lapg_gg.sym = new InstanceOfNode(((ExpressionNode)lapg_gg.sym), ((String)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 121:  // instanceof_expression ::= instanceof_expression Lis ccon
				 lapg_gg.sym = new InstanceOfNode(((ExpressionNode)lapg_gg.sym), ((String)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 123:  // equality_expression ::= equality_expression '==' instanceof_expression
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.EQ, ((ExpressionNode)lapg_gg.sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 124:  // equality_expression ::= equality_expression '!=' instanceof_expression
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.NE, ((ExpressionNode)lapg_gg.sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 126:  // conditional_and_expression ::= conditional_and_expression '&&' equality_expression
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.AND, ((ExpressionNode)lapg_gg.sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 128:  // conditional_or_expression ::= conditional_or_expression '||' conditional_and_expression
				 lapg_gg.sym = new ConditionalNode(ConditionalNode.OR, ((ExpressionNode)lapg_gg.sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 130:  // conditional_expression ::= conditional_or_expression '?' conditional_expression ':' conditional_expression
				 lapg_gg.sym = new TriplexNode(((ExpressionNode)lapg_m[lapg_head-4].sym), ((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 132:  // assignment_expression ::= identifier '=' conditional_expression
				 lapg_gg.sym = new AssignNode(((String)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 134:  // expression ::= expression ',' assignment_expression
				 lapg_gg.sym = new CommaNode(((ExpressionNode)lapg_m[lapg_head-2].sym), ((ExpressionNode)lapg_m[lapg_head-0].sym), source, lapg_gg.offset, lapg_gg.endoffset); 
				break;
			case 135:  // expression_list ::= conditional_expression
				 lapg_gg.sym = new ArrayList(); ((ArrayList)lapg_gg.sym).add(((ExpressionNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 136:  // expression_list ::= expression_list ',' conditional_expression
				 ((ArrayList)lapg_gg.sym).add(((ExpressionNode)lapg_m[lapg_head-0].sym)); 
				break;
			case 137:  // body ::= instructions
				
							lapg_gg.sym = new TemplateNode("inline", null, null, templatePackage, source, lapg_gg.offset, lapg_gg.endoffset);
							((TemplateNode)lapg_gg.sym).setInstructions(((ArrayList<Node>)lapg_m[lapg_head-0].sym));
						
				break;
		}
		for (int e = lapg_rlen[rule]; e > 0; e--) {
			lapg_m[lapg_head--] = null;
		}
		lapg_m[++lapg_head] = lapg_gg;
		lapg_m[lapg_head].state = lapg_state_sym(lapg_m[lapg_head-1].state, lapg_gg.lexem);
	}

	public List<IBundleEntity> parseInput(TemplatesLexer lexer) throws IOException, ParseException {
		return (List<IBundleEntity>) parse(lexer, 0);
	}

	public TemplateNode parseBody(TemplatesLexer lexer) throws IOException, ParseException {
		return (TemplateNode) parse(lexer, 1);
	}
}