package jkern.mapper.parser;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class SyntaxTree {
    private final List<Statement> statements;
    private final Map<String, Integer> tagLookup;
}
