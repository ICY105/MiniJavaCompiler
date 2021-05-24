package syntax;

import java.util.TreeMap;
import java.util.Map;

public class Container {
	
	final Map<String,Container> map;
	final String type;
	final String name;
	final Location loc;
	final int index;
	private boolean assigned;
	
	Container(boolean hasMap, String name, String type, Location loc) {
		if(hasMap)
			map = new TreeMap<String,Container>();
		else
			map = null;
		this.name = name;
		this.type = type;
		this.loc = loc;
		this.assigned = false;
		this.index = -1;
	}
	
	Container(boolean hasMap, String name, String type, Location loc, int index) {
		if(hasMap)
			map = new TreeMap<String,Container>();
		else
			map = null;
		this.name = name;
		this.type = type;
		this.loc = loc;
		this.assigned = false;
		this.index = index;
	}
	
	public String toString() {
		return name;
	}
	
	public void setAssigned() {
		assigned = true;
	}
	
	public boolean getAssigned() {
		return assigned;
	}
	
	public boolean equals(Container c) {
		return this.type == c.type;
	}

	public static String convertType(Type n) {
		if(n instanceof BooleanType)	return "boolean";
		if(n instanceof IdentifierType)	return ((IdentifierType) n).s;
		if(n instanceof IntArrayType)	return "int_array";
		if(n instanceof IntegerType)	return "int";
		if(n instanceof PrimitiveType)	return "primitive";
		if(n instanceof VoidType)		return "void";
		return null;
	}
	
	public enum Location {
		clazz,
		ext,
		method,
		param,
		var;
	}
	
	public void print(String indent) {
		for (Map.Entry<String,Container> entry : this.map.entrySet()) {
			Container next = entry.getValue();
			if(next.map != null) {
				System.out.println(indent + next.type + " " + entry.getKey() + "-" + next.loc.name() + ":");
				next.print(indent + "  ");
			} else {
				System.out.println(indent + next.type + " " + entry.getKey() + "-" + next.loc.name());
			}
		}
	}
}
