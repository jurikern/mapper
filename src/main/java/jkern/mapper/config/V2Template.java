package jkern.mapper.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class V2Template {
    private Set<String> tag;
    private List<String> output;
    private List<String> keys;
    private Map<String, Integer> protocols;
    private Map<String, String> files;
}
