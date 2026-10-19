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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class krajInterpret {
    private Map<String, Variable> variables = new HashMap<>();

    // I/O Listeners
    private Consumer<String> outputListener; // For disp
    private Function<String, String> inputListener; // For input (GUI)
    
    public void setOutputListener(Consumer<String> listener) {
        this.outputListener = listener;
    }
    
    public void setInputListener(Function<String, String> listener) {
        this.inputListener = listener;
    }

    private void printOut(String message) {
        if (outputListener != null)
            outputListener.accept(message);
        else 
            System.out.println(message); // Fallback
    }

    // ============================================================
    // VARIABLE DATA CLASS
    // ============================================================
    private static class Variable {
        String type;
        Object value;

        public Variable(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    // ============================================================
    // MAIN INTERPRETER ENTRY
    // ============================================================
    public void run(String code) {
        variables.clear(); // Reset variables for new run
        
        // Use the krajParse parser instead of unimplemented parse method
        krajParse parser = new krajParse();
        List<String> lines;

        try {
            lines = parser.parse(code); // Parse and validate first
            execute(lines, 0); // Start execution

        } catch (krajParse.ParserException e) {
            printOut("\n❌ Parser Error at line " + e.lineNumber + ": " + e.getMessage());
            if (e.hint != null) printOut("💡 Hint: " + e.hint);

        } catch (InterpreterException e) {
            printOut("\n❌ Interpreter Error at line " + e.lineNumber + ": " + e.getMessage());
            if (e.hint != null) printOut("💡 Hint: " + e.hint);

        } catch (Exception e) {
            printOut("\n❌ Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ============================================================
    // EXECUTE LINE BY LINE
    // ============================================================
    private void execute(List<String> lines, int startLineIndex) throws InterpreterException {

        for (int i = startLineIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 2; // +2 accounts for 'begin' and 0-index

            try {
                if (line.startsWith("disp")) {
                    parseDisp(line, lineNumber);
                } else if (line.startsWith("string ") || line.startsWith("nmbr ") || line.startsWith("fort ") || line.startsWith("deci ")) {
                    parseDeclaration(line, lineNumber);
                } else if (line.startsWith("input ")) {
                    parseInput(line, lineNumber);
                } else if (line.matches("[a-zA-Z][a-zA-Z0-9_]*\\s*=\\s*.*")) {
                    parseAssignment(line, lineNumber);
                } else if (line.startsWith("for ") || line.startsWith("while ") || line.equals("do")) {
                    i = parseLoop(lines, i);
                } else if (line.startsWith("if ")) {
                    i = parseIf(lines, i);
                } else if (line.equals("stop") || line.equals("endif") || line.startsWith("else") || line.startsWith("elif") || line.startsWith("until")) {
                    // Control flow keywords, handled by block parsers. Skip if encountered here.
                    continue;
                } else if (!line.trim().isEmpty()) {
                    throw new InterpreterException("Unknown command: " + line, lineNumber, "Check spelling or command syntax.");
                }

            } catch (InterpreterException e) {
                throw e; // Propagate with line info
            } catch (Exception e) {
                throw new InterpreterException("Syntax or execution error: " + e.getMessage(), lineNumber, "Check your syntax carefully.");
            }
        }
    }

    // ============================================================
    // DISP (OUTPUT) - FIXED CONCATENATION
    // ============================================================
private void parseDisp(String line, int lineNumber) throws InterpreterException {
    String expr = line.trim().substring(4).trim();
    
    // Remove parentheses if present
    if (expr.startsWith("(") && expr.endsWith(")")) {
        expr = expr.substring(1, expr.length() - 1).trim();
    }
    
    // Handle string concatenation directly
    if (expr.contains("+")) {
        // Split by + and evaluate each part
        String[] parts = expr.split("\\+");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            part = part.trim();
            
            // Check if part is a string literal
            if ((part.startsWith("\"") && part.endsWith("\"")) || 
                (part.startsWith("'") && part.endsWith("'"))) {
                result.append(part.substring(1, part.length() - 1));
            } else {
                // Try to evaluate as expression
                try {
                    Object value = evaluateExpression(part, lineNumber);
                    // Handle BigDecimal specially
                    if (value instanceof BigDecimal) {
                        BigDecimal bd = (BigDecimal) value;
                        bd = bd.stripTrailingZeros();
                        result.append(bd.toPlainString());
                    } else {
                        result.append(value.toString());
                    }
                } catch (Exception e) {
                    // If evaluation fails, treat as literal string
                    result.append(part);
                }
            }
        }
        
        printOut(result.toString());
    } else {
        // Single value
        Object result = evaluateExpression(expr, lineNumber);
        // Handle BigDecimal specially
        if (result instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) result;
            bd = bd.stripTrailingZeros();
            printOut(bd.toPlainString());
        } else {
            printOut(result.toString());
        }
    }
}


    private String evaluateConcatenation(String expr, int lineNumber) throws InterpreterException {
        // Split by + but preserve string literals
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inString = false;
        char stringChar = '"';

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if ((c == '"' || c == '\'') && (i == 0 || expr.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
                currentPart.append(c);
            } else if (c == '+' && !inString) {
                // Found a + operator outside of a string
                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
            } else {
                currentPart.append(c);
            }
        }

        // Add the last part
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }

        // Evaluate each part
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            part = part.trim();

            // Check if part is a string literal
            if ((part.startsWith("\"") && part.endsWith("\"")) || 
                (part.startsWith("'") && part.endsWith("'"))) {
                // Remove quotes
                result.append(part.substring(1, part.length() - 1));
            } else {
                // Evaluate as expression (could be variable, number, etc.)
                Object value = evaluateExpression(part, lineNumber);
                result.append(value.toString());
            }
        }

        return result.toString();
    }


    // ============================================================
    // INPUT - UPDATED FOR GUI
    // ============================================================
    private void parseInput(String line, int lineNumber) throws InterpreterException {
        // input <variable>
        String varName = line.substring(6).trim();
        if (!variables.containsKey(varName))
            throw new InterpreterException("Cannot accept input into undeclared variable: " + varName, lineNumber, "Declare variable first (e.g., nmbr x)");

        try {
            String prompt = "Enter value for '" + varName + "' (" + variables.get(varName).type + "): ";
            
            String input;
            if (inputListener != null) {
                // Use GUI input listener
                input = inputListener.apply(prompt);
                if (input == null) {
                    throw new InterpreterException("Input cancelled by user", lineNumber);
                }
            } else {
                // Fallback to console
                printOut(prompt);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                input = consoleReader.readLine();
            }
            
            Variable var = variables.get(varName);
            Object value = null;

            switch (var.type) {
                case "nmbr":
                    value = Integer.parseInt(input.trim());
                    break;
                case "deci":
                    value = new BigDecimal(input.trim());
                    break;
                case "string":
                    value = input;
                    break;
                case "fort":
                    if (!input.trim().toUpperCase().matches("TRUE|FALSE")) 
                         throw new InterpreterException("Invalid boolean input", lineNumber, "Input must be TRUE or FALSE.");
                    value = input.trim().toUpperCase().equals("TRUE");
                    break;
                default:
                    throw new InterpreterException("Unknown variable type: " + var.type, lineNumber);
            }
            var.value = value;

        } catch (NumberFormatException e) {
            throw new InterpreterException("Invalid number format", lineNumber, "Enter a valid number.");
        } catch (Exception e) {
            throw new InterpreterException("Input error: " + e.getMessage(), lineNumber, "Check your input format.");
        }
    }

    // ============================================================
    // DECLARATION & ASSIGNMENT
    // ============================================================
    private void parseDeclaration(String line, int lineNumber) throws InterpreterException {
        // <DATATYPE> <VAR> = <EXPRESSION>
        String[] parts = line.split(" ", 2);
        if (parts.length < 2) {
            throw new InterpreterException("Invalid declaration syntax", lineNumber, "Use: string name = value");
        }
        
        String type = parts[0];
        String expr = parts[1].trim();
        
        if (!expr.contains("=")) {
            // Declaration without assignment (e.g., nmbr x) - initialize with default value
            String varName = expr;
            if (variables.containsKey(varName))
                 throw new InterpreterException("Variable already declared: " + varName, lineNumber);
            
            Object def;
            switch (type) {
                case "string":
                    def = "";
                    break;
                case "nmbr":
                    def = 0;
                    break;
                case "deci":
                    def = BigDecimal.ZERO;
                    break;
                case "fort":
                    def = false;
                    break;
                default:
                    throw new InterpreterException("Unknown data type: " + type, lineNumber);
            }
            variables.put(varName, new Variable(type, def));
            return;
        }
        
        String[] assignParts = expr.split("=", 2);
        String varName = assignParts[0].trim();
        String valueExpr = assignParts[1].trim();

        if (variables.containsKey(varName))
            throw new InterpreterException("Variable already declared: " + varName, lineNumber);

        Object value = evaluateExpression(valueExpr, lineNumber);
        variables.put(varName, new Variable(type, castValueToType(value, type, lineNumber)));
    }

    private void parseAssignment(String line, int lineNumber) throws InterpreterException {
        // <VAR> = <EXPRESSION>
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            throw new InterpreterException("Invalid assignment syntax", lineNumber, "Use: variable = value");
        }
        
        String varName = parts[0].trim();
        String valueExpr = parts[1].trim();

        if (!variables.containsKey(varName))
            throw new InterpreterException("Variable not declared: " + varName, lineNumber, "Declare it first (e.g., nmbr " + varName + ")");

        Variable var = variables.get(varName);
        Object value = evaluateExpression(valueExpr, lineNumber);
        var.value = castValueToType(value, var.type, lineNumber);
    }
    
    // Helper to cast/convert values
    private Object castValueToType(Object value, String targetType, int lineNumber) throws InterpreterException {
        if (value == null) return null;

        try {
            switch (targetType) {
                case "string":
                    return value.toString();
                case "nmbr":
                    if (value instanceof Integer) return value;
                    if (value instanceof BigDecimal) return ((BigDecimal) value).intValue();
                    if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
                    if (value instanceof String) {
                        try {
                            return Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            throw new InterpreterException("Cannot convert string to integer: " + value, lineNumber);
                        }
                    }
                    throw new InterpreterException("Cannot convert to nmbr: " + value.getClass().getSimpleName(), lineNumber);
                case "deci":
                    if (value instanceof BigDecimal) return value;
                    if (value instanceof Integer) return new BigDecimal((Integer) value);
                    if (value instanceof Boolean) return new BigDecimal(((Boolean) value) ? 1 : 0);
                    if (value instanceof String) {
                        try {
                            return new BigDecimal((String) value);
                        } catch (NumberFormatException e) {
                            throw new InterpreterException("Cannot convert string to decimal: " + value, lineNumber);
                        }
                    }
                    throw new InterpreterException("Cannot convert to deci: " + value.getClass().getSimpleName(), lineNumber);
                case "fort":
                    if (value instanceof Boolean) return value;
                    if (value instanceof Integer) return (Integer) value != 0;
                    if (value instanceof BigDecimal) return !((BigDecimal) value).equals(BigDecimal.ZERO);
                    if (value instanceof String) {
                        String str = ((String) value).toUpperCase();
                        if (str.equals("TRUE")) return true;
                        if (str.equals("FALSE")) return false;
                        if (str.equals("1")) return true;
                        if (str.equals("0")) return false;
                        throw new InterpreterException("Cannot convert string to boolean: " + value, lineNumber);
                    }
                    throw new InterpreterException("Cannot convert to fort: " + value.getClass().getSimpleName(), lineNumber);
                default:
                    throw new InterpreterException("Unknown target type: " + targetType, lineNumber);
            }
        } catch (NumberFormatException e) {
            throw new InterpreterException("Type mismatch: Cannot assign value of type " + value.getClass().getSimpleName() + " to declared type " + targetType, lineNumber);
        }
    }

    // ============================================================
    // ARITHMETIC EXPRESSION EVALUATION (Improved)
    // ============================================================
    private Object evaluateExpression(String expr, int lineNumber) throws InterpreterException {
        expr = expr.trim();

        // Handle string literals
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }

        // Check for single literal or variable
        if (expr.matches("TRUE|FALSE")) return expr.equals("TRUE"); // Boolean Literal
        if (expr.matches("[0-9]+\\.[0-9]+")) return new BigDecimal(expr); // Deci Literal
        if (expr.matches("[0-9]+")) return Integer.parseInt(expr); // Nmbr Literal

        // Variable lookup
        if (variables.containsKey(expr)) {
            Variable var = variables.get(expr);
            // Convert BigDecimal to string with proper formatting
            if (var.value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) var.value;
                // Remove trailing zeros
                bd = bd.stripTrailingZeros();
                // Use plain string to avoid scientific notation
                return bd.toPlainString();
            }
            return var.value;
        }

        // Handle parentheses first
        while (expr.contains("(") && expr.contains(")")) {
            int open = expr.lastIndexOf("(");
            int close = expr.indexOf(")", open);
            if (close == -1) {
                throw new InterpreterException("Mismatched parentheses", lineNumber);
            }

            String inner = expr.substring(open + 1, close);
            Object innerResult = evaluateExpression(inner, lineNumber);
            expr = expr.substring(0, open) + innerResult.toString() + expr.substring(close + 1);
        }

        // Handle operators with precedence: * and / before + and -
        // First handle * and /
        Pattern mulDivPattern = Pattern.compile("([^*/]+)([*/])([^*/]+)");
        Matcher mulDivMatcher = mulDivPattern.matcher(expr);

        while (mulDivMatcher.find()) {
            String left = mulDivMatcher.group(1).trim();
            String op = mulDivMatcher.group(2);
            String right = mulDivMatcher.group(3).trim();

            Object leftVal = evaluateExpression(left, lineNumber);
            Object rightVal = evaluateExpression(right, lineNumber);
            Object result = performOperation(leftVal, rightVal, op, lineNumber);

            expr = expr.replace(mulDivMatcher.group(), result.toString());
            mulDivMatcher = mulDivPattern.matcher(expr);
        }

        // Then handle + and - (for numbers only, strings already handled)
        Pattern addSubPattern = Pattern.compile("([^+-]+)([+-])([^+-]+)");
        Matcher addSubMatcher = addSubPattern.matcher(expr);

        while (addSubMatcher.find()) {
            String left = addSubMatcher.group(1).trim();
            String op = addSubMatcher.group(2);
            String right = addSubMatcher.group(3).trim();

            Object leftVal = evaluateExpression(left, lineNumber);
            Object rightVal = evaluateExpression(right, lineNumber);
            Object result = performOperation(leftVal, rightVal, op, lineNumber);

            expr = expr.replace(addSubMatcher.group(), result.toString());
            addSubMatcher = addSubPattern.matcher(expr);
        }

        // Try to parse the final result
        try {
            if (expr.matches("[0-9]+\\.[0-9]+")) return new BigDecimal(expr);
            if (expr.matches("[0-9]+")) return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            throw new InterpreterException("Invalid expression: " + expr, lineNumber);
        }

        throw new InterpreterException("Unknown identifier or value in expression: " + expr, lineNumber);
    }

    
    private Object performOperation(Object left, Object right, String op, int lineNumber) throws InterpreterException {
        // Handle string concatenation
        if (op.equals("+") && (left instanceof String || right instanceof String)) {
            return left.toString() + right.toString();
        }
        
        // Handle numeric operations
        if (left instanceof String || right instanceof String) {
            throw new InterpreterException("Unsupported operation on String type: " + op, lineNumber);
        }
        
        BigDecimal numLeft, numRight;
        
        if (left instanceof Integer) numLeft = new BigDecimal((Integer) left);
        else if (left instanceof BigDecimal) numLeft = (BigDecimal) left;
        else if (left instanceof Boolean) numLeft = new BigDecimal(((Boolean) left) ? 1 : 0);
        else throw new InterpreterException("Unsupported type for arithmetic: " + left.getClass().getSimpleName(), lineNumber);
        
        if (right instanceof Integer) numRight = new BigDecimal((Integer) right);
        else if (right instanceof BigDecimal) numRight = (BigDecimal) right;
        else if (right instanceof Boolean) numRight = new BigDecimal(((Boolean) right) ? 1 : 0);
        else throw new InterpreterException("Unsupported type for arithmetic: " + right.getClass().getSimpleName(), lineNumber);
        
        BigDecimal result;
        switch (op) {
            case "+":
                result = numLeft.add(numRight);
                break;
            case "-":
                result = numLeft.subtract(numRight);
                break;
            case "*":
                result = numLeft.multiply(numRight);
                break;
            case "/":
                if (numRight.equals(BigDecimal.ZERO)) {
                    throw new InterpreterException("Division by zero", lineNumber);
                }
                result = numLeft.divide(numRight, 9, RoundingMode.HALF_UP);
                break;
            default:
                throw new InterpreterException("Unknown operator: " + op, lineNumber);
        }
        
        // Return Integer if possible, else BigDecimal
        try {
            return result.intValueExact();
        } catch (ArithmeticException e) {
            return result.stripTrailingZeros();
        }
    }
    
    // ============================================================
    // CONDITION EVALUATION
    // ============================================================
    private boolean evaluateCondition(String cond, int lineNumber) throws InterpreterException {
        cond = cond.trim();
        
        // Handle parentheses
        if (cond.startsWith("(") && cond.endsWith(")")) {
            return evaluateCondition(cond.substring(1, cond.length() - 1), lineNumber);
        }
        
        // Check for AND (&&) and OR (||) with proper precedence
        // First check for OR
        String[] orParts = cond.split("\\|\\|");
        if (orParts.length > 1) {
            for (String part : orParts) {
                if (evaluateCondition(part.trim(), lineNumber)) {
                    return true;
                }
            }
            return false;
        }
        
        // Then check for AND
        String[] andParts = cond.split("&&");
        if (andParts.length > 1) {
            for (String part : andParts) {
                if (!evaluateCondition(part.trim(), lineNumber)) {
                    return false;
                }
            }
            return true;
        }
        
        // Handle comparison operators
        Pattern p = Pattern.compile("(<=|>=|==|!=|<|>)");
        Matcher m = p.matcher(cond);
        
        if (m.find()) {
            String op = m.group();
            String[] parts = cond.split(Pattern.quote(op), 2);
            if (parts.length != 2) throw new InterpreterException("Invalid condition syntax", lineNumber);

            Object left = evaluateExpression(parts[0].trim(), lineNumber);
            Object right = evaluateExpression(parts[1].trim(), lineNumber);

            // Handle numeric comparison
            if (left instanceof Number && right instanceof Number) {
                BigDecimal numLeft = (left instanceof Integer) ? new BigDecimal((Integer) left) : (BigDecimal) left;
                BigDecimal numRight = (right instanceof Integer) ? new BigDecimal((Integer) right) : (BigDecimal) right;
                int comparison = numLeft.compareTo(numRight);

                return switch (op) {
                    case "<" -> comparison < 0;
                    case ">" -> comparison > 0;
                    case "<=" -> comparison <= 0;
                    case ">=" -> comparison >= 0;
                    case "==" -> comparison == 0;
                    case "!=" -> comparison != 0;
                    default -> throw new InterpreterException("Unsupported operator for numbers: " + op, lineNumber);
                };
            }
            
            // Handle string comparison
            if (op.equals("==")) {
                return Objects.equals(left.toString(), right.toString());
            }
            if (op.equals("!=")) {
                return !Objects.equals(left.toString(), right.toString());
            }

            throw new InterpreterException("Incompatible types or operator for condition: " + op, lineNumber);
        }

        // Handle single boolean expression
        Object result = evaluateExpression(cond, lineNumber);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        
        throw new InterpreterException("Invalid condition: " + cond, lineNumber);
    }

    // ============================================================
    // LOOP PARSERS - FIXED
    // ============================================================
    private int parseLoop(List<String> lines, int startIndex) throws InterpreterException {
        String header = lines.get(startIndex);
        int lineNumber = startIndex + 2;
        
        if (header.startsWith("for ")) {
            return executeForLoop(lines, startIndex, lineNumber);
        } else if (header.startsWith("while ")) {
            return executeWhileLoop(lines, startIndex, lineNumber);
        } else if (header.equals("do")) {
            return executeDoUntilLoop(lines, startIndex, lineNumber);
        }
        
        throw new InterpreterException("Invalid loop syntax", lineNumber);
    }

    private int executeForLoop(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        // for <expression> times
        String header = lines.get(startIndex);
        String expr = header.substring(4).trim().replace("times", "").trim();
        Object countObj = evaluateExpression(expr, lineNumber);

        if (!(countObj instanceof Integer))
             throw new InterpreterException("For loop count must be an integer", lineNumber);
        
        // Find the matching 'stop' for this for loop
        int stopIndex = findMatchingStop(lines, startIndex, lineNumber);
        
        // Get loop body (lines between for and stop)
        List<String> body = new ArrayList<>();
        for (int i = startIndex + 1; i < stopIndex; i++) {
            body.add(lines.get(i));
        }
        
        int times = (Integer) countObj;
        for (int i = 0; i < times; i++) {
            execute(body, 0);
        }
        return stopIndex; // Return index of the stop line
    }

    private int executeWhileLoop(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        // while <condition>
        String header = lines.get(startIndex);
        String cond = header.substring(6).trim();
        
        // Find the matching 'stop' for this while loop
        int stopIndex = findMatchingStop(lines, startIndex, lineNumber);
        
        // Get loop body
        List<String> body = new ArrayList<>();
        for (int i = startIndex + 1; i < stopIndex; i++) {
            body.add(lines.get(i));
        }
        
        while (evaluateCondition(cond, lineNumber)) {
            execute(body, 0);
        }
        return stopIndex;
    }

    private int executeDoUntilLoop(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        // Find the matching 'until' line
        int untilIndex = findMatchingUntil(lines, startIndex, lineNumber);
        
        if (untilIndex == -1) {
            throw new InterpreterException("Do loop missing 'until' statement", lineNumber);
        }
        
        // Get loop body (lines between do and until)
        List<String> body = new ArrayList<>();
        for (int i = startIndex + 1; i < untilIndex; i++) {
            body.add(lines.get(i));
        }
        
        // Extract condition from until line
        String untilLine = lines.get(untilIndex);
        String cond = untilLine.substring(6).trim();
        
        do {
            execute(body, 0);
        } while (!evaluateCondition(cond, lineNumber));
        
        return untilIndex + 1; // Return index after until line
    }

    // ============================================================
    // IF-ELSE-IF PARSER - FIXED TO USE ENDIF
    // ============================================================
    private int parseIf(List<String> lines, int startIndex) throws InterpreterException {
        int current = startIndex;
        boolean executed = false;
        int endIfIndex = -1;
        
        // First, find the matching endif for this if block
        endIfIndex = findMatchingEndIf(lines, startIndex, startIndex + 2);
        
        // Process the if-elif-else structure
        while(current < endIfIndex) {
            String line = lines.get(current);
            
            if (line.startsWith("if ")) {
                if (executed) {
                    // Skip to endif
                    current = endIfIndex;
                    break;
                }
                String cond = line.substring(3).trim();
                if (evaluateCondition(cond, current + 2)) {
                    executed = true;
                    // Execute until next elif, else, or endif
                    int next = findNextIfBlock(lines, current + 1, endIfIndex);
                    execute(lines.subList(current + 1, next), 0);
                    current = next;
                } else {
                    current = findNextIfBlock(lines, current + 1, endIfIndex);
                }
            } else if (line.startsWith("elif ")) {
                if (executed) {
                    current = findNextIfBlock(lines, current + 1, endIfIndex);
                } else {
                    String cond = line.substring(5).trim();
                    if (evaluateCondition(cond, current + 2)) {
                        executed = true;
                        int next = findNextIfBlock(lines, current + 1, endIfIndex);
                        execute(lines.subList(current + 1, next), 0);
                        current = next;
                    } else {
                        current = findNextIfBlock(lines, current + 1, endIfIndex);
                    }
                }
            } else if (line.equals("else")) {
                if (!executed) {
                    int next = findNextIfBlock(lines, current + 1, endIfIndex);
                    execute(lines.subList(current + 1, next), 0);
                    executed = true;
                }
                current = findNextIfBlock(lines, current + 1, endIfIndex);
            } else {
                current++;
            }
        }
        
        return endIfIndex; // Return index of endif
    }
    
    // Helper to find next elif, else, or endif
    private int findNextIfBlock(List<String> lines, int start, int endIfIndex) {
        for (int i = start; i <= endIfIndex; i++) {
            String line = lines.get(i);
            if (line.startsWith("elif ") || line.equals("else") || line.equals("endif")) {
                return i;
            }
        }
        return endIfIndex;
    }

    // ============================================================
    // SIMPLIFIED BLOCK FINDERS - FIXED
    // ============================================================

    // Find matching 'stop' for loops
    private int findMatchingStop(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        int depth = 1;
        
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (line.startsWith("for ") || line.startsWith("while ") || line.equals("do") || line.startsWith("if ")) {
                depth++; // Nested block
            } else if (line.equals("stop")) {
                depth--;
                if (depth == 0) {
                    return i; // Found matching stop
                }
            } else if (line.equals("endif")) {
                depth--; // endif also decreases depth for nested if blocks
                if (depth == 0) {
                    throw new InterpreterException("Unexpected 'endif' in loop", i + 2);
                }
            }
        }
        
        throw new InterpreterException("Missing 'stop' for loop", lineNumber);
    }
    
    // Find matching 'endif' for if blocks
    private int findMatchingEndIf(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        int depth = 1;
        
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (line.startsWith("if ")) {
                depth++; // Nested if
            } else if (line.equals("endif")) {
                depth--;
                if (depth == 0) {
                    return i; // Found matching endif
                }
            } else if (line.equals("stop")) {
                // Ignore stop in if blocks - it's for loops only
            }
        }
        
        throw new InterpreterException("Missing 'endif'", lineNumber);
    }
    
    // Find matching 'until' for do loops
    private int findMatchingUntil(List<String> lines, int startIndex, int lineNumber) throws InterpreterException {
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("until ")) {
                return i;
            }
        }
        return -1;
    }

    // ============================================================
    // CUSTOM EXCEPTION CLASS
    // ============================================================
    public static class InterpreterException extends Exception {
        int lineNumber;
        String hint;

        public InterpreterException(String message, int lineNumber) {
            super(message);
            this.lineNumber = lineNumber;
        }

        public InterpreterException(String message, int lineNumber, String hint) {
            super(message);
            this.lineNumber = lineNumber;
            this.hint = hint;
        }
    }
}