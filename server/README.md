Neo Pay Embedded Server
=======================

A proof of concept online payment risk management system for a payment gateway running Neo4j as an Embedded Server with Undertow.

## Instructions

Follow blog post and instructions in parent folder to create your graph.
Edit the Neo4jServer.java file to point to your graph.db directory:

    private static final String STOREDIR = "your graph.db directory ";
    private static final String PATHTOCONFIG = "your neo4j conf directory";

Compile it and run it:

    mvn clean compile assembly:single
    java -jar target/Neo4jServer-jar-with-dependencies.jar

Go to http://localhost:7474 and you should get a "Hello World".