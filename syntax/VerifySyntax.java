package syntax;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.Scan;
import syntax.Container.Location;
import tree.SEQ;

public class VerifySyntax {

	private final Program p;
	private final File file;
	private SymbolTable table;
	private int errors;

	public VerifySyntax(Program p, File file) {
		errors = 0;
		this.p = p;
		this.file = file;
	}

	public int verify() {
		this.table = new SymbolTable(p,this);
		verifyTable(p);
		return errors;
	}
	
	public List<SEQ> fragment() {
		List<SEQ> out = new ArrayList<SEQ>();
		fragment(out,p);
		return out;
	}
	
	private void fragment(final List<SEQ> out, final Program n) {
		if (n==null || n.m==null) {
			//null program
		} else {
			try {
				out.add(Fragments.createFragment(table, n.m));
			} catch(Exception e) {
				Scan.logError("Failed to generate a fragment " + n.m.i1.s + ":main.");
			}
			for (ClassDecl c: n.cl) {
				for(MethodDecl m: c.methods) {
					try {
						out.add(Fragments.createFragment(table,c,m));
					} catch(Exception e) {
						Scan.logError("Failed to generate a fragment " + c.i.s + ":" + m.i.s + ".");
						e.printStackTrace();
					}
				}
			}
		}
	}

	/*
	 * Verify Table code
	 */	
	private void error(String m) {
		Scan.logError(file.getPath() + ": ERROR -- " + m);
	}
	
	public String error(Expression ex, String m) {
		errors += 1;
		if(ex != null)
			Scan.logError(file.getPath() + ":" + ex.lineNumber + "." + ex.columnNumber + ": ERROR -- " + m);
		else
			error(m);
		return "";
	}
	
	public String error(Statement st, String m) {
		errors += 1;
		if(st != null)
			Scan.logError(file.getPath() + ":" + st.lineNumber + "." + st.columnNumber + ": ERROR -- " + m);
		else
			error(m);
		return "";
	}
	
	public String error(Identifier id, String m) {
		errors += 1;
		if(id != null)
			Scan.logError(file.getPath() + ":" + id.lineNumber + "." + id.columnNumber + ": ERROR -- " + m);
		else
			error(m);
		return "";
	}
	
	private void verifyTable(final Program n) {
		if (n==null) {
			//print ("// null Program!!");
		} else if (n.m==null) {
			//print ("// null Main class!!");
		} else {
			verifyTable(n.m);
			for (ClassDecl c: n.cl)
				verifyTable(c);
		}
	}

	private void verifyTable(final MainClass n) {
		verifyTable(n.s, null, null);
	}
	
	private void verifyTable(ClassDecl n) {
		if(n instanceof SimpleClassDecl)	verifyTable((SimpleClassDecl)n);
		if(n instanceof ExtendingClassDecl)	verifyTable((ExtendingClassDecl)n);
	}

	private void verifyTable(final SimpleClassDecl n) {
		Container classCont = table.map.get(n.i.s);
		for (MethodDecl m: n.methods) verifyTable(m,classCont);
	}

	private void verifyTable(final ExtendingClassDecl n) {
		Container classCont = table.map.get(n.i.s);
		if(!table.map.containsKey(n.j.s))
			error(n.j,"unknown inherited class");
		for (final MethodDecl m: n.methods) verifyTable(m,classCont);
	}

	private void verifyTable(final MethodDecl n, Container classCont) {
		Container metCont = classCont.map.get("m:"+n.i.s);
		for (final Statement s: n.sl) 
			verifyTable(s,classCont,metCont);
		if(!verifyTable(n.e,classCont,metCont).equals(Container.convertType(n.t)))
			error(n.e,"incorrect return type, expected " + n.t);
	}

	private void verifyTable(final Statement n, Container classCont, Container metCont) {
		if(n instanceof If)				verifyTable((If)n, classCont, metCont);
		if(n instanceof Block)			verifyTable((Block)n, classCont, metCont);
		if(n instanceof While)			verifyTable((While)n, classCont, metCont);
		if(n instanceof Print)			verifyTable((Print)n, classCont, metCont);
		if(n instanceof Assign)			verifyTable((Assign)n, classCont, metCont);
		if(n instanceof ArrayAssign)	verifyTable((ArrayAssign)n, classCont, metCont);
	}
	
	private void verifyTable(final Block n, Container classCont, Container metCont) {
		for (Statement s: n.sl)
			verifyTable(s, classCont, metCont);
	}

	private void verifyTable(final If n, Container classCont, Container metCont) {
		if(!verifyTable(n.e,classCont,metCont).equals("boolean"))
			error(n.e,"expected boolean");
		verifyTable(n.s1, classCont, metCont);
		verifyTable(n.s2, classCont, metCont);
	}

	private void verifyTable(final While n, Container classCont, Container metCont) {
		if(!verifyTable(n.e,classCont,metCont).equals("boolean"))
			error(n.e,"expected boolean");
		verifyTable(n.s, classCont, metCont);
	}

	private void verifyTable(final Print n, Container classCont, Container metCont) {
		verifyTable(n.e,classCont,metCont);
	}

	private void verifyTable(final Assign n, Container classCont, Container metCont) {
		String type = verifyTable(n.e,classCont,metCont);
		if(metCont.map.containsKey(n.i.s)) {
			Container v = metCont.map.get(n.i.s);
			if(v.type.equals(type))
				v.setAssigned();
			else
				error(n.e,"expected " + v.type);
			
		} else if(classCont.map.containsKey(n.i.s)) {
			Container v = classCont.map.get(n.i.s);
			if(v.type.equals(type))
				v.setAssigned();
			else
				error(n.e,"expected type " + v.type);
			
		} else if(classCont.map.containsKey("extends")) {
			Container clazz = classCont;
			do {
				clazz = table.map.get(clazz.map.get("extends").name);
				if(clazz.map != null && clazz.map.containsKey(n.i.s)) {
					if(clazz.map.get(n.i.s).type.equals(type))
						clazz.map.get(n.i.s).setAssigned();
					break;
				}
			} while(clazz.map != null && clazz.map.containsKey("extends"));
			if(clazz.map == null)
				error(n,"variable not declared");
		} else {
			error(n,"variable not declared");
		}
	}

	private void verifyTable(ArrayAssign n, Container classCont, Container metCont) {
		if(!verifyTable(n.indexInArray,classCont,metCont).equals("int"))
			error(n.indexInArray,"expected int");
		if(!verifyTable(n.e,classCont,metCont).equals("int"))
			error(n.e,"expected int");
	}
	
	private String verifyTable(final Expression n, Container classCont, Container metCont) {
		if(n instanceof And)			return verifyTable((And)n,classCont,metCont);
		if(n instanceof Not)			return verifyTable((Not)n,classCont,metCont);
		if(n instanceof This)			return verifyTable((This)n,classCont,metCont);
		if(n instanceof Plus)			return verifyTable((Plus)n,classCont,metCont);
		if(n instanceof Call)			return verifyTable((Call)n,classCont,metCont);
		if(n instanceof True)			return verifyTable((True)n,classCont,metCont);
		if(n instanceof False)			return verifyTable((False)n,classCont,metCont);
		if(n instanceof Minus)			return verifyTable((Minus)n,classCont,metCont);
		if(n instanceof Times)			return verifyTable((Times)n,classCont,metCont);
		if(n instanceof NewArray)		return verifyTable((NewArray)n,classCont,metCont);
		if(n instanceof LessThan)		return verifyTable((LessThan)n,classCont,metCont);
		if(n instanceof NewObject)		return verifyTable((NewObject)n,classCont,metCont);
		if(n instanceof ArrayLookup)	return verifyTable((ArrayLookup)n,classCont,metCont);
		if(n instanceof ArrayLength)	return verifyTable((ArrayLength)n,classCont,metCont);
		if(n instanceof IdentifierExp)	return verifyTable((IdentifierExp)n,classCont,metCont);
		if(n instanceof IntegerLiteral)	return verifyTable((IntegerLiteral)n,classCont,metCont);
		return "";
	}
	
	private String verifyTable(final Plus n, Container classCont, Container metCont) {
		if(!verifyTable(n.e1,classCont,metCont).equals("int"))
			return error(n.e1, "expected int");
		else if(!verifyTable(n.e2,classCont,metCont).equals("int"))
			return error(n.e2, "expected int");
		else
			return Container.convertType(Type.THE_INTEGER_TYPE);
	}
	
	private String verifyTable(final Minus n, Container classCont, Container metCont) {
		if(!verifyTable(n.e1,classCont,metCont).equals("int"))
			return error(n.e1, "expected int");
		else if(!verifyTable(n.e2,classCont,metCont).equals("int"))
			return error(n.e2, "expected int");
		else
			return Container.convertType(Type.THE_INTEGER_TYPE);
	}
	
	private String verifyTable(final Times n, Container classCont, Container metCont) {
		if(!verifyTable(n.e1,classCont,metCont).equals("int"))
			return error(n.e1, "expected int");
		else if(!verifyTable(n.e2,classCont,metCont).equals("int"))
			return error(n.e2, "expected int");
		else
			return Container.convertType(Type.THE_INTEGER_TYPE);
	}
	
	private String verifyTable(Call n, Container classCont, Container metCont) {
		String type = verifyTable(n.e,classCont,metCont);
		if(type != null) {
			Container clazz = table.map.get(type);
			if(clazz != null) {
				Container met = null;
				String func = "m:"+n.i.s;
				
				if(clazz.map.containsKey(func)) {
					met = clazz.map.get(func);
				} else if(clazz.map.containsKey("extends")) {
					Container clazz2 = clazz;
					do {
						clazz2 = table.map.get(clazz2.map.get("extends").name);
						if(clazz2.map != null && clazz2.map.containsKey(func)) {
							met = clazz2.map.get(func);
							break;
						}
					} while(clazz2.map != null && clazz2.map.containsKey("extends"));
				}
				
				if(met != null && met.map != null) {
					int i = 0;
					for (Map.Entry<String,Container> entry : met.map.entrySet()) {
						if(entry.getValue().loc == Location.param) {
							if(n.el.size() <= entry.getValue().index)
								error(n,"missing arguement of type " + entry.getValue().type);
							else if(!verifyTable(n.el.get(entry.getValue().index),classCont,metCont).equals(entry.getValue().type)) {
								Container clazz2 = table.map.get(verifyTable(n.el.get(entry.getValue().index),classCont,metCont));
								if(clazz2.map.containsKey("extends")) {
									do {
										clazz2 = table.map.get(clazz2.map.get("extends").name);
										if(clazz2.name.equals(entry.getValue().type)) {
											break;
										}
									} while(clazz2.map != null && clazz2.map.containsKey("extends"));
									if(clazz2.map == null)
										error(n,"wrong type, expected " + entry.getValue().type);
								} else {
									error(n,"wrong type, expected " + entry.getValue().type);
								}
							}
							i += 1;
						}
					}
					while(i < n.el.size()) {
						error(n,"extra arguement");
						i += 1;
					}
					return met.type;
				}
			}
		} 
		error(n, "unknown method");
		return "";
	}
	
	private String verifyTable(IdentifierExp n, Container classCont, Container metCont) { 
		if(metCont.map.containsKey(n.s)) {
			return metCont.map.get(n.s).type;
		} else if(classCont.map.containsKey(n.s)) {
			return classCont.map.get(n.s).type;
		} else if(classCont.map.containsKey("extends")) {
			Container clazz = classCont;
			do {
				clazz = table.map.get(clazz.map.get("extends").name);
				if(clazz.map != null && clazz.map.containsKey(n.s)) {
					return clazz.map.get(n.s).type;
				}
			} while(clazz.map != null && clazz.map.containsKey("extends"));
			if(clazz.map == null)
				error(n,"undefined variable");
		}
		error(n,"undefined variable");
		return "";
	}
	
	private String verifyTable(final ArrayLookup n, Container classCont, Container metCont) {
		if(!verifyTable(n.indexInArray, classCont, metCont).equals("int"))
			error(n.indexInArray,"expected int");
		verifyTable(n.expressionForArray, classCont, metCont);
		return Container.convertType(Type.THE_INTEGER_TYPE); 
	}
	
	private String verifyTable(NewObject n, Container classCont, Container metCont) { 
		if(!table.map.containsKey(n.i.s))
			error(n.i,"unkown object type");
		return n.i.s;
	}
	
	private String verifyTable(final ArrayLength n, Container classCont, Container metCont) {
		if(!verifyTable(n.expressionForArray, classCont, metCont).equals("int_array"))
			error(n,"wrong type, expected int array");
		return Container.convertType(Type.THE_INTEGER_TYPE);
	}

	private String verifyTable(True n, Container classCont, Container metCont) { return Container.convertType(Type.THE_BOOLEAN_TYPE); }
	private String verifyTable(False n, Container classCont, Container metCont) { return Container.convertType(Type.THE_BOOLEAN_TYPE); }
	private String verifyTable(Not n, Container classCont, Container metCont) { return Container.convertType(Type.THE_BOOLEAN_TYPE); }
	private String verifyTable(final And n, Container classCont, Container metCont) { return Container.convertType(Type.THE_BOOLEAN_TYPE); }
	private String verifyTable(final LessThan n, Container classCont, Container metCont) { return Container.convertType(Type.THE_BOOLEAN_TYPE); }
	
	private String verifyTable(IntegerLiteral n, Container classCont, Container metCont) { return Container.convertType(Type.THE_INTEGER_TYPE); }
	private String verifyTable(NewArray n, Container classCont, Container metCont) { return Container.convertType(Type.THE_INT_ARRAY_TYPE); }
	
	private String verifyTable(This n, Container classCont, Container metCont) { return classCont.name; }
	
}
