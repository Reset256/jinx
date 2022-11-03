# Description

Text indexing library. To use it you should add folder you want to index via POST request to `localhost:8085/index`, all files in it will be indexed. Every file you create in the watched folder will be added to index as well, removed files also removed from index, changed files reindexed completely. Reindexation delay is around 15 seconds.

Files are tokenized word by word. To add your own tokenization algorithm implement `Tokenizer.class` interface.  

Usage examples are in `jinx.http` file.

# Start

There are several ways to start this app:

- via `webapi/src/main/java/org/java/indexer/webapi/WebapiApplication.java` and its `main()` method
- via `spring-boot-maven-plugin:run`

# Improvements to do:

- WatchService and it's duplicate events
- Way to remove folders and files from index
- Parallel write access to index
- Inject properties from `application.properties`
- Modify query API to request type with body for querying not only letters  