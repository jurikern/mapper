package jkern.mapper.parser;

import java.util.ArrayList;
import java.util.HashMap;

import jkern.mapper.config.V2Template;
import jkern.mapper.lexer.Lexer;
import jkern.mapper.lexer.Token;
import jkern.mapper.lexer.TokenType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class V2ParserImpl implements Parser {
    private final V2Template template;

    @Override
    public SyntaxTree parse(String input) {
        SyntaxTree root = new SyntaxTree(new ArrayList<>(), new HashMap<>());
        Lexer lexer = new Lexer(input);

        int i = 0;
        for (String key : template.getKeys()) {
            Token<?> token = lexer.nextToken();
            if (token.getType() == TokenType.EOF) break;
            root.getStatements().add(
                new Statement(key, token)
            );
            if (template.getTag().contains(key)) {
                root.getTagLookup().put(key, i);
            }
            i++;
        }

        return root;
    }
}
