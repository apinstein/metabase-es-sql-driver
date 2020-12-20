# Adding Support to Metabase for Elasticsearch

Modern versions of elasticsearch (ship with a JDBC driver)[https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-jdbc.html], which converts all normal SQL requests into compatible ESQL and communicates with the ES backend.

This project maintains a metabase driver for this setup.

## Setting up a test ES

This repo contains a (full ES+Kibana)[https://www.elastic.co/guide/en/elastic-stack-get-started/current/get-started-docker.html] stack.

### Boot up sample ES stack

```
docker-compose up

# test
curl -X GET "localhost:9200/_cat/nodes?v&pretty"
```

### Add sample data

Open the (local Kibana Server)[http://localhost:5601], click on the "Sample Data" tab, and load the "Sample Flight Data". Click "Dashboard" when done to verify it's all working.




