package jkern.mapper.lexer;

import javax.lang.model.type.NullType;

import lombok.Getter;

@Getter
public class Lexer {
    private final String input;
    private int pos;
    private int readPos;
    private char ch;

    public Lexer(String input) {
        this.input = input;
        readChar();
    }

    private void readChar() {
        if (readPos >= input.length()) {
            ch = 0;
        } else {
            ch = input.charAt(readPos);
        }
        pos = readPos;
        readPos++;
    }

    private String readIdent() {
        int currPos = pos;

        while (isLetter()) {
            readChar();
        }

        return input.substring(currPos, pos);
    }

    private String readIpAddr(String ident) {
        for (int i = 0; i < ident.length(); i++) {
            char ch = ident.charAt(i);
            if (!Character.isDigit(ch) && ch != '.') {
                return null;
            }
        }

        return ident;
    }

    private Integer readIntNumber(String ident) {
        Integer intNum = 0;

        for (int i = 0; i < ident.length(); i++) {
            char ch = ident.charAt(i);
            if (!Character.isDigit(ch)) return null;

            if (Integer.MAX_VALUE / 10 - (ch - '0') < intNum)
                return null;
            intNum = intNum * 10 + (ch - '0');
        }
        return intNum;
    }

    private Long readLongNumber(String ident) {
        Long longNum = 0L;

        for (int i = 0; i < ident.length(); i++) {
            char ch = ident.charAt(i);
            if (!Character.isDigit(ch)) {
                return null;
            }
            longNum = longNum * 10 + (ch - '0');
        }
        return longNum;
    }

    private void eatWhitespace() {
        while (ch == ' ') {
            readChar();
        }
    }

    private boolean isLetter() {
        if (ch == '.' || ch == '-') {
            return true;
        }
        return Character.isLetter(ch) || Character.isDigit(ch);
    }

    public Token<?> nextToken() {
        Token<?> token;
        eatWhitespace();

        if (isLetter()) {
            String ident = readIdent();

            Integer intNum = readIntNumber(ident);
            if (intNum != null) return new Token<Integer>(TokenType.INT, intNum);

            Long longNum = readLongNumber(ident);
            if (longNum != null)  return new Token<Long>(TokenType.LONG, longNum);


            String ipAddr = readIpAddr(ident);
            if (ipAddr != null) return new Token<String>(TokenType.IP_ADDR, ipAddr);

            return new Token<String>(TokenType.IDENT, ident);
        } else if (ch == 0) {
            token = new Token<NullType>(TokenType.EOF, null);
        } else {
            token = new Token<Character>(TokenType.INVALID, ch);
        }

        readChar();
        return token;
    }
}
