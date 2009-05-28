import ij.IJ;

import common.RefreshScripts;

import java.io.File;

import jmathlib.core.functions.FileFunctionLoader;
import jmathlib.core.functions.WebFunctionLoader;

import jmathlib.core.interpreter.Interpreter;
import jmathlib.core.interfaces.JMathLibOutput;

public class Refresh_JMathLib_Scripts extends RefreshScripts 
			implements JMathLibOutput {
	Interpreter interpreter;
	
	public void run(String arg) {
		setLanguageProperties(".m", "Matlab");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String filename) {
		interpreter = new Interpreter(false);
		interpreter.setOutputPanel(this);
interpreter.executeExpression("cos(1)");
		File file = new File(filename);
/*
		interpreter.globals.getFunctionManager().addFunctionLoader(new
			FunctionLoader(file.getParentFile());
*/
		String name = file.getName();
		if (name.endsWith(".m"))
			name = name.substring(0, name.length() - 2);
System.err.println("executing " + name + "();");
		interpreter.executeExpression(name + "()");
	}
	
	public void displayText(String text) {
		IJ.write(text);
	}

	public void displayPrompt() {
		IJ.write("> ");
    	}

	public void setStatusText(String text) {
		IJ.showStatus(text);
	}

	class FunctionLoader extends WebFunctionLoader {
		FileFunctionLoader realLoader;

		public FunctionLoader(File directory) {
			super();
		}
	}
}
