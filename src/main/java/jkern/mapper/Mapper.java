package jkern.mapper;

import java.util.HashMap;
import java.util.Map;

import jkern.mapper.config.V2Template;
import jkern.mapper.models.TagLookupGraph;
import jkern.mapper.parser.Statement;
import jkern.mapper.parser.SyntaxTree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
public class Mapper {
    private final String PROTOCOL = "protocol";
    private final String DSTPORT = "dstport";

    private final TagLookupGraph lookupGraph;
    private final V2Template template;

    @AllArgsConstructor
    @ToString
    public static class Pair {
        public String tag;
        public Integer count;
    }

    private final Map<Integer, Map<Integer, Pair>> graph = new HashMap<>();

    public void processLogLine(SyntaxTree tree) {
        Map<String, Integer> treeLookup = tree.getTagLookup();
        Statement protocol = tree.getStatements().get(treeLookup.get(PROTOCOL));
        Statement port = tree.getStatements().get(treeLookup.get(DSTPORT));

        Integer intProtocol = (Integer) protocol.getToken().getValue();
        Integer intPort = (Integer) port.getToken().getValue();

        graph.putIfAbsent(intProtocol, new HashMap<>());
        String tag = lookupGraph.getAdjList()
            .getOrDefault(intProtocol, new HashMap<>())
            .getOrDefault(intPort, "Untagged");

        Pair pair = graph.get(intProtocol).getOrDefault(intPort, new Pair(tag, 0));
        pair.count++;

        graph.get(intProtocol).put(intPort, pair);
    }

    public String outputTagCounts() {
        StringBuilder sb = new StringBuilder();
        sb.append("tag,count\n");

        Map<String, Integer> prefix = new HashMap<>();

        for (Map<Integer, Pair> edges : graph.values()) {
            for (Pair pair : edges.values()) {
                prefix.put(pair.tag, prefix.getOrDefault(pair.tag, 0) + pair.count);
            }
        }

        for (Map.Entry<String, Integer> e : prefix.entrySet()) {
            sb.append(e.getKey()).append(",").append(e.getValue()).append("\n");
        }

        return sb.toString();
    }

    public String outputPPCounts() {
        StringBuilder sb = new StringBuilder();
        sb.append("port,protocol,count\n");

        Map<Integer, String> revProtocol = new HashMap<>();
        for (Map.Entry<String, Integer> e : template.getProtocols().entrySet()) {
            revProtocol.put(e.getValue(), e.getKey());
        }

        Map<Integer, Map<Integer, Integer>> prefix = new HashMap<>();

        for (Map.Entry<Integer, Map<Integer, Pair>> edge : graph.entrySet()) {
            int protocol = edge.getKey();
            for (Map.Entry<Integer, Pair> entry : edge.getValue().entrySet()) {
                int port = entry.getKey();
                int count = entry.getValue().count;
                prefix.putIfAbsent(port, new HashMap<>());
                count += prefix.get(port).getOrDefault(protocol, 0);
                prefix.get(port).put(protocol, count);
            }
        }

        for (Map.Entry<Integer, Map<Integer, Integer>> entry : prefix.entrySet()) {
            int port = entry.getKey();
            for (Map.Entry<Integer, Integer> counts : entry.getValue().entrySet()) {
                int protocol = counts.getKey();
                int count = counts.getValue();
                sb.append(port);
                sb.append(",").append(revProtocol.getOrDefault(protocol, String.valueOf(protocol)));
                sb.append(",").append(count);
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
