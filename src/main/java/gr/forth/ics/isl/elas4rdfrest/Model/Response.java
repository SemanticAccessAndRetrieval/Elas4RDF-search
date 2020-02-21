package gr.forth.ics.isl.elas4rdfrest.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packages and returns to Controller a REST request.
 */
public class Response {

    private Map<String, List<Map<String, String>>> results;

    public Response(Triples triples) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
    }

    public Response(Entities entities) {
        this.results = new HashMap<>();
        this.results.put("entities", entities.getResults());
    }

    public Response(Triples triples, Entities entities) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
        this.results.put("entities", entities.getResults());
    }

    public Response(String error) {

    }

    public Map<String, List<Map<String, String>>> getResults() {
        return results;
    }
}
