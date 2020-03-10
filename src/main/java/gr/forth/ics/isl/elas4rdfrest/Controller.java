package gr.forth.ics.isl.elas4rdfrest;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gr.forth.ics.isl.elas4rdfrest.Elasticsearch.ElasticController;
import gr.forth.ics.isl.elas4rdfrest.Model.Entities;
import gr.forth.ics.isl.elas4rdfrest.Model.Response;
import gr.forth.ics.isl.elas4rdfrest.Model.Triples;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@RestController
public class Controller implements ErrorController {


    public static String INDEX = "";
    public static Map<String, Float> indexFieldsMap;
    public static List<String> indexFieldsList;

    public static int LIMIT_RESULTS = 20;
    public static boolean highlightResults = false;
    public static boolean aggregationPenalty = true;
    public static TimeValue elasticTook;
    public static ElasticController elasticControl;

    private static final Set<String> KNOWN_NAME_SPACES
            = Stream.of("http://dbpedia.org/resource").collect(Collectors.toCollection(HashSet::new));

    @Autowired
    private Environment environment;

    /**
     * Read "application.properties" file
     * and initialize service.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {

        /* store index information */
        if (environment.containsProperty("index.name")) {
            INDEX = environment.getProperty("index.name");
        } else {
            INDEX = "bindex";
        }

        if (environment.containsProperty("index.fields")) {
            indexFieldsMap = new HashMap<>();
            indexFieldsList = new ArrayList<>(0);
            for (String field_entry : environment.getProperty("index.fields").split(";")) {
                String field = field_entry.split("^")[0];
                Float boost = 1f;
                indexFieldsMap.put(field, boost);
                indexFieldsList.add(field);
            }

        } else {
            indexFieldsMap = new HashMap<>();
            indexFieldsList = new ArrayList<>();
        }

        /* initialize Elasticsearch */
        if (environment.containsProperty("elastic.address")) {
            ElasticController.setHost(environment.getProperty("elastic.address"));
        } else {
            ElasticController.setHost("localhost");
        }
        if (environment.containsProperty("elastic.port")) {
            try {
                ElasticController.setPort(Integer.parseInt(environment.getProperty("elastic.port")));
            } catch (NumberFormatException e) {
                System.err.println("Elas4RDF: error in application.properties: elastic.port not an integer. Default port (9200) selected");
                ElasticController.setPort(9200);
            }
        } else {
            ElasticController.setPort(9200);
        }

        elasticControl = new ElasticController();

        System.out.println("^^^^^ Elas4RDF: initialization completed ^^^^^");
        System.out.println("index.name: " + INDEX);
        System.out.println("index.fields: " + indexFieldsMap);
        System.out.println("elastic.address: " + elasticControl.getHost());
        System.out.println("elastic.port: " + elasticControl.getPort());
        System.out.println("^^^^^^^^^^^^");

    }


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
    public Response response(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "index", defaultValue = "") String index,
            @RequestParam(value = "size", defaultValue = "100") String size,
            @RequestParam(value = "field", defaultValue = "allKeywords") String field,
            @RequestParam(value = "type", defaultValue = "both") String type,
            @RequestParam(value = "highlightResults", defaultValue = "false") String highlightResults,
            @RequestParam(value = "aggregationPenalty", defaultValue = "true") String aggregationPenalty,
            @RequestBody(required = false) String body

    ) throws IOException {

        /* parse Request Params */
        try {
            Controller.LIMIT_RESULTS = Integer.parseInt(size);

        } catch (NumberFormatException e) {
            Controller.LIMIT_RESULTS = 10;
        }

        if (!index.isEmpty()) {
            Controller.INDEX = index;
        }

        if (highlightResults.equals("true")) {
            Controller.highlightResults = true;
        }

        if (aggregationPenalty.equals("false")) {
            Controller.aggregationPenalty = false;
        } else if (aggregationPenalty.equals("true")) {
            Controller.aggregationPenalty = true;
        }

        /* Serve response based on the Request Param 'type' */
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
                entities = getEntities(query, triples);
                response = new Response(entities);
                break;
            case "both":
                triples = getTriples(query, body, index, field);
                entities = getEntities(query, triples);
                response = new Response(triples, entities);
                break;
            default:
                response = new Response("Error, unrecognized type : " + type);
                break;
        }

        return response;

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
                error = "Server Error 505" + "<br>" + exception + "<br> <br> <br>";

                /* Print help message for URL params */
                if (param_error) {
                    error += Response.getHelpMessage();
                }

                return error;

            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                error = "Error 400 : BAD REQUEST <br> <br> <br>" +
                        Response.getHelpMessage();

                return error;

            }

        }
        return "error" + status;
    }

    @Override
    public String getErrorPath() {
        return "/error";

    }

    public Triples getTriples(String query, String body, String index, String field) throws IOException {

        /* low-level client -> use param 'body' */
        if (query.isEmpty()) {
            String elResponse = elasticControl.restLow(body);
            return new Triples(elResponse);
        }
        /* high-level client -> use param 'query' */
        else {
            SearchHits elHits = elasticControl.restHigh(index, field, query);
            return new Triples(elHits);
        }
    }

    public Entities getEntities(String query, Triples triples) {
        return new Entities(query, triples.getResults());
    }

    public static boolean isResource(String fullUri) {
        for (Object nameSpace : KNOWN_NAME_SPACES) {
            return fullUri.contains(nameSpace.toString());
        }
        return false;
    }

}
