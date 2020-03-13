package gr.forth.ics.isl.elas4rdfrest.Model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gr.forth.ics.isl.elas4rdfrest.Controller;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Models a GET request of type "triples"
 */
public class Triples {

    private List<Map<String, Object>> results;
    private long total_results = 0;

    /**
     * Parses @param hits returned by Elasticsearch HighLevel client
     * and creates triple-results as a List of Map<String, Object> with keys:
     * "sub", "pre", "obj", "sub_ext", "obj_ext", "score" ..
     *
     * @param hits : Elasticsearch HighLevel answer
     */
    public Triples(SearchHits hits, List<String> indexFieldsList, Map<String, String> indexKeywordsProperties) {
        results = new ArrayList<>();

        int numHit = 0;
        this.total_results = hits.totalHits;

        for (SearchHit hit : hits) {

            if (numHit > Controller.LIMIT_RESULTS) {
                break;
            }

            Map<String, Object> result = new HashMap<>();
            String objNspace = "", objK = "", obj;
            boolean literal = false;

            Map<String, Object> sourceMap = hit.getSourceAsMap();
            Map<String, HighlightField> highlightMap = hit.getHighlightFields();

            Map<String, String> sub_ext = new HashMap<>();
            Map<String, String> pre_ext = new HashMap<>();
            Map<String, String> obj_ext = new HashMap<>();

            String subjectKeywordsField = "subjectKeywords";
            String predicateKeywordsField = "predicateKeywords";
            String objectKeywordsField = "objectKeywords";

            /* if a property must replace URI keywords */
            if (indexKeywordsProperties.containsKey("subjectKeywords")) {
                subjectKeywordsField = indexKeywordsProperties.get("subjectKeywords");
            }
            if (indexKeywordsProperties.containsKey("predicateKeywords")) {
                predicateKeywordsField = indexKeywordsProperties.get("predicateKeywords");
            }
            if (indexKeywordsProperties.containsKey("objectKeywords")) {
                objectKeywordsField = indexKeywordsProperties.get("objectKeywords");
            }

            /* add  all 'extended' fields */
            for (String ext_field : indexFieldsList) {


                if (sourceMap.containsKey(ext_field)) {

                    StringBuilder ext_field_b = new StringBuilder(ext_field);
                    if (ext_field.endsWith("_sub")) {
                        int end = ext_field.lastIndexOf("_sub");
                        ext_field_b.replace(end, end + 4, "");
                        sub_ext.put(ext_field_b.toString(), sourceMap.get(ext_field).toString());
                    } else if (ext_field.endsWith("_pre")) {
                        int end = ext_field.lastIndexOf("_pre");
                        ext_field_b.replace(end, end + 4, "");
                        pre_ext.put(ext_field_b.toString(), sourceMap.get(ext_field).toString());
                    } else if (ext_field.endsWith("_obj")) {
                        int end = ext_field.lastIndexOf("_obj");
                        ext_field_b.replace(end, end + 4, "");
                        obj_ext.put(ext_field_b.toString(), sourceMap.get(ext_field).toString());
                    }

                }

            }

            if (sourceMap.get("objectNspaceKeys").toString().equals("")) {
                literal = true;
            }

            objNspace += sourceMap.get("objectNspaceKeys");
            objK += sourceMap.get("objectKeywords");

            if (literal) {
                objK = "\"" + objK + "\"";
                obj = objK;
            } else {
                obj = objNspace + "/" + objK;
            }

            result.put("sub", sourceMap.get("subjectNspaceKeys").toString() + "/" + sourceMap.get("subjectKeywords"));
            result.put("pre", sourceMap.get("predicateNspaceKeys").toString() + "/" + sourceMap.get("predicateKeywords"));
            result.put("obj", obj);

            /* handle keywords */
            if (sourceMap.containsKey(subjectKeywordsField)) {
                result.put("sub_keywords", sourceMap.get(subjectKeywordsField).toString());
            } else {
                result.put("sub_keywords", sourceMap.get("subjectKeywords").toString());
            }
            if (sourceMap.containsKey(predicateKeywordsField)) {
                result.put("pre_keywords", sourceMap.get(predicateKeywordsField).toString());
            } else {
                result.put("pre_keywords", sourceMap.get("predicateKeywords").toString());
            }
            if (sourceMap.containsKey(objectKeywordsField)) {
                result.put("obj_keywords", sourceMap.get(objectKeywordsField).toString());
            } else {
                result.put("obj_keywords", sourceMap.get("objectKeywords").toString());
            }

            result.put("sub_ext", sub_ext);
            result.put("pre_ext", pre_ext);
            result.put("obj_ext", obj_ext);

            /* include highlighted fields (replace existing) */
            if (Controller.highlightResults) {
                if (highlightMap.containsKey(subjectKeywordsField)) {
                    result.put("sub_keywords", highlightMap.get(subjectKeywordsField).fragments()[0].string());
                }
                if (highlightMap.containsKey(predicateKeywordsField)) {
                    result.put("pre_keywords", highlightMap.get(predicateKeywordsField).fragments()[0].string());
                }
                if (highlightMap.containsKey(objectKeywordsField)) {
                    result.put("obj_keywords", highlightMap.get(objectKeywordsField).fragments()[0].string());
                }

                /* add  all 'extended' fields */
                for (String ext_field : indexFieldsList) {

                    if (highlightMap.containsKey(ext_field)) {

                        StringBuilder ext_field_b = new StringBuilder(ext_field);
                        if (ext_field.endsWith("_sub")) {
                            int end = ext_field.lastIndexOf("_sub");
                            ext_field_b.replace(end, end + 4, "");
                            sub_ext.put(ext_field_b.toString(), highlightMap.get(ext_field).fragments()[0].string());
                            result.put("sub_ext", sub_ext);
                        } else if (ext_field.endsWith("_pre")) {
                            int end = ext_field.lastIndexOf("_pre");
                            ext_field_b.replace(end, end + 4, "");
                            pre_ext.put(ext_field_b.toString(), highlightMap.get(ext_field).fragments()[0].string());
                            result.put("pre_ext", pre_ext);
                        } else if (ext_field.endsWith("_obj")) {
                            int end = ext_field.lastIndexOf("_obj");
                            ext_field_b.replace(end, end + 4, "");
                            obj_ext.put(ext_field_b.toString(), highlightMap.get(ext_field).fragments()[0].string());
                            result.put("obj_ext", obj_ext);
                        }

                    }

                }
            }


            result.put("score", String.valueOf(hit.getScore()));
            results.add(result);

            numHit++;

        }

    }

    /**
     * Parses @param jsonBody returned by Elasticsearch LowLevel client
     * and creates triple-results as a List of Map<String,String> with keys:
     * "sub", "pre", "obj", "sub_ext", "obj_ext", "score" ..
     *
     * @param jsonBody : Elasticsearch LowLevel answer
     */
    public Triples(String jsonBody, List<String> indexFieldsList, Map<String, String> indexKeywordsProperties) {

        results = new ArrayList<>();
        int numHit = 0;


        try {
            JsonObject jsonObject = new JsonParser().parse(jsonBody).getAsJsonObject();
            JsonArray arr = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");
            this.total_results = Long.parseLong(jsonObject.getAsJsonObject("hits").get("total").getAsString());

            for (int i = 0; i < arr.size(); i++) {

                if (numHit > Controller.LIMIT_RESULTS) {
                    break;
                }
                Map<String, Object> result = new HashMap<>();
                JsonObject hit = arr.get(i).getAsJsonObject();
                JsonObject hit_src = arr.get(i).getAsJsonObject().get("_source").getAsJsonObject();
                String objNspace = "", objK = "", obj;
                boolean literal = false;
                Map<String, String> sub_ext = new HashMap<>();
                Map<String, String> pre_ext = new HashMap<>();
                Map<String, String> obj_ext = new HashMap<>();

                String subjectKeywordsField = "subjectKeywords";
                String predicateKeywordsField = "predicateKeywords";
                String objectKeywordsField = "objectKeywords";

                /* if a property must replace URI keywords */
                if (indexKeywordsProperties.containsKey("subjectKeywords")) {
                    subjectKeywordsField = indexKeywordsProperties.get("subjectKeywords");
                }
                if (indexKeywordsProperties.containsKey("predicateKeywords")) {
                    predicateKeywordsField = indexKeywordsProperties.get("predicateKeywords");
                }
                if (indexKeywordsProperties.containsKey("objectKeywords")) {
                    objectKeywordsField = indexKeywordsProperties.get("objectKeywords");
                }


                if (hit_src.get("objectNspaceKeys").getAsString().equals("")) {
                    literal = true;
                }

                objNspace += hit_src.get("objectNspaceKeys").getAsString();
                objK += hit_src.get("objectKeywords").getAsString();

                if (literal) {
                    objK = "\"" + objK + "\"";
                    obj = objK;
                } else {
                    obj = objNspace + "/" + objK;
                }

                /* add  all 'extended' fields */
                for (String ext_field : indexFieldsList) {

                    if (hit_src.get(ext_field) != null) {

                        StringBuilder ext_field_b = new StringBuilder(ext_field);
                        if (ext_field.endsWith("_sub")) {
                            int end = ext_field.lastIndexOf("_sub");
                            ext_field_b.replace(end, end + 4, "");
                            sub_ext.put(ext_field_b.toString(), hit_src.get(ext_field).toString());
                        } else if (ext_field.endsWith("_pre")) {
                            int end = ext_field.lastIndexOf("_pre");
                            ext_field_b.replace(end, end + 4, "");
                            pre_ext.put(ext_field_b.toString(), hit_src.get(ext_field).toString());
                        } else if (ext_field.endsWith("_obj")) {
                            int end = ext_field.lastIndexOf("_obj");
                            ext_field_b.replace(end, end + 4, "");
                            obj_ext.put(ext_field_b.toString(), hit_src.get(ext_field).toString());
                        }

                    }


                }

                result.put("sub", hit_src.get("subjectNspaceKeys").getAsString() + "/" + hit_src.get("subjectKeywords").getAsString());
                result.put("pre", hit_src.get("predicateNspaceKeys").getAsString() + "/" + hit_src.get("predicateKeywords").getAsString());
                result.put("obj", obj);

                result.put("sub_keywords", hit_src.get(subjectKeywordsField).getAsString());
                result.put("pre_keywords", hit_src.get(predicateKeywordsField).getAsString());
                result.put("obj_keywords", hit_src.get(objectKeywordsField).getAsString());

                result.put("sub_ext", sub_ext);
                result.put("pre_ext", pre_ext);
                result.put("obj_ext", obj_ext);

                result.put("score", hit.get("_score").getAsString());

                results.add(result);

                numHit++;

            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "Internal error, JSON parse error.");
            results.add(result);
        }
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public long getTotal_results() {
        return total_results;
    }
}
