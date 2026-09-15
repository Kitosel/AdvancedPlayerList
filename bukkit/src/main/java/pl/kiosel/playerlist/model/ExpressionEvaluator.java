package pl.kiosel.playerlist.model;

import javax.script.Bindings;

final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    static Object evaluate(String expression, Bindings bindings) {
        return new Parser(expression, bindings).parse();
    }

    private static final class Parser {

        private final String input;
        private final Bindings bindings;
        private int position;

        private Parser(String input, Bindings bindings) {
            this.input = input == null ? "" : input;
            this.bindings = bindings;
        }

        private Object parse() {
            Object result = conditional();
            skipWhitespace();
            if (match(";")) {
                skipWhitespace();
            }
            if (position != input.length()) {
                throw error("Unexpected token");
            }
            return result;
        }

        private Object conditional() {
            Object condition = or();
            if (!match("?")) {
                return condition;
            }
            Object positive = conditional();
            require(":");
            Object negative = conditional();
            return truthy(condition) ? positive : negative;
        }

        private Object or() {
            Object value = and();
            while (match("||")) {
                Object right = and();
                value = truthy(value) || truthy(right);
            }
            return value;
        }

        private Object and() {
            Object value = equality();
            while (match("&&")) {
                Object right = equality();
                value = truthy(value) && truthy(right);
            }
            return value;
        }

        private Object equality() {
            Object value = comparison();
            while (true) {
                if (match("===")) {
                    value = strictEquals(value, comparison());
                } else if (match("!==")) {
                    value = !strictEquals(value, comparison());
                } else if (match("==")) {
                    value = looseEquals(value, comparison());
                } else if (match("!=")) {
                    value = !looseEquals(value, comparison());
                } else {
                    return value;
                }
            }
        }

        private Object comparison() {
            Object value = additive();
            while (true) {
                if (match("<=")) {
                    value = compare(value, additive()) <= 0;
                } else if (match(">=")) {
                    value = compare(value, additive()) >= 0;
                } else if (match("<")) {
                    value = compare(value, additive()) < 0;
                } else if (match(">")) {
                    value = compare(value, additive()) > 0;
                } else {
                    return value;
                }
            }
        }

        private Object additive() {
            Object value = multiplicative();
            while (true) {
                if (match("+")) {
                    Object right = multiplicative();
                    if (value instanceof CharSequence || right instanceof CharSequence) {
                        value = stringify(value) + stringify(right);
                    } else {
                        value = number(value) + number(right);
                    }
                } else if (match("-")) {
                    value = number(value) - number(multiplicative());
                } else {
                    return value;
                }
            }
        }

        private Object multiplicative() {
            Object value = unary();
            while (true) {
                if (match("*")) {
                    value = number(value) * number(unary());
                } else if (match("/")) {
                    value = number(value) / number(unary());
                } else if (match("%")) {
                    value = number(value) % number(unary());
                } else {
                    return value;
                }
            }
        }

        private Object unary() {
            if (match("!")) {
                return !truthy(unary());
            }
            if (match("-")) {
                return -number(unary());
            }
            if (match("+")) {
                return number(unary());
            }
            return primary();
        }

        private Object primary() {
            skipWhitespace();
            if (match("(")) {
                Object value = conditional();
                require(")");
                return value;
            }
            if (position >= input.length()) {
                throw error("Expected a value");
            }

            char current = input.charAt(position);
            if (current == '\'' || current == '"') {
                return string();
            }
            if (Character.isDigit(current) || current == '.' && hasNextDigit()) {
                return numericLiteral();
            }
            if (isIdentifierStart(current)) {
                return identifier();
            }
            throw error("Expected a value");
        }

        private String string() {
            char quote = input.charAt(position++);
            StringBuilder result = new StringBuilder();
            while (position < input.length()) {
                char current = input.charAt(position++);
                if (current == quote) {
                    return result.toString();
                }
                if (current != '\\') {
                    result.append(current);
                    continue;
                }
                if (position >= input.length()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    default:
                        result.append(escaped);
                        break;
                }
            }
            throw error("Unterminated string");
        }

        private Double numericLiteral() {
            int start = position;
            boolean exponent = false;
            while (position < input.length()) {
                char current = input.charAt(position);
                if (Character.isDigit(current) || current == '.') {
                    position++;
                    continue;
                }
                if ((current == 'e' || current == 'E') && !exponent) {
                    exponent = true;
                    position++;
                    if (position < input.length()
                            && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                        position++;
                    }
                    continue;
                }
                break;
            }
            try {
                return Double.valueOf(input.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private Object identifier() {
            int start = position++;
            while (position < input.length() && isIdentifierPart(input.charAt(position))) {
                position++;
            }
            String name = input.substring(start, position);
			switch (name) {
		        case "true":
					return true;
				case "false":
					return false;
				case "null":
		        case "undefined":
					return null;
			}
			if (bindings != null && bindings.containsKey(name)) {
                return bindings.get(name);
            }
            throw error("Unknown variable '" + name + "'");
        }

        private void require(String token) {
            if (!match(token)) {
                throw error("Expected '" + token + "'");
            }
        }

        private boolean match(String token) {
            skipWhitespace();
            if (!input.regionMatches(position, token, 0, token.length())) {
                return false;
            }
            position += token.length();
            return true;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean hasNextDigit() {
            return position + 1 < input.length() && Character.isDigit(input.charAt(position + 1));
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at column " + (position + 1));
        }
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '$';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            return number != 0.0D && !Double.isNaN(number);
        }
        return !(value instanceof CharSequence) || ((CharSequence) value).length() > 0;
    }

    private static boolean strictEquals(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || !left.getClass().equals(right.getClass())) {
            return false;
        }
        if (left instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue()) == 0;
        }
        return left.equals(right);
    }

    private static boolean looseEquals(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        Double leftNumber = optionalNumber(left);
        Double rightNumber = optionalNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Double.compare(leftNumber, rightNumber) == 0;
        }
        return stringify(left).equals(stringify(right));
    }

    private static int compare(Object left, Object right) {
        Double leftNumber = optionalNumber(left);
        Double rightNumber = optionalNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Double.compare(leftNumber, rightNumber);
        }
        return stringify(left).compareTo(stringify(right));
    }

    private static double number(Object value) {
        Double result = optionalNumber(value);
        if (result == null) {
            throw new IllegalArgumentException("Value '" + stringify(value) + "' is not a number");
        }
        return result;
    }

    private static Double optionalNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1.0D : 0.0D;
        }
        if (value instanceof CharSequence) {
            String text = value.toString().trim();
            if (text.isEmpty()) {
                return 0.0D;
            }
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String stringify(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
