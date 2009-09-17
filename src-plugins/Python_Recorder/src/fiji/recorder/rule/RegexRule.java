package fiji.recorder.rule;

import ij.Command;

public class RegexRule extends Rule {
	
	private String command;
	private String class_name;
	private String args;
	private int modifiers;
	
	private String name;
	private String description;
	
	private String python_translator;
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public boolean match(Command cmd) {
		boolean match = true;
		match = match && cmd.getCommand().matches(command);
		return match;		
	}
	
	
	
	public String toString() {
		return new StringBuffer("Command translator rule: ").append(name).append("\n")
//			.append(description).append("\n")
			.append("\tPriority:\t").append(priority).append("\n")
			.append("\tMatching:\n")
			.append("\t\tCommand:\t").append(command).append("\n")
			.append("\t\tClass name:\t").append(class_name).append("\n")
			.append("\t\tArguments:\t").append(args).append("\n")
			.append("\t\tMofiers:\t").append(modifiers).append("\n")
			.append("\tPython translator:\t").append(python_translator).append("\n")
			.toString();
	}
	

	
	
	/*
	 * SETTERS AND GETTERS
	 */
	
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getClassName() {
		return class_name;
	}
	public void setClassName(String className) {
		class_name = className;
	}
	public String getArguments() {
		return args;
	}
	public void setArguments(String args) {
		this.args = args;
	}
	public int getModifiers() {
		return modifiers;
	}
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}
	public void setPythonTranslator(String python_translator) {
		this.python_translator = python_translator;
	}
	public String getPythonTranslator() {
		return python_translator;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getDescription() {
		return description;
	}




	public void setName(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}




	
}
