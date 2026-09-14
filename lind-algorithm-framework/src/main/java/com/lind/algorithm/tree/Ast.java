package com.lind.algorithm.tree;

import java.util.List;
import java.util.Objects;

/**
 * 抽象语法树（AST）：去掉括号等表层语法噪音后的表达式语义树，可直接求值。
 * <p>
 * 内置极简递归下降解析器，支持 {@code + - * /}、括号与浮点数。
 * </p>
 */
public final class Ast {

	private Ast() {
	}

	public static AstNode parse(String expression) {
		Parser parser = new Parser(expression);
		AstNode root = parser.parseExpression();
		parser.expectEnd();
		return root;
	}

	/**
	 * AST 节点。
	 */
	public abstract static class AstNode {

		public abstract double evaluate();

		public abstract String toInfix();

		public List<AstNode> children() {
			return List.of();
		}

	}

	public static final class NumberNode extends AstNode {

		private final double value;

		public NumberNode(double value) {
			this.value = value;
		}

		public double value() {
			return value;
		}

		@Override
		public double evaluate() {
			return value;
		}

		@Override
		public String toInfix() {
			if (value == Math.rint(value)) {
				return String.valueOf((long) value);
			}
			return String.valueOf(value);
		}

	}

	public static final class UnaryMinusNode extends AstNode {

		private final AstNode operand;

		public UnaryMinusNode(AstNode operand) {
			this.operand = Objects.requireNonNull(operand, "operand");
		}

		public AstNode operand() {
			return operand;
		}

		@Override
		public double evaluate() {
			return -operand.evaluate();
		}

		@Override
		public String toInfix() {
			return "-(" + operand.toInfix() + ")";
		}

		@Override
		public List<AstNode> children() {
			return List.of(operand);
		}

	}

	public static final class BinaryOpNode extends AstNode {

		public enum Op {

			ADD("+"), SUB("-"), MUL("*"), DIV("/");

			private final String symbol;

			Op(String symbol) {
				this.symbol = symbol;
			}

			public String symbol() {
				return symbol;
			}

		}

		private final Op op;

		private final AstNode left;

		private final AstNode right;

		public BinaryOpNode(Op op, AstNode left, AstNode right) {
			this.op = Objects.requireNonNull(op, "op");
			this.left = Objects.requireNonNull(left, "left");
			this.right = Objects.requireNonNull(right, "right");
		}

		public Op op() {
			return op;
		}

		public AstNode left() {
			return left;
		}

		public AstNode right() {
			return right;
		}

		@Override
		public double evaluate() {
			double l = left.evaluate();
			double r = right.evaluate();
			return switch (op) {
				case ADD -> l + r;
				case SUB -> l - r;
				case MUL -> l * r;
				case DIV -> l / r;
			};
		}

		@Override
		public String toInfix() {
			return "(" + left.toInfix() + " " + op.symbol() + " " + right.toInfix() + ")";
		}

		@Override
		public List<AstNode> children() {
			return List.of(left, right);
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

		private AstNode parseExpression() {
			AstNode node = parseTerm();
			while (true) {
				skipWs();
				if (match('+')) {
					node = new BinaryOpNode(BinaryOpNode.Op.ADD, node, parseTerm());
				}
				else if (match('-')) {
					node = new BinaryOpNode(BinaryOpNode.Op.SUB, node, parseTerm());
				}
				else {
					return node;
				}
			}
		}

		private AstNode parseTerm() {
			AstNode node = parseFactor();
			while (true) {
				skipWs();
				if (match('*')) {
					node = new BinaryOpNode(BinaryOpNode.Op.MUL, node, parseFactor());
				}
				else if (match('/')) {
					node = new BinaryOpNode(BinaryOpNode.Op.DIV, node, parseFactor());
				}
				else {
					return node;
				}
			}
		}

		private AstNode parseFactor() {
			skipWs();
			if (match('+')) {
				return parseFactor();
			}
			if (match('-')) {
				return new UnaryMinusNode(parseFactor());
			}
			if (match('(')) {
				AstNode inner = parseExpression();
				skipWs();
				if (!match(')')) {
					throw new IllegalArgumentException("missing ')' at position " + pos);
				}
				return inner;
			}
			return parseNumber();
		}

		private AstNode parseNumber() {
			skipWs();
			int start = pos;
			while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
				pos++;
			}
			if (start == pos) {
				throw new IllegalArgumentException("expected number at position " + pos);
			}
			return new NumberNode(Double.parseDouble(src.substring(start, pos)));
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
