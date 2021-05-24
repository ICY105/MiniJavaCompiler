package syntax;

import java.util.ArrayList;
import java.util.List;

import tree.*;

public class Fragments {

	private static SymbolTable table;
	private static String clazz;
	private static int temp;
	
	public static SEQ createFragment(SymbolTable t, ClassDecl c, MethodDecl m) {
		table = t;
		clazz = c.i.s;
		temp = -1;
		
		SEQ head = new SEQ(
				headLabel(c,m),
				new SEQ(
					new SEQ(
						methodSEQ(m,0),
						tailMove(c,m)
					),
					tailJump(c,m)
				)
			);
		return head;
	}
	
	public static SEQ createFragment(SymbolTable t, MainClass c) {
		table = t;
		clazz = c.i1.s;
		temp = -1;
		
		SEQ head = new SEQ(
				LABEL.generateLABEL(c.i1.s + "$main$preludeEnd"),
				new SEQ(
					stmSEQ(c.s,null),
					new JUMP(c.i1.s + "$main$epilogBegin")
				)
			);
		return head;
	}
	
	
	
	private static LABEL headLabel(ClassDecl c, MethodDecl m) {
		return LABEL.generateLABEL(c.i.s + "$" + m.i.s + "$preludeEnd");
	}
	
	private static JUMP tailJump(ClassDecl c, MethodDecl m) {
		return new JUMP(c.i.s + "$" + m.i.s + "$epilogBegin");
	}
	
	private static MOVE tailMove(ClassDecl c, MethodDecl m) {
		return new MOVE(
				new TEMP("%i1"),
				new MEM(
						new BINOP(BINOP.MINUS, new TEMP("%fp"), new CONST(4))
				)
			);
	}
	
	private static Stm methodSEQ(MethodDecl m, int index) {
		if(m.sl.size() > 0)
			if(index == m.sl.size()-1)
				return stmSEQ(m.sl.get(index),null);
			else
				return new SEQ(stmSEQ(m.sl.get(index),null),methodSEQ(m,index+1));
		return new LABEL("NULL");
	}
	
	private static Stm stmSEQ(Statement stm, Stm term) {
		Stm out = null;
		if(stm instanceof If)			out = stmSEQ((If)stm,term);
		if(stm instanceof Block)		out = stmSEQ((Block)stm,term,0);
		if(stm instanceof While)		out = stmSEQ((While)stm,term);
		if(stm instanceof Print)		out = stmSEQ((Print)stm,term);
		if(stm instanceof Assign)		out = stmSEQ((Assign)stm,term);
		if(stm instanceof ArrayAssign)	out = stmSEQ((ArrayAssign)stm,term);
		if(term == null)
			return out;
		else if(out != null)
			return new SEQ(out,term);
		return null;
	}
	
	private static Stm stmSEQ(If stm, Stm term) {
		NameOfLabel thenL = NameOfLabel.generateLabel("if$then");
		NameOfLabel elseL = NameOfLabel.generateLabel("if$else");
		NameOfLabel endL = NameOfLabel.generateLabel("if$end");
		
		return new SEQ(
				expCJUMP(stm.e,thenL,elseL),
				new SEQ(
					new LABEL(thenL),
					stmSEQ(stm.s1,new SEQ(
						new JUMP(endL),
						new SEQ(
							new LABEL(elseL),
							stmSEQ(stm.s2,new LABEL(endL))
						)
					))
				)
			);
	}
	
	private static Stm stmSEQ(Block stm, Stm term, int index) {
		if(index == stm.sl.size()-1)
			return stmSEQ(stm.sl.get(index),term);
		else
			return new SEQ(stmSEQ(stm.sl.get(index),term),stmSEQ(stm,term,index+1));
	}
	
	private static Stm stmSEQ(While stm, Stm term) {
		NameOfLabel beginL = NameOfLabel.generateLabel("while$begin");
		NameOfLabel endL = NameOfLabel.generateLabel("while$end");
		
		return new SEQ(
				new SEQ(
					new SEQ(
						new LABEL(beginL),
						stmSEQ(stm.s,term)
					),
					expCJUMP(stm.e,beginL,endL)
				),
				new LABEL(endL)
			);
	}
	
	private static Stm stmSEQ(Print stm, Stm term) {
		return new EVAL(
				new CALL(new NAME("print_int"),exp(stm.e))
			);
	}
	
	private static Stm stmSEQ(Assign stm, Stm term) {
		return new MOVE(new MEM(new BINOP(BINOP.MINUS, new TEMP("%fp"), new CONST(4))),exp(stm.e));
	}
	
	private static Stm stmSEQ(ArrayAssign stm, Stm term) {
		return new MOVE(new BINOP(BINOP.MINUS, new TEMP("%fp"), exp(stm.indexInArray)),exp(stm.e));
	}
	
	
	private static CJUMP expCJUMP(Expression n, NameOfLabel l1, NameOfLabel l2) {
		if(n instanceof LessThan)
			return new CJUMP(CJUMP.LT, exp(((LessThan) n).e1), exp(((LessThan) n).e2), l1, l2);
		else
			return new CJUMP(CJUMP.EQ, exp(n), new CONST(1), l1, l2);
	}
	
	
	private static Exp exp(Expression n) {
		if(n instanceof And)			return exp((And)n);
		if(n instanceof Not)			return exp((Not)n);
		if(n instanceof This)			return new TEMP(exp((This)n));
		if(n instanceof Plus)			return exp((Plus)n);
		if(n instanceof Call)			return exp((Call)n);
		if(n instanceof True)			return exp((True)n);
		if(n instanceof False)			return exp((False)n);
		if(n instanceof Minus)			return exp((Minus)n);
		if(n instanceof Times)			return exp((Times)n);
		if(n instanceof NewArray)		return exp((NewArray)n);
		if(n instanceof LessThan)		return exp((LessThan)n);
		if(n instanceof NewObject)		return exp((NewObject)n);
		if(n instanceof ArrayLookup)	return exp((ArrayLookup)n);
		if(n instanceof ArrayLength)	return exp((ArrayLength)n);
		if(n instanceof IdentifierExp)	return exp((IdentifierExp)n);
		if(n instanceof IntegerLiteral)	return exp((IntegerLiteral)n);
		return null;
	}
	
	private static Exp exp(And n) {
		return new BINOP(BINOP.AND, exp(n.e1), exp(n.e2));
	}
	
	private static Exp exp(Not n) {
		return new BINOP(BINOP.XOR,exp(n.e),new CONST(1));
	}
	
	private static String exp(This n) {
		return clazz;
	}
	
	private static Exp exp(Plus n) {
		return new BINOP(BINOP.PLUS, exp(n.e1), exp(n.e2));
	}
	
	private static Exp exp(Call n) {
		Exp par = exp(n.e);
		if(par instanceof TEMP)
			return new CALL(
				new NAME( ((TEMP)par).temp + "$" + n.i.s ),
				exp(n.el)
			);
		else
			return new CALL(
				new NAME( n.i.s ),
				exp(n.e)
			);
	}
	
	private static Exp exp(True n) {
		return new CONST(1);
	}
	
	private static Exp exp(False n) {
		return new CONST(0);
	}
	
	private static Exp exp(Minus n) {
		return new BINOP(BINOP.MINUS, exp(n.e1), exp(n.e2));
	}
	
	private static Exp exp(Times n) {
		return new BINOP(BINOP.MUL, exp(n.e1), exp(n.e2));
	}
	
	private static Exp exp(NewArray n) {
		return new CALL(new NAME("malloc"),exp(n.e));
	}
	
	private static Exp exp(LessThan n) {
		return null;
	}
	
	private static Exp exp(NewObject n) {
		return new CALL(new NAME("malloc"),new CONST(
					table.map.get(n.i.s).map.size() * 4
				));
	}
	
	private static Exp exp(ArrayLookup n) {
		return new MEM(
				new BINOP(BINOP.MINUS, new TEMP("%fp"),exp(n.indexInArray))
			);
	}
	
	private static Exp exp(ArrayLength n) {
		return new CALL(new NAME("array_len"), exp(n.expressionForArray));
	}
	
	private static Exp exp(IdentifierExp n) {
		temp += 1;
		return new TEMP("'s" + temp);
	}
	
	private static Exp exp(IntegerLiteral n) {
		return new CONST(n.i);
	}
	
	private static List<Exp> exp(List<Expression> n) {
		List<Exp> list = new ArrayList<Exp>();
		for(Expression e:n)
			list.add(exp(e));
		return list;
	}
	
}
