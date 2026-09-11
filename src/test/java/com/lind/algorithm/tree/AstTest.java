package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link Ast} 单元测试。
 */
public class AstTest {

	@Test
	void evaluatePrecedenceAndParens() {
		assertEquals(7.0, Ast.parse("1+2*3").evaluate());
		assertEquals(9.0, Ast.parse("(1+2)*3").evaluate());
		assertEquals(-5.0, Ast.parse("-(2+3)").evaluate());
		assertEquals(1.5, Ast.parse("3/2").evaluate());
	}

	@Test
	void structureIsAbstract() {
		Ast.AstNode root = Ast.parse("(1+2)*3");
		assertInstanceOf(Ast.BinaryOpNode.class, root);
		Ast.BinaryOpNode mul = (Ast.BinaryOpNode) root;
		assertEquals(Ast.BinaryOpNode.Op.MUL, mul.op());
		assertInstanceOf(Ast.BinaryOpNode.class, mul.left());
	}

	@Test
	void rejectInvalid() {
		assertThrows(IllegalArgumentException.class, () -> Ast.parse(""));
		assertThrows(IllegalArgumentException.class, () -> Ast.parse("1+"));
		assertThrows(IllegalArgumentException.class, () -> Ast.parse("1+(2"));
	}

}
