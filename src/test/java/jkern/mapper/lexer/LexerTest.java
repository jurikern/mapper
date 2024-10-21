package jkern.mapper.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LexerTest {
    private static final Logger logger = LoggerFactory.getLogger(LexerTest.class);

    @Test
    public void testNextToken() {
        String raw = "2 123456789012 @ eni-0a1b2c3d 10.0.1.201 ACCEPT";

        logger.debug(raw);
        Lexer lexer = new Lexer(raw);

        Map<String, TokenType> exp = Map.of(
           "2", TokenType.INT,
           "123456789012", TokenType.LONG,
           "@", TokenType.INVALID,
           "eni-0a1b2c3d", TokenType.IDENT,
           "10.0.1.201", TokenType.IP_ADDR,
           "ACCEPT", TokenType.IDENT
        );

        var token = lexer.nextToken();
        while (token.getType() != TokenType.EOF) {
            logger.debug(token.toString());

            String value = String.valueOf(token.getValue());
            assertEquals(exp.get(value), token.getType());

            token = lexer.nextToken();
        }

    }
}
