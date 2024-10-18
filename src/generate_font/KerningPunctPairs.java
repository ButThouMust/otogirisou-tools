
package generate_font;

public class KerningPunctPairs {

    // yes, the lists here are big enough that I had to put them into their own
    // source code file

    public static String[] getKerningEncodings() {
        // "auto" handle kerning with table file, without manually replacing all
        // instances of [ch1][ch2] in the script with [ch1]<KERN><##>[ch2]
        String encodings[] = {
            "'d", "'m",  "'s",  "'t", "'v", "ac",  "ad", "adj", "af",  "ag",  "aj",
            "ao", "at",  "av",  "Aw", "aw", "aw,", "ay", "ay,", "bj",  "bt",  "ej",
            "Fa", "Fat", "Fe",  "Fo", "Fu", "fo",  "ij", "nj",  "n's", "n't", "of",
            "oj", "OT",  "o'c", "ox", "Po", "pt",  "r,", "rc",  "rd",  "ro",  "roj",
            "Tc", "Te",  "To",  "Va", "Ve", "w,",  "Wa", "Wat", "Wo",  "We",  "xc",
            "xo", "y,",  "Ya",  "Ye", "Yo", "zo",  "17",
            "...T", "...W", "...Wa", "éj", "“j"
        };
        return encodings;
    }

    public static String[][] getKerningHexSequences() {
        final String KERN_LEFT = "<KERN LEFT>";
        final String ONE_PIXEL = "0001";
        final String TWO_PIXELS = "0002";
        final String THREE_PIXELS = "0003";
        final String apostr = "'";

        String hexSequenceStrings[][] = {
            {apostr, KERN_LEFT, ONE_PIXEL, "d"},
            {apostr, KERN_LEFT, ONE_PIXEL, "m"},
            {apostr, KERN_LEFT, ONE_PIXEL, "s"},
            {apostr, KERN_LEFT, ONE_PIXEL, "t"},
            {apostr, KERN_LEFT, ONE_PIXEL, "v"},
            {"a", KERN_LEFT, ONE_PIXEL, "c"},
            {"a", KERN_LEFT, ONE_PIXEL, "d"},
            {"a", KERN_LEFT, ONE_PIXEL, "d", KERN_LEFT, TWO_PIXELS, "j"},
            {"a", KERN_LEFT, ONE_PIXEL, "f"},
            {"a", KERN_LEFT, ONE_PIXEL, "g"},
            {"a", KERN_LEFT, TWO_PIXELS, "j"},

            {"a", KERN_LEFT, ONE_PIXEL, "o"},
            {"a", KERN_LEFT, ONE_PIXEL, "t"},
            {"a", KERN_LEFT, ONE_PIXEL, "v"},
            {"A", KERN_LEFT, ONE_PIXEL, "w"},
            {"a", KERN_LEFT, ONE_PIXEL, "w"},
            {"a", KERN_LEFT, ONE_PIXEL, "w", KERN_LEFT, ONE_PIXEL, ","},
            {"a", KERN_LEFT, ONE_PIXEL, "y"},
            {"a", KERN_LEFT, ONE_PIXEL, "y", KERN_LEFT, ONE_PIXEL, ","},
            {"b", KERN_LEFT, TWO_PIXELS, "j"},
            {"b", KERN_LEFT, ONE_PIXEL, "t"},
            {"e", KERN_LEFT, TWO_PIXELS, "j"},

            {"F", KERN_LEFT, ONE_PIXEL, "a"},
            {"F", KERN_LEFT, ONE_PIXEL, "a", KERN_LEFT, ONE_PIXEL, "t"},
            {"F", KERN_LEFT, ONE_PIXEL, "e"},
            {"F", KERN_LEFT, ONE_PIXEL, "o"},
            {"F", KERN_LEFT, ONE_PIXEL, "u"},
            {"f", KERN_LEFT, ONE_PIXEL, "o"},
            {"i", KERN_LEFT, TWO_PIXELS, "j"},
            {"n", KERN_LEFT, TWO_PIXELS, "j"},
            {"n", KERN_LEFT, ONE_PIXEL, apostr, KERN_LEFT, ONE_PIXEL, "s"},
            {"n", KERN_LEFT, ONE_PIXEL, apostr, KERN_LEFT, ONE_PIXEL, "t"},
            {"o", KERN_LEFT, ONE_PIXEL, "f"},

            {"o", KERN_LEFT, TWO_PIXELS, "j"},
            // {"o", KERN_LEFT, ONE_PIXEL, "t"},
            {"O", KERN_LEFT, ONE_PIXEL, "T"},
            {"o", KERN_LEFT, ONE_PIXEL, apostr, KERN_LEFT, ONE_PIXEL, "c"},
            {"o", KERN_LEFT, ONE_PIXEL, "x"},
            {"P", KERN_LEFT, ONE_PIXEL, "o"},
            {"p", KERN_LEFT, ONE_PIXEL, "t"},
            {"r", KERN_LEFT, ONE_PIXEL, ","},
            {"r", KERN_LEFT, ONE_PIXEL, "c"},
            {"r", KERN_LEFT, ONE_PIXEL, "d"},
            {"r", KERN_LEFT, ONE_PIXEL, "o"},
            {"r", KERN_LEFT, ONE_PIXEL, "o", KERN_LEFT, TWO_PIXELS, "j"},

            {"T", KERN_LEFT, TWO_PIXELS, "c"},
            {"T", KERN_LEFT, ONE_PIXEL, "e"},
            {"T", KERN_LEFT, ONE_PIXEL, "o"},
            {"V", KERN_LEFT, ONE_PIXEL, "a"},
            {"V", KERN_LEFT, ONE_PIXEL, "e"},
            {"w", KERN_LEFT, ONE_PIXEL, ","},
            {"W", KERN_LEFT, ONE_PIXEL, "a"},
            {"W", KERN_LEFT, ONE_PIXEL, "a", KERN_LEFT, ONE_PIXEL, "t"},
            {"W", KERN_LEFT, ONE_PIXEL, "o"},
            {"W", KERN_LEFT, ONE_PIXEL, "e"},
            {"x", KERN_LEFT, ONE_PIXEL, "c"},
            
            {"x", KERN_LEFT, ONE_PIXEL, "o"},
            {"y", KERN_LEFT, ONE_PIXEL, ","},
            {"Y", KERN_LEFT, ONE_PIXEL, "a"},
            {"Y", KERN_LEFT, ONE_PIXEL, "e"},
            {"Y", KERN_LEFT, TWO_PIXELS, "o"},
            {"z", KERN_LEFT, ONE_PIXEL, "o"},
            {"1", KERN_LEFT, TWO_PIXELS, "7"},

            {"...", KERN_LEFT, THREE_PIXELS, "T"},
            {"...", KERN_LEFT, TWO_PIXELS, "W"},
            {"...", KERN_LEFT, TWO_PIXELS, "W", KERN_LEFT, ONE_PIXEL, "a"},
            {"é", KERN_LEFT, TWO_PIXELS, "j"},
            {"“", KERN_LEFT, ONE_PIXEL, "j"},
        };
        return hexSequenceStrings;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static final String AUTO_ADV_STR = "<AUTO ADV 13>";
    public static final String DELAY_STR = "<DELAY 17>";
    public static final String QUES_WAIT = "?";
    public static final String EXCL_WAIT = "!";
    public static final String PERI_WAIT = ".";
    public static final String R_IN_QUOTE_WAIT = "ʼ";
    public static final String R_OUT_QUOTE_WAIT = "”";

    public static final String QUES_NO_WAIT = AUTO_ADV_STR + QUES_WAIT;
    public static final String EXCL_NO_WAIT = AUTO_ADV_STR + EXCL_WAIT;
    public static final String PERI_NO_WAIT = AUTO_ADV_STR + PERI_WAIT;
    public static final String R_IN_QUOTE_NO_WAIT = "'";

    public static String[] getPunctuationEncodings() {
        // generate a series of table file entries that make the script file
        // easier to read while also preventing unnecessary automatic WAITs
        
        // e.g. the string "?!" should only use one WAIT like "<AUTO ADV 13>?!"
        // similar for "?!”", should act like "<AUTO ADV 13>?<AUTO ADV 13>!”"
        // note you only need to do this for the generated BIG endian table file

        // example starting set:  0001=. 0002=! 0003=? 0004=" 0005=<ADV>. 0006=<ADV>! 0007=<ADV>?
        // series should be like: 00050004=." 000610130004=<ADV>!" 000700060004=?!"
        String encodings[] = {
            PERI_WAIT + R_OUT_QUOTE_WAIT,             // ."
            QUES_WAIT + R_OUT_QUOTE_WAIT,             // ?"
            EXCL_WAIT + R_OUT_QUOTE_WAIT,             // !"
            QUES_WAIT + EXCL_WAIT + R_OUT_QUOTE_WAIT, // ?!"
            EXCL_WAIT + EXCL_WAIT + R_OUT_QUOTE_WAIT, // !!"

            AUTO_ADV_STR + PERI_WAIT + R_OUT_QUOTE_WAIT,             // <AUTO ADV 13>."
            AUTO_ADV_STR + QUES_WAIT + R_OUT_QUOTE_WAIT,             // <AUTO ADV 13>?"
            AUTO_ADV_STR + EXCL_WAIT + R_OUT_QUOTE_WAIT,             // <AUTO ADV 13>!"
            AUTO_ADV_STR + QUES_WAIT + EXCL_WAIT + R_OUT_QUOTE_WAIT, // <AUTO ADV 13>?!"

            PERI_WAIT + R_IN_QUOTE_WAIT,            // .'
            QUES_WAIT + R_IN_QUOTE_WAIT,            // ?'
            EXCL_WAIT + R_IN_QUOTE_WAIT,            // !'
            QUES_WAIT + EXCL_WAIT + R_IN_QUOTE_WAIT, // ?!'
            EXCL_WAIT + EXCL_WAIT + R_IN_QUOTE_WAIT, // !!'

            AUTO_ADV_STR + PERI_WAIT + R_IN_QUOTE_WAIT,            // <AUTO ADV 13>.'
            AUTO_ADV_STR + QUES_WAIT + R_IN_QUOTE_WAIT,            // <AUTO ADV 13>?'
            AUTO_ADV_STR + EXCL_WAIT + R_IN_QUOTE_WAIT,            // <AUTO ADV 13>!'
            AUTO_ADV_STR + QUES_WAIT + EXCL_WAIT + R_IN_QUOTE_WAIT, // <AUTO ADV 13>?!'

            PERI_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // .'"
            QUES_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // ?'"
            EXCL_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // !'"
            QUES_WAIT + EXCL_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT, // ?!'"

            AUTO_ADV_STR + PERI_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // <AUTO ADV 13>.'"
            AUTO_ADV_STR + QUES_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // <AUTO ADV 13>?'"
            AUTO_ADV_STR + EXCL_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT,            // <AUTO ADV 13>!'"
            AUTO_ADV_STR + QUES_WAIT + EXCL_WAIT + R_IN_QUOTE_WAIT + R_OUT_QUOTE_WAIT, // <AUTO ADV 13>?!'"

            PERI_WAIT + DELAY_STR,  // .<DELAY 17>
            QUES_WAIT + EXCL_WAIT,  // ?!
            EXCL_WAIT + EXCL_WAIT,  // !!
            
            AUTO_ADV_STR + QUES_WAIT + EXCL_WAIT // <AUTO ADV 13>?!
        };
        return encodings;
    }

    public static String[][] getPunctuationHexSequences() {
        String hexSequenceStrings[][] = {
            {PERI_NO_WAIT, R_OUT_QUOTE_WAIT},               // ."
            {QUES_NO_WAIT, R_OUT_QUOTE_WAIT},               // ?"
            {EXCL_NO_WAIT, R_OUT_QUOTE_WAIT},               // !"
            {QUES_NO_WAIT, EXCL_NO_WAIT, R_OUT_QUOTE_WAIT}, // ?!"
            {EXCL_NO_WAIT, EXCL_NO_WAIT, R_OUT_QUOTE_WAIT}, // !!"

            {PERI_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>."
            {QUES_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>?"
            {EXCL_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>!"
            {QUES_NO_WAIT, EXCL_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT}, // <AUTO ADV 13>?!"

            {PERI_NO_WAIT, R_IN_QUOTE_WAIT},               // .'
            {QUES_NO_WAIT, R_IN_QUOTE_WAIT},               // ?'
            {EXCL_NO_WAIT, R_IN_QUOTE_WAIT},               // !'
            {QUES_NO_WAIT, EXCL_NO_WAIT, R_IN_QUOTE_WAIT}, // ?!'
            {EXCL_NO_WAIT, EXCL_NO_WAIT, R_IN_QUOTE_WAIT}, // !!'

            {PERI_NO_WAIT, R_IN_QUOTE_NO_WAIT},            // <AUTO ADV 13>.'
            {QUES_NO_WAIT, R_IN_QUOTE_NO_WAIT},            // <AUTO ADV 13>?'
            {EXCL_NO_WAIT, R_IN_QUOTE_NO_WAIT},            // <AUTO ADV 13>!'
            {QUES_NO_WAIT, EXCL_WAIT, R_IN_QUOTE_NO_WAIT}, // <AUTO ADV 13>?!'

            {PERI_NO_WAIT, R_IN_QUOTE_NO_WAIT, R_OUT_QUOTE_WAIT},                // .'"
            {QUES_NO_WAIT, R_IN_QUOTE_NO_WAIT, R_OUT_QUOTE_WAIT},                // ?'"
            {EXCL_NO_WAIT, R_IN_QUOTE_NO_WAIT, R_OUT_QUOTE_WAIT},                // !'"
            {QUES_NO_WAIT, EXCL_NO_WAIT, R_IN_QUOTE_NO_WAIT, R_OUT_QUOTE_WAIT}, // ?!'"

            {PERI_NO_WAIT, R_IN_QUOTE_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>.'"
            {QUES_NO_WAIT, R_IN_QUOTE_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>?'"
            {EXCL_NO_WAIT, R_IN_QUOTE_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT},               // <AUTO ADV 13>!'"
            {QUES_NO_WAIT, EXCL_NO_WAIT, R_IN_QUOTE_NO_WAIT, AUTO_ADV_STR, R_OUT_QUOTE_WAIT}, // <AUTO ADV 13>?!'"

            {PERI_NO_WAIT, DELAY_STR}, // .<DELAY 17>
            {QUES_NO_WAIT, EXCL_WAIT}, // ?!
            {EXCL_NO_WAIT, EXCL_WAIT}, // !!
            
            {QUES_NO_WAIT, EXCL_NO_WAIT} // <AUTO ADV 13>?!
        };
        return hexSequenceStrings;
    }
}
