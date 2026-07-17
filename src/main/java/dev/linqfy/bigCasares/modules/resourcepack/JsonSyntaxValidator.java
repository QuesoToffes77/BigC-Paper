package dev.linqfy.bigCasares.modules.resourcepack;

public final class JsonSyntaxValidator {

    public void validate(String json, String resourceId) {
        Parser parser = new Parser(json == null ? "" : json, resourceId);
        parser.value();
        parser.whitespace();
        if (!parser.end()) {
            parser.fail("Unexpected trailing content");
        }
    }

    private static final class Parser {
        private final String input;
        private final String resourceId;
        private int index;

        private Parser(String input, String resourceId) {
            this.input = input;
            this.resourceId = resourceId;
        }

        private void value() {
            whitespace();
            if (end()) {
                fail("Expected a JSON value");
            }
            switch (input.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true");
                case 'f' -> literal("false");
                case 'n' -> literal("null");
                default -> number();
            }
        }

        private void object() {
            index++;
            whitespace();
            if (consume('}')) {
                return;
            }
            while (true) {
                whitespace();
                if (end() || input.charAt(index) != '"') {
                    fail("Expected an object key");
                }
                string();
                whitespace();
                require(':');
                value();
                whitespace();
                if (consume('}')) {
                    return;
                }
                require(',');
            }
        }

        private void array() {
            index++;
            whitespace();
            if (consume(']')) {
                return;
            }
            while (true) {
                value();
                whitespace();
                if (consume(']')) {
                    return;
                }
                require(',');
            }
        }

        private void string() {
            require('"');
            while (!end()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return;
                }
                if (current < 0x20) {
                    fail("Unescaped control character in string");
                }
                if (current != '\\') {
                    continue;
                }
                if (end()) {
                    fail("Incomplete string escape");
                }
                char escaped = input.charAt(index++);
                if (escaped == 'u') {
                    for (int count = 0; count < 4; count++) {
                        if (end() || Character.digit(input.charAt(index++), 16) < 0) {
                            fail("Invalid unicode escape");
                        }
                    }
                } else if ("\"\\/bfnrt".indexOf(escaped) < 0) {
                    fail("Invalid string escape");
                }
            }
            fail("Unterminated string");
        }

        private void number() {
            int start = index;
            consume('-');
            if (consume('0')) {
                if (!end() && Character.isDigit(input.charAt(index))) {
                    fail("Leading zero in number");
                }
            } else {
                digits(true);
            }
            if (consume('.')) {
                digits(true);
            }
            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                digits(true);
            }
            if (index == start) {
                fail("Expected a JSON value");
            }
        }

        private void digits(boolean required) {
            int start = index;
            while (!end() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (required && start == index) {
                fail("Expected a digit");
            }
        }

        private void literal(String literal) {
            if (!input.startsWith(literal, index)) {
                fail("Invalid literal");
            }
            index += literal.length();
        }

        private void require(char expected) {
            if (!consume(expected)) {
                fail("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (!end() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void whitespace() {
            while (!end() && switch (input.charAt(index)) {
                case ' ', '\t', '\n', '\r' -> true;
                default -> false;
            }) {
                index++;
            }
        }

        private boolean end() {
            return index >= input.length();
        }

        private void fail(String message) {
            throw new IllegalArgumentException(
                "Invalid JSON in " + resourceId + " at offset " + index + ": " + message);
        }
    }
}
