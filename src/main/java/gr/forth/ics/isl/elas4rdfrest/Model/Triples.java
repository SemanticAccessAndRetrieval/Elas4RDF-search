package gr.forth.ics.isl.elas4rdfrest.Model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Models the return type of requests on <host:port>/elas4rdf/triples
 */
public class Triples {

    private List<Map<String, String>> results;


    /**
     * Parses @param hits returned by Elasticsearch HighLevel client
     * and creates results as a List of Map<String,String> with keys:
     * "sub", "pre", "obj", "sub_ext", "obj_ext", "score"
     *
     * @param hits : Elasticsearch HighLevel answer
     */
    public Triples(SearchHits hits) {
        results = new ArrayList<>();

        for (SearchHit hit : hits) {

            Map<String, String> result = new HashMap<>();
            boolean literal = false;
            String objNspace = "", objK = "", obj;

            Map<String, Object> sourceMap = hit.getSourceAsMap();
            String sub_ext = "", obj_ext = "";

            if (sourceMap.containsKey("rdfs_comment_sub")) {
                sub_ext = sourceMap.get("rdfs_comment_sub").toString();
            }
            if (sourceMap.containsKey("rdfs_comment_obj")) {
                obj_ext = sourceMap.get("rdfs_comment_obj").toString();
            }

            result.put("sub", sourceMap.get("subjectNspaceKeys").toString() + "/" + sourceMap.get("subjectKeywords"));
            result.put("pre", sourceMap.get("predicateNspaceKeys").toString() + "/" + sourceMap.get("predicateKeywords"));

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

            result.put("obj", obj);
            result.put("sub_ext", sub_ext);
            result.put("obj_ext", obj_ext);
            result.put("score", String.valueOf(hit.getScore()));

            results.add(result);

        }

    }

    /**
     * Parses @param jsonBody returned by Elasticsearch LowLevel client
     * and creates results as a List of Map<String,String> with keys:
     * "sub", "pre", "obj", "sub_ext", "obj_ext", "score"
     *
     * @param jsonBody : Elasticsearch LowLevel answer
     */
    public Triples(String jsonBody) {

        results = new ArrayList<>();

        try {
            JsonObject jsonObject = new JsonParser().parse(jsonBody).getAsJsonObject();
            JsonArray arr = jsonObject.getAsJsonObject("hits").getAsJsonArray("hits");

            for (int i = 0; i < arr.size(); i++) {

                Map<String, String> result = new HashMap<>();
                JsonObject hit = arr.get(i).getAsJsonObject();
                JsonObject hit_src = arr.get(i).getAsJsonObject().get("_source").getAsJsonObject();
                boolean literal = false;
                String objNspace = "", objK = "", obj;
                String sub_ext = "", obj_ext = "";

                result.put("sub", hit_src.get("subjectNspaceKeys").getAsString() + "/" + hit_src.get("subjectKeywords").getAsString());
                result.put("pre", hit_src.get("predicateNspaceKeys").getAsString() + "/" + hit_src.get("predicateKeywords").getAsString());

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

                result.put("obj", obj);

                try {
                    result.put("sub_ext", hit_src.get("rdfs_comment_sub").toString());
                    result.put("obj_ext", hit_src.get("rdfs_comment_obj").toString());
                } catch (Exception e) {

                }
                result.put("score", hit.get("_score").getAsString());

                results.add(result);

            }
        } catch (Exception e) {
            Map<String, String> result = new HashMap<>();
            result.put("error", "Internal error, JSON parse error.");
            results.add(result);
        }
    }

    public List<Map<String, String>> getResults() {
        return results;
    }

}
