package jkern.mapper.models;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter @RequiredArgsConstructor
public class TagLookupGraph {
    private final Map<Integer, Map<Integer, String>> adjList;
}
