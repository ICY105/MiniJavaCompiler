package syntax;

import java.util.HashMap;
import java.util.Map;

import syntax.Container.Location;

public class SymbolTable {
	
	final Map<String,Container> map;
	final VerifySyntax vs;

	public SymbolTable (Program p, VerifySyntax vs) {
		this.vs = vs;
		this.map = new HashMap<String,Container>();
		constructTable(p);
	}

	public void print() {
		for (Map.Entry<String,Container> entry : map.entrySet()) {
			System.out.println(entry.getKey() + ":");
			entry.getValue().print("  ");
		}
	}
	
	
	/*
	 * Construct Table code
	 */
	
	private void constructTable(final Program n) {
		if (n==null) {
			//print ("// null Program!!");
		} else if (n.m==null) {
			//print ("// null Main class!!");
		} else {
			constructTable(n.m);
			for (ClassDecl c: n.cl)
				constructTable(c);
		}
	}

	private void constructTable(final MainClass n) {
		Container c = new Container(true,n.i1.s,null,Location.clazz);
		map.put(n.i1.s, c);
	}
	
	private void constructTable(ClassDecl n) {
		if(n instanceof SimpleClassDecl)	constructTable((SimpleClassDecl)n);
		if(n instanceof ExtendingClassDecl)	constructTable((ExtendingClassDecl)n);
	}

	private void constructTable(final SimpleClassDecl n) {
		Container c = new Container(true,n.i.s,null,Location.clazz);
		map.put(n.i.s, c);
		for (FieldDecl v: n.fields)	  constructTable(v,c);
		for (MethodDecl m: n.methods) constructTable(m,c);
	}

	private void constructTable(final ExtendingClassDecl n) {
		Container c = new Container(true,n.i.s,null,Location.clazz);
		map.put(n.i.s, c);
		c.map.put("extends",new Container(false,n.j.s,null,Location.ext));
		for (final FieldDecl v: n.fields)	constructTable(v,c);
		for (final MethodDecl m: n.methods) constructTable(m,c);
	}

	private void constructTable(final MethodDecl n, Container node) {
		Container c = new Container(true,"m:"+n.i.s,Container.convertType(n.t),Location.method);
		node.map.put("m:"+n.i.s, c);
		int i = 0;
		for (final FormalDecl f: n.fl) {
			constructTable(f,c,i);
			i += 1;
		}
		for (final LocalDecl v: n.locals) 
			constructTable(v,c);
	}
	
	private void constructTable(FieldDecl n, Container node) {
		if(node.map.containsKey(n.i.s))
			vs.error(n.i, "duplicate field");
		else
			node.map.put(n.i.s, new Container(false,n.i.s,Container.convertType(n.t),Location.var));
	}
	private void constructTable(LocalDecl n, Container node) {
		if(node.map.containsKey(n.i.s))
			vs.error(n.i, "duplicate field");
		else
			node.map.put(n.i.s, new Container(false,n.i.s,Container.convertType(n.t),Location.var));
	}
	private void constructTable(FormalDecl n, Container node, int i) {
		if(node.map.containsKey(n.i.s))
			vs.error(n.i, "duplicate field");
		else
			node.map.put(n.i.s, new Container(false,n.i.s,Container.convertType(n.t),Location.param,i));
	}
}
