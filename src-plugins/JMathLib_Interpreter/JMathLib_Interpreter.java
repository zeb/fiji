import common.AbstractInterpreter;

import ij.IJ;

// Required ImageJ and Java imports
import jmathlib.core.interpreter.Interpreter;
import jmathlib.core.interfaces.JMathLibOutput;

public class JMathLib_Interpreter extends AbstractInterpreter
		implements JMathLibOutput {

	Interpreter runtime;

	/*
	 * This method is based off the interpretLine(String line) method found
	 * in the JMathLib source file:
	 * jmathlib/ui/awt/GUI.java
	 */
	protected String eval(String text) throws Throwable {
		String result = runtime.executeExpression(text);
		return result;
	}

	/*
	 * For now, this method is similar to the run method in the JRuby 
	 * interpreter.
	 * Subject to change.
	 */
	public void run(String ignored) {
		super.run(ignored);
		setTitle("JMathLib Interpreter");
		print_out.println("Starting JMathLib... ");
		runtime = new Interpreter(true);
		runtime.setOutputPanel(this);
		print_out.println("Done.");

		//runtime.executeExpression(getStartupScript());
	}

	protected String getLineCommentMark() {
		return "#";
	}

	// JMathLibOutput
	public void displayText(String text) {
		print_out.println(text);
	}

	public void setStatusText(String text) {
		print_out.println(text);
	}
}
