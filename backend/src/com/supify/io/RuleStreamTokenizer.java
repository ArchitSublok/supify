package com.supify.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates:
 * - StreamTokenizer usage with character streams
 * - Token classification: TT_WORD, TT_NUMBER, TT_EOF, TT_EOL
 * - Custom character syntax configuration (wordChars, ordinaryChars)
 */
public class RuleStreamTokenizer {

    public record Token(int type, String sval, double nval) {
        @Override
        public String toString() {
            if (type == StreamTokenizer.TT_WORD) return "WORD[" + sval + "]";
            if (type == StreamTokenizer.TT_NUMBER) return "NUM[" + nval + "]";
            if (type == StreamTokenizer.TT_EOL) return "EOL";
            if (type == StreamTokenizer.TT_EOF) return "EOF";
            return "CHAR['" + (char) type + "']";
        }
    }

    public static List<Token> tokenizeCriteria(String expression) throws IOException {
        return tokenizeReader(new StringReader(expression));
    }

    public static List<Token> tokenizeReader(Reader reader) throws IOException {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);

        // Treat '_' and '-' as word characters (for identifiers like MIN_CAPACITY, ISO-9001)
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('-', '-');
        tokenizer.ordinaryChar('>');
        tokenizer.ordinaryChar('<');
        tokenizer.ordinaryChar('=');
        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);

        List<Token> tokens = new ArrayList<>();
        int tokenType;
        while ((tokenType = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            tokens.add(new Token(tokenType, tokenizer.sval, tokenizer.nval));
        }
        return tokens;
    }
}
