package kut.compiler.lexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

import kut.compiler.exception.CompileErrorException;


/**
 * @author hnishino
 *
 */
public class Lexer 
{
	/**
	 * the filename of a program to load.
	 */
	protected File				file	;	
	
	protected FileReader 		reader	;
	protected Stack<Integer>	unreadCharacters;
	
	protected int				lineNo	;
	/**
	 * @param program
	 */
	public Lexer(String filename) throws CompileErrorException
	{
		this.unreadCharacters = new Stack<Integer>();
		
		this.file = new File(filename);
		
		reader = null;
		try {
			reader = new FileReader(file);
		}
		catch (FileNotFoundException e) {
			throw new CompileErrorException("file not found: " + this.file.getAbsolutePath());
		}
		
		this.lineNo = 0;
		
		return;
	}
	


	/**
	 * @return
	 * @throws IOException
	 */
	protected int read() throws IOException
	{
		int i = 0;
		if (!unreadCharacters.isEmpty()) {
			i = unreadCharacters.pop();
		}
		else {
			i = reader.read();
		}
		
		if (i == '\n') {
			lineNo++;
		}
		
		return i;
	}
	
	/**
	 * @param i
	 * @throws IOException
	 */
	protected void unread(int i) throws IOException
	{
		if (i == '\n') {
			lineNo--;
		}
		unreadCharacters.push(i);
	}
	
	/**
	 * @return
	 */
	public Token getNextToken() throws IOException, CompileErrorException
	{
		if (this.reader == null) {
			return null;
		}

		int i = this.read();

		if (i < 0) {
			return new Token(-1, "EOF", lineNo);
		}

		char c = (char)i;


		//skip the white space character.
		if (Character.isWhitespace(c)) {
			//tail-call optimization.
			return getNextToken();
		}

		//if it is a digit, then get a number token (integer or double).
		if (Character.isDigit(c)) {
			this.unread(i); 
			return this.getNextTokenNumber();
		}
		
		//check if it is a constant string
		if (c == '\"') {
			this.unread(i);
			return this.getNextTokenString();
		}

		//+-/*/;
		switch(c) {
		case '*':
		case '/':
		case ';':
		case '(':
		case ')':
		case '!':
		case '{':
		case '}':
		case ',':
		case '%':
		case '^':
			return new Token(i, "" + c, lineNo);
		default:
			break;
		}

		//+ and ++
		if (c == '+') {
			i = this.read();
			c = (char)i;
			if (c != '+') {
				this.unread(i);
				return new Token('+', "+", lineNo);
			}
			return new Token(TokenClass.PLUS_PLUS, "++", lineNo);
		}

		//- and --
		if (c == '-') {
			i = this.read();
			c = (char)i;
			if (c != '-') {
				this.unread(i);
				return new Token('-', "-", lineNo);
			}
			return new Token(TokenClass.MINUS_MINUS, "--", lineNo);
		}

		//= and ==
		if (c == '=') {
			i = this.read();
			c = (char)i;
			if (c != '=') {
				this.unread(i);
				return new Token('=', "=", lineNo);
			}
			return new Token(TokenClass.EQ, "==", lineNo);
		}
		
		//!=
		if (c == '!') {
			i = this.read();
			c = (char)i;
			if (c == '=') {
				return new Token(TokenClass.NEQ, "!=", lineNo);
			}
			this.unread(i);
		}
		
		//<, <=
		if (c == '<') {
			i = this.read();
			c = (char)i;
			if (c == '=') {
				return new Token(TokenClass.LTEQ, "<=", lineNo);
			}
			if (c== '<') {
				return new Token(TokenClass.SAL, "<<", lineNo);
			}
			this.unread(i);
			return new Token('<', "<", lineNo);
		}
		
		//>, >=
		if (c == '>') {
			i = this.read();
			c = (char)i;
			if (c == '=') {
				return new Token(TokenClass.GTEQ, ">=", lineNo);
			}
			if (c == '>') {
				i = this.read();
				c = (char)i;
				if (c != '>') {
					this.unread(i);
					return new Token(TokenClass.SAR, ">>", lineNo);		
				}
				return new  Token(TokenClass.SHR, ">>>", lineNo);	
			}
			this.unread(i);
			return new Token('>', ">", lineNo);
		}
		
		//& and &&
		if (c == '&') {
			i = this.read();
			c = (char)i;
			if (c == '&'){
				return new Token(TokenClass.LAND, "&&", lineNo);
			}
			this.unread(i);
			return new Token('&', "&", lineNo); 
		}
		
		//| and ||
		if (c == '|') {
			i = this.read();
			c = (char)i;
			if (c == '|'){
				return new Token(TokenClass.LOR, "||", lineNo);
			}
			this.unread(i);
			return new Token('|', "|", lineNo); 
		}
		
		//check if the character is an identifier start or not.
		if (!Character.isJavaIdentifierStart(i)) {
			return new Token(TokenClass.ERROR, "" + c, lineNo);
		}

		this.unread(i);
		String lexeme = this.identifierOrKeyword();
		
		//reserved keywords and 
		if (lexeme.equals("global")) {
			return new Token(TokenClass.GLOBAL, lexeme, lineNo);	
		}
		
		if (lexeme.equals("local")) {
			return new Token(TokenClass.LOCAL, lexeme, lineNo);	
		}
		
		if (lexeme.equals("return")) {
			return new Token(TokenClass.RETURN, lexeme, lineNo);
		}
		
		if (lexeme.equals("print")){
			return new Token(TokenClass.PRINT, lexeme, lineNo);
		}
		
		if (lexeme.equals("int")) {
			return new Token(TokenClass.INT, lexeme, lineNo);
		}
		
		if (lexeme.equals("string")) {
			return new Token(TokenClass.STRING, lexeme, lineNo);
		}

		if (lexeme.equals("double")) {
			return new Token(TokenClass.DOUBLE, lexeme, lineNo);
		}
		
		if (lexeme.equals("boolean")) {
			return new Token(TokenClass.BOOLEAN, lexeme, lineNo);
		}
		
		if (lexeme.equals("true") || lexeme.equals("false")) {
			return new Token(TokenClass.BooleanLiteral, lexeme, lineNo);
		}
		
		if (lexeme.equals("if")) {
			return new Token(TokenClass.IF, lexeme, lineNo);
		}
		
		if (lexeme.equals("else")) {
			return new Token(TokenClass.ELSE, lexeme, lineNo);
		}
		
		if (lexeme.equals("while")) {
			return new Token(TokenClass.WHILE, lexeme, lineNo);
		}
		
		if (lexeme.equals("void")) {
			return new Token(TokenClass.VOID, lexeme, lineNo);
		}
		
		if (lexeme.equals("def")) {
			return new Token(TokenClass.DEF, lexeme, lineNo);
		}
		
		if (lexeme.equals("do")) {
			//TODO:ここにコードを書くこと。
		}

		//if none of above, it's an identifier.
		return new Token(TokenClass.Identifier, lexeme, lineNo);		
	}
	
	/**
	 * @return
	 */
	public String identifierOrKeyword() throws IOException
	{
		StringBuffer sb = new StringBuffer();

		//we know that the first character is already a　valid identifier start character.
		//when this method is called.
		while(true) {
			int i = this.read();
			char c = (char)i;
			if (!Character.isJavaIdentifierPart(c)) {
				this.unread(i);
				break;
			}
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	public Token getNextTokenString() throws IOException, CompileErrorException
	{
		StringBuffer sb = new StringBuffer();

		int i = this.read();
		if (i == -1) {
			throw new CompileErrorException("reached EOF before the string terminates.");
		}
		
		char c = (char)i;
		if (c != '\"') {
			throw new CompileErrorException("expected '\"' but found :" + c);			
		}
		sb.append(c);
		
		do {
			i = this.read();
			if (i == -1) {
				throw new CompileErrorException("reached EOF before the string terminates.");
			}

			c = (char)i;
			
			if (c == '\n') {
				throw new CompileErrorException("expected '\"' but found :'\n'");			
			}
			sb.append(c);			

			if (c == '\"') {
				break;
			}
			
		} while(true);

		return new Token(TokenClass.StringLiteral, sb.toString(), lineNo);
	}
	
	/**
	 * @return
	 * @throws IOException
	 */
	public Token getNextTokenNumber() throws IOException
	{
		StringBuffer sb = new StringBuffer();
		
		boolean dotFound = false;
		int i;
		do {
			i = this.read();
			if (i < 0) {
				break;
			}
			char c = (char)i;
			if (c == '.') {
				if (dotFound) {
					break;
				}
				dotFound = true;
			}
			else if (!Character.isDigit(c)) {
				break;
			}
			
			sb.append(c);
		} while(true);

		this.unread(i);

		Token t = null;
		if (dotFound) {
			t = new Token(TokenClass.DoubleLiteral, sb.toString(), lineNo);
		}
		else {
			t = new Token(TokenClass.IntLiteral, sb.toString(), lineNo);
		}
		return t;
	}
}
