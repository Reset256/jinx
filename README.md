# Task

Text indexing library. To use it you should add folder you want to index via POST request to `localhost:8085/index`, all files in it will be indexed. Every file you create in the watched folder will be added to index as well, removed files also removed from index, changed files reindexed completely. Reindexation delay is around 15 seconds.

Files are tokenized word by word. To add your own tokenization algorithm implement `Tokenizer.class` interface.  

To look for token use GET request to `localhost:8085/index`.

# Improvements to do:

- WatchService and it's duplicate events
- Way to add separate files to index
- Way to remove folders and files from index
- Parallel write access to index
- More available tokenization algorithms and ability to add your own regex for tokenization rules 
- Inject properties from `application.properties`
- Modify query API to request type with body for querying not only letters  