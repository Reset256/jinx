# Description

Text indexing Java application. Every file you create in the watched 
folder will be added to index as well, removed files also removed from index, changed files 
reindexed completely. Reindexation delay is around 15 seconds.

Files are tokenized word by word. To add your own tokenization algorithm 
implement `Tokenizer.class` interface.  

# Usage as web app 

There are several ways to start this app:

- via `webapi/src/main/java/org/java/indexer/webapi/WebapiApplication.java` and its `main()` method
- via `spring-boot-maven-plugin:run`

To use it you should add folder you want to index via POST request
to `localhost:8085/index`, all files in it will be indexed.

Usage examples of web api are in `jinx.http` file.

# Usage as Library

- build `core` module using maven, add it as a dependency into the project, create `Indexer.class` 
object and use it as library API. 

Example of usage could be found in `IndexController.class` of `webapi` module.   

# Improvements to do:

- WatchService and it's duplicate events
- Way to remove folders and files from index/watchService
- Parallel write access to index
- Inject properties from `application.properties`
- Modify query API to request type with body for querying not only letters  
- Develop way to remove files from watchService in case of failed indexing