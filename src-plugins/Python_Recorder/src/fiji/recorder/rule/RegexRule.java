package fiji.recorder.rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fiji.recorder.RecorderBase.Language;

import ij.Command;

public class RegexRule extends Rule {
	
	private Pattern pattern_command;
	private Pattern pattern_class_name;
	private Pattern pattern_arguments;
	private Pattern pattern_modifiers;
	
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
		// Create matchers
		Matcher matcher_command = 		pattern_command.matcher(cmd.getCommand());
		Matcher matcher_class_name =	pattern_class_name.matcher(cmd.getClassName());
		Matcher matcher_arguments = 		pattern_arguments.matcher(cmd.getArg());
		Matcher matcher_modifiers = 		pattern_modifiers.matcher(String.format("%d", cmd.getModifiers()));
		
		String result = null;
		
		switch (lang) {
		case Python:
			// command
			String command_template = Pattern.compile("<command.(\\d+)>").matcher(python_translator).replaceAll("\\$$1");
			result = matcher_command.replaceAll(command_template);
			// class name
			String class_name_template = Pattern.compile("<class_name.(\\d+)>").matcher(result).replaceAll("\\$$1");
			result = matcher_class_name.replaceAll(class_name_template);
			// arguments
			String arguments_template  = Pattern.compile("<arguments.(\\d+)>").matcher(result).replaceAll("\\$$1");
			result = matcher_arguments.replaceAll(arguments_template);
			// modifiers
			String modifiers_template  = Pattern.compile("<modifiers.(\\d+)>").matcher(result).replaceAll("\\$$1");
			result = matcher_modifiers.replaceAll(modifiers_template);
			// new lines - i did not find a way to put them in the regex string correctly
			result = Pattern.compile("<newline>").matcher(result).replaceAll("\n");
			System.out.println(result);
		}		
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
		pattern_command = Pattern.compile(command);
	}
	public String getClassName() {
		return pattern_class_name.toString();
	}
	public void setClassName(String className) {
		pattern_class_name = Pattern.compile(className);
	}
	public String getArguments() {
		return pattern_arguments.toString();
	}
	public void setArguments(String args) {
		pattern_arguments = Pattern.compile(args);
	}
	public String getModifiers() {
		return pattern_modifiers.toString();
	}
	public void setModifiers(String modifiers) {
		pattern_modifiers = Pattern.compile(modifiers);
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
