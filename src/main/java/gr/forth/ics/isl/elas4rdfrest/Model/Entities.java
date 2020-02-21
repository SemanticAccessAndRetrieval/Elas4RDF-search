package gr.forth.ics.isl.elas4rdfrest.Model;


import gr.forth.ics.isl.elas4rdfrest.Controller;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Models the requests of type : entities
 */
public class Entities {

    private List<Map<String, String>> results;

    public Entities(List<Map<String, String>> triples) {
        createEntities(triples);
    }

    public void createEntities(List<Map<String, String>> triples) {

        Map<String, Double> entitiesGain = new HashMap<>();
        Map<String, String> entitiesExt = new HashMap<>();

        double prev_score = 0;
        double max_score = 0, min_score = 0;
        int i = 1;

        if (!triples.isEmpty()) {
            max_score = Double.parseDouble(triples.get(0).get("score"));
            min_score = Double.parseDouble(triples.get(triples.size() - 1).get("score"));
        }

        /* construct entities */
        for (Map<String, String> triple : triples) {

            double score = Double.parseDouble(triple.get("score"));
            double norm_score = 1;
            double gain;

            /* normalize score */
            if (max_score != min_score) {
                norm_score = score / max_score;
            }

            /* calculate the 'ndcg-like, log-based' gain */
            gain = (Math.pow(2, norm_score) - 1) / (Math.log(i + 1) / Math.log(2));

            /* Store entities based on subject OR/AND object */
            String subject = triple.get("sub");
            String object = triple.get("obj");

            if (Controller.isResource(subject)) {
                entitiesGain.compute(subject, (k, v) -> (v == null) ? gain : v + gain);
                entitiesExt.putIfAbsent(subject, triple.get("sub_ext"));
            }

            if (Controller.isResource(object)) {
                entitiesGain.compute(object, (k, v) -> (v == null) ? gain : v + gain);
                entitiesExt.putIfAbsent(object, triple.get("obj_ext"));
            }

            if (prev_score != score) {
                prev_score = score;
                i++;
            }

        }


        /* prepare response */
        this.results = new ArrayList<>();
        entitiesGain = entitiesGain.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        for (Map.Entry<String, Double> e : entitiesGain.entrySet()) {
            String entity = e.getKey();
            Double gain = e.getValue();
            Map<String, String> entityRes = new HashMap<>();

            entityRes.put("entity", entity);
            entityRes.put("gain", Double.toString(gain));
            entityRes.put("ext", entitiesExt.get(entity));

            this.results.add(entityRes);
            System.out.println(entity + " -- " + gain);

        }

    }

    public List<Map<String, String>> getResults() {
        return results;
    }

}
