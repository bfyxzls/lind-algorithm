package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link Cst} 单元测试。
 */
public class CstTest {

	@Test
	void keepsParenthesesInLexicalTokens() {
		Cst.CstNode cst = Cst.parse("(1+2)*3");
		List<String> tokens = cst.lexicalTokens();
		assertTrue(tokens.contains("("));
		assertTrue(tokens.contains(")"));
		assertEquals(List.of("(", "1", "+", "2", ")", "*", "3"), tokens);
	}

	@Test
	void projectsToAstWithSameValue() {
		String expr = "(1+2)*3-4/2"; // 9 - 2 = 7
		Cst.CstNode cst = Cst.parse(expr);
		Ast.AstNode ast = Cst.toAst(cst);
		assertEquals(Ast.parse(expr).evaluate(), ast.evaluate());
		assertEquals(7.0, ast.evaluate());
	}

	@Test
	void groupNodePresent() {
		Cst.CstNode cst = Cst.parse("(1)");
		Cst.CstNode expr = cst;
		assertEquals(Cst.Kind.EXPR, expr.kind());
		// EXPR -> TERM -> FACTOR path ends with GROUP
		boolean hasGroup = containsKind(cst, Cst.Kind.GROUP);
		assertTrue(hasGroup);
	}

	private static boolean containsKind(Cst.CstNode node, Cst.Kind kind) {
		if (node.kind() == kind) {
			return true;
		}
		for (Cst.CstNode child : node.children()) {
			if (containsKind(child, kind)) {
				return true;
			}
		}
		return false;
	}

}
