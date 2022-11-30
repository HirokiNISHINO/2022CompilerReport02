package kut.compiler.parser.ast;

import java.io.IOException;

import kut.compiler.compiler.CodeGenerator;
import kut.compiler.compiler.CondLabels;
import kut.compiler.exception.CompileErrorException;
import kut.compiler.lexer.Token;
import kut.compiler.lexer.TokenClass;
import kut.compiler.symboltable.ExprType;

public class AstBinOp extends AstNode 
{
	/**
	 * 
	 */
	protected Token t;
	
	protected AstNode	lhs;
	protected AstNode	rhs;
	
	protected ExprType rtype;
	protected ExprType ltype;
	
	/**
	 * @param t
	 */
	public AstBinOp(AstNode lhs, AstNode rhs, Token t)
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.t = t;
	}
	
	/**
	 * @param gen
	 */
	public void preprocessStringLiterals(CodeGenerator gen) {
		lhs.preprocessStringLiterals(gen);
		rhs.preprocessStringLiterals(gen);
		return;
	}
	
	/**
	 *
	 */
	public void printTree(int indent) {
		this.println(indent, "binop:" + t);
		lhs.printTree(indent + 1);
		rhs.printTree(indent + 1);
	}

	
	/**
	 *
	 */
	public ExprType checkTypes(CodeGenerator gen) throws CompileErrorException
	{		
		ltype = lhs.checkTypes(gen);
		rtype = rhs.checkTypes(gen);

		int tc = this.t.getC();
		
		if (tc == TokenClass.LAND || tc == TokenClass.LOR) {
			if (ltype != ExprType.BOOLEAN || rtype != ExprType.BOOLEAN) {
				throw new CompileErrorException("logical and (&&) and logical or (||) can be applied only to boolean values. : " + t);
			}
			return ExprType.BOOLEAN;
		}
		
		if (tc == '&' || tc == '|' || tc == '^') {
			if (ltype != ExprType.INT || rtype != ExprType.INT) {
				throw new CompileErrorException("bit and (&) or (|), and xor (^), can be applied only to integer values. : " + t);
			}
			return ExprType.INT;			
		}
		
		if (tc == TokenClass.EQ || tc == TokenClass.NEQ  ||
			tc == '<'			|| tc == TokenClass.LTEQ ||
			tc == '>' 			|| tc == TokenClass.GTEQ ){
			return this.doTypeCheckComparision();
		}
		
		if (tc == TokenClass.SAL || tc == TokenClass.SAR || tc == TokenClass.SHR) {
			if (ltype != ExprType.INT || rtype != ExprType.INT) {
				throw new CompileErrorException("shift operators can be applied only to integer values. : " + t);
			}
			return ExprType.INT;
		}
		
		
		boolean rtypeok = (rtype == ExprType.INT || rtype == ExprType.DOUBLE);
		boolean ltypeok = (ltype == ExprType.INT || ltype == ExprType.DOUBLE);
		
		if (rtypeok != true || ltypeok != true) {
			throw new CompileErrorException("invalid binary operation (only integer and double values can be used). : " + t);
		}

		if (rtype == ExprType.DOUBLE || ltype == ExprType.DOUBLE) {
			return ExprType.DOUBLE;
		}
		return ExprType.INT;
	}
	
	/**
	 * @return
	 * @throws CompileErrorException
	 */
	protected ExprType doTypeCheckComparision() throws CompileErrorException
	{
		//string can be compared only between string values.
		if ((ltype == ExprType.STRING || rtype == ExprType.STRING) && (ltype != rtype)) {
			throw new CompileErrorException("a string value can be only compared to another string value. : " + t.toString());
		}
		
		//boolean values can be only compared between boolean values.
		if ((ltype == ExprType.BOOLEAN || rtype == ExprType.BOOLEAN)){
			if (t.getC() != TokenClass.EQ && t.getC() != TokenClass.NEQ) {
				throw new CompileErrorException("the operator is undefined for boolean values. : " + t.toString());				
			}
			if ((ltype != rtype)) {
				throw new CompileErrorException("a boolean value can be only compared to another boolean value. : " + t.toString());
			}
		}
		
		return ExprType.BOOLEAN;
	}

	/**
	 *
	 */
	@Override
	public void cgen(CodeGenerator gen) throws IOException, CompileErrorException
	{	
		//logical and/or doesn't require both operands.
		int tc = t.getC();
		if (tc == TokenClass.LAND || tc == TokenClass.LOR) {
			this.opLandLor(gen);
			return;
		}
		
		if (tc == '&' || tc == '|' || tc == '^') {
			this.opBitOp(gen);
			return;			
		}

		
		//when we need both operands.
		lhs.cgen(gen);
		gen.printCode("push rax");
		rhs.cgen(gen);
		
		if (ltype == ExprType.INT && rtype == ExprType.INT) {
			this.opIntegerInteger(gen);
		}
		else if (ltype == ExprType.DOUBLE && rtype == ExprType.INT) {
			this.opDoubleInteger(gen);
		}
		else if (ltype == ExprType.INT && rtype == ExprType.DOUBLE) {
			this.opIntegerDouble(gen);
		}
		else if (ltype == ExprType.DOUBLE && rtype == ExprType.DOUBLE) {
			this.opDoubleDouble(gen);
		}
		else if (ltype == ExprType.BOOLEAN && rtype == ExprType.BOOLEAN){
			this.opBooleanEqNeq(gen);
		}
		else if (ltype == ExprType.STRING && rtype == ExprType.STRING){
			this.opString(gen);
		}
		else {
			throw new CompileErrorException("the code shouldn't reach here. There may be a bug in the parser.");	
		}
		return;
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opString(CodeGenerator gen) throws IOException, CompileErrorException
	{
		switch(t.getC())
		{
		case TokenClass.EQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jne " + labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;
			
		case TokenClass.NEQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jne " + labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;

		case '<':
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jl " + labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;
			
		case '>':
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jg " + labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;
			
		case  TokenClass.LTEQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jle " + labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;
		
		case  TokenClass.GTEQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			gen.printCode	("mov rsi, rax");
			gen.printCode	("pop rdi");
			gen.printCode	("call " + gen.getExternalFunctionName("strcmp"));
			gen.printCode	("cmp rax, 0");
			gen.printCode	("jge " + labels1.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels1.labelCondEnd);
			gen.printLabel	(labels1.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
		}
			break;
			
		default:
			throw new CompileErrorException("the code shouldn't reach here. There may be a bug in the parser.");				
		}			
	}
	
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opBitOp(CodeGenerator gen) throws IOException, CompileErrorException
	{
		lhs.cgen(gen);
		gen.printCode("push rax");
		rhs.cgen(gen);
		if (t.getC() == '&') {
			gen.printCode("and rax, [rsp]");
			gen.printCode("add rsp, 8");
		}
		else if (t.getC() == '|') {
			gen.printCode("or rax, [rsp]");
			gen.printCode("add rsp, 8");
			
		}
		else if (t.getC() == '^') {
			gen.printCode("xor rax, [rsp]");
			gen.printCode("add rsp, 8");
		}

		return;
	}
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opLandLor(CodeGenerator gen) throws IOException, CompileErrorException
	{
		CondLabels lbls = gen.generateCondLabels();
		
		// &&
		if (t.getC() == TokenClass.LAND) {
			lhs.cgen(gen);
			gen.printCode("cmp rax, 0");
			gen.printCode("je " + lbls.labelFalse);
			gen.printCode();
			
			rhs.cgen(gen);
			gen.printCode("cmp rax, 0");
			gen.printCode("je " + lbls.labelFalse);
			gen.printCode("mov rax, 1");
			gen.printCode("jmp " + lbls.labelCondEnd);
			
			gen.printLabel(lbls.labelFalse);
			gen.printCode("mov rax, 0");			
			gen.printLabel(lbls.labelCondEnd);
			
			return;
		}
		
		// ||
		lhs.cgen(gen);
		gen.printCode("cmp rax, 0");
		gen.printCode("jne " + lbls.labelTrue);
		gen.printCode();

		rhs.cgen(gen);
		gen.printCode("cmp rax, 0");
		gen.printCode("jne " + lbls.labelTrue);
		gen.printCode("mov rax, 0");
		gen.printCode("jmp " + lbls.labelCondEnd);

		gen.printLabel(lbls.labelTrue);
		gen.printCode("mov rax, 1");			
		gen.printLabel(lbls.labelCondEnd);

		
		//end
		return;
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opBooleanEqNeq(CodeGenerator gen) throws IOException, CompileErrorException
	{
		switch(t.getC())
		{
		case TokenClass.EQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			CondLabels labels2 = gen.generateCondLabels();
			CondLabels labels3 = gen.generateCondLabels();
			gen.printCode	("cmp rax, 0");
			gen.printCode	("je " + labels1.labelCondEnd);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, 0");
			gen.printCode	("je " + labels2.labelCondEnd);
			gen.printCode	("mov rbx, 1");
			gen.printLabel	(labels2.labelCondEnd);
			gen.printCode	("cmp rax, rbx");
			gen.printCode	("jne " + labels3.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printCode	("jmp " + labels3.labelCondEnd);
			gen.printLabel	(labels3.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printLabel	(labels3.labelCondEnd);
		}
			break;
			
		case TokenClass.NEQ:
		{
			CondLabels labels1 = gen.generateCondLabels();
			CondLabels labels2 = gen.generateCondLabels();
			CondLabels labels3 = gen.generateCondLabels();
			gen.printCode	("cmp rax, 0");
			gen.printCode	("je " + labels1.labelCondEnd);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels1.labelCondEnd);
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, 0");
			gen.printCode	("je " + labels2.labelCondEnd);
			gen.printCode	("mov rbx, 1");
			gen.printLabel	(labels2.labelCondEnd);
			gen.printCode	("cmp rax, rbx");
			gen.printCode	("jne " + labels3.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels3.labelCondEnd);
			gen.printLabel	(labels3.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels3.labelCondEnd);
		}
			break;
						
		default:
			throw new CompileErrorException("the code shouldn't reach here. There may be a bug in the parser.");				
		}	

	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	public void opXmm(CodeGenerator gen) throws IOException, CompileErrorException
	{
		switch(t.getC())
		{
		case '+':
			gen.printCode("addsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");

			break;
			
		case '-':
			gen.printCode("subsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			break;
			
		case '*':
			gen.printCode("mulsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			break;
			
		case '/':
			gen.printCode("divsd xmm0, xmm1");
			gen.printCode("movq rax, xmm0");
			break;
			
		case '%':
			gen.printCode("call " + gen.getExternalFunctionName("fmod"));
			gen.printCode("movq rax, xmm0");
			break;
			
			
		case TokenClass.EQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("jne " + labels.labelFalse);
			
			gen.printCode	("mov rax, 1");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;

		case '<':
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("jb " + labels.labelFalse);
			
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case '>':
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("ja " + labels.labelFalse);
			
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case TokenClass.LTEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("jbe " + labels.labelFalse);
			
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
		case TokenClass.GTEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("jae " + labels.labelFalse);
			
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case TokenClass.NEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("UCOMISD xmm0, xmm1");
			gen.printCode	("jne " + labels.labelFalse);
			
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);

			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;

		default:
			throw new CompileErrorException("the code shouldn't reach here. There may be a bug in the parser.");	
		}
	}
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected void opDoubleDouble(CodeGenerator gen) throws IOException, CompileErrorException
	{
		gen.printCode("movq xmm1, rax");
		gen.printCode("pop rax");		
		gen.printCode("movq xmm0, rax");

		this.opXmm(gen);
		
		
		return;
	}
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected void opIntegerDouble(CodeGenerator gen) throws IOException, CompileErrorException
	{
		gen.printCode("movq xmm1, rax");
		gen.printCode("pop rax");		
		gen.printCode("cvtsi2sd xmm0, rax");

		this.opXmm(gen);

		
		return;
	}
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected void opDoubleInteger(CodeGenerator gen) throws IOException, CompileErrorException
	{
		gen.printCode("cvtsi2sd xmm1, rax");
		gen.printCode("pop rax");		
		gen.printCode("movq xmm0, rax");

		this.opXmm(gen);
		
		return;
	}
	
	
	/**
	 * @param gen
	 * @throws IOException
	 * @throws CompileErrorException
	 */
	protected void opIntegerInteger(CodeGenerator gen) throws IOException, CompileErrorException
	{
		switch(t.getC())
		{
		case '+':
			gen.printCode("add rax, [rsp]");
			gen.printCode("add rsp, 8");
			break;
			
		case '-':
			gen.printCode("mov rbx, rax");
			gen.printCode("pop rax");
			gen.printCode("sub rax, rbx");
			break;
			
		case '*':
			gen.printCode("imul rax, [rsp]");
			gen.printCode("add rsp, 8");
			break;
			
		case '/':
			gen.printCode("mov rbx, rax");
			gen.printCode("mov rdx, 0");
			gen.printCode("mov rax, [rsp]");
			gen.printCode("add rsp, 8");
			gen.printCode("idiv rbx");
			break;
			
		case '%':
			gen.printCode("mov rbx, rax");
			gen.printCode("mov rdx, 0");
			gen.printCode("mov rax, [rsp]");
			gen.printCode("add rsp, 8");
			gen.printCode("idiv rbx");
			gen.printCode("mov rax, rdx");
			break;
			
			
		case TokenClass.EQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rax, rbx");
			gen.printCode	("jne " + labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;

		case TokenClass.NEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rax, rbx");
			gen.printCode	("jne " + labels.labelFalse);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelFalse);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case '<':
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, rax");
			gen.printCode	("jl " + labels.labelTrue);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelTrue);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case '>':
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, rax");
			gen.printCode	("jg " + labels.labelTrue);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelTrue);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;

		case TokenClass.LTEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, rax");
			gen.printCode	("jle " + labels.labelTrue);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelTrue);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case TokenClass.GTEQ:
		{
			CondLabels labels = gen.generateCondLabels();
			
			gen.printCode	("pop rbx");
			gen.printCode	("cmp rbx, rax");
			gen.printCode	("jge " + labels.labelTrue);
			gen.printCode	("mov rax, 0");
			gen.printCode	("jmp " + labels.labelCondEnd);
			gen.printLabel	(labels.labelTrue);
			gen.printCode	("mov rax, 1");
			gen.printLabel	(labels.labelCondEnd);
		}
			break;
			
		case TokenClass.SAL:
		{
			gen.printCode("mov rcx, rax");
			gen.printCode("pop rax");
			gen.printCode("sal rax, cl");
		}
			break;
			
		case TokenClass.SAR:
		{
			gen.printCode("mov rcx, rax");
			gen.printCode("pop rax");
			gen.printCode("sar rax, cl");			
		}
			break;
			
		case TokenClass.SHR:
		{
			gen.printCode("mov rcx, rax");
			gen.printCode("pop rax");
			gen.printCode("shr rax, cl");			
		}
			break;

		default:
			throw new CompileErrorException("the code shouldn't reach here. There may be a bug in the parser.");	
		}
		return;	
	}
	

	/**
	 *
	 */
	public void preprocessLocalVariables(CodeGenerator gen) throws CompileErrorException
	{
		this.lhs.preprocessLocalVariables(gen);
		this.rhs.preprocessLocalVariables(gen);
	}


}
