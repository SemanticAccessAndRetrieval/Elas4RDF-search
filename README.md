# Elas4RDF-search 
Performs keyword search over RDF data, with classic IR techniques, upon triple-based documents using Elasticsearch (ES).
This project initiliazes a REST API for exploiting the indexes created [here](https://github.com/SemanticAccessAndRetrieval/Elas4RDF-index). 
System's response contains a ranked list of both a) triples and b) entities.


### Install

## Search service 
Queries are expressed through the `GET /` method while requests accept either a high-level or a low-level syntax. URL parameters include:

* **High-Level syntax**
   
   `query=[string]`
   `size=[int]`
   `index=[string]`
   `field=[string]`
   `type=[string]`
    
    where `field` values include: "subjectKeywords", "predicateKeywords", "objectKeywords" and "allKeywords" (default). 
    Parameter `type` corresponds to the answer return type: "triples", "entities" or "both" (default).
            
* **Low-Level syntax**   
    `body=[json]`
    
    where parameter `body` is used for expressing more complicated queries through the use of ES [Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).
     
 Using the `curl` command a request can be expressed as:
 
    `curl --header "Content-Type: application/json" --request GET '<host>:<port>/elas4rdf_rest/?query=the%20beatles&index=my_index'`
      
