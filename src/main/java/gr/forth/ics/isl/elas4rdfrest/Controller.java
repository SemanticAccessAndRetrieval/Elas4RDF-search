package gr.forth.ics.isl.elas4rdfrest;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gr.forth.ics.isl.elas4rdfrest.Elasticsearch.ElasticController;
import gr.forth.ics.isl.elas4rdfrest.Model.Entities;
import gr.forth.ics.isl.elas4rdfrest.Model.Response;
import gr.forth.ics.isl.elas4rdfrest.Model.Triples;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@RestController
public class Controller implements ErrorController {


    public static String INDEX_NAME = "mini-eindex";
    public static int LIMIT_RESULTS = 20;
    public static TimeValue elasticTook;
    private static final ElasticController elasticControl = new ElasticController();

    private static final Set<String> KNOWN_NAME_SPACES
            = Stream.of("http://dbpedia.org/resource").collect(Collectors.toCollection(HashSet::new));

    /**
     * @param query : input query
     * @param size  : input size (default = 10)
     * @param index : input index
     * @param field : input fields (e.g. subjectKeywords .. -> default: allKeywords)
     * @param body  : use instead of parameters (low-level request)
     * @return
     * @throws IOException
     */
    @GetMapping("/")
    public Response response(@RequestParam(value = "size", defaultValue = "10") String size,
                             @RequestParam(value = "index", defaultValue = "bindex") String index,
                             @RequestParam(value = "query", defaultValue = "") String query,
                             @RequestParam(value = "field", defaultValue = "allKeywords") String field,
                             @RequestParam(value = "type", defaultValue = "both") String type,
                             @RequestBody(required = false) String body
    ) throws IOException {

        try {
            Controller.LIMIT_RESULTS = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            Controller.LIMIT_RESULTS = 10;
        }

        if (!index.equals("bindex")) {
            Controller.INDEX_NAME = index;
        }


        /* Serve response based on the request param 'type' */
        Triples triples;
        Entities entities;
        Response response;

        switch (type) {
            case "triples":
                triples = getTriples(query, body, index, field);
                response = new Response(triples);
                break;
            case "entities":
                triples = getTriples(query, body, index, field);
                entities = getEntities(triples);
                response = new Response(entities);
                break;
            case "both":
                triples = getTriples(query, body, index, field);
                entities = getEntities(triples);
                response = new Response(triples, entities);
                break;
            default:
                response = new Response("Error, unrecognized type : " + type);
                break;
        }

        return response;

    }


    public Triples getTriples(String query, String body, String index, String field) throws IOException {
        // low-level client -> use body
        if (query.isEmpty()) {
            String elResponse = elasticControl.restLow(body);
            return new Triples(elResponse);
        }
        // high-level client -> use param 'query'
        else {
            SearchHits elHits = elasticControl.restHigh(index, field, query);
            return new Triples(elHits);
        }
    }

    public Entities getEntities(Triples triples) {
        return new Entities(triples.getResults());
    }

    public static boolean isResource(String fullUri) {
        for (Object nameSpace : KNOWN_NAME_SPACES) {
            return fullUri.contains(nameSpace.toString());
        }
        return false;
    }

    @RequestMapping(value = "/error")
    public String error(HttpServletRequest request) throws IOException {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = "", exception_type = "";
        boolean param_error = false;

        /* Identify exception */
        if (request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) != null) {
            exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION).toString();

            /* Param error */
            if (exception.toString().contains("java.lang.IllegalArgumentException")) {
                param_error = true;
            }
        }

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            String error = "";

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "Error 404 : NOT FOUND";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                error += "Server Error 505" + "<br>" + exception + "<br> <br> <br>";

                /* Print help message for URL params */
                if (param_error) {
                    error += "<u>URL PARAMS</u>" +
                            "<br> <br>" +
                            "ΗIGH-LEVEL syntax params: <b>query</b>=[string] <b>size</b>=[int] <b>index</b>=[string] <b>field</b>=[string] <b>type</b>=[string] " +
                            "<br> <br>" +
                            "LOW-LEVEL syntax params: <b>body</b>=[json]";
                }
                return error;
            }

        }
        return "error";
    }

    @Override
    public String getErrorPath() {
        return "/error";

    }

}
