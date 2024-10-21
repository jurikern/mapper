package jkern.mapper.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import jkern.mapper.config.V2Template;
import jkern.mapper.models.TagLookupGraph;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TagConfigParser {
   private final V2Template template;

   public TagLookupGraph parse() throws IOException {
        String filename = template.getFiles().get("lookup_table_file");
        TagLookupGraph graph = new TagLookupGraph(new HashMap<>());

        try(InputStream in = getClass().getClassLoader()
            .getResourceAsStream(filename)) {

            InputStreamReader is = new InputStreamReader(in);
            BufferedReader buff = new BufferedReader(is);

            String line = buff.readLine();

            for (line = buff.readLine(); line != null; line = buff.readLine()) {
                StringTokenizer st = new StringTokenizer(line, ",");

                Integer port = Integer.parseInt(st.nextToken());
                Integer protocol = template.getProtocols().get(st.nextToken().toLowerCase());
                String tag = st.nextToken().toUpperCase();

                graph.getAdjList().putIfAbsent(protocol, new HashMap<>());
                graph.getAdjList().get(protocol).put(port, tag);
            }
        }

        return graph;
     }

}
