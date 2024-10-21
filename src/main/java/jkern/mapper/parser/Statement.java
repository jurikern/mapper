package jkern.mapper.parser;

import jkern.mapper.lexer.Token;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @ToString
public class Statement {
    public final String name;
    public final Token<?> token;
}
