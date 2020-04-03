package gr.forth.ics.isl.elas4rdfrest.Model;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Packages and returns to Controller the Response of a request.
 */
public class Response {

    private Map<String, Object> results;

    public Response(String resultMessage, Map<String, Object> result) {
        this.results = new HashMap<>();
        this.results.put(resultMessage, result);
    }

    /**
     * Serves GET requests of "type" : triples
     *
     * @param triples
     */
    public Response(Triples triples) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
        this.results.put("total_triples", triples.getTotal_results());
    }

    /**
     * Serves GET requests of "type" : entities
     *
     * @param entities
     */
    public Response(Entities entities) {
        this.results = new HashMap<>();
        this.results.put("entities", entities.getResults());
        this.results.put("total_entities", entities.getTotal_res());
    }

    /**
     * Serves GET requests of "type" : both
     *
     * @param triples
     * @param entities
     */
    public Response(Triples triples, Entities entities) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
        this.results.put("entities", entities.getResults());
        this.results.put("total_triples", triples.getTotal_results());
        this.results.put("total_entities", entities.getTotal_res());
    }

    public Response(Map<String, Object> errorMap) {
        this.results = new HashMap<>();
        this.results.put("error", errorMap);
    }

    public static Map<String, Object> getHelpMessage() {

        Map<String, Object> helpMap = new LinkedHashMap<>();

        /* url params map */
        Map<String, Object> urlParamsMap = new LinkedHashMap<>();
        Map<String, Object> highLevelMap = new LinkedHashMap<>();
        Map<String, Object> lowLevelMap = new LinkedHashMap<>();

        highLevelMap.put("required", Stream.of("query=[string]", "id=[string]").collect(Collectors.toCollection(LinkedHashSet::new)));
        highLevelMap.put("optional", Stream.of("size=[int]", "offset=[int]", "type=[string]", "highlightResults=[boolean]").collect(Collectors.toCollection(LinkedHashSet::new)));

        lowLevelMap.put("required", Stream.of("body=[string]", "index=[string]").collect(Collectors.toCollection(LinkedHashSet::new)));
        lowLevelMap.put("optional", Stream.of("size=[int]", "type=[string]").collect(Collectors.toCollection(LinkedHashSet::new)));

        urlParamsMap.put("High-Level syntax -> 'GET /'", highLevelMap);
        urlParamsMap.put("Low-Level  syntax -> 'GET /low_level'", lowLevelMap);

        helpMap.put("URL-Params", urlParamsMap);

        return helpMap;

    }

    public Map<String, Object> getResults() {
        return results;
    }

}
