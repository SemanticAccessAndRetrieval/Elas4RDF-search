package gr.forth.ics.isl.elas4rdfrest.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packages and returns to Controller the Response of a request.
 */
public class Response {

    private Map<String, Object> results;

    public Response(Triples triples) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
        this.results.put("total_triples", triples.getTotal_results());
    }

    public Response(Entities entities) {
        this.results = new HashMap<>();
        this.results.put("entities", entities.getResults());
        this.results.put("total_entities", entities.getTotal_res());
    }

    public Response(Triples triples, Entities entities) {
        this.results = new HashMap<>();
        this.results.put("triples", triples.getResults());
        this.results.put("entities", entities.getResults());
        this.results.put("total_triples", triples.getTotal_results());
        this.results.put("total_entities", entities.getTotal_res());
    }

    public Response(String error) {

    }

    public Map<String, Object> getResults() {
        return results;
    }
}
