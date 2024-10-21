package jkern.mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;

import jkern.mapper.config.V2Template;
import jkern.mapper.models.TagLookupGraph;
import jkern.mapper.parser.SyntaxTree;
import jkern.mapper.parser.TagConfigParser;
import jkern.mapper.parser.V2ParserImpl;

public class App {
    public V2Template configure() {
        Yaml yaml = new Yaml(new Constructor(V2Template.class, new LoaderOptions()));
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("config.yaml");
        V2Template template = yaml.load(is);
        return template;
    }

    public static void main(String[] args) throws IOException {
        App app = new App();
        V2Template template = app.configure();

        V2ParserImpl parser = new V2ParserImpl(template);
        TagLookupGraph lookupGraph = new TagConfigParser(template).parse();
        Mapper mapper = new Mapper(lookupGraph, template);

        String filename = template.getFiles().get("log_data");

        try(InputStream in = App.class.getClassLoader()
            .getResourceAsStream(filename)) {

            InputStreamReader is = new InputStreamReader(in);
            BufferedReader buff = new BufferedReader(is);

            for (String line = buff.readLine(); line != null; line = buff.readLine()) {
                SyntaxTree root = parser.parse(line);
                if (root.getStatements().size() == 0)
                    continue;

                mapper.processLogLine(root);
            }
        }

        PrintWriter out = new PrintWriter(System.out);

        out.println("Tag Counts: ");
        out.println(mapper.outputTagCounts());
        out.println("Port/Protocol Combination Counts:");
        out.println(mapper.outputPPCounts());

        out.flush();
    }
}
