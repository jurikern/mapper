package jkern.mapper.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkern.mapper.App;
import jkern.mapper.config.V2Template;
import jkern.mapper.models.TagLookupGraph;

public class TagConfigParserTest {
    private final Logger logger = LoggerFactory.getLogger(TagConfigParserTest.class);
    @Test
    public void testParse() throws IOException {
        App app = new App();
        V2Template template = app.configure();

        TagLookupGraph graph = new TagConfigParser(template).parse();
        logger.debug(graph.getAdjList().toString());

        for (Integer protocol : template.getProtocols().values()) {
            assertEquals(graph.getAdjList().containsKey(protocol), true);
        }
    }
}
