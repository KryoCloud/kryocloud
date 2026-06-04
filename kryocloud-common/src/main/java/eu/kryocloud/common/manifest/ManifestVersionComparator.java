package eu.kryocloud.common.manifest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ManifestVersionComparator implements Comparator<String> {

    public static final ManifestVersionComparator INSTANCE = new ManifestVersionComparator();

    private ManifestVersionComparator() {
    }

    @Override
    public int compare(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }

        if (left == null) {
            return -1;
        }

        if (right == null) {
            return 1;
        }

        List<Token> leftTokens = tokenize(left);
        List<Token> rightTokens = tokenize(right);
        int size = Math.max(leftTokens.size(), rightTokens.size());

        for (int index = 0; index < size; index++) {
            Token leftToken = index < leftTokens.size() ? leftTokens.get(index) : Token.zero();
            Token rightToken = index < rightTokens.size() ? rightTokens.get(index) : Token.zero();
            int result = leftToken.compareTo(rightToken);

            if (result != 0) {
                return result;
            }
        }

        return left.compareToIgnoreCase(right);
    }

    public static List<String> sortNewestFirst(Iterable<String> versions) {
        List<String> result = new ArrayList<>();

        for (String version : versions) {
            if (version == null || version.isBlank()) {
                continue;
            }

            result.add(version);
        }

        result.sort(INSTANCE.reversed());
        return List.copyOf(result);
    }

    public static String newest(Iterable<String> versions) {
        return sortNewestFirst(versions).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("versions must not be empty"));
    }

    private static List<Token> tokenize(String value) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean numeric = false;
        boolean active = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            boolean digit = Character.isDigit(current);

            if (!active) {
                buffer.append(current);
                numeric = digit;
                active = true;
                continue;
            }

            if (digit == numeric) {
                buffer.append(current);
                continue;
            }

            tokens.add(Token.of(buffer.toString(), numeric));
            buffer.setLength(0);
            buffer.append(current);
            numeric = digit;
        }

        if (buffer.length() > 0) {
            tokens.add(Token.of(buffer.toString(), numeric));
        }

        return tokens;
    }

    private record Token(String value, boolean numeric) implements Comparable<Token> {

        private static Token zero() {
            return new Token("0", true);
        }

        private static Token of(String value, boolean numeric) {
            if (!numeric) {
                return new Token(value, false);
            }

            String normalized = value.replaceFirst("^0+(?!$)", "");
            return new Token(normalized, true);
        }

        @Override
        public int compareTo(Token other) {
            if (numeric && other.numeric) {
                int length = Integer.compare(value.length(), other.value.length());

                if (length != 0) {
                    return length;
                }

                return value.compareTo(other.value);
            }

            if (numeric != other.numeric) {
                return numeric ? 1 : -1;
            }

            return value.compareToIgnoreCase(other.value);
        }
    }
}
