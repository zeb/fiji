
package JMathLib_Interpreter;

import ij.IJ;

import common.RefreshScripts;
import jmathlib.core.interpreter.Interpreter;
import jmathlib.core.interfaces.JMathLibOutput;

import java.io.*;

public class Refresh_JMathLib_Scripts extends RefreshScripts 
			implements JMathLibOutput {
	
	private DataInputStream input;
	private boolean interactiveMode = true;
	private String functionCode = "";
	private boolean exiting = false ;
	Interpreter interpreter;
	
	final protected ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
	final protected BufferedOutputStream out = new BufferedOutputStream(byte_out);
	final protected PrintWriter print_out = new PrintWriter(out);

	public void run(String arg) {
		setLanguageProperties(".m","Matlab");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String filename) {
		interpreter = new Interpreter(true);
		interpreter.setOutputPanel(this);
		//interpreter.executeExpression("startup");		
		input = new DataInputStream(filename);
		displayPrompt();
        	while(!exiting) {
       		
            		String command = readLine();
           		interpretLine(command);    
        	}

	}

		

	private String readLine() {
        	try {
        		return input.readLine();
        	}
        	catch(IOException error) {
        		System.out.println("IO Exception");
        	}
        	return "";
    	}

	public void interpretLine(String line) {
    		if(interactiveMode) {    
			//check to see if this is the beginning of a user function
        		if(line.length() > 7 && line.substring(0, 8).equalsIgnoreCase("FUNCTION")) {
        			functionCode = line;
        			interactiveMode = false;	
		
				displayPrompt();
        		}
        		else {
                    		String answerString = interpreter.executeExpression(line);
            
                    		displayText(answerString);
                    		displayPrompt();		        
	        	}
	   	}
	   	else {   		
	   		if(line.equalsIgnoreCase("END")) {
		   		String answerString = "";
	   			//process the function
	   			//answerString = interpreter.readFunction(functionCode, true, false);
	   			
	   			interactiveMode = true;

		        	displayText(answerString);
	   		}		    
	   		else
		   		functionCode += line;
	   		
	        displayPrompt();
	   	}
    	}
	
	public void displayText(String text) {
		print_out.println(text);
	}

	public void displayPrompt() {
        	print_out.print("> ");
    	}

	public void setStatusText(String text) {
		print_out.println(text);
	}
}
