package fiji.recorder.util.regex2;


public class Test {

    public static void main(String[] args) {
        Pattern patUrl = Pattern.compile("(?<login>\\w+) (?<id>\\d+)");
        Matcher matcher = patUrl.matcher("TEST 222");
        if (matcher.find() && matcher.groupCount() > 0) {
            System.out.println("groups count : "+matcher.groupCount());
            System.out.println("group 1 value : "+matcher.group(1));
            System.out.println("group 1 value : "+matcher.name(1));
            if(matcher.groupCount() > 1){
                System.out.println("group 2 value : "+matcher.group(2));
                System.out.println("group 2 value : "+matcher.name(2));
            }

            /// perform replace
            System.out.println("RESULT_NUMBR="+matcher.replaceAll("aaaaa_$1_sssss_$2____"));
            System.out.println("RESULT_NAMED="+matcher.replaceAll("aaaaa_${id}_sssss_${login}____"));

        }
    }
}

