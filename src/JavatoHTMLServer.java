import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;

public class JavatoHTMLServer {
    private String outputFileName = "";

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    //Sets HTML span style
    private static String styleHTML(String s, String color) {
        return "<span style=\"color:" + color + ";\">" + s + "</span>";
    }

    //Used later to tell if portion is block, line. string, or char
    private static boolean[] indicators = new boolean[4];
    private static int block = 0;
    private static int line = 1;
    private static int string = 2;
    private static int character = 3;

    public static int getIndicator(String s, int startIndex) {
        int[] indices = new int[4];
        String[] newIndicator = {"/*", "//", "\"", "\'"};

        int base = -1;
        int count = 0;
        for (int i = 0; i < indices.length; i++) {
            indices[i] = s.indexOf(newIndicator[i], startIndex);
            if (base == -1) {
                base = indices[i];
                count = i;
            }
            else if (base > indices[i] && indices[i] >= 0) {
                base = indices[i];
                count = i;
            }
        }

        //set indicator value at count
        if (count != -1) {
            indicators[count] = true;
        }

        return indices[count];
    }

    //all of the java keywords
    private static HashSet<String> keywords = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default",
            "do", "double", "else", "enum", "extends", "for", "final",
            "finally", "float", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile",
            "while", "true", "false", "null"));

    public int convertToHTML(File file) throws Exception {
        Scanner input = new Scanner(file);
        int count = 0;
        String javaFile = "";
        while (input.hasNext()) {
            javaFile += input.nextLine() + "\n";
        }

        int index;
        int prevIndex = 0;
        while ((index = getIndicator(javaFile, lastIndex)) != -1) {
            if (index > lastIndex) {
                javaFile = convertSyntax(javaFile, lastIndex, index);
                index = lastIndex;
            }
            if (indicators[block])
                javaFile = convertBlock(javaFile, index);
            else if (indicators[line])
                javaFile = convertLine(javaFile, index);
            else if (indicators[string])
                javaFile = convertString(javaFile, index);
            else if (indicators[character])
                javaFile = convertChar(javaFile, index);
            prevIndex = lastIndex;
        }

        if (prevIndex < javaFile.length()) {
            javaFile = convertSyntax(javaFile, prevIndex, javaFile.length());
        }
        javaFile = getHeader() + javaFile + getFooter();

        //Output to file indicated earlier
        try (FileOutputStream out = new FileOutputStream(outputFileName)) {
            out.write(javaFile.getBytes());
        } catch (IOException ex) {
        }
        return count;
    }

    private static String convertSyntax(String s, int startIndex, int last) {
        String beforeSyntax = s.substring(0, startIndex);
        String syntax = s.substring(startIndex, last);

        syntax = convertSyntax(syntax, false);
        String[] splitSyntax = syntax.split("(\\|\\|\\|\\|\\|)|((?<=\\()|(?=\\)))");
        StringBuilder syntaxBuilder = new StringBuilder();
        for (String split : splitSyntax) {
            if (keywords.contains(split)) {
                syntaxBuilder.append(styleHTML(split, "blue"));
            }
            else {
                syntaxBuilder.append(split);
            }
        }
        syntax = syntaxBuilder.toString();

        String afterString = s.substring(last);
        lastIndex = beforeSyntax.length() + syntax.length();
        return beforeSyntax + syntax + afterString;
    }

    private static int lastIndex;

    //HTML syntax stuff
    private static String convertSyntax(String s, boolean isStrLiteral) {
        s = s.replaceAll("&", "|||||&#x26;|||||");
        s = s.replaceAll("<", "|||||&#x3C;|||||");
        s = s.replaceAll(">", "|||||&#x3E;|||||");
        s = s.replaceAll("\"", "|||||&#x22;|||||");
        s = s.replaceAll(" ", "|||||&nbsp;|||||");
        if (!isStrLiteral) {
            s = s.replaceAll("\n", "|||||<br>|||||");
            s = s.replaceAll("\t", "|||||&nbsp;|||||&nbsp;|||||&nbsp;|||||&nbsp;|||||&nbsp;");
        }
        return s;
    }

    //replaces certain characters with HTML counterparts
    private static String convertToHTML(String s, boolean isStrLiteral) {
        s = s.replaceAll("&", "&#x26;");
        s = s.replaceAll("<", "&#x3C;");
        s = s.replaceAll(">", "&#x3E;");
        s = s.replaceAll("\"", "&#x22;");
        s = s.replaceAll(" ", "&nbsp;");
        if (!isStrLiteral) {
            s = s.replaceAll("\n", "<br>");
            s = s.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        return s;
    }

    //converts block to HTML
    private static String convertBlock(String s, int index) {
        indicators[block] = false;
        String beforeBlock = s.substring(0, index);

        int end = s.indexOf("*/", index) + 2;
        String block = s.substring(index, end);
        String afterBlock = s.substring(end);

        block = convertToHTML(block, false);
        block = styleHTML(block, "green");
        lastIndex = beforeBlock.length() + block.length();
        block = beforeBlock + block + afterBlock;
        return block;
    }

    //converts line to HTML
    private static String convertLine(String s, int index) {
        indicators[line] = false;
        String beforeLine = s.substring(0, index);

        String unknownLine = s.substring(index);
        int whereNewLine = unknownLine.indexOf("\n");
        String line = unknownLine.substring(0, whereNewLine);

        line = convertToHTML(line, false);

        String afterLine = unknownLine.substring(whereNewLine + 1); //ignore new line (/n)
        line = styleHTML(line, "green") + "<br>"; //new line (<br>)

        lastIndex = beforeLine.length() + line.length();
        line = beforeLine + line + afterLine;
        return line;
    }

    //converts string to HTML
    private static String convertString(String javaFile, int index) {
        indicators[string] = false;
        String beforeString = javaFile.substring(0, index);
        String literal = javaFile.substring(index);

        int escape = literal.indexOf("\\", 1); //escapes
        int quote = literal.indexOf("\"", 1); //quotes

        //checks for escape sequences and saves string literal to hash map, then return the string if none
        if (escape == -1 || escape > quote) {
            String strLiteral = literal.substring(0, quote + 1);
            String afterLiteral = literal.substring(quote + 1);

            strLiteral = convertToHTML(strLiteral, true);
            strLiteral = styleHTML(strLiteral, "skyblue");
            lastIndex = beforeString.length() + strLiteral.length();
            return beforeString + strLiteral + afterLiteral;
        }

        //if escape character, finds end of escape and returns string
        Stack<Character> escapes = new Stack<>();
        char[] charArray = literal.toCharArray();
        for (int i = 1; i < charArray.length; i++) {
            char ch = charArray[i];
            if (ch == '\"' && escapes.isEmpty()) {
                String strLiteral = literal.substring(0, i + 1);
                strLiteral = convertToHTML(strLiteral, true);
                strLiteral = styleHTML(strLiteral, "skyblue");
                lastIndex = beforeString.length() + strLiteral.length();
                return beforeString + strLiteral + literal.substring(i + 1);
            }
            if (!escapes.isEmpty())
                escapes.pop();
            else if (ch == '\\') {
                escapes.push(ch);
            }
        }
        return "Error - no end of string ";
    }

    //converts char to HTML
    private static String convertChar(String javaFile, int index) {
        indicators[character] = false;
        String beforeChar = javaFile.substring(0, index);
        String literal = javaFile.substring(index);

        String charLiteral;
        if (literal.charAt(1) == '\\') //if escape sequence
            charLiteral = literal.substring(0, 4);
        else
            charLiteral = literal.substring(0, 3);

        String afterChar = javaFile.substring(index + charLiteral.length());

        charLiteral = convertToHTML(charLiteral, true);
        charLiteral = styleHTML(charLiteral, "skyblue");

        lastIndex = beforeChar.length() + charLiteral.length();

        return beforeChar + charLiteral + afterChar;
    }

    private static String getHeader() {
        return "<!DOCTYPE html><html><head lang=\"en\"><meta charset=\"UTF-8\"><title></title></head><body>";
    }

    private static String getFooter() {
        return "</body></html>";
    }
}
