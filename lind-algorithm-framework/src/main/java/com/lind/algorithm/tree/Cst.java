package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 具体语法树（CST / Parse Tree）：保留括号、运算符等词法单元，贴近文法推导过程。
 * <p>
 * 与 {@link Ast} 对比：CST 强调「源码长什么样」，AST 强调「语义是什么」。本实现用极简表达式文法生成 CST， 并可投影为 AST。
 * </p>
 */
public final class Cst {

	private Cst() {
	}

	public static CstNode parse(String expression) {
		Parser parser = new Parser(expression);
		CstNode root = parser.parseExpression();
		parser.expectEnd();
		return root;
	}

	/**
	 * 将 CST 投影为 AST（丢弃括号等纯语法节点）。
	 */
	public static Ast.AstNode toAst(CstNode node) {
		Objects.requireNonNull(node, "node");
		return switch (node.kind()) {
			case NUMBER -> new Ast.NumberNode(Double.parseDouble(node.lexeme()));
			case UNARY -> new Ast.UnaryMinusNode(toAst(node.child(0)));
			case BINARY -> {
				Ast.BinaryOpNode.Op op = switch (node.lexeme()) {
					case "+" -> Ast.BinaryOpNode.Op.ADD;
					case "-" -> Ast.BinaryOpNode.Op.SUB;
					case "*" -> Ast.BinaryOpNode.Op.MUL;
					case "/" -> Ast.BinaryOpNode.Op.DIV;
					default -> throw new IllegalArgumentException("unknown op: " + node.lexeme());
				};
				yield new Ast.BinaryOpNode(op, toAst(node.child(0)), toAst(node.child(1)));
			}
			case GROUP -> toAst(node.child(0));
			case EXPR, TERM, FACTOR -> {
				if (node.childCount() == 1) {
					yield toAst(node.child(0));
				}
				throw new IllegalArgumentException("ambiguous nonterminal node: " + node);
			}
		};
	}

	public enum Kind {

		/** 非终结符：表达式 */
		EXPR,
		/** 非终结符：项 */
		TERM,
		/** 非终结符：因子 */
		FACTOR,
		/** 数字词法 */
		NUMBER,
		/** 一元负号 */
		UNARY,
		/** 二元运算，lexeme 为运算符 */
		BINARY,
		/** 括号分组，子节点为内部表达式；自身保留 '(' ')' 信息 */
		GROUP

	}

	/**
	 * CST 节点。
	 */
	public static final class CstNode {

		private final Kind kind;

		private final String lexeme;

		private final int start;

		private final int end;

		private final List<CstNode> children;

		public CstNode(Kind kind, String lexeme, int start, int end, List<CstNode> children) {
			this.kind = Objects.requireNonNull(kind, "kind");
			this.lexeme = lexeme;
			this.start = start;
			this.end = end;
			this.children = List.copyOf(children == null ? List.of() : children);
		}

		public Kind kind() {
			return kind;
		}

		public String lexeme() {
			return lexeme;
		}

		public int start() {
			return start;
		}

		public int end() {
			return end;
		}

		public List<CstNode> children() {
			return children;
		}

		public int childCount() {
			return children.size();
		}

		public CstNode child(int index) {
			return children.get(index);
		}

		/**
		 * 收集所有叶子词法片段（含括号），便于对照源码。
		 */
		public List<String> lexicalTokens() {
			List<String> tokens = new ArrayList<>();
			collectTokens(this, tokens);
			return Collections.unmodifiableList(tokens);
		}

		private static void collectTokens(CstNode node, List<String> out) {
			if (node.kind == Kind.GROUP) {
				out.add("(");
				for (CstNode c : node.children) {
					collectTokens(c, out);
				}
				out.add(")");
				return;
			}
			if (node.kind == Kind.NUMBER || node.kind == Kind.BINARY || node.kind == Kind.UNARY) {
				if (node.kind == Kind.UNARY) {
					out.add("-");
					for (CstNode c : node.children) {
						collectTokens(c, out);
					}
					return;
				}
				if (node.kind == Kind.BINARY) {
					collectTokens(node.child(0), out);
					out.add(node.lexeme);
					collectTokens(node.child(1), out);
					return;
				}
				out.add(node.lexeme);
				return;
			}
			for (CstNode c : node.children) {
				collectTokens(c, out);
			}
		}

		@Override
		public String toString() {
			return kind + (lexeme == null ? "" : "(" + lexeme + ")") + children;
		}

	}

	private static final class Parser {

		private final String src;

		private int pos;

		private Parser(String expression) {
			this.src = Objects.requireNonNull(expression, "expression").trim();
			if (this.src.isEmpty()) {
				throw new IllegalArgumentException("expression must not be blank");
			}
		}

		private CstNode parseExpression() {
			int start = pos;
			CstNode node = parseTerm();
			while (true) {
				skipWs();
				int opPos = pos;
				if (match('+') || match('-')) {
					String op = src.substring(opPos, opPos + 1);
					CstNode right = parseTerm();
					node = new CstNode(Kind.BINARY, op, start, pos, List.of(node, right));
				}
				else {
					return new CstNode(Kind.EXPR, null, start, pos, List.of(node));
				}
			}
		}

		private CstNode parseTerm() {
			int start = pos;
			CstNode node = parseFactor();
			while (true) {
				skipWs();
				int opPos = pos;
				if (match('*') || match('/')) {
					String op = src.substring(opPos, opPos + 1);
					CstNode right = parseFactor();
					node = new CstNode(Kind.BINARY, op, start, pos, List.of(node, right));
				}
				else {
					return new CstNode(Kind.TERM, null, start, pos, List.of(node));
				}
			}
		}

		private CstNode parseFactor() {
			skipWs();
			int start = pos;
			if (match('+')) {
				return parseFactor();
			}
			if (match('-')) {
				CstNode operand = parseFactor();
				return new CstNode(Kind.UNARY, "-", start, pos, List.of(operand));
			}
			if (match('(')) {
				CstNode inner = parseExpression();
				skipWs();
				if (!match(')')) {
					throw new IllegalArgumentException("missing ')' at position " + pos);
				}
				return new CstNode(Kind.GROUP, "()", start, pos, List.of(inner));
			}
			return parseNumber();
		}

		private CstNode parseNumber() {
			skipWs();
			int start = pos;
			while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
				pos++;
			}
			if (start == pos) {
				throw new IllegalArgumentException("expected number at position " + pos);
			}
			String lexeme = src.substring(start, pos);
			return new CstNode(Kind.NUMBER, lexeme, start, pos, List.of());
		}

		private void expectEnd() {
			skipWs();
			if (pos != src.length()) {
				throw new IllegalArgumentException("unexpected input at " + pos + ": " + src.substring(pos));
			}
		}

		private void skipWs() {
			while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
				pos++;
			}
		}

		private boolean match(char c) {
			if (pos < src.length() && src.charAt(pos) == c) {
				pos++;
				return true;
			}
			return false;
		}

	}

}
