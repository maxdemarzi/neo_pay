package org.neopay;


import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


public class Neo4jServer {

    static ObjectMapper objectMapper = new ObjectMapper();

    private static final String STOREDIR = "/Users/maxdemarzi/Projects/neo_pay/server/neo4j/data/graph.db";
    private static final String PATHTOCONFIG = "/Users/maxdemarzi/Projects/neo_pay/server/neo4j/conf/";

    static GraphDatabaseService graphDb = new GraphDatabaseFactory()
            .newEmbeddedDatabaseBuilder( STOREDIR )
            .loadPropertiesFromFile( PATHTOCONFIG + "neo4j.properties" )
            .newGraphDatabase();


    public static void main(final String[] args) {
        registerShutdownHook(graphDb);
        Undertow server = Undertow.builder()
                .addListener(7474, "localhost")
                .setHandler(new PathHandler()
                        .addPath("/", new HelloWorldHandler())
                        .addPath("/example/service/crossreference", new CrossReferenceHandler(objectMapper, graphDb))
                ).build();

        server.start();
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

}
