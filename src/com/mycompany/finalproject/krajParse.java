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

public class krajParse {
    // ============================================================
    // MAIN PARSE FUNCTION
    // Cleans input and validates block structure
    // ============================================================
    public List<String> parse(String code) throws ParserException {

        List<String> lines = preprocess(code);

        // Check program boundaries
        if (lines.isEmpty() || !lines.get(0).equalsIgnoreCase("begin"))
            throw new ParserException("Program must start with: begin", 1, "Add 'begin' at the first line.");
        if (lines.size() < 2 || !lines.get(lines.size() - 1).equalsIgnoreCase("close"))
            throw new ParserException("Program must end with: close", lines.size(), "Add 'close' at the last line.");

        // Remove start and end lines
        lines = lines.subList(1, lines.size() - 1);

        // Validate block structure
        validateStructure(lines);

        return lines;
    }

    // ============================================================
    // PREPROCESS - FIXED COMMENT HANDLING
    // Removes empty lines, single-line comments, and normalizes spaces
    // ============================================================
    private List<String> preprocess(String code) {
        String[] raw = code.split("\n");
        List<String> result = new ArrayList<>();

        for (String line : raw) {
            line = line.trim();
            
            // Remove inline comments
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex).trim();
            }
            
            if (line.isEmpty()) continue;

            result.add(line);
        }
        return result;
    }

    // ============================================================
    // VALIDATE BLOCK STRUCTURE - FIXED FOR ENDIF
    // Checks for missing 'stop', 'until', 'endif'
    // ============================================================
    private void validateStructure(List<String> lines) throws ParserException {

        Stack<String> stack = new Stack<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 2;

            if (line.startsWith("for ") || line.startsWith("while ") || line.equals("do")) {
                stack.push("loop");
            } else if (line.startsWith("if ")) {
                stack.push("if");
            } else if (line.equals("stop")) {
                if (stack.isEmpty() || !stack.peek().equals("loop")) {
                    throw new ParserException("Unexpected 'stop'", lineNumber, "Check your loop structure.");
                }
                stack.pop();
            } else if (line.startsWith("until ")) {
                if (stack.isEmpty() || !stack.peek().equals("loop")) {
                    throw new ParserException("Unexpected 'until'", lineNumber, "An 'until' must follow a 'do'.");
                }
                stack.pop();
            } else if (line.equals("else") || line.startsWith("elif ")) {
                if (stack.isEmpty() || !stack.peek().equals("if")) {
                    throw new ParserException("Unexpected 'else' or 'elif'", lineNumber, "These must follow an 'if'.");
                }
            } else if (line.equals("endif")) {
                if (stack.isEmpty() || !stack.peek().equals("if")) {
                    throw new ParserException("Unexpected 'endif'", lineNumber, "An 'endif' must close an 'if' block.");
                }
                stack.pop();
            }
        }

        // Check if all blocks were closed
        if (!stack.isEmpty()) {
            String missing = stack.peek();
            String hint = missing.equals("loop") ? "Add 'stop' to close your loop." : "Add 'endif' to close your if block.";
            throw new ParserException("Missing closing block for: " + missing, lines.size() + 1, hint);
        }
    }

    // ============================================================
    // CUSTOM EXCEPTION CLASS
    // ============================================================
    public static class ParserException extends Exception {
        int lineNumber;
        String hint;

        public ParserException(String message, int lineNumber) {
            super(message);
            this.lineNumber = lineNumber;
        }

        public ParserException(String message, int lineNumber, String hint) {
            super(message);
            this.lineNumber = lineNumber;
            this.hint = hint;
        }
    }
}

