import ij.QuotedStringTokenizer;
import java.util.NoSuchElementException;

// Program to test QuotedStringTokenizer
// Arguments:
//    -V<n> set debug output level to <n> (default <n>=1)
//    -U<n> set escaping mode to Unix style (<n>=non-zero, default) or simple (<n>=0)
//    -E<c> set escape character to <c>
//    -C<a,b...> set comment strings to {"a", "b"...}, escaping ',' with '\' in the argument string
//    -D<x,y...> set delimiters to {"x","y"...}, escaping ',' with '\' in the argument string
//    string not starting with '-' : print extracted tokens
//
// If no strings are supplied to be tokenized, runs a standard test
//
// Can run from Fiji with command line: fiji --console --main-class QuotedStringTester <args> -batch
//
public class QuotedStringTester
{
	// Test entry
    public static void main(String args[])
    {
		QuotedStringTokenizer qst = new QuotedStringTokenizer();
		int done = 0;
		
		for( int iArg = 0; iArg < args.length; iArg++ )
		{
			if( args[iArg].length() >= 2 && '-' == args[iArg].charAt(0) ) {
				if ('V' == args[iArg].charAt(1))
					qst.debugMode = (args[iArg].length() == 2) ? 1 : Integer.decode(args[iArg].substring(2));
			    else if( 'U' == args[iArg].charAt(1) ) 
					qst.setUnixStyle( (args[iArg].length() == 2 || '0' != args[iArg].charAt(2)) ? true : false );
				else if( 'E' == args[iArg].charAt(1) )
					qst.setEscape( (args[iArg].length() == 2) ? '\0' : args[iArg].charAt(2) );
				else if( 'C' == args[iArg].charAt(1) && args[iArg].length() > 2)
				{
					String[] newDelims = {","};
					char oldEscape=qst.setEscape('\\');
					final String[] oldDelims = qst.setDelims(newDelims);
					newDelims = qst.split(args[iArg].substring(2));
					System.out.println("Setting new comments");
					qst.setComments(newDelims);
					qst.setDelims(oldDelims);
					qst.setEscape(oldEscape);
				}
				else if( 'D' == args[iArg].charAt(1) && args[iArg].length() > 2 )
				{
					String[] newDelims = {","};
					char oldEscape=qst.setEscape('\\');
					qst.setDelims(newDelims);
					newDelims = qst.split(args[iArg].substring(2));
					System.out.println("Setting new delims");
					qst.setDelims(newDelims);
					qst.setEscape(oldEscape);
				}
			}
			else
			{				
				if( qst.debugMode > 0 ) {
					String[] array = qst.getDelims();
					for( int i=0; i<array.length; i++ )
						System.out.println( "delim " + (i+1) + ": '" + array[i] +"'");
					array = qst.getComments();
					for( int i=0; i<array.length; i++ )
						System.out.println( "comment " + (i+1) + ": '" + array[i] +"'");
					System.out.println("escape: '" + qst.getEscape() + "'");
				}
			
				System.out.println( "arg = '" + args[iArg] + "'");
				String token_list[] = qst.split(args[iArg]);			
				for( int i=0; i<token_list.length; i++ )
					System.out.println( (i+1) + ": '" + token_list[i] +"'");
				done = 1;
			}
		}
		if( 0 == done )
			if (!StandardTest(qst)) System.exit(1);
		System.exit(0);
    }
	
	private static boolean StandardTest(QuotedStringTokenizer qst)
	{
		boolean ret = true;
		boolean thisTest = true;
		String t1 = ",tom{ \"and# 'dick, ' \\\"and\" ,  ,,erm}   \\'harry\" or bill\\\\\\\\\\ or \\#someone\\' #else";
		//           0          1         2            3          4          5              6          7          8
		//           012345 678901234567890 1 2345 67890123456789 0123456 789012345 6 7 8 9 01234 567890123 45678901
		// Tests at char positions:
		// 0: leading delimiter gives null string token
		// 1-5: whitespace delimited token
		// 4-36: open-close bracketing delimiters (for test 3)
		// 6-26: bracketing quote marks
		// 10: comment char ignored inside quote
		// 11: delimiter ignored inside bracketing quotes
		// 12-19: bracketing quotes ignored inside other type of quotes
		// 13-4: comment string ignored inside bracketing quotes
		// 22: escaped terminating quote ignored inside bracketing quote
		// 28: bracketing quote condensed with trailing whitespace and delimiter
		// 32: consecutive delimiters give null string token
		// 41,75: escaping otherwise-bracketing delimiter
		// 47: unpaired bracketing delimiter acts as normal char
		// 56-9: consecutive paired escapes condense to non-escaping escape char
		// 61: escaping otherwise-single delimiter
		// 66: escaped comment char ignored
		// 77: start of comment (when comments defined)
		
		System.out.println(t1);
		
		// First split without any comment checking
		thisTest = true;
		String[] result = qst.split(t1);
		String[] c1 = {"",  "tom{",  "and# 'dick, ' \"and",  "",  "", "erm}",  "'harry\"",  "or",  "bill\\\\ or", "\\#someone'", "#else"};
		if( !CheckResult(c1,result) ) thisTest = false;
		else System.out.println("Test1 OK");
		ret = ret && thisTest;
		
		// Add comment checking to split. Should only lose the final token
		thisTest = true;
		String[] m1 = {"#",";","di"};
		qst.setComments(m1);
		result = qst.split(t1);
		String[] c2 = {"",  "tom{",  "and# 'dick, ' \"and",  "",  "", "erm}",  "'harry\"",  "or",  "bill\\\\ or", "#someone'"};
		if( !CheckResult(c2,result) ) thisTest = false;
		else System.out.println("Test2 OK");
		ret = ret && thisTest;
		
		// Test nextToken interface (although also used by internal implementation)
		thisTest = true;
		qst.setText(t1);
		for( int i=0; qst.hasMoreTokens(); i++) {
			String token = qst.nextToken();
			if( !token.equals(c2[i]) ) {
				System.out.println(String.format("nextToken %d returned '%s', expected '%s'", i+1, token, c2[i]));
				thisTest = false;
			}
		}
		if( thisTest ) System.out.println("Test3 OK");
		ret = ret && thisTest;

		// Test alternate delimiter interface, and null escape
		thisTest = true;
		String[] d1 = {" ","{}","\"\"","''"};
		qst.setDelims(d1);
		qst.setEscape('\0');
		String[] c3 = {",tom",  "\"and# 'dick, ' \\\"and\" ,  ,, \"erm",  "\\", "harry\" or bill\\\\\\\\\\ or \\#someone\\"};
		if( !CheckResult(c2,result) ) thisTest = false;
		else System.out.println("Test4 OK");
		qst.setEscape('\\');
		ret = ret && thisTest;
		
		// Test changing delimiters between successive calls to nextToken
		thisTest = true;
		String[][] d2 = {{"#"},
						 {","},
						 {","},
						 {","," "},
						 {" "},
						 {" "},
						 {"#"},
						 {"#"},
						 {"#"}};
		String[] c4 = {",tom{ \"and",
					   " 'dick",
					   " ' \\\"and\" ",
					   "",
					   ",erm}",
					   "\\'harry\"",
					   "or bill\\\\\\ or #someone\\' ",
					   "else"};
		qst.setComments(null);
		qst.setText(t1);
		for( int i=0; true; i++) {
			qst.setDelims(d2[i]);
			if (!qst.hasMoreTokens()) break;
			String token = qst.nextToken();
			if( !token.equals(c4[i]) ) {
				System.out.println(String.format("nextToken %d returned '%s', expected '%s'", i+1, token, c4[i]));
				thisTest = false;
			}
		}
		if( thisTest ) System.out.println("Test5 OK");
		ret = ret && thisTest;

		// Tests for exception throwing by nextToken
		// String has just null delimiters followed by comment - so no token to be found
		thisTest = false;
		qst.setComments(m1);
		qst.setDelims(d1);
		qst.setText("   #some comment");
		boolean excepted = false;
		try {
			qst.nextToken();
		}
		catch (NoSuchElementException e) {
			System.out.println("Test6 OK");
			thisTest = true;
		}
		if( !thisTest ) {
			System.out.println("Test6: Expected exception not thrown");
			ret = false;
		}
		
		thisTest = false;
		qst.setText("   \\#some comment");
		try {
			qst.nextToken();
		}
		catch (NoSuchElementException e) {
			System.out.println("Test7 threw unexpected exception");
			thisTest = true;
		}
		if( !thisTest ) System.out.println("Test7 OK");
		ret = ret && thisTest;
		
		return ret;
	}
	
	// Compare reference string array to test string array. Print any mismatches
	private static boolean CheckResult( String[] ref, String[] test )
	{
		boolean ret = true;
		int len = test.length;
		if( ref.length != len ) {
			System.out.println(String.format("Arrays of different length ref=%d and test=%d", ref.length, len));
			if( ref.length > len ) len = ref.length;
			ret = false;
		}
		for( int i=0; i<len; i++ ) {
		    if( i>=ref.length)
				System.out.println(String.format("Test[%d]='%s' unpaired in ref", i+1, test[i]));
			else if( i>=test.length)
				System.out.println(String.format("Ref[%d]='%s' unpaired in test", i+1, ref[i]));
			else if( !test[i].equals(ref[i]) ) {
				System.out.println(String.format("Ref[%d]='%s' does not match test[%d]='%s'", i+1, ref[i], i+1, test[i]));
				ret = false;
			}
		}
		return ret;
	}
}