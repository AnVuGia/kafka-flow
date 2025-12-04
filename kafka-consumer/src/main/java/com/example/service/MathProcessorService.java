package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MathProcessorService {

    // Pattern to validate simple math expressions (numbers and basic operators)
    private static final Pattern MATH_PATTERN = Pattern.compile("^[\\d\\s+\\-*/().]+$");

    /**
     * Process a math expression and return the result
     */
    public String processExpression(String expression) {
        log.info("Processing math expression: {}", expression);
        
        if (expression == null || expression.trim().isEmpty()) {
            return "Error: Empty expression";
        }

        String cleanExpression = expression.trim();
        
        // Validate the expression only contains safe characters
        if (!MATH_PATTERN.matcher(cleanExpression).matches()) {
            log.warn("Invalid expression received: {}", expression);
            return "Error: Invalid expression. Only numbers and +, -, *, /, (), . are allowed";
        }

        try {
            double result = evaluateExpression(cleanExpression);
            
            // Format result - remove unnecessary decimal places
            if (result == (long) result) {
                return String.valueOf((long) result);
            } else {
                return String.valueOf(result);
            }
        } catch (Exception e) {
            log.error("Error evaluating expression '{}': {}", expression, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Evaluate a math expression using a simple recursive descent parser
     */
    private double evaluateExpression(String expression) {
        return new ExpressionParser(expression).parse();
    }

    /**
     * Simple recursive descent parser for math expressions
     */
    private static class ExpressionParser {
        private final String expression;
        private int pos = -1;
        private int ch;

        ExpressionParser(String expression) {
            this.expression = expression.replaceAll("\\s+", "");
        }

        void nextChar() {
            ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            nextChar();
            double result = parseExpression();
            if (pos < expression.length()) {
                throw new RuntimeException("Unexpected character: " + (char) ch);
            }
            return result;
        }

        double parseExpression() {
            double result = parseTerm();
            for (;;) {
                if (eat('+')) result += parseTerm();
                else if (eat('-')) result -= parseTerm();
                else return result;
            }
        }

        double parseTerm() {
            double result = parseFactor();
            for (;;) {
                if (eat('*')) result *= parseFactor();
                else if (eat('/')) {
                    double divisor = parseFactor();
                    if (divisor == 0) {
                        throw new RuntimeException("Division by zero");
                    }
                    result /= divisor;
                }
                else return result;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double result;
            int startPos = pos;
            
            if (eat('(')) {
                result = parseExpression();
                if (!eat(')')) {
                    throw new RuntimeException("Missing closing parenthesis");
                }
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                result = Double.parseDouble(expression.substring(startPos, pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            return result;
        }
    }
}

