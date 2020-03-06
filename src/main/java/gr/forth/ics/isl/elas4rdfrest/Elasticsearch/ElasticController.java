package gr.forth.ics.isl.elas4rdfrest.Elasticsearch;

import gr.forth.ics.isl.elas4rdfrest.Controller;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;


public class ElasticController {

    private static RestClient restLowClient;
    private static RestHighLevelClient restHighClient;
    private final String host = "localhost";
    private final int port = 9200;

    public ElasticController() {
        initConnections();
    }


    /**
     * Starts all connections on both High and Low level clients
     */
    private void initConnections() {

        // Starting a Rest Client (high & low)
        restLowClient = RestClient.builder(
                new HttpHost(host, port, "http"),
                new HttpHost(host, port + 1, "http")).build();

        restHighClient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, 9200, "http"),
                        new HttpHost(host, port + 1, "http")));

        // Configuring header for future requests
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));
        Header[] defaultHeaders = new Header[]{new BasicHeader("header", "value")};
        builder.setDefaultHeaders(defaultHeaders);

    }

    /**
     * Stops all connections on both High and Low level clients
     *
     * @throws IOException
     */
    public static void closeConnection() throws IOException {
        restLowClient.close();
        restHighClient.close();
    }

    /**
     * Performs a HighLevel client search. Params are given through
     * the REST API.
     *
     * @param index    : name of Elasticsearch index
     * @param field    : values include : "subjectKeywords", "objectKeywords", "allKeywords"
     * @param keywords : input query
     * @return
     * @throws IOException
     */
    public SearchHits restHigh(String index, String field, String keywords) throws IOException {

        /* Initializing a search request and a search source builder */
        SearchRequest searchRequest = new SearchRequest(Controller.INDEX_NAME);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /* Highlight for source builder */
        HighlightBuilder highlightBuilder = new HighlightBuilder();

        switch (field) {

            /* Case allKeywords a multi-match query must be build.*/
            /* Different implementations for allKeywords
             *   1st : multi match query builder and cross fields
             *   2nd : single query with allKeywords field ( supports fuzziness)
             *
             */
            case "allKeywords":

                float ext_b = 0;
                if (index.contains("eindex")) {
                    ext_b = 1;
                }

                /* Make query builder */
                QueryBuilder allQueryBuilder = QueryBuilders
                        .multiMatchQuery(keywords, "subjectKeywords", "predicateKeywords", "objectKeywords",
                                "subjectNspaceKeys", "predicateNspaceKeys", "objectNspaceKeys", "rdfs_comment_sub", "rdfs_comment_obj")
                        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                        .field("subjectKeywords")
                        .field("predicateKeywords")
                        .field("objectKeywords", 2f)
                        .field("rdfs_comment_sub", ext_b)
                        .field("rdfs_comment_obj", ext_b)
                        .tieBreaker(0.1f);

                searchSourceBuilder.query(allQueryBuilder);

                /* Highlight specific field(s) */
                if (Controller.highlightResults) {
                    highlightBuilder.field("subjectKeywords");
                    highlightBuilder.field("predicateKeywords");
                    highlightBuilder.field("objectKeywords");
                    //highlightBuilder.field("subjectNspaceKeys");
                    //highlightBuilder.field("predicateNspaceKeys");
                    //highlightBuilder.field("objectNspaceKeys");
                    //highlightBuilder.postTags("predicateNspace");
                    highlightBuilder.preTags("<strong>");
                    highlightBuilder.postTags("</strong>");
                }

                break;


            /* Case "subjectKeywords", "predicateKeywords", "objectKeywords" a single-match query must be build.*/
            default:

                /* Specifying a query builder with different parameters for the search request */
                //QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(field);
                //MatchQueryBuilder defaultQueryBuilder = new MatchQueryBuilder(field, keywords)
                //        .fuzziness(Fuzziness.AUTO)
                //        .operator(Operator.OR);

                /* Highlight specific field */
                if (Controller.highlightResults) {
                    highlightBuilder.field(field);
                    highlightBuilder.preTags("<strong>");
                    highlightBuilder.postTags("</strong>");
                }

                QueryBuilder qb = QueryBuilders
                        .boolQuery()
                        .must(QueryBuilders.multiMatchQuery(keywords, field));

                /* Applying query builder to source buidler */
                searchSourceBuilder.query(qb);

                break;

        }

        /* Sort results based on score(descending) */
        searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        searchSourceBuilder.size(Controller.LIMIT_RESULTS);

        /* Applying source builder upon request */
        searchRequest.source(searchSourceBuilder.highlighter(highlightBuilder));

        /* Retrieving a search response */
        SearchResponse searchResponse = restHighClient.search(searchRequest);

        Controller.elasticTook = searchResponse.getTook();
        return searchResponse.getHits();
    }

    /**
     * Performs a LowLevel client search. Param is given through
     * the REST API 'body'.
     *
     * @param body : JSON low-level syntax for Elasticsearch
     * @return
     * @throws IOException
     */
    public String restLow(String body) throws IOException {

        HttpEntity entity = new NStringEntity(body, ContentType.APPLICATION_JSON);
        Request request = new Request("GET", "/" + Controller.INDEX_NAME + "/_search");
        request.setEntity(entity);
        request.addParameter("pretty", "true");
        request.addParameter("size", Integer.toString(Controller.LIMIT_RESULTS));
        String responseBody = EntityUtils.toString(restLowClient.performRequest(request).getEntity());

        return responseBody;
    }

    /**
     * Analyzes (tokenizes, stems ..) an input text of keywords.
     *
     * @param field : analyze based on particular field
     * @param text  : input keywords
     * @return
     */
    public Set<String> analyze(String field, String text) {

        Set<String> analyzedKeywords = new HashSet<>();
        AnalyzeRequest request = new AnalyzeRequest();
        request.index(Controller.INDEX_NAME);
        request.field(field);
        request.text(text);

        try {

            AnalyzeResponse response = restHighClient.indices().analyze(request, RequestOptions.DEFAULT);
            List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();


            for (AnalyzeResponse.AnalyzeToken token : tokens) {
                analyzedKeywords.add(token.getTerm());
            }
        } catch (IOException e) {

        }

        return analyzedKeywords;

    }

}
