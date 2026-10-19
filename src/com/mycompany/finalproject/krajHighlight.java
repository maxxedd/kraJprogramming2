/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.finalproject;

/**
 *
 * @author Cloy
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.List;

public class krajHighlight {
    private final JTextArea editor;
    private final Highlighter highlighter;

    // COLORS
    private final Highlighter.HighlightPainter START_END = new DefaultHighlighter.DefaultHighlightPainter(new Color(80, 160, 240)); // Blue
    private final Highlighter.HighlightPainter FLOW_CONTROL = new DefaultHighlighter.DefaultHighlightPainter(new Color(40, 160, 180)); // Cyan
    private final Highlighter.HighlightPainter LITERAL_STRING = new DefaultHighlighter.DefaultHighlightPainter(new Color(60, 140, 80)); // Green
    private final Highlighter.HighlightPainter LITERAL_NUMBER = new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 160, 60)); // Yellow
    private final Highlighter.HighlightPainter OPERATOR = new DefaultHighlighter.DefaultHighlightPainter(new Color(240, 100, 40)); // Dark Orange
    private final Highlighter.HighlightPainter DATATYPE = new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 120, 40)); // Light Orange
    private final Highlighter.HighlightPainter ERROR = new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 60, 60)); // Red

    public krajHighlight(JTextArea editor) {
        this.editor = editor;
        this.highlighter = editor.getHighlighter();
    }

    // ============================================================
    // MAIN HIGHLIGHT METHOD
    // ============================================================
    public void highlight() {
        highlighter.removeAllHighlights();

        String code = editor.getText();
        krajToken tokenizer = new krajToken(code);
        List<krajToken.Token> tokens = tokenizer.tokenize();

        for (krajToken.Token token : tokens) {
            applyColor(token, code);
        }
    }

    // ============================================================
    // APPLY COLOR BASED ON TOKEN TYPE - UPDATED FOR ENDIF
    // ============================================================
    private void applyColor(krajToken.Token token, String code) {

        int start = findIndexInText(code, token.value, token.lineNumber);
        if (start < 0) return;
        int end = start + token.value.length();

        Highlighter.HighlightPainter painter = null;

        switch (token.type) {
            case "START":
            case "END":
                painter = START_END;
                break;

            case "DATATYPE":
            case "DISP":
            case "INPUT":
                painter = DATATYPE;
                break;
                
            case "FOR":
            case "WHILE":
            case "DO":
            case "UNTIL":
            case "IF":
            case "ELSE":
            case "ELIF":
            case "ENDIF":  // ADDED
            case "STOP":
            case "TIMES":
                painter = FLOW_CONTROL;
                break;

            case "NMBR_LITERAL":
            case "DECI_LITERAL":
            case "BOOL_LITERAL":
                painter = LITERAL_NUMBER;
                break;
            
            case "STRING_LITERAL":
                painter = LITERAL_STRING;
                break;
                
            case "OPERATOR":
            case "ASSIGNMENT":
                painter = OPERATOR;
                break;

            case "ERROR":
                painter = ERROR;
                break;
        }

        if (painter != null) {
            try {
                highlighter.addHighlight(start, end, painter);
            } catch (BadLocationException e) {}
        }
    }

    // ============================================================
    // FIND TOKEN POSITION IN THE RAW TEXT
    // ============================================================
    private int findIndexInText(String text, String value, int lineNumber) {
        String[] lines = text.split("\n");

        if (lineNumber > lines.length) return -1;

        int offset = 0;
        for (int i = 0; i < lineNumber - 1; i++) {
            offset += lines[i].length() + 1; // +1 for the newline character
        }

        // Search only in the relevant line
        String line = lines[lineNumber - 1];
        int indexInLine = line.indexOf(value);
        
        // This simple indexOf is not perfect for complex tokenizing but serves the highlighter
        return (indexInLine >= 0) ? offset + indexInLine : -1;
    }
}