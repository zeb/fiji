package fiji.recorder.util;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamedGroupPattern  {
	static Pattern patternSplitter =
		Pattern.compile("\\((<([^>]+)>;)?([^\\)]*)\\)");
	static Pattern replacementSplitter =
		Pattern.compile("\\$\\{([^>}]+)\\}");
	Pattern pattern;
	Map<String, Integer> map;

	public NamedGroupPattern(String pattern) {
		map = new HashMap<String, Integer>();
		StringBuilder builder = new StringBuilder();
		Matcher matcher = patternSplitter.matcher(pattern);
		int offset = 0, group = 0;
		while (matcher.find()) {
			group++;
			if (matcher.start(1) < 0) {
				builder.append(pattern.substring(offset,
							matcher.end()));
				offset = matcher.end();
				continue;
			}
			builder.append(pattern.substring(offset,
					matcher.start() + 1));
			String sub = pattern.substring(matcher.start(3),
					matcher.end());
			if (sub.indexOf('(') >= 0)
				throw new RuntimeException("Nested named groups"
					+ " not yet supported");
			builder.append(sub);
			map.put(pattern.substring(matcher.start(2),
					matcher.end(2)), new Integer(group));
			offset = matcher.end();
		}
		builder.append(pattern.substring(offset));
		this.pattern = Pattern.compile(builder.toString());
	}
	
	public Matcher matcher(String haystack) {
		return pattern.matcher(haystack);
	}
	
	/** returns null if the pattern does not match */
	public String replace(String haystack, String template) {
		// TODO: template can be parsed almost the same as pattern
		Matcher m = pattern.matcher(haystack);
		if (!m.matches())
			return null;

		StringBuilder builder = new StringBuilder();
		Matcher matcher = replacementSplitter.matcher(template);
		int offset = 0;
		while (matcher.find()) {
			String key = template.substring(matcher.start(1),
					matcher.end(1));
			try {
				int group = map.containsKey(key) ?
					map.get(key).intValue() :
					Integer.parseInt(key);
				builder.append(template.substring(offset,
							matcher.start()));
				builder.append(haystack
						.substring(m.start(group),
							m.end(group)));
				offset = matcher.end();
			} catch (NumberFormatException e) {
				// unknown named group; leave it alone
			}
		}
		builder.append(template.substring(offset));

		return builder.toString();
	}
	
	public String toString() {
		return pattern.toString();
	}

	public static void main(String[] args) {
		String pattern = args.length > 0 ? args[0] :
			" *(<first>;.*) (<second>;.*[^ ]) *";
		String haystack = args.length > 1 ? args[1] :
			"  Joe Hacker  ";
		String replacement = args.length > 2 ? args[2] :
			"Is your name ${second}, ${first}???!?";

		NamedGroupPattern p = new NamedGroupPattern(pattern);
		String result = p.replace(haystack, replacement);
		String expect = "Is your name Hacker, Joe???!?";
		if (result == null || !result.equals(expect))
			System.err.println("Does not match: " + result + " != "
				+ expect);
		else
			System.err.println("It worked!");
	}

}
