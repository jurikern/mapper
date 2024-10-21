package jkern.mapper;

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
import jkern.mapper.models.TagLookupGraph;
import jkern.mapper.parser.SyntaxTree;
import jkern.mapper.parser.TagConfigParser;
import jkern.mapper.parser.V2ParserImpl;

public class MapperTest {
    private final Logger logger = LoggerFactory.getLogger(MapperTest.class);
    @Test
    public void testProcessLogLine() throws IOException {
        String raw = """
            2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK

            2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK

            2 123456789012 eni-5e6f7g8h 192.168.1.101 198.51.100.3 25 49155 6 10 8000 1620140761 1620140821 ACCEPT OK
            """;

        App app = new App();
        V2Template template = app.configure();
        V2ParserImpl parser = new V2ParserImpl(template);

        TagLookupGraph lookupGraph = new TagConfigParser(template).parse();
        logger.debug(lookupGraph.getAdjList().toString());
        Mapper mapper = new Mapper(lookupGraph, template);


        try (Reader in = new StringReader(raw)) {
            BufferedReader buff = new BufferedReader(in);

            while (buff.ready()) {
                String line = buff.readLine();
                if (line == null) break;

                SyntaxTree root = parser.parse(line);
                if (root.getStatements().size() == 0)
                    continue;

                mapper.processLogLine(root);
            }
        }

        logger.debug(mapper.getGraph().toString());
        logger.debug(mapper.outputTagCounts());
        logger.debug(mapper.outputPPCounts());
    }
}
