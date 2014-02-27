package org.neopay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrossReferenceHandler implements HttpHandler {

    private static final RelationshipType RELATED = DynamicRelationshipType.withName("RELATED");
    ObjectMapper objectMapper;
    GraphDatabaseService graphDb;

    public CrossReferenceHandler(ObjectMapper objectMapper, GraphDatabaseService graphDb){
        this.objectMapper = objectMapper;
        this.graphDb = graphDb;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        try {
            if (exchange.getRequestMethod().equals(Methods.POST)) {
                exchange.startBlocking();
                final InputStream inputStream = exchange.getInputStream();
                final String body = new String(ByteStreams.toByteArray(inputStream), Charsets.UTF_8);
                HashMap input = objectMapper.readValue(body, HashMap.class);

                List<Map<String, AtomicInteger>> results = new ArrayList<Map<String, AtomicInteger>>();
                ArrayList<Node> nodes = new ArrayList<Node>();
                IndexHits<Node> ccIndex = graphDb.index().forNodes("ccs").get("cc", input.get("cc"));
                IndexHits<Node> phoneIndex = graphDb.index().forNodes("phones").get("phone", input.get("phone"));
                IndexHits<Node> emailIndex = graphDb.index().forNodes("emails").get("email", input.get("email"));
                IndexHits<Node> ipIndex = graphDb.index().forNodes("ips").get("ip", input.get("ip"));
                nodes.add (ccIndex.getSingle());
                nodes.add (phoneIndex.getSingle());
                nodes.add (emailIndex.getSingle());
                nodes.add (ipIndex.getSingle());

                for(Node node : nodes){
                    HashMap<String, AtomicInteger> crosses = new HashMap<String, AtomicInteger>();
                    crosses.put("ccs", new AtomicInteger(0));
                    crosses.put("phones", new AtomicInteger(0));
                    crosses.put("emails", new AtomicInteger(0));
                    crosses.put("ips", new AtomicInteger(0));
                    if(node != null){
                        for ( Relationship relationship : node.getRelationships(RELATED, Direction.BOTH) ){
                            Node thing = relationship.getOtherNode(node);
                            String type = thing.getPropertyKeys().iterator().next() + "s";
                            crosses.get(type).getAndIncrement();
                        }
                    }
                    results.add(crosses);
                }

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
                exchange.getResponseSender().send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(results)));

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
