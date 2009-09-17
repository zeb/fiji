package fiji.recorder;

import ij.plugin.PlugIn;
import ij.Command;
import ij.CommandListenerPlus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecorderBase implements CommandListenerPlus, PlugIn {
	/**
	 * Every rule must implement this interface.
	 *
	 * If the rule does not apply, its handle() method must return null.
	 * The idea is that you implement a specific recorder like this:
	 *
	 * public class FooRecorder extends RecorderBase {
	 *	FooRecorder() {
	 *		rules.add(regexRule("^Image_Calculator", ...));
	 *		rules.add(...);
	 *		rules.add(...); // last one must be the catch-all rule
	 *	}
	 * }
	 */
	interface Rule {
		String handle(Command command);
	}

	public static class NotHandledException extends Exception {
		/** Default SUID */
		private static final long serialVersionUID = 1L;

		NotHandledException(String reason) {
			super(reason);
		}
	}

	Collection<Rule> rules;

	public RecorderBase() {
		rules = new ArrayList<Rule>();
	}

	public RecorderBase(Collection<Rule> rules) {
		this.rules = rules;
	}

// It is conceivable that we have a way to specify a location for
// custom rules, based on the language, for operations that are
// implemented in plugins.
//
// This method should go into the constructors, too, which would then
// need to know the language so they can do the job of looking things
// up.

	/**
	 * The principal rule.
	 *
	 * You can use the helper methods to handle the command
	 * appropriately.
	 */
	public String handle(Command command) throws NotHandledException {
		for (Rule rule : rules) {
			String result = rule.handle(command);
			if (result != null)
				return result;
		}
		throw new NotHandledException("Unhandled command: " + command);
	}

	/**
	 * A helper using regular expressions.
	 *
	 * Use this if the rule is very simple and can be expressed as
	 * regular expressions matching the class name, command and arguments.
	 */
	class RegexRule implements Rule {
		Pattern command, arguments, className;
		String outputTemplate; // can be optimized if needed
		List<String> matchGroups;

		RegexRule(String commandRegex, String argumentsRegex,
				String classNameRegex, String outputTemplate) {
			command = compile(commandRegex);
			arguments = compile(argumentsRegex);
			className = compile(classNameRegex);
			this.outputTemplate = outputTemplate;
			matchGroups = new ArrayList<String>();
		}

		Pattern compile(String regex) {
			if (regex == null)
				return null;
			return Pattern.compile(regex);
		}

		boolean matches(Pattern pattern, String string) {
			if (pattern == null)
				return true;
			Matcher matcher = pattern.matcher(string);
			if (!matcher.matches())
				return false;
			for (int i = 0; i < matcher.groupCount(); i++)
				matchGroups.add(matcher.group(i));
			return true;
		}

		/*
		 * If the regexes which are not null match, interpret the
		 * \<n> parts of the outputTemplate as referring to the
		 * matching groups.
		*/
		public String handle(Command command) {
//			matchGroups.removeAll();
			if (!matches(this.command, command.getCommand())
					|| !matches(arguments, command.getArg())
					|| !matches(className,
						command.getClassName()))
				return null;
			StringBuilder result = new StringBuilder();
			int pos = -1, length = outputTemplate.length();
			for (;;) {
				int slash = 0; //outputTemplate.find('\\', pos + 1);
				if (pos < slash) {
					result.append(outputTemplate
						.substring(pos + 1));
					return result.toString();
				}
				result.append(outputTemplate
						.substring(pos + 1, slash));
				if (pos + 1 == length || !Character
						.isDigit(outputTemplate
							.charAt(pos + 1)))
					continue;
				int group = outputTemplate.charAt(++pos) - '0';
				// result.append(groups.get(group));
				pos++;
			}
				
		}
	}

	public void stateChanged(Command command, int state) {
		// TODO Auto-generated method stub
		
	}

	public String commandExecuting(String command) {
		// TODO Auto-generated method stub
		return null;
	}

	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}
