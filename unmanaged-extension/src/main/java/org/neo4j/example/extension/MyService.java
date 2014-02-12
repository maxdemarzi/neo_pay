package org.neo4j.example.extension;

import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.tooling.GlobalGraphOperations;


import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


@Path("/service")
public class MyService {

    ObjectMapper objectMapper = new ObjectMapper();
    private static final RelationshipType RELATED = DynamicRelationshipType.withName("RELATED");

    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        Node start;
        for ( Node n : GlobalGraphOperations.at( db ).getAllNodes() ) {
            n.getPropertyKeys();
            for ( Relationship relationship : n.getRelationships() ) {
                start = relationship.getStartNode();
            }
        }
        for ( Relationship r : GlobalGraphOperations.at(db).getAllRelationships() ) {
            r.getPropertyKeys();
            start = r.getStartNode();
        }
        return "Warmed up and ready to go!";
    }

    @POST
    @Path("/crossreference")
    public Response crossReference(String body, @Context GraphDatabaseService db) throws IOException {
        List<Map<String, AtomicInteger>> results = new ArrayList<Map<String, AtomicInteger>>();
        HashMap input = objectMapper.readValue( body, HashMap.class);
        ArrayList<Node> nodes = new ArrayList<Node>();
        IndexHits<Node> ccIndex = db.index().forNodes("ccs").get("cc", input.get("cc"));
        IndexHits<Node> phoneIndex = db.index().forNodes("phones").get("phone", input.get("phone"));
        IndexHits<Node> emailIndex = db.index().forNodes("emails").get("email", input.get("email"));
        IndexHits<Node> ipIndex = db.index().forNodes("ips").get("ip", input.get("ip"));
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

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

}
