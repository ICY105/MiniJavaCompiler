package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import parser.MiniJava;
import syntax.PrettyPrint;
import syntax.Program;
import syntax.VerifySyntax;
import tree.SEQ;
import tree.TreePrint;

public class Scan {
	
	private static boolean verbose = false;
	private static FileWriter outStream;

	public static void main(String[] args) throws IOException {

		//check command line arguments
		if(args.length > 0 && args[0] instanceof String ) {
			for(String arg: args) {
				if(arg.equals("-v") || arg.equals("-verbose")) {
					verbose = true;
					File outFile = new File("verbose.txt");
					if(!outFile.exists())
						outFile.createNewFile();
					outStream = new FileWriter(outFile);
				}
				if(arg.equals("?")) {
					System.out.println("<input file> <flags>");
					System.out.println("verbose: -v");
					System.exit(0);
				}
			}
			
			//compile program
			File file = new File(args[0]);
			if(!file.exists()) {
				System.err.println("Provide input file as the first command line arguement.");
			} else {
				
				//parse input
				int errors = 0;
				Program p = MiniJava.parseFile(file);
				errors = MiniJava.errorCount();
				
				if(errors == 0) {
					if(verbose) {
						PrettyPrint printer = new PrettyPrint(new PrintWriter(outStream));
						printer.visit(p);
					}
					//verify with syntax tree
					VerifySyntax syntax = new VerifySyntax(p,file);
					errors = syntax.verify();
					
					//generate fragments
					List<SEQ> fragments = syntax.fragment();
					if(verbose)
						for(SEQ fragment: fragments)
							log(TreePrint.toString(fragment));
					
					File output = new File(args[0].replace(".java", ".s"));
					output.createNewFile();
				}
				System.err.flush();
				System.out.println("./" + file.getPath() + ", errors=" + errors);
			}
		} else {
			System.err.println("Provide input file as the first command line arguement.");
		}
		if(outStream != null)
			outStream.close();
	}
	
	//return if in verbose mode
	public static boolean isVerbose() {
		return verbose;
	}
	
	//logs something to the verbose output stream
	public static void log(String m) {
		if(verbose) {
			try {
				outStream.append(m + "\n");
			} catch (IOException e) {}
		}
	}

	//logs something to standard error and verbose output stream
	public static void logError(String m) {
	  	System.err.println(m);
		if(verbose) {
			try {
				outStream.append(m + "\n");
			} catch (IOException e) {}
		}
	}

}
