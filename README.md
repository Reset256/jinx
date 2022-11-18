# Description

Text indexing Java application. Every file you create in the watched 
folder will be added to index as well, removed files also removed from index, changed files 
reindexed completely. Reindexation delay is around 15 seconds.

Files are tokenized word by word by default. Customization available 
via adding regex or using your own tokenization algorithm by implementing
`Tokenizer.class` interface and passing it to `Indexer.class` constructor.  

# Use as web app 

There are several ways to start this app:

- via `webapi/src/main/java/org/java/indexer/webapi/WebapiApplication.java` and its `main()` method
- via `spring-boot-maven-plugin:run`

To use it you should add folder you want to index via POST request
to `localhost:8085/index`, all files in it will be indexed.

## Examples
Create index with custom regex and ignored name
```http
POST localhost:8085/index
Content-Type: application/json

{
"paths": ["/path/to/folder"],
"regEx" : "\\s+",
"ignoredNames": ["ignoredName"]
}
```

Create index with multiple paths
```http
POST localhost:8085/index
Content-Type: application/json

{
"paths": ["path/to/folder","path/to/file.file"]
}
```

Delete index
```http
DELETE localhost:8085/index
```
Query token from index
```http
GET localhost:8085/index/query/dependency
```

# Use as Library

- build `core` module using maven, add it as a dependency into the project, create `Indexer.class` 
object and use it as library API. 

## Examples

Index should be created via one of available constructors.
```java 
Indexer indexer = new Indexer();
```

All the files in the listed folders and single files will be indexed only if filename is not contained in the ignoredNames collection.
```java 
indexer.index(List.of("/path/to/folder", "path/to/file.file"));
```

Query result are aggregated in QueryResult objects.
```java 
QueryResult queryResult = indexer.queryToken("token1");
```

If the index is no longer needed Indexer::close should be called upon Indexer object. It is used to stop file watcher and to prevent memory leakage.
```java 
indexer.close();
indexer = null;
```

# Improvements to do:

- WatchService and it's duplicate events
- Way to remove folders and files from index via Indexer
- Parallel write access to index
- Inject properties from `application.properties`
- Modify query API to request type with body for querying not only letters  
- Develop way to remove files from watchService in case of failed indexing
- Shutting down tokenization process after upon index closing.