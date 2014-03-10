package org.neopay;


import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.handler.WebSocketConnectionCallback;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import static io.undertow.Handlers.websocket;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Neo4jServer{

    static ObjectMapper objectMapper = new ObjectMapper();

    private static final String STOREDIR = "/Users/maxdemarzi/Projects/neo_pay/server/neo4j/data/graph.db";
    private static final String PATHTOCONFIG = "/Users/maxdemarzi/Projects/neo_pay/server/neo4j/conf/";
    private static final RelationshipType RELATED = DynamicRelationshipType.withName("RELATED");

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
                        .addPath("/websocket", websocket(new WebSocketConnectionCallback() {
                            @Override
                            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                                channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                    @Override
                                    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {

                                        String data = message.getData();

                                        try {
                                            List<Map<String, AtomicInteger>> results = getCrossReferences(data);
                                            WebSockets.sendBinary(ByteBuffer.wrap(objectMapper.writeValueAsBytes(results)), channel, null);
                                        } catch (IOException e) {
                                            WebSockets.sendText("Error:" + e.toString(), channel, null);
                                        }

                                    }
                                });
                                channel.resumeReceives();
                            }
                        }))
                ).build();

        server.start();
    }

    private static List<Map<String, AtomicInteger>> getCrossReferences(String data) throws IOException {
        HashMap input = objectMapper.readValue(data, HashMap.class);

        List<Map<String, AtomicInteger>> results = new ArrayList<Map<String, AtomicInteger>>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        IndexHits<Node> ccIndex = graphDb.index().forNodes("ccs").get("cc", input.get("cc"));
        IndexHits<Node> phoneIndex = graphDb.index().forNodes("phones").get("phone", input.get("phone"));
        IndexHits<Node> emailIndex = graphDb.index().forNodes("emails").get("email", input.get("email"));
        IndexHits<Node> ipIndex = graphDb.index().forNodes("ips").get("ip", input.get("ip"));
        nodes.add(ccIndex.getSingle());
        nodes.add(phoneIndex.getSingle());
        nodes.add(emailIndex.getSingle());
        nodes.add(ipIndex.getSingle());

        for (Node node : nodes) {
            HashMap<String, AtomicInteger> crosses = new HashMap<String, AtomicInteger>();
            crosses.put("ccs", new AtomicInteger(0));
            crosses.put("phones", new AtomicInteger(0));
            crosses.put("emails", new AtomicInteger(0));
            crosses.put("ips", new AtomicInteger(0));
            if (node != null) {
                for (Relationship relationship : node.getRelationships(RELATED, Direction.BOTH)) {
                    Node thing = relationship.getOtherNode(node);
                    String type = thing.getPropertyKeys().iterator().next() + "s";
                    crosses.get(type).getAndIncrement();
                }
            }
            results.add(crosses);
        }
        return results;
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
