package gr.forth.ics.isl.elas4rdfrest;

import java.io.IOException;

import gr.forth.ics.isl.elas4rdfrest.Elasticsearch.ElasticController;
import gr.forth.ics.isl.elas4rdfrest.Model.Entities;
import gr.forth.ics.isl.elas4rdfrest.Model.Triples;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {


    public static String INDEX_NAME = "mini-eindex";
    public static int LIMIT_RESULTS = 20;
    public static TimeValue elasticTook;
    private static final ElasticController elasticControl = new ElasticController();

    /**
     * @param query : input query
     * @param size  : input size (default = 10)
     * @param index : input index
     * @param field : input fields (e.g. subjectKeywords .. -> default: allKeywords)
     * @param body  : use instead of parameters (low-level request)
     * @return
     * @throws IOException
     */
    @GetMapping("/triples")
    public Triples triples(@RequestParam(value = "query", defaultValue = "") String query,
                           @RequestParam(value = "size", defaultValue = "10") String size,
                           @RequestParam(value = "index", defaultValue = "bindex") String index,
                           @RequestParam(value = "field", defaultValue = "allKeywords") String field,
                           @RequestBody(required = false) String body
    ) throws IOException {

        try {
            Controller.LIMIT_RESULTS = Integer.parseInt(size);
        } catch (NumberFormatException e) {
        }

        if (!index.equals("bindex")) {
            Controller.INDEX_NAME = index;
        }

        // low-level client -> use body
        if (query.isEmpty()) {
            String response = elasticControl.restLow(body);
            return new Triples(response);
        }
        // high-level client -> use param 'query'
        else {
            SearchHits hits = elasticControl.restHigh(index, field, query);
            return new Triples(hits);
        }
    }

    // TODO
    @GetMapping("/entities")
    public Entities entities(@RequestParam(value = "query") String query, @RequestParam(value = "size", defaultValue = "100") String size) {
        return null;
    }

}
