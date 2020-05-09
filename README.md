# Elas4RDF-search 
Performs **keyword** search over **RDF** data, with classic IR techniques, upon triple-based documents using **Elasticsearch** (ES).

This project initializes a REST API for exploiting the indexes created [here](https://github.com/SemanticAccessAndRetrieval/Elas4RDF-index). 
System's response contains a ranked list of both a) triples and b) entities. 

Requires Java 8 (or later) and a running Elasticsearch (elasticsearch-6.8) instance.

### Install
Build project and package with `mvn package`. The generated .war file inside the `Elas4RDF-search/target` 
folder can be used for deploying the service in any server (e.g. apache-tomcat).

When deployed, application expects an `application.properties` file with options about the ES running instance: `elastic.address=[string]` & `elastic.port=[int]`. 
If file is not found, options default to `localhost:9200`.
## Search service 

#### 1) Initializing an index
Application can serve different indexes (collections) by preserving a state (non-persistent) that corresponds to a given configuration. 

Initialize an index through a `POST /datasets` method with request body a .json file with the following
syntax:

```
  {
     "id": <dataset_identifier>,
     "index.name": <ES_index_name>,
     "index.fields": {
       <field_1> : <field_boost_1>,
       <field_2> : <field_boost_2>,
        ...
     }
   }
```

On success, response contains a confirmation .JSON message. 

Note, this .json file should be automatically created after completing the index process from [here](https://github.com/SemanticAccessAndRetrieval/Elas4RDF-index). An example
can be found in `src/resources/examples/` folder.

#### 2) Performing a Query

Queries are expressed through the `GET` method while requests accept either a high-level or a low-level syntax.

* **High-Level syntax** -> `GET /high-level`

   _required_ `id=[string]` `query=[string]`
   
   _optional_ `size=[int]`  `offset=[int]` `type=[string]`
    
    `id` is the dataset identifier created in **(1)** and
    `query` can include any free-text keywords. 
    
    `size` corresponds to the number of the returned triples, `offset` supports pagination (only for triples) and 
    `type` corresponds to the answer return type: "triples", "entities" or "both" (default).
    
            
* **Low-Level syntax** ->`GET /low-level`    

    _required_ `body=[json]` `index[string]`
    
    _optional_ `size=[int]` `type[string]`
    
    `body` is used for expressing more complicated queries through the use of ES [Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).
    Parameter `index` correponds to the ES index name.
    
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
    
    
     
 example: using the `curl` command a (high-level syntax) request can be expressed as:
 
    `curl --header "Content-Type: application/json" --request GET '<host>:<port>/elas4rdf_rest/high-level/?id=dataset_id&query=the%20beatles'`
      
