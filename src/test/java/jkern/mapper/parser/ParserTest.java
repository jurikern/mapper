package jkern.mapper.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkern.mapper.App;
import jkern.mapper.config.V2Template;

public class ParserTest {
    private static final Logger logger = LoggerFactory.getLogger(ParserTest.class);

    @Test
    public void testParse() throws IOException {
        String raw = """
            2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK

            2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK

            2 123456789012 eni-5e6f7g8h 192.168.1.101 198.51.100.3 25 49155 6 10 8000 1620140761 1620140821 ACCEPT OK
            """;

        logger.debug(raw);

        App app = new App();
        V2Template template = app.configure();
        logger.debug(template.toString());

        V2ParserImpl parser = new V2ParserImpl(template);

        try (Reader in = new StringReader(raw)) {
            BufferedReader buff = new BufferedReader(in);

            while (buff.ready()) {
                String line = buff.readLine();
                if (line == null) break;

                SyntaxTree root = parser.parse(line);
                if (root.getStatements().size() == 0)
                    continue;

                for (Statement statement : root.getStatements()) {
                    logger.debug(statement.toString());
                }

                logger.debug(root.getTagLookup().toString());

                assertEquals(root.getTagLookup().size(), template.getTag().size());
                assertEquals(root.getStatements().size(), template.getKeys().size());

                for (String key : template.getTag()) {
                    assertEquals(root.getTagLookup().containsKey(key), true);
                    Integer index = root.getTagLookup().get(key);
                    Statement statement = root.getStatements().get(index);
                    assertEquals(key, statement.getName());
                }
            }
        }
    }
}
