Neo Pay POC
===========

A proof of concept online payment risk management system for a payment gateway.

## Instructions


```ruby 
bundle install
rake neo4j:install[enterprise,1.9.6]
cp neo4jextension-1.0.jar neo4j/plugins

edit neo4j/conf/neo4j-server.properties
add the line:
org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.example.extension=/example
tar zxf graphdb.tar.gz -C neo4j/data/.
rake neo4j:start
```

The sample data comes from the transactions.csv which is generated when running rake neo4j:create_csv.
Every 100 items it generates between 1 and 10 additional entries for a given record, mixing up the type and number of properties it changes.

For example:

```ruby
cc,phone,email,ip
4357448,096-241-0858,jade@bechtelarrodriguez.us,139.3.123.176
8606234,746-290-4350,jade@bechtelarrodriguez.us,139.3.123.176
7639259,746-290-4350,jade@bechtelarrodriguez.us,139.3.123.176
7639259,746-290-4350,jade@bechtelarrodriguez.us,97.37.184.243
8970488,227-224-0263,jade@bechtelarrodriguez.us,97.37.184.243
8970488,227-224-0263,brendan@blick.name,97.37.184.243
7201748,227-224-0263,kayden@bergstrom.info,104.53.211.92
````

At this point the Graph should have some data in it and has the unmanaged extension loaded.
Go to the "Power Tool Console", then click on the HTTP Tab.

Type:

```java
get /example/service/helloworld
```

This should return:
```java
==> 200 OK
==> Hello World!
```

Type:

```java
get /example/service/warmup
```

After a few seconds, this should return:
```java
==> 200 OK
==> Warmed up and ready to go!
```

Try a manual call:

```java
post /example/service/crossreference {"cc": "4357448", "phone": "096-241-0858", "email": "jade@bechtelarrodriguez.us", "ip": "139.3.123.176" }
```

This should return:
```java
==> 200 OK
==> [{"ips":1,"emails":1,"ccs":0,"phones":1},{"ips":1,"emails":1,"ccs":1,"phones":0},{"ips":2,"emails":0,"ccs":4,"phones":3},{"ips":0,"emails":1,"ccs":3,"phones":2}]
```

It's a bit crude, but this is what it means:
```ruby
[{"ips":1,"emails":1,"ccs":0,"phones":1}, -- cc returned just 1 item for each cross reference check.
{"ips":1,"emails":1,"ccs":1,"phones":0}, -- phone returned just 1 item for each cross reference check.
{"ips":2,"emails":0,"ccs":4,"phones":3}, -- email returned 2 ips, 4 credit cards and 3 phones.
{"ips":0,"emails":1,"ccs":3,"phones":2}] -- ip returned 3 credit cards and 2 phones.
```

Now run the performance test.

Open the Performance folder in IntelliJ or Eclipse and run the Engine.
You can hit enter twice to accept the defaults and then the test will run.
If you have not warmed up the database first, run the test a few times to get realistic numbers.

The Unmanaged Extension is in the unmanaged-extension folder, you can run the test to see functionality.

To generate a new set of data :

```ruby
rake neo4j:stop
rm -rf neo4j/data/graph.db
rake neo4j:create_csv
rake neo4j:create_graph
rake neo4j:load_graph
rake neo4j:start
```
You will have to replace the contents of performance/src/test/data/test-data.txt with the transactions.csv file to run the performance tests with this new data.

To increase the size of the data or the frequency of multiple entries sharing attributes, edit neo_generate.rb.
