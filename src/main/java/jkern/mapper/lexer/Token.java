package jkern.mapper.lexer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @ToString
public class Token<T> {
    private final TokenType type;
    private final T value;
}
