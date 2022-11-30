package kut.compiler.parser.ast;

import java.io.IOException;

import kut.compiler.compiler.CodeGenerator;
import kut.compiler.exception.CompileErrorException;
import kut.compiler.lexer.Token;
import kut.compiler.lexer.TokenClass;
import kut.compiler.symboltable.ExprType;

public class AstPostfixOp extends AstNode 
{
	/**
	 * 
	 */
	protected Token t;
	
	protected AstNode id;
	
	protected ExprType 	etype;
	
	/**
	 * @param t
	 */
	public AstPostfixOp(AstNode id, Token t)
	{
		this.id = id;
		this.t = t;
	}
	
	/**
	 * @param gen
	 */
	public void preprocessStringLiterals(CodeGenerator gen) {
		id.preprocessStringLiterals(gen);		
		return;
	}
	
	/**
	 *
	 */
	public void printTree(int indent) {
		this.println(indent, "postfix" + t);
		id.printTree(indent + 1);
	}

	
	/**
	 *
	 */
	public ExprType checkTypes(CodeGenerator gen) throws CompileErrorException
	{		
		if (!(id instanceof AstIdentifier)) {
			throw new CompileErrorException("postfix operators can be applied only to identifiers. :" + t);
		}
		
		etype = id.checkTypes(gen);
		if (etype != ExprType.INT && etype != ExprType.DOUBLE) {
			throw new CompileErrorException("postfix operators can be applied only to integer or float values. :" + t);
		}
		
		return etype;
	}
	
	/**
	 *
	 */
	@Override
	public void cgen(CodeGenerator gen) throws IOException, CompileErrorException
	{	
		if (t.getC() == TokenClass.PLUS_PLUS) {
			this.opInc(gen);
		}
		else if (t.getC() == TokenClass.MINUS_MINUS) {
			this.opDec(gen);
		}
		return;
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opInc(CodeGenerator gen) throws IOException, CompileErrorException
	{
		id.cgen(gen);

		String varname = ((AstIdentifier)id).getIdentifier();
		if (etype == ExprType.INT) {
			gen.printCode("push rax");
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("add rax, 1");
			gen.printCode("mov [rbp + " + idx + "], rax");
			gen.printCode("pop rax");
			return;
		}
		else if (etype == ExprType.DOUBLE) {
			gen.printCode("push rax");
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("movq xmm0, rax");
			gen.printCode("mov rax, 1");
			gen.printCode("cvtsi2sd xmm1, rax");
			gen.printCode("addsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			gen.printCode("mov [rbp + " + idx + "], rax");
			gen.printCode("pop rax");
			return;
		}
		throw new CompileErrorException("the code should't reach here. a bug.");
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opDec(CodeGenerator gen) throws IOException, CompileErrorException
	{
		id.cgen(gen);
		String varname = ((AstIdentifier)id).getIdentifier();

		if (etype == ExprType.INT) {
			gen.printCode("push rax");
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("sub rax, 1");
			gen.printCode("mov [rbp + " + idx + "], rax");
			gen.printCode("pop rax");
			return;
		}
		else if (etype == ExprType.DOUBLE) {
			gen.printCode("push rax");
			int idx = gen.getStackIndexOfLocalVariable(varname);
			gen.printCode("movq xmm0, rax");
			gen.printCode("mov rax, 1");
			gen.printCode("cvtsi2sd xmm1, rax");
			gen.printCode("subsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			gen.printCode("mov [rbp + " + idx + "], rax");
			gen.printCode("pop rax");
			return;
		}
		throw new CompileErrorException("the code should't reach here. a bug.");	
	}


	/**
	 *
	 */
	public void preprocessLocalVariables(CodeGenerator gen) throws CompileErrorException
	{
		id.preprocessLocalVariables(gen);
	}


}
