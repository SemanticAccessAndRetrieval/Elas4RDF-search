package gr.forth.ics.isl.elas4rdfrest.Model;


import gr.forth.ics.isl.elas4rdfrest.Controller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Models a GET request of type "entities"
 */
public class Entities {

    private List<Map<String, Object>> results;
    private Set<String> analyzedQueryTokens;
    private final double e = Math.exp(1);

    private double max_score = 0;

    public Entities(String query, List<Map<String, Object>> triplesRes, String index) {

        /* store analyzed (e.g. stemming) query keywords */
        analyzedQueryTokens = Controller.elasticControl.analyze("subjectKeywords", query, index);

        System.out.println("#### entities ####");
        System.out.println("index: " + index);
        System.out.println("aggregation-penalty: " + Controller.aggregationPenalty);
        System.out.println("query: " + query);

        createEntities(triplesRes, index);

    }

    /**
     * Constructs a ranking list of entities by grouping (aggregating) triplesRes
     * of the same URI (expressed either in subject or object)
     *
     * @param triplesRes : ranked list of triplesRes
     */
    private void createEntities(List<Map<String, Object>> triplesRes, String index) {

        Map<String, Double> entitiesGain = new HashMap<>();
        Map<String, Object> entitiesExt = new HashMap<>();

        double prev_score = 0;
        double max_score = 0, min_score = 0;
        int i = 1;

        if (!triplesRes.isEmpty()) {
            max_score = Double.parseDouble(triplesRes.get(0).get("score").toString());
            min_score = Double.parseDouble(triplesRes.get(triplesRes.size() - 1).get("score").toString());
        }

        /* construct entities */
        for (Map<String, Object> triple : triplesRes) {

            double score = Double.parseDouble(triple.get("score").toString());
            double norm_score = 1;

            String subject = triple.get("sub").toString();
            String object = triple.get("obj").toString();
            String sub_keys = triple.get("sub_keywords").toString();
            String obj_keys = triple.get("obj_keywords").toString();

            /* normalize score */
            if (max_score != min_score) {
                //norm_score = score / max_score;
                norm_score = (score - min_score) / (max_score - min_score);
            }

            /* Store entities based on subject OR/AND object */
            if (Controller.isResource(subject)) {
                /* apply aggregation penalty */
                double local_norm = norm_score * calculateAggregationPenalty("subjectKeywords", sub_keys, index);

                /* calculate the 'ndcg-like, log-based' gain */
                double localGain = (Math.pow(2, local_norm) - 1) / (Math.log(i + 1) / Math.log(2));

                entitiesGain.compute(subject, (k, v) -> (v == null) ? localGain : v + localGain);
                entitiesExt.putIfAbsent(subject, triple.get("sub_ext"));
            }

            if (Controller.isResource(object)) {
                /* apply aggregation penalty */
                double local_norm = norm_score * calculateAggregationPenalty("objectKeywords", obj_keys, index);

                /* calculate the 'ndcg-like, log-based' gain */
                double localGain = (Math.pow(2, local_norm) - 1) / (Math.log(i + 1) / Math.log(2));

                entitiesGain.compute(object, (k, v) -> (v == null) ? localGain : v + localGain);
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

        /* store max-score of entities for normalization */
        try {
            String[] max_e = entitiesGain.entrySet().toArray()[0].toString().split("=");
            this.max_score = Double.parseDouble(max_e[max_e.length - 1]);

        } catch (Exception e) {
            System.err.println("Elas4RDF-rest, error when parsing entities - number format of score.\n\t" + e.getMessage());
        }

        /* create the response */
        for (Map.Entry<String, Double> e : entitiesGain.entrySet()) {
            String entity = e.getKey();
            Double gain = e.getValue();
            Map<String, Object> entityRes = new HashMap<>();

            entityRes.put("entity", entity);
            entityRes.put("gain", Double.toString(gain));
            entityRes.put("score", Double.toString(getNormScore(gain)));
            entityRes.put("ext", entitiesExt.get(entity));

            this.results.add(entityRes);

        }

    }

    /**
     * If a resource (URI) does not a contain (multiple) query keywords -> apply aggregation penalty
     *
     * @param keywords : of the URI
     * @return penalty
     */
    private double calculateAggregationPenalty(String field, String keywords, String index) {

        if (!Controller.aggregationPenalty) {
            return 1;
        }

        Set<String> analyzedKeywords = Controller.elasticControl.analyze(field, keywords, index);
        analyzedKeywords.retainAll(analyzedQueryTokens);

        /*  t : number of common terms between URI keywords & query
         *  n : number of query keywords
         * */
        int t = analyzedKeywords.size();
        int n = analyzedQueryTokens.size();

        return (Math.pow(e, t) / (2 * Math.pow(e, n)) + Controller.aggregationFactor);

    }

    /**
     * Normalizes an entity score
     *
     * @param score : triple score
     * @return n_score : normalized score
     */
    private double getNormScore(double score) {
        if (this.max_score == 0) {
            return max_score;
        }

        return score / max_score;

    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    long getTotal_res() {
        return results.size();
    }

}
