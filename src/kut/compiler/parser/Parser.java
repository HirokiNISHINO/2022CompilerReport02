package kut.compiler.parser;

import java.io.IOException;
import java.util.Vector;

import kut.compiler.exception.CompileErrorException;
import kut.compiler.exception.SyntaxErrorException;
import kut.compiler.lexer.Lexer;
import kut.compiler.lexer.Token;
import kut.compiler.lexer.TokenClass;
import kut.compiler.parser.ast.AstNode;
import kut.compiler.parser.ast.AstPostfixOp;
import kut.compiler.parser.ast.AstAssignment;
import kut.compiler.parser.ast.AstBinOp;
import kut.compiler.parser.ast.AstBlockStatements;
import kut.compiler.parser.ast.AstBooleanLiteral;
import kut.compiler.parser.ast.AstDoubleLiteral;
import kut.compiler.parser.ast.AstIntLiteral;
import kut.compiler.parser.ast.AstStringLiteral;
import kut.compiler.parser.ast.AstUnaryOp;
import kut.compiler.parser.ast.AstWhile;
import kut.compiler.parser.ast.AstEmptyStatement;
import kut.compiler.parser.ast.AstFunCall;
import kut.compiler.parser.ast.AstFunDef;
import kut.compiler.parser.ast.AstFunDefs;
import kut.compiler.parser.ast.AstIdentifier;
import kut.compiler.parser.ast.AstIfElse;
import kut.compiler.parser.ast.AstLocal;
import kut.compiler.parser.ast.AstPrint;
import kut.compiler.parser.ast.AstGlobal;
import kut.compiler.parser.ast.AstProgram;
import kut.compiler.parser.ast.AstReturn;
import kut.compiler.parser.ast.AstStatements;

/**
 * @author hnishino
 *
 */
public class Parser 
{
	protected Lexer lexer		;
	protected Token	currentToken;
	
	/**
	 * @param lexer
	 */
	public Parser(Lexer lexer) {
		this.lexer = lexer;
	}
	
	/**
	 * @return
	 */
	protected Token getCurrentToken()
	{
		return this.currentToken;
	}
	
	/**
	 * 
	 */
	/**
	 * 
	 */
	protected void consumeCurrentToken() throws IOException, CompileErrorException
	{
		this.currentToken = lexer.getNextToken();
		return;
	}
	
	/**
	 * @return
	 */
	public AstProgram parse() throws IOException, CompileErrorException
	{
		//read the first token.
		consumeCurrentToken();
		return program();
	}
	 

	/**
	 * @return
	 */
	protected AstProgram program() throws IOException, CompileErrorException
	{
		AstStatements 	topLevelCode 	= new AstStatements();
		AstFunDefs		functions		= new AstFunDefs();
		
		// [funcdef | statement]*
		while(this.getCurrentToken().getC() != TokenClass.EOF) {
			Token t = this.getCurrentToken();
			if (t.getC() == TokenClass.DEF) {
				AstFunDef def = def();
				functions.addFunction(def);
			}
			else {
				AstNode stmt = statement();
				topLevelCode.addStatement(stmt);
			}	
		}
		
		return new AstProgram(topLevelCode, functions);
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode emptyStmt() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != ';') {
			throw new CompileErrorException("expected ';', but found :" + t);
		}
		
		this.consumeCurrentToken();
		
		return new AstEmptyStatement();
	}
	
	/**
	 * @return
	 */
	public AstNode statement() throws IOException, CompileErrorException
	{
		int tc = this.getCurrentToken().getC();
		
		switch(tc)
		{
		case ';':
			return emptyStmt();
			
		case TokenClass.GLOBAL:
			return global();

		case TokenClass.LOCAL:
			return local();
			
		case TokenClass.RETURN:
			return ret();
			
		case TokenClass.PRINT:
			return print();
			
		case TokenClass.IF:
			return ifelse();
			
		case '{':
			return blockStmts();
	
		case TokenClass.WHILE:
			return whileStmt();
			
		case TokenClass.DO:
			return dowhileStmt();
			
		default:
			return exprStmt();
		}
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode dowhileStmt() throws IOException, CompileErrorException
	{
		//TODO:ここにコードを書くこと。
		return null;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstFunDef def() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.DEF) {
			throw new SyntaxErrorException("expected 'def' but found: " + t);
		}
		this.consumeCurrentToken();
		
		t = this.getCurrentToken();
		int tc = t.getC();
		if (tc != TokenClass.VOID 		&& tc != TokenClass.INT 	&&
			tc != TokenClass.BOOLEAN	&& tc != TokenClass.DOUBLE	&&
			tc != TokenClass.STRING	){
			throw new SyntaxErrorException("expected a type name but found: " + t);
		}
		this.consumeCurrentToken();
		
		Token t2 = this.getCurrentToken();
		if (t2.getC() != TokenClass.Identifier) {
			throw new SyntaxErrorException("expected a function name but found: " + t2);
		}
		this.consumeCurrentToken();
		
		
		Token t3 = this.getCurrentToken();
		if (t3.getC() != '(') {
			throw new SyntaxErrorException("expected '(' but found: " + t3);			
		}
		this.consumeCurrentToken();
	
		Vector<AstLocal> params = new Vector<AstLocal>();
		
		t3 = this.getCurrentToken();
		while (t3.getC() != ')') {
			int t3c = t3.getC();
			if (t3c != TokenClass.VOID 		&& t3c != TokenClass.INT 	&&
				t3c != TokenClass.BOOLEAN	&& t3c != TokenClass.DOUBLE	&&
				t3c != TokenClass.STRING	){	
				throw new SyntaxErrorException("expected a type name but found: " + t3);
			}
			this.consumeCurrentToken();

			Token t4 = this.getCurrentToken();
			if (t4.getC() != TokenClass.Identifier) {
				throw new SyntaxErrorException("expected a parameter name but found: " + t4);
			}

			AstIdentifier id = new AstIdentifier(t4);
			this.consumeCurrentToken();
			
			AstLocal param = new AstLocal(id, t3, t4);

			params.add(param);	

			t3 = this.getCurrentToken();
			if (t3.getC() == ')') {
				break;
			}
			if (t3.getC() != ',') {
				throw new SyntaxErrorException("expected ',' but found: " + t4);				
			}
			this.consumeCurrentToken();
			t3 = this.getCurrentToken();
		}
		
		if (t3.getC() != ')'){
			throw new SyntaxErrorException("expected ')' but found: " + t3);			
		}
		this.consumeCurrentToken();
		
		AstNode funcBody = blockStmts();
		
		return new AstFunDef(t2, t, funcBody, params);
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode whileStmt() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.WHILE) {
			throw new SyntaxErrorException("expected 'while' but found: " + t);
		}
		this.consumeCurrentToken();
		
		Token t2 = this.getCurrentToken();
		if (t2.getC() != '(') {
			throw new SyntaxErrorException("expected '(' but found: " + t);			
		}
		this.consumeCurrentToken();
		
		AstNode expr = expr();
		
		t2 = this.getCurrentToken();
		if (t2.getC() != ')') {
			throw new SyntaxErrorException("expected ')' but found: " + t);			
		}
		this.consumeCurrentToken();
		
		AstNode body = statement();
		
		return new AstWhile(expr, body, t);
	}
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode blockStmts() throws IOException, CompileErrorException
	{
		AstBlockStatements stmts = new AstBlockStatements();
		
		Token t = this.getCurrentToken();
		if (t.getC() != '{') {
			throw new SyntaxErrorException("expected ';' but found: " + t);
		}
		this.consumeCurrentToken();

		while(true) {
			t = this.getCurrentToken();
			if (t.getC() == '}') {
				break;
			}
			AstNode stmt = statement();
			stmts.addStatement(stmt);
		}
		
		t = this.getCurrentToken();
		if (t.getC() != '}') {
			throw new SyntaxErrorException("expected ';' but found: " + t);
		}
		this.consumeCurrentToken();
		return stmts;
	}
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode exprStmt() throws IOException, CompileErrorException
	{
		AstNode e = this.expr();
		
		Token t = this.getCurrentToken();
		if (t.getC() != ';') {
			throw new SyntaxErrorException("expected ';' but found: " + t);
		}
		this.consumeCurrentToken();
		
		return e;
	}
	
	/**
	 * @return
	 */
	public AstNode ifelse() throws IOException, CompileErrorException
	{
		//skip the if keyword.
		Token t = this.getCurrentToken();
		this.consumeCurrentToken();
		
		
		//cond
		Token t2 = this.getCurrentToken();
		if (t2.getC() != '(') {
			throw new SyntaxErrorException("expected '(' but found: " + t2);
		}
		this.consumeCurrentToken();
		
		AstNode expr = expr();
		
		t2 = this.getCurrentToken();
		if (t2.getC() != ')') {
			throw new SyntaxErrorException("expected ')' but found: " + t2);
		}
		this.consumeCurrentToken();
		
		//then clause
		AstNode thenClause = statement();
		
		AstNode elseClause = null;
		
		t2 = this.getCurrentToken();
		if (t2.getC() == TokenClass.ELSE) {
			this.consumeCurrentToken();
			elseClause = statement();
		}
		
		return new AstIfElse(expr, thenClause, elseClause, t);
		
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode print() throws IOException, CompileErrorException
	{
		//skip the print keyword.
		Token t = this.getCurrentToken();
		this.consumeCurrentToken();

		AstPrint printNode = new AstPrint(t);

		Token t2 = this.getCurrentToken();
		if (t2.getC() != '(') {
			throw new SyntaxErrorException("expected '(' but found: " + t2);
		}
		this.consumeCurrentToken();
			
		while(true) {
			AstNode e = expr();
			printNode.addExpr(e);
			t2 = this.getCurrentToken();
			if (t2.getC() == ')') {
				this.consumeCurrentToken();
				break;
			}
			
			if (t2.getC() != ',') {
				throw new SyntaxErrorException("expected ',' but found: " + t2);
			}
			this.consumeCurrentToken();
		}

		t2 = this.getCurrentToken();
		if (t2.getC() != ';') {
			throw new SyntaxErrorException("expected ';' but found: " + t2);
		}
		this.consumeCurrentToken();
		
		return printNode;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode ret() throws IOException, CompileErrorException
	{
		//skip the return keyword.
		Token t = this.getCurrentToken();
		this.consumeCurrentToken();
		
		Token t2 = this.getCurrentToken();
		if (t2.getC() == ';') {
			this.consumeCurrentToken();
			return new AstReturn(null, t);
		}
		
		AstNode expr = expr();
		
		t2 = this.getCurrentToken();
		if (t2.getC() != ';') {
			throw new SyntaxErrorException("expected ';' but found: " + t2);
		}
		this.consumeCurrentToken();

		return new AstReturn(expr, t);
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode local() throws IOException, CompileErrorException
	{
		//skip the global keyword.
		Token t = this.getCurrentToken();
		this.consumeCurrentToken();
		
		Token type = this.getCurrentToken();
		if (type.getC() != TokenClass.INT && type.getC() != TokenClass.STRING && type.getC() != TokenClass.DOUBLE) {
			throw new SyntaxErrorException("expected a type name but found: " + type);
			
		}
		this.consumeCurrentToken();
		
		Token t2 = this.getCurrentToken();
		if (t2.getC() != TokenClass.Identifier) {
			throw new SyntaxErrorException("expected an identifier but found: " + t2);
		}
		
		AstIdentifier id = new AstIdentifier(t2);
		this.consumeCurrentToken();
		
		t2 = this.getCurrentToken();
		if (t2.getC() != ';') {
			throw new SyntaxErrorException("expected ';' but found: " + t2);
		}
		this.consumeCurrentToken();
		
		return new AstLocal(id, type, t);
	}
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode global() throws IOException, CompileErrorException
	{
		//skip the global keyword.
		Token t = this.getCurrentToken();
		this.consumeCurrentToken();
		
		Token type = this.getCurrentToken();
		if (type.getC() != TokenClass.INT && type.getC() != TokenClass.STRING && type.getC() != TokenClass.DOUBLE) {
			throw new SyntaxErrorException("expected a type name but found: " + type);
			
		}
		this.consumeCurrentToken();
				
		Token t2 = this.getCurrentToken();
		if (t2.getC() != TokenClass.Identifier) {
			throw new SyntaxErrorException("expected an identifier but found: " + t2);
		}
		
		AstIdentifier id = new AstIdentifier(t2);
		this.consumeCurrentToken();
		
		t2 = this.getCurrentToken();
		if (t2.getC() != ';') {
			throw new SyntaxErrorException("expected ';' but found: " + t2);
		}
		this.consumeCurrentToken();
		
		return new AstGlobal(id, type, t);
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode expr() throws IOException, CompileErrorException
	{
		return lorExpr();
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode lorExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = landExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != TokenClass.LOR) {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = landExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode landExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = bitorExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != TokenClass.LAND) {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = bitorExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode bitorExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = bitxorExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != '|') {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = bitxorExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode bitxorExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = bitandExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != '^') {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = bitandExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode bitandExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = equalityExp();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != '&') {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = equalityExp();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode equalityExp() throws IOException, CompileErrorException
	{
		AstNode lhs = relationalExp();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != TokenClass.EQ && t.getC()!= TokenClass.NEQ) {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = relationalExp();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode relationalExp() throws IOException, CompileErrorException
	{
		AstNode lhs = shiftExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != TokenClass.LTEQ && t.getC()!= TokenClass.GTEQ &&
				t.getC() != '<'				&& t.getC()!= '>'			  ) {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = shiftExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;		
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode shiftExpr() throws IOException, CompileErrorException
	{
		AstNode lhs = additiveExp();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != TokenClass.SAL  && t.getC()!= TokenClass.SAR &&
				t.getC() != TokenClass.SHR	){
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = additiveExp();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;		
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode additiveExp() throws IOException, CompileErrorException
	{
		AstNode lhs = multiplicativeExpr();
		while(true) {
			Token t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC()!= '+' && t.getC() != '-') {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = multiplicativeExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode multiplicativeExpr() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();

		AstNode lhs = unaryOpExpr();
		
		while(true) {
			t = this.getCurrentToken();
			
			if (t == null) {
				break;
			}
			
			if (t.getC() != '*' && t.getC() != '/' && t.getC() != '%') {
				break;
			}
			this.consumeCurrentToken();
			
			AstNode rhs = unaryOpExpr();
			AstNode binop = new AstBinOp(lhs, rhs, t);
			lhs = binop;
		}
		return lhs;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode unaryOpExpr() throws IOException, CompileErrorException
	{		
		Token t = this.getCurrentToken();
		if (t.getC() == '-' || t.getC() == '!' || t.getC() == TokenClass.PLUS_PLUS || t.getC() == TokenClass.MINUS_MINUS) {
			this.consumeCurrentToken();
			AstNode p = postfixExpr();
			return new AstUnaryOp(p,t);
		}
		return postfixExpr();
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode postfixExpr() throws IOException, CompileErrorException
	{
		AstNode n = primary();
		Token t = this.getCurrentToken();
		while (t.getC() == TokenClass.PLUS_PLUS || t.getC() == TokenClass.MINUS_MINUS) {
			n = new AstPostfixOp(n, t);
			this.consumeCurrentToken();
			t = this.getCurrentToken();
		}
		return n;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode primary() throws IOException, CompileErrorException
	{
		//( expr )
		Token t = this.getCurrentToken();
		if (t.getC() == '(') {
			this.consumeCurrentToken();
			
			AstNode e = expr();
			
			t = this.getCurrentToken();
			if (t.getC() != ')') {
				throw new SyntaxErrorException("expected ')' but found : " + t);
			}
			
			this.consumeCurrentToken();
			
			return e;
		}

		t = this.getCurrentToken();
		if (t.getC() == TokenClass.IntLiteral) {
			return integer();
		}
		
		if (t.getC() == TokenClass.Identifier) {
			return identifierAssignmentAndFunctionCall();
		}
		
		if (t.getC() == TokenClass.StringLiteral) {
			return string();
		}
		
		if (t.getC() == TokenClass.DoubleLiteral) {
			return dbl();
		}
		
		if (t.getC() == TokenClass.BooleanLiteral) {
			return bln();
		}
		
		throw new SyntaxErrorException("expected an identifier, a string literal, a double literal, a boolean literal , or an integer literal, but found: " + t);
		
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected AstNode identifierAssignmentAndFunctionCall() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.Identifier) {
			throw new SyntaxErrorException("expected an identifier, but found: " + t);
		}
		
		AstIdentifier id = new AstIdentifier(t);
		this.consumeCurrentToken();
		
		Token t2 = this.getCurrentToken();
		if (t2.getC() == '=') {
			//assignment
			this.consumeCurrentToken();
			
			AstNode rhs = expr();
			
			return new AstAssignment(id, rhs, t);
		}
		else if (t2.getC() == '(') {
			this.consumeCurrentToken();
			AstFunCall fc = new AstFunCall(id, t);
			t2 = this.getCurrentToken();
			while(t2.getC() != ')') {
				AstNode e = expr();
				fc.addExpr(e);
				
				t2 = this.getCurrentToken();
				if (t2.getC() == ')') {
					break;
				}
				if (t2.getC() != ',') {
					throw new SyntaxErrorException("expected ',', but found: " + t2);
				}
				this.consumeCurrentToken();
			}
			this.consumeCurrentToken();
			return fc;
		}
		
		return id;
	}
	
	


	 
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected AstNode integer() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.IntLiteral) {
			throw new SyntaxErrorException("expected an integer literal, but found: " + t);
		}
		
		AstNode node = new AstIntLiteral(t);
		this.consumeCurrentToken();
		
		return node;
	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected AstNode dbl() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.DoubleLiteral) {
			throw new SyntaxErrorException("expected a double literal, but found: " + t);
		}
		
		AstNode node = new AstDoubleLiteral(t);
		this.consumeCurrentToken();
		return node;

	}
	
	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public AstNode bln() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.BooleanLiteral) {
			throw new SyntaxErrorException("expected a boolean literal, but found: " + t);
		}

		AstNode node = new AstBooleanLiteral(t);
		this.consumeCurrentToken();

		return node;
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected AstNode string() throws IOException, CompileErrorException
	{
		Token t = this.getCurrentToken();
		if (t.getC() != TokenClass.StringLiteral) {
			throw new SyntaxErrorException("expected a string literal, but found: " + t);
		}
		
		AstNode node = new AstStringLiteral(t);
		this.consumeCurrentToken();
		
		return node;
	}
}
