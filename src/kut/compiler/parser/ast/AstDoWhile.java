package kut.compiler.parser.ast;

import java.io.IOException;

import kut.compiler.compiler.CodeGenerator;
import kut.compiler.compiler.DoWhileLabels;
import kut.compiler.exception.CompileErrorException;
import kut.compiler.lexer.Token;
import kut.compiler.symboltable.ExprType;

public class AstDoWhile extends AstNode 
{
	/**
	 * 
	 */
	protected Token t;
	
	protected AstNode	loopBody;
	protected AstNode	expr;
	
	protected ExprType 	etype;
	
	/**
	 * @param t
	 */
	public AstDoWhile(AstNode loopBody, AstNode expr, Token t)
	{
		this.loopBody 	= loopBody;
		this.expr 		= expr;
		this.t = t;
	}
	
	/**
	 * @param gen
	 */
	public void preprocessStringLiterals(CodeGenerator gen) {
		expr.preprocessStringLiterals(gen);
		loopBody.preprocessStringLiterals(gen);
		return;
	}
	
	/**
	 *
	 */
	public void printTree(int indent) {
		this.println(indent, "do while:" + t);
		expr.printTree(indent + 1);
		loopBody.printTree(indent + 1);
	}

	
	/**
	 *
	 */
	public ExprType checkTypes(CodeGenerator gen) throws CompileErrorException
	{		
		etype = expr.checkTypes(gen);
	
		if (etype != ExprType.BOOLEAN) {
			throw new CompileErrorException("the condition expression must be boolean :" + t);
		}
		
		loopBody.checkTypes(gen);
		
		return ExprType.VOID;
	}
	

	/**
	 *
	 */
	@Override
	public void cgen(CodeGenerator gen) throws IOException, CompileErrorException
	{	
		//TODO:ここにコードを書くこと。
		DoWhileLabels l = gen.generateDoWhileLabels();
						
		return;
	}
	
	/**
	 *
	 */
	public void preprocessLocalVariables(CodeGenerator gen) throws CompileErrorException
	{
		this.expr.preprocessLocalVariables(gen);
		this.loopBody.preprocessLocalVariables(gen);
	}


}
