package kut.compiler.parser.ast;

import java.io.IOException;

import kut.compiler.compiler.CodeGenerator;
import kut.compiler.compiler.CondLabels;
import kut.compiler.exception.CompileErrorException;
import kut.compiler.lexer.Token;
import kut.compiler.lexer.TokenClass;
import kut.compiler.symboltable.ExprType;

public class AstUnaryOp extends AstNode 
{
	/**
	 * 
	 */
	protected Token t;
	
	protected AstNode	expr;
	
	protected ExprType 	etype;
	
	/**
	 * @param t
	 */
	public AstUnaryOp(AstNode expr, Token t)
	{
		this.expr = expr;
		this.t = t;
	}
	
	/**
	 * @param gen
	 */
	public void preprocessStringLiterals(CodeGenerator gen) {
		expr.preprocessStringLiterals(gen);		
		return;
	}
	
	/**
	 *
	 */
	public void printTree(int indent) {
		this.println(indent, "unaryop" + t);
		expr.printTree(indent + 1);
	}

	
	/**
	 *
	 */
	public ExprType checkTypes(CodeGenerator gen) throws CompileErrorException
	{		
		etype = expr.checkTypes(gen);
		if (t.getC() == '-') {
			if (etype != ExprType.DOUBLE && etype != ExprType.INT) {
				throw new CompileErrorException("the '-' operator can be applied only to an integer value or a double value." + t);
			}
			return etype;
		}
		
		if (t.getC() == '!'){
			if (etype != ExprType.BOOLEAN) {
				throw new CompileErrorException("the '!' operator can be applied only to a boolean value." + t);
			}
			return etype;
		}
		
		if (t.getC() == TokenClass.PLUS_PLUS || t.getC() == TokenClass.MINUS_MINUS) {
			if (!(expr instanceof AstIdentifier)) {
				throw new CompileErrorException("the prefix '++' and '--' operators can be applied only to identifiers. :" + t);
			}

			if (etype != ExprType.INT && etype != ExprType.DOUBLE) {
				throw new CompileErrorException("the prefix '++' and '--' operators can be applied only to an integer or double value." + t);
			}
			return etype;
		}
		throw new CompileErrorException("the code shouldn't reach here. a bug?.: " + t);
	}
	
	/**
	 *
	 */
	@Override
	public void cgen(CodeGenerator gen) throws IOException, CompileErrorException
	{	
		if (t.getC() == '-') {
			this.opUnaryMinus(gen);
		}
		else if (t.getC() == '!') {
			this.opNegation(gen);
		}
		else if (t.getC() == TokenClass.PLUS_PLUS){
			this.opPrefixInc(gen);
		}
		else if (t.getC() == TokenClass.MINUS_MINUS) {
			this.opPrefixDec(gen);
		}
		
		return;
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opPrefixInc(CodeGenerator gen)  throws IOException, CompileErrorException
	{
		expr.cgen(gen);

		String varname = ((AstIdentifier)expr).getIdentifier();
		if (etype == ExprType.INT) {
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("add rax, 1");
			gen.printCode("mov [rbp + " + idx + "], rax");
			return;
		}
		else if (etype == ExprType.DOUBLE) {
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("movq xmm0, rax");
			gen.printCode("mov rax, 1");
			gen.printCode("cvtsi2sd xmm1, rax");
			gen.printCode("addsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			gen.printCode("mov [rbp + " + idx + "], rax");
			return;
		}
		throw new CompileErrorException("the code should't reach here. a bug.");		
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opPrefixDec(CodeGenerator gen)  throws IOException, CompileErrorException
	{
		expr.cgen(gen);
		String varname = ((AstIdentifier)expr).getIdentifier();

		if (etype == ExprType.INT) {
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("sub rax, 1");
			gen.printCode("mov [rbp + " + idx + "], rax");
			return;
		}
		else if (etype == ExprType.DOUBLE) {
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("movq xmm0, rax");
			gen.printCode("mov rax, 1");
			gen.printCode("cvtsi2sd xmm1, rax");
			gen.printCode("subsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			gen.printCode("mov [rbp + " + idx + "], rax");
			return;
		}
		throw new CompileErrorException("the code should't reach here. a bug.");	
	}
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opNegation(CodeGenerator gen) throws IOException, CompileErrorException
	{
		expr.cgen(gen);
		if (etype == ExprType.BOOLEAN) {
			CondLabels lbls = gen.generateCondLabels();
			gen.printCode	("cmp rax, 0");
			gen.printCode	("je " + lbls.labelTrue);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + lbls.labelCondEnd);
			gen.printLabel	(lbls.labelTrue);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(lbls.labelCondEnd);
		}
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opUnaryMinus(CodeGenerator gen) throws IOException, CompileErrorException
	{
		expr.cgen(gen);
		if (etype == ExprType.INT) {
			gen.printCode("xor rax, 0xFFFFFFFFFFFFFFFF");
			gen.printCode("add rax, 1");
		}
		else if (etype == ExprType.DOUBLE) {
			gen.printCode("movq xmm0, rax");
			gen.printCode("mov rax, -1");
			gen.printCode("cvtsi2sd xmm1, rax");
			gen.printCode("mulsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
		}
		return;
	}


	/**
	 *
	 */
	public void preprocessLocalVariables(CodeGenerator gen) throws CompileErrorException
	{
		this.expr.preprocessLocalVariables(gen);
	}


}
