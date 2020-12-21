# Adding Support to Metabase for Elasticsearch

Modern versions of elasticsearch (ship with a JDBC driver)[https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-jdbc.html], which converts all normal SQL requests into compatible ESQL and communicates with the ES backend.

This project maintains a metabase driver for this setup.

# TODO

This project is not even alpha quality yet...

* Make sure username/passwords work
* Make sure https support works
* Make sure non-placeholder values for all connection params work
* Impemlement proper defaults for all connection properties
* Implement table discovery

## Setting up a test ES

This repo contains a (full ES+Kibana)[https://www.elastic.co/guide/en/elastic-stack-get-started/current/get-started-docker.html] stack.

### Boot up sample ES stack

```
docker-compose up

# test
curl -X GET "localhost:9200/_cat/nodes?v&pretty"
```

### Add sample Elasticsearch data

Open the (local Kibana Server)[http://localhost:5601], click on the "Sample Data" tab, and load the "Sample Flight Data". Click "Dashboard" when done to verify it's all working.

### Enable Trial license (required to access SQL functionality)

Open the (local Kibana Server's license page)[http://localhost:5601/app/management/stack/license_management] and click "Activate Trial".

## Setting up a test Metabase

(Download a recent Metabase jar([https://www.metabase.com/docs/v0.37.4/operations-guide/running-the-metabase-jar-file.html] and move it to ./metabase/metabase.jar

```
cd metabase/
java -jar metabase.jar
```

Open the (Metabase Server)[http://localhost:3000] and complete the initialization steps. Skip adding a data source.

## Development Environment for Java

(Set up JDK & clojure for dev)[https://purelyfunctional.tv/guide/how-to-install-clojure/].

You'll need to add the new JDK to your path... something like:

```
export PATH=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/bin:$PATH

# Check for JDK...
java -version
> openjdk version "11.0.9.1" 2020-11-04
> OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.9.1+1)
> OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.9.1+1, mixed mode)
```

Once you have the full `clj` stack working...

### Metabase Dependency

I am not sure how/why exactly this works, but the latest version of the metabase code won't be available to this project until you build the driver support from the core metabase project.

```
# elsewhere on your machine...
git clone git@github.com:metabase/metabase.git
cd metabase
lein install-for-building-drivers
```

Failure to do this will result in errors that look like the compiler not seeing functions from metabase-core that you know exist.

### Dev Cycle

Make your changes and run this to compile & re-run metabase.

```
DEBUG=1 LEIN_SNAPSHOTS_IN_RELEASE=true lein uberjar \
 && cp target/uberjar/elasticsearch.metabase-driver.jar ./metabase/plugins 

# kill and restart metabase 
java -jar metabase.jar
```

# Test ideas

select 1

select * from kibana_sample_data_flights
