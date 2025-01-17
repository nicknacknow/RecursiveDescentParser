// Run with `java Main.java test`
// Version 2025-01-16
// Changes:
//  2025-01-16 Fix NullPointerException in the Parser.expect() methods

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


public class Main {
    public static boolean iUsedAi() {
        return false;
    }

    public static String aiExplanation() {
        return "i can find data online instead";
    }

    public static void main(String[] args) {
        String fileExtension = ".mod";
        if (args.length != 1) {
            System.out.println("Usage: java Main.java test | $file" + fileExtension);
            System.out.println();
            System.out.println("  Please provide an argument.");
            System.out.println("  The argument can be either test or a file name.");
            System.out.println("   - test: this will run the provided tests");
            System.out.println("   - a " + fileExtension + " file name: this will check whether it is a valid ModuleLang file");
            return;
        }

        String inputFile = args[0];
        if (!inputFile.endsWith(fileExtension) && !inputFile.equals("test")) {
            System.out.println("Unrecognized command or file type: " + inputFile);
            return;
        }

        if (inputFile.equals("test")) {
            test();
            return;
        }

        // looks like inputFile
        try {
            Parser parser = Parser.create(inputFile);
            boolean valid = parser.parse();
            if (valid) {
                System.out.println("The input file is valid.");
            } else {
                System.out.println("The input file is invalid.");
            }
        } catch (IOException ex) {
            System.out.println("Exception parsing: " + inputFile);
            System.out.println(ex);
        }
    }

    private static void test() {
        String[] testNames = new String[]{
                "validLectures",
                "invalidAssessments",
                "validAssessments", "validClasses",
                "validFullExample"};
        String[] input = new String[]{
                Tests.validLectures,
                Tests.invalidAssessments,
                Tests.validAssessments, Tests.validClasses,
                Tests.validFullExample};
        boolean[] expectedResults = new boolean[]{true, false, true, true, true};

        for (int i = 0; i < testNames.length; i += 1) {
            runParserTest(testNames[i], input[i], expectedResults[i]);
        }

        System.out.println("\n");

        try {
            boolean usedAi = iUsedAi();
            System.out.println("I used AI: " + usedAi);
        } catch (RuntimeException ex) {
            System.out.println("Main.iUsedAi() method not yet adapted");
            System.out.println(ex.getMessage());
        }

        try {
            String reasoning = aiExplanation();
            System.out.println("My reasoning: " + reasoning);
        } catch (RuntimeException ex) {
            System.out.println("Main.aiExplanation() method not yet adapted");
            System.out.println(ex.getMessage());
        }
    }

    private static void runParserTest(String name, String input, boolean expectedValid) {
        Parser parser = new Parser(input);
        try {
            boolean actual = parser.parse();
            if (expectedValid == actual) {
                System.out.println("Test " + name + " passed.\tThe input is " + (expectedValid ? "valid" : "invalid") + ", as expected.");
            } else {
                System.out.println("Test " + name + " failed.\tThe input is " + (expectedValid ? "invalid" : "valid") + " but was expected to be " + (expectedValid ? "valid" : "invalid"));
            }
        } catch (Throwable t) {
            System.out.println("Test " + name + " failed with exception: ");
            System.out.print("\t");
            System.out.println(t);
        }
    }
}

class Tests {
    public static final String invalidAssessments = "assessments {}";

    public static final String validAssessments = """
            assessments {
            assessment A1 {
              type = in-class-test;
              title = "Logic Gates";
              weighting = 10%;
              after = [c1];
            }}""";

    public static final String validClasses = """
              classes {
            class c1 {
              title = "Prep for A1";
              groups = 14;
            }}""";

    public static final String validLectures = """
                     lectures {
            lecture L1 {
              title = "Lecture 1";
            }}""";

    public static final String validFullExample = """
            assessments {
            assessment A2 {
              type = in-class-test;
              title = "Hack Assembler";
              weighting = 10%;
              after = [A1, c2];
            }
            }
            
            classes {
              class c1 {
                title = "Prep for A1";
                groups = 14;
              }
            
              class c2 {
                title = "Prep for A2";
                after = [A1, c1];
                groups = 14;
              }
            }
            
            lectures {
              lecture L1 {
                title = "Lecture 1";
              }
            }""";
}

enum TokenType {
    KEYWORD, SYMBOL, IDENTIFIER, STRING, INTEGER
}

class Token {
    public final TokenType type;
    public final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public String toString() {
        return "Token(" + type + ", " + value + ")";
    }
}

class Tokenizer {
    private final List<String> input;

    private int currentLine = 0;
    private int currentChar = 0;

    private Token currentToken = null;

    public Tokenizer(List<String> input) {
        this.input = input;
    }

    public String toString() {
        return "Tokenizer(" + (currentLine + 1) + ", " + (currentChar + 1) + (currentToken != null ? ", " + currentToken : "") + ")";
    }

    public Token getCurrent() {
        return currentToken;
    }

    public Token next() {
        currentToken = nextToken();
        return currentToken;
    }

    private Token nextToken() {
        if (currentLine >= input.size()) {
            return null;
        }

        String line = input.get(currentLine);

        while (currentChar < line.length() && Character.isWhitespace(line.charAt(currentChar))) {
            currentChar += 1;
        }

        if (currentChar >= line.length()) {
            currentLine += 1;
            currentChar = 0;
            return next();
        }

        char c = line.charAt(currentChar);
        if (Character.isDigit(c)) {
            return readInteger(line);
        } else if (Character.isLetter(c) || c == '_') {
            return readIdentifier(line);
        } else if (c == '"') {
            return readString(line);
        } else {
            return readSymbol(line);
        }
    }

    private Token readInteger(String line) {
        String text = "";

        char c = line.charAt(currentChar);
        while (Character.isDigit(c)) {
            text += c;
            currentChar += 1;
            if (currentChar >= line.length()) {
                break;
            }
            c = line.charAt(currentChar);
        }

        return new Token(TokenType.INTEGER, text);
    }

    private Token readIdentifier(String line) {
        String text = "";

        char c = line.charAt(currentChar);
        while (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
            text += c;
            currentChar += 1;
            if (currentChar >= line.length()) {
                break;
            }
            c = line.charAt(currentChar);
        }

        switch (text) {
            case "assessments":
            case "assessment":
            case "classes":
            case "class":
            case "lectures":
            case "lecture":
            case "type":
            case "title":
            case "weighting":
            case "after":
            case "groups":
                return new Token(TokenType.KEYWORD, text);
            default:
                return new Token(TokenType.IDENTIFIER, text);
        }
    }

    private Token readString(String line) {
        String text = "";

        assert line.charAt(currentChar) == '"';
        currentChar += 1;

        char c = line.charAt(currentChar);
        while (c != '"') {
            text += c;
            currentChar += 1;
            if (currentChar >= line.length()) {
                break;
            }
            c = line.charAt(currentChar);
        }

        currentChar += 1;

        return new Token(TokenType.STRING, text);
    }

    private Token readSymbol(String line) {
        char c = line.charAt(currentChar);
        currentChar += 1;

        switch (c) {
            case '{':
            case '}':
            case '=':
            case ';':
            case '[':
            case ']':
            case ',':
            case '%':
                return new Token(TokenType.SYMBOL, "" + c);
        }

        throw new RuntimeException("Unexpected symbol: " + c);
    }
}

class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}

class Parser {
    private final Tokenizer tokenizer;

    public Parser(List<String> input) {
        tokenizer = new Tokenizer(input);
        tokenizer.next();
    }

    public Parser(String string) {
        this(Arrays.asList(string.split("(\n)|(\r\n)|(\r)")));
    }

    public static Parser create(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(fileName));
        return new Parser(lines);
    }

    /**
     * Consume the next token, if and only if it is of the expected type.
     */
    private boolean accept(TokenType type) {
        Token current = tokenizer.getCurrent();
        if (current != null && current.type == type) {
            tokenizer.next();
            return true;
        }
        return false;
    }

    /**
     * Consume the next token, if and only if it is of the expected type and value.
     */
    private boolean accept(TokenType type, String value) {
        Token current = tokenizer.getCurrent();
        if (current != null && current.type == type && current.value.equals(value)) {
            tokenizer.next();
            return true;
        }
        return false;
    }

    /**
     * Consume the next token, if and only if it is of the expected type.
     * Otherwise, throw a ParseException.
     */
    private void expect(TokenType type) throws ParseException {
        if (!accept(type)) {
            Token current = tokenizer.getCurrent();
            if (current == null) {
                throw new ParseException("Expected token of type " + type.name() + " but reached end of file");
            }
            throw new ParseException("Expected token of type " + type.name() + " but got " + current.type.name());
        }
    }

    /**
     * Consume the next token, if and only if it is of the expected type and value.
     * Otherwise, throw a ParseException.
     */
    private void expect(TokenType type, String value) throws ParseException {
        if (!accept(type, value)) {
            Token current = tokenizer.getCurrent();
            if (current == null) {
                throw new ParseException("Expected token of type " + type.name() + " with the text " + value + ", but reached end of file");
            }
            throw new ParseException("Expected token of type " + type.name() + " with the text " + value + ", but got " + current.type.name() + " with the text " + current.value);
        }
    }

    public boolean parse() {
        try {
            parseModule();
            return true;
        } catch (ParseException ex) {
            System.out.println();
            System.out.println(ex.toString());
            return false;
        }
    }

    public void parseLectures() throws ParseException {
        // lectures: 'lectures' '{' lecture+ '}'
        expect(TokenType.KEYWORD, "lectures");
        expect(TokenType.SYMBOL, "{");

        parseLecture();
        
        // how to know if multiple lectures ? (must parse multiple surely?) 
        expect(TokenType.SYMBOL, "}");
    }

    public void parseLecture() throws ParseException {
        // lecture: 'lecture' IDENTIFIER '{' title '}'

        expect(TokenType.KEYWORD, "lecture");
        expect(TokenType.IDENTIFIER); // lecture name
        expect(TokenType.SYMBOL, "{");

        parseTitle();
        
        expect(TokenType.SYMBOL, "}");
    }

    public void parsePercent() throws ParseException {
        // percent: INT '%'

        expect(TokenType.INTEGER);
        expect(TokenType.SYMBOL, "%");
    }
    
    public void parseType() throws ParseException {
        // type: 'type' '=' IDENTIFIER ';'
        
        expect(TokenType.KEYWORD, "type");
        expect(TokenType.SYMBOL, "=");
        expect(TokenType.IDENTIFIER); // type name
        expect(TokenType.SYMBOL, ";");
    }
    
    public void parseTitle() throws ParseException {
        // title: 'title' '=' STRING ';'

        expect(TokenType.KEYWORD, "title");
        expect(TokenType.SYMBOL, "=");
        expect(TokenType.STRING); // title name
        expect(TokenType.SYMBOL, ";");
    }

    public void parseWeighting() throws ParseException {
        // weighting: 'weighting' '=' percent ';'

        expect(TokenType.KEYWORD, "weighting");
        expect(TokenType.SYMBOL, "=");
        
        parsePercent();

        expect(TokenType.SYMBOL, ";");
    }

    public void parseAfter() throws ParseException { // incomplete
        // after: 'after' '=' '[' IDENTIFIER (',' IDENTIFIER)* ']' ';'

        expect(TokenType.KEYWORD, "after");
        expect(TokenType.SYMBOL, "=");
        expect(TokenType.SYMBOL, "[");

        expect(TokenType.IDENTIFIER);
        // what about those brackets ?? weird ? Parentheses group terminal and non-terminal symbols as a single item.
        // look at example program for example .. 
    }

    public void parseGroups() throws ParseException {
        // weighting: 'weighting' '=' percent ';'

        expect(TokenType.KEYWORD, "groups");
        expect(TokenType.SYMBOL, "=");
        expect(TokenType.INTEGER);
        expect(TokenType.SYMBOL, ";");
    }
    
    private void parseModule() throws ParseException {
        // module: assessments? classes? lectures?

        parseLectures();

        // i think use accept for ? condition stuff

        
        //Token t = tokenizer.getCurrent();

        //System.out.println(t.toString());
    }
}