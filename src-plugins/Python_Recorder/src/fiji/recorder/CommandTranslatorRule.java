package fiji.recorder;

import java.util.Comparator;

import ij.Command;

public class CommandTranslatorRule implements Comparable<CommandTranslatorRule>{
	
	private String command;
	private String class_name;
	private String args;
	private int modifiers;
	
	private String name;
	private String description;
	private int priority;
	
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
	
    /**
     * Compares this rule with another with respect to <b>priority</b>.  
     * Returns a negative integer, zero, or a positive integer as this object 
	 * has a priority less than, equal to, or greater than the specified object.<p>
     *
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 * Equality simply means that priorities are equal. 
     * 
     * @param   o the CommandTranslatorRule to be compared.
     * @return  a negative integer, zero, or a positive integer as this rule
     *		has a priority less than, equal to, or greater than the specified object.
     */
	public int compareTo(CommandTranslatorRule o) {
		return ( priority - o.getPriority() );
	}
	
	/**
	 * Returns a comparator based on the instance method 
	 * {@link CommandTranslatorRule#compareTo(CommandTranslatorRule)}
	 * @return  a comparator for this class
	 */
	public static Comparator<CommandTranslatorRule> getComparator() {
		return new Comparator<CommandTranslatorRule>() {
			public int compare(CommandTranslatorRule o1,
					CommandTranslatorRule o2) {
				return o1.compareTo(o2);
			}
		};
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


	public void setPriority(int priority) {
		this.priority = priority;
	}


	public int getPriority() {
		return priority;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}




	
}
