package fiji.recorder.rule;

import java.util.Comparator;

import fiji.recorder.RecorderBase.Language;

import ij.Command;

public abstract class  Rule implements Comparable<Rule> {
	
	/**
	 * The priority of this rule.
	 */
	protected int priority;
	/**
	 * A descriptive name for this rule.
	 */
	protected String name;
	
	/*
	 * ABSTRACT METHODS
	 */
	
	/**
	 * Returns true if the given Command can be handled by this rule. 
	 * @param command  the Command to check
	 * @return  true if handled by this rule
	 */
	public abstract boolean match(Command command);
	
	/**
	 * Returns a String, that is the translation of the caught command,
	 * suitable for the language given in argument. If the command does not
	 * match this Rule (<i>i.e.</i> <tt>match(cmd)</tt> returns false), this 
	 * command returns a null String. If the cammand match this rule, but the
	 * language asked for is not yet implemented, this method also returns a 
	 * null String. 
	 */
	public abstract String handle(Command cmd, Language lang);
	
	/*
	 * METHODS
	 */
	
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
	public int compareTo(Rule o) {
		return ( priority - o.getPriority() );
	}
	
	/**
	 * Returns a comparator based on the instance method 
	 * {@link RegexRule#compareTo(RegexRule)}
	 * @return  a comparator for this class
	 */
	public static Comparator<Rule> getComparator() {
		return new Comparator<Rule>() {
			public int compare(Rule o1,	Rule o2) {
				return o1.compareTo(o2);
			}
		};
	}
	
	/**
	 * Returns a reversed comparator that will effectively sort rules by decreasing
	 * priority. 
	 * 
	 * @return  a comparator for this class
	 */
	public static Comparator<Rule> getReversedComparator() {
		return new Comparator<Rule>() {
			public int compare(Rule o1,	Rule o2) {
				return (-o1.compareTo(o2));
			}
		};
	}
	
	/*
	 * GETTER AND SETTERS
	 */
	
	/**
	 * Return the priority of this rule. 0 is the lowest and should be reserved to the
	 * default rule that prints a comment in any Recorder. In case a Command is matched
	 * by multiple Rule objects, the one with the highest priority will be used to generate
	 * the string.
	 *  
	 * @return the priority of this rule
	 */
	public int getPriority() {
		return priority;
	}
	
	public void setPriority(int _priority) {
		this.priority = _priority;
	}
	
	public void setName(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}



}
