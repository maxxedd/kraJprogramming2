/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.finalproject;

/**
 *
 * @author Cloy
 */
public class errorHandler {

    // These methods are not directly used by the new Interpreter/Parser, 
    // which use custom exceptions, but are provided for general utility.
    
    public static void error(String message) {
        System.err.println("❌ ERROR: " + message);
        throw new RuntimeException(message);
    }

    public static void error(String message, int line) {
        System.err.println("❌ ERROR on line " + line + ": " + message);
        throw new RuntimeException(message);
    }

    public static void warn(String message) {
        System.out.println("⚠️ WARNING: " + message);
    }

    public static void hint(String message) {
        System.out.println("💡 HINT: " + message);
    }
}
