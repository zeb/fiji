package fiji.recorder.rule;

import java.util.regex.Pattern;

import fiji.recorder.RecorderBase.Language;
import fiji.recorder.util.NamedGroupPattern;

import ij.Command;

public class RegexRule extends Rule {
	
	private NamedGroupPattern pattern_command;
	private NamedGroupPattern pattern_class_name;
	private NamedGroupPattern pattern_arguments;
	private NamedGroupPattern pattern_modifiers;
	
	private String description;
	
	private String python_translator;
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public boolean match(Command cmd) {
		boolean match = 
			pattern_command.matcher(cmd.getCommand()).matches()
			&& pattern_class_name.matcher(cmd.getClassName()).matches()
			&& pattern_arguments.matcher(cmd.getArg()).matches()
			&& pattern_modifiers.matcher(String.format("%d", cmd.getModifiers())).matches();
		return match;		
	}
	
	
	public String handle(Command cmd, Language lang) {
		
		String result = null;
		
		switch (lang) {
		case Python:
			result = python_translator;
			break;
		}		

		// command		
		result = pattern_command.replace(cmd.getCommand(), result);
		// class name
		result = pattern_class_name.replace(cmd.getClassName(), result);
		// arguments
		result = pattern_arguments.replace(cmd.getArg(), result);
		// modifiers
		result = pattern_modifiers.replace(String.format("%d", cmd.getModifiers()), result);
		// new lines - i did not find a way to put them in the regex string correctly
		result = Pattern.compile("\\$\\{newline\\}").matcher(result).replaceAll("\n");
		return result;
	}
	
	
	public String toString() {
		return new StringBuffer("Command translator rule: ").append(name).append("\n")
//			.append(description).append("\n")
			.append("\tPriority:\t").append(priority).append("\n")
			.append("\tMatching:\n")
			.append("\t\tCommand:\t").append(getCommand()).append("\n")
			.append("\t\tClass name:\t").append(getClassName()).append("\n")
			.append("\t\tArguments:\t").append(getArguments()).append("\n")
			.append("\t\tMofiers:\t").append(getModifiers()).append("\n")
			.append("\tPython translator:\t").append(python_translator).append("\n")
			.toString();
	}
	

	
	
	/*
	 * SETTERS AND GETTERS
	 */
	
	public String getCommand() {
		return pattern_command.toString();
	}
	public void setCommand(String command) {
		pattern_command = new NamedGroupPattern(command);
	}
	public String getClassName() {
		return pattern_class_name.toString();
	}
	public void setClassName(String className) {
		pattern_class_name = new NamedGroupPattern(className);
	}
	public String getArguments() {
		return pattern_arguments.toString();
	}
	public void setArguments(String args) {
		pattern_arguments = new NamedGroupPattern(args);
	}
	public String getModifiers() {
		return pattern_modifiers.toString();
	}
	public void setModifiers(String modifiers) {
		pattern_modifiers = new NamedGroupPattern(modifiers);
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
	
}
