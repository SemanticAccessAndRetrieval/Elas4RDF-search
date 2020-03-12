# Elas4RDF-search 
Performs **keyword** search over **RDF** data, with classic IR techniques, upon triple-based documents using **Elasticsearch** (ES).

This project initializes a REST API for exploiting the indexes created [here](https://github.com/SemanticAccessAndRetrieval/Elas4RDF-index). 
System's response contains a ranked list of both a) triples and b) entities. 

Requires Java 8 (or later) and a running Elasticsearch instance.

### Install
Build project and package with `mvn package`. The generated .war file inside the `Elas4RDF-search/target` 
folder can be used for deploying the service in any server (e.g. apache-tomcat).

When deployed, application expects an `application.properties` file with options about the ES running instance: `elastic.address=[string]` & `elastic.port=[int]`. 
If file is not found, options default to `localhost:9200`.
## Search service 

Queries are expressed through the `GET /` method while requests accept either a high-level or a low-level syntax. URL parameters include:

* **High-Level syntax**

   `query=[string]`
   `size=[int]`
   `index=[string]`
   `field=[string]`
   `type=[string]`
    
    where `field` parameter determines the index field in which the input query will be evaluated. Includes "subjectKeywords", "predicateKeywords", "objectKeywords" and "allKeywords" (default).  
    Parameter `type` corresponds to the answer return type: "triples", "entities" or "both" (default).
    
            
* **Low-Level syntax**   

    `body=[json]`
    
    where parameter `body` is used for expressing more complicated queries through the use of ES [Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html). 
    
    e.g. a multi-match type query
   ```
  body = 
  {
    "query": {
      "multi_match" : {
        "query":    "beatles abbey road", 
        "fields": ["subjectKeywords", "objectKeywords^2", "rdfs_comment_sub"],
        "type" : "cross_fields"
      }
    }
  }
    ```
    
    
     
 Using the `curl` command a (high-level syntax) request can be expressed as:
 
    `curl --header "Content-Type: application/json" --request GET '<host>:<port>/elas4rdf_rest/?query=the%20beatles&index=my_index'`
      
