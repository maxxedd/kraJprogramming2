/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.finalproject;

/**
 *
 * @author Cloy
 */

import java.util.*;
import java.util.regex.*;


public class krajToken {
    private String code;

    public krajToken(String code) {
        this.code = code;
    }

    // ============================================================
    // TOKEN CLASS
    // ============================================================
    public static class Token {
        public String type;
        public String value;
        public int lineNumber;

        public Token(String type, String value, int lineNumber) {
            this.type = type;
            this.value = value;
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return "[" + type + " : " + value + " (Line " + lineNumber + ")]";
        }
    }

    // New Regex patterns for simplification
    private static final String KEYWORDS = "begin|close|disp|for|stop|while|do|until|if|else|elif|string|nmbr|fort|deci|input|endif";
    private static final String OPERATORS = "\\+|-|\\*|/|=|<=|>=|<|>";

    // ============================================================
    // MAIN TOKENIZER ENTRY
    // ============================================================
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        String[] lines = this.code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            // Remove comments before tokenizing
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }
            
            line = line.trim();
            if (line.isEmpty()) continue; // Skip empty lines

            try {
                // The main line is tokenized using regex
                tokenizeLine(line, lineNumber, tokens);
            } catch (Exception e) {
                tokens.add(new Token("ERROR", "Exception during tokenizing: " + e.getMessage(), lineNumber));
            }
        }

        return tokens;
    }

    // ============================================================
    // LINE TOKENIZATION
    // ============================================================
    private void tokenizeLine(String line, int lineNumber, List<Token> tokens) {

        // Regex to capture the next token:
        // 1. Double/Single quoted strings (STRING)
        // 2. Numbers (NUMBER)
        // 3. Operators (OPERATOR) - Note: = is an operator AND assignment
        // 4. Keywords (KEYWORD)
        // 5. Identifiers (IDENTIFIER)
        String pattern = "("
                + "'[^']*'"               // 1. Single-quoted String
                + "|\"[^\"]*\""           // 1. Double-quoted String
                + "|\\b" + KEYWORDS + "\\b" // 4. Keywords
                + "|(?:" + OPERATORS + ")"  // 3. Operators
                + "|[a-zA-Z][a-zA-Z0-9_]*" // 5. Identifiers
                + "|[0-9]+\\.[0-9]{1,9}"    // 2. Float (Deci) - Up to 9 decimal places
                + "|[0-9]+"               // 2. Integer (Nmbr)
                + ")";

        Pattern tokenPattern = Pattern.compile(pattern);
        Matcher matcher = tokenPattern.matcher(line);

        while (matcher.find()) {
            String value = matcher.group().trim();

            if (value.isEmpty()) continue;

            String type = getTokenType(value);
            tokens.add(new Token(type, value, lineNumber));
        }
    }

    // ============================================================
    // TOKEN TYPE CLASSIFICATION - UPDATED FOR ENDIF
    // ============================================================
    private String getTokenType(String value) {
        // String Literals
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
            return "STRING_LITERAL";

        // Boolean Literals
        if (value.equals("TRUE") || value.equals("FALSE"))
            return "BOOL_LITERAL";

        // Data Type Keywords
        if (value.equals("string") || value.equals("nmbr") || value.equals("fort") || value.equals("deci"))
            return "DATATYPE";

        // Control Flow Keywords
        switch (value) {
            case "begin": return "START";
            case "close": return "END";
            case "disp": return "DISP";
            case "input": return "INPUT";
            case "for": return "FOR";
            case "stop": return "STOP";
            case "while": return "WHILE";
            case "do": return "DO";
            case "until": return "UNTIL";
            case "if": return "IF";
            case "else": return "ELSE";
            case "elif": return "ELIF";
            case "times": return "TIMES";
            case "endif": return "ENDIF"; // ADDED
        }

        // Operators
        if (value.matches(OPERATORS)) {
            if (value.equals("=")) return "ASSIGNMENT";
            return "OPERATOR";
        }

        // Numbers
        if (value.matches("[0-9]+\\.[0-9]{1,9}")) return "DECI_LITERAL";
        if (value.matches("[0-9]+")) return "NMBR_LITERAL";

        // Identifier
        if (value.matches("[a-zA-Z][a-zA-Z0-9_]*")) return "IDENTIFIER";

        return "UNKNOWN";
    }
}