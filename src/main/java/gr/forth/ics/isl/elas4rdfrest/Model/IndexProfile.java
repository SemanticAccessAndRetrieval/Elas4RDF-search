package gr.forth.ics.isl.elas4rdfrest.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Models a POST request '/initialize_index' index profile
 */
public class IndexProfile {

    private String id;

    private String index_name;
    private Map<String, Float> index_fields_map;
    private List<String> index_fields_list;
    private Map<String, String> index_keywords_properties;

    private Map<String, Object> response;

    public IndexProfile(String id, String index_name, Map<String, Float> index_fields_map, Map<String, String> index_keywords_properties) {

        response = new HashMap<>();

        this.id = id;
        this.index_name = index_name;
        this.index_fields_map = index_fields_map;
        this.index_fields_list = new ArrayList<>();
        this.index_fields_list.addAll(index_fields_map.keySet());
        this.index_keywords_properties = index_keywords_properties;

        response.put("result", "success");
        response.put("id", id);
        response.put("index.name", index_name);
        response.put("index.fields", index_fields_map);

    }

    public String getIndex_name() {
        return index_name;
    }

    public Map<String, Float> getIndex_fields_map() {
        return index_fields_map;
    }

    public List<String> getIndex_fields_list() {
        return index_fields_list;
    }

    public Map<String, String> getKeywords_properties() {
        return index_keywords_properties;
    }

    public Map<String, Object> getResponse() {
        return response;
    }


}
