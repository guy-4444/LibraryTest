package com.guy2.simpleStockGraph;

public class MyCalculator {

    /**
     * Adds two numbers together.
     *
     * @param a first number
     * @param b second number
     * @return the sum of the two numbers
     */
    public static int add(int a, int b) {
        return a + b;
    }

    public static int subtract(int a, int b) {
        return a - b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    public static int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed.");
        }
        return a / b;
    }
}
