package gr.forth.ics.isl.elas4rdfrest;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gr.forth.ics.isl.elas4rdfrest.Elasticsearch.ElasticController;
import gr.forth.ics.isl.elas4rdfrest.Model.Entities;
import gr.forth.ics.isl.elas4rdfrest.Model.IndexProfile;
import gr.forth.ics.isl.elas4rdfrest.Model.Response;
import gr.forth.ics.isl.elas4rdfrest.Model.Triples;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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


    public static Map<String, IndexProfile> indexProfilesMap;

    public static int LIMIT_RESULTS = 100;
    public static boolean highlightResults = false;
    public static boolean aggregationPenalty = true;
    public static TimeValue elasticTook;

    public static ElasticController elasticControl;

    private static Set<String> KNOWN_NAME_SPACES
            = Stream.of("http://dbpedia.org/resource", "http://data.gesis.org/claimskg").collect(Collectors.toCollection(HashSet::new));

    @Autowired
    private Environment environment;

    /**
     * Read "application.properties" file
     * and initialize service.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {

        /* initialize Elasticsearch from application.properties file */
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

        indexProfilesMap = new HashMap<>();
        elasticControl = new ElasticController();

        System.out.println("^^^^^ Elas4RDF: initialization completed ^^^^^");
        System.out.println("elastic.address: " + elasticControl.getHost());
        System.out.println("elastic.port: " + elasticControl.getPort());
        System.out.println("^^^^^^^^^^^^");

    }

    /**
     * Handles '/index_initialize' POST request
     *
     * @param jsonBody : input (-d) body
     * @return response : JSON-formatted
     */
    @PostMapping(value = "/initialize_index", consumes = "application/json", produces = "application/json")
    public Response initializeIndex(@RequestBody String jsonBody) {

        Response response;
        IndexProfile indexProfile;

        /* read POST request*/
        try {

            JSONParser parser = new JSONParser();
            JSONObject initObj = (JSONObject) parser.parse(jsonBody);

            /* parse id, index.name & index.fields */
            String id = (String) initObj.get("id");
            String index = (String) initObj.get("index.name");
            JSONObject indexFields = (JSONObject) initObj.get("index.fields");
            JSONObject keywordsProperties = (JSONObject) initObj.get("index.keywords");
            String resourceUri = (String) initObj.get("resource_uri");
            Map<String, Float> indexFieldsMap = new HashMap<>();
            Map<String, String> indexKeywordProps = new HashMap<>();
            Map<String, Object> errorMap = new HashMap<>();

            if (id == null) {
                errorMap.put("initialize_index error", "In POST request '/initialize_index': id is empty");
                return new Response(errorMap);
            }
            if (index == null) {
                errorMap.put("initialize_index error", "In POST request '/initialize_index': index.name is empty");
                return new Response(errorMap);
            }

            /* store index-fields */
            if (indexFields != null) {
                for (Object field : indexFields.keySet()) {
                    Float boost = Float.parseFloat(indexFields.get(field).toString());
                    indexFieldsMap.put((String) field, boost);
                }
            } else {
                errorMap.put("initialize_index error", "In POST request '/initialize_index': index.fields is empty");
                return new Response(errorMap);
            }

            /* store property for URI keywords (may not be specified) */
            if (keywordsProperties != null) {

                for (Object keyword_property : keywordsProperties.keySet()) {
                    indexKeywordProps.put(keyword_property.toString(), keywordsProperties.get(keyword_property).toString());
                }

            }

            if (resourceUri != null && !resourceUri.isEmpty()) {
                this.KNOWN_NAME_SPACES.add(resourceUri);
            }

            /* prepare a confirmation (IndexProfile) response */
            indexProfile = new IndexProfile(id, index, indexFieldsMap, indexKeywordProps);
            this.indexProfilesMap.put(id, indexProfile);

            response = new Response("initialization_results", indexProfile.getResponse());


        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("initialize_index error", "Exception occured: " + e.getMessage());

            response = new Response(errorMap);
        }

        return response;

    }

    /**
     * Handles general GET requests
     *
     * @param query : input query
     * @param id    : input id - correspond to unique IndexProfile
     * @param size  : input size (defaults 100)
     * @param field : input fields (e.g. subjectKeywords .. -> default: allKeywords)
     * @return
     * @throws IOException
     */
    @GetMapping("/")
    public Response highLevelResponse(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "id") String id,
            @RequestParam(value = "size", defaultValue = "100") String size,
            @RequestParam(value = "field", defaultValue = "allKeywords") String field,
            @RequestParam(value = "type", defaultValue = "both") String type,
            @RequestParam(value = "highlightResults", defaultValue = "false") String highlightResults,
            @RequestParam(value = "aggregationPenalty", defaultValue = "true") String aggregationPenalty
    ) throws IOException {

        IndexProfile indexProfile;
        Map<String, Object> errorMap = new HashMap<>();

        /* parse Request Params */
        try {
            Controller.LIMIT_RESULTS = Integer.parseInt(size);

        } catch (NumberFormatException e) {
            Controller.LIMIT_RESULTS = 100;
        }

        if (highlightResults.equals("true")) {
            Controller.highlightResults = true;
        } else if (highlightResults.equals("false")) {
            Controller.highlightResults = false;
        }

        if (aggregationPenalty.equals("false")) {
            Controller.aggregationPenalty = false;
        } else if (aggregationPenalty.equals("true")) {
            Controller.aggregationPenalty = true;
        }

        if (indexProfilesMap.containsKey(id)) {
            indexProfile = indexProfilesMap.get(id);
        } else {
            errorMap.put("GET /", "Requested id '" + id + "' does not exist. Perform a POST '/initialize_index' request first.");
            return new Response(errorMap);
        }

        /* Serve response based on the Request Param 'type' */
        Triples triples;
        Entities entities;
        Response response;

        switch (type) {
            case "triples":
                triples = getTriples(query, "", indexProfile);
                response = new Response(triples);
                break;
            case "entities":
                triples = getTriples(query, "", indexProfile);
                entities = getEntities(query, triples, indexProfile.getIndex_name());
                response = new Response(entities);
                break;
            case "both":
                triples = getTriples(query, "", indexProfile);
                entities = getEntities(query, triples, indexProfile.getIndex_name());
                response = new Response(triples, entities);
                break;
            default:
                errorMap.put("GET /", "Error, unrecognized type : " + type);
                response = new Response(errorMap);
                break;
        }

        return response;

    }

    /**
     * @param id : unique id of dataset
     * @return a Response containing all
     * properties: 'id', 'index', 'fields'
     */
    @GetMapping("/get_properties")
    public Response getProperties(@RequestParam(value = "id") String id) {

        IndexProfile indexProfile;
        Map<String, Object> errorMap = new HashMap<>();

        if (indexProfilesMap.containsKey(id)) {
            indexProfile = indexProfilesMap.get(id);
        } else {
            errorMap.put("GET /properties", " requested id '" + id + "' does not exist. Perform a POST '/initialize_index' request first.");
            return new Response(errorMap);
        }


        /* prepare & send Response */
        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put("id", id);
        propertiesMap.put("index", indexProfile.getIndex_name());
        propertiesMap.put("fields", indexProfile.getIndex_fields_map());

        return new Response(id + "_properties", propertiesMap);

    }

    /**
     * Handles Low-Level syntax requests.
     *
     * @param body  : JSON string containing query expressed in ES DSL
     * @param index : index name
     * @param type  : return type ('triples', 'entities', 'both')
     * @param size  : size of the response (defaults 100)
     * @return
     * @throws IOException
     */
    @GetMapping("/low_level")
    public Response lowLevelResponse(
            @RequestBody() String body,
            @RequestParam(value = "index") String index,
            @RequestParam(value = "type", defaultValue = "both") String type,
            @RequestParam(value = "size", defaultValue = "100") String size
    ) throws IOException {

        Response response;
        Triples triples;
        Entities entities;
        Map<String, Object> erroMap = new HashMap<>();

        try {
            Controller.LIMIT_RESULTS = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            Controller.LIMIT_RESULTS = 100;
        }

        //TODO, remove hard-coded id
        String id = "dbpedia";
        IndexProfile indexProfile = indexProfilesMap.get(id);
        //

        switch (type) {
            case "triples":
                triples = getTriples("", body, indexProfile);
                response = new Response(triples);
                break;
            case "entities":
                triples = getTriples("", body, indexProfile);
                entities = getEntities("", triples, index);
                response = new Response(entities);
                break;
            case "both":
                triples = getTriples("", body, indexProfile);
                entities = getEntities("", triples, index);
                response = new Response(triples, entities);
                break;
            default:
                erroMap.put("GET /low_level", "Error, unrecognized type : " + type);
                response = new Response(erroMap);
                break;
        }

        return response;

    }

    @RequestMapping(value = "/error")
    public Map<String, Object> error(HttpServletRequest request) throws IOException {
        Map<String, Object> errorMap = new LinkedHashMap<>();
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = "";
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
                errorMap.put("Error", "Error 404 : Not Found");
                return errorMap;
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                errorMap.put("Error", "Server Error 505: " + exception);

                /* Print help message for URL params */
                if (param_error) {
                    errorMap.put("Help", Response.getHelpMessage());
                }

                return errorMap;

            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                ;
                errorMap.put("Error", "400 : Bad Request");
                errorMap.put("Help", Response.getHelpMessage());

                return errorMap;

            }

        }
        errorMap.put("Error", "An undefined exception has occured");
        return errorMap;
    }

    @Override
    public String getErrorPath() {
        return "/error";

    }


    /**
     * Helper function
     */
    public Triples getTriples(String query, String body, IndexProfile indexProfile) throws IOException {

        /* low-level client -> use param 'body' */
        if (query.isEmpty()) {
            String elResponse = elasticControl.restLow(body, indexProfile.getIndex_name());
            return new Triples(elResponse, indexProfile.getIndex_fields_list(), indexProfile.getKeywords_properties());
        }
        /* high-level client -> use param 'query' */
        else {
            SearchHits elHits = elasticControl.restHigh(indexProfile.getIndex_name(), query, indexProfile.getIndex_fields_map());
            return new Triples(elHits, indexProfile.getIndex_fields_list(), indexProfile.getKeywords_properties());
        }
    }

    /**
     * Helper function
     */
    public Entities getEntities(String query, Triples triples, String index) {
        return new Entities(query, triples.getResults(), index);
    }

    public static boolean isResource(String fullUri) {
        for (String nameSpace : KNOWN_NAME_SPACES) {
            if (fullUri.contains(nameSpace)) {
                return true;
            }
        }
        return false;
    }

}
