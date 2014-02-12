package org.neo4j.example.extension;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import static org.junit.Assert.assertEquals;

public class MyServiceTest {

    private GraphDatabaseService db;
    private MyService service;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final RelationshipType RELATED = DynamicRelationshipType.withName("RELATED");

    @Before
    public void setUp() {
        db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        populateDb(db);
        service = new MyService();
    }

    private void populateDb(GraphDatabaseService db) {
        Transaction tx = db.beginTx();
        try
        {
            Node cc1 = createNode(db, "1", "cc");
            Node cc2 = createNode(db, "2", "cc");
            Node cc3 = createNode(db, "3", "cc");
            Node phone1 = createNode(db, "1234567890", "phone");
            Node phone2 = createNode(db, "0987654321", "phone");
            Node phone3 = createNode(db, "5558675309", "phone");
            Node email1 = createNode(db, "email1@hotmail.com", "email");
            Node email2 = createNode(db, "email2@hotmail.com", "email");
            Node email3 = createNode(db, "email3@hotmail.com", "email");
            Node ip1 = createNode(db, "1.1.1.1", "ip");
            Node ip2 = createNode(db, "2.2.2.2", "ip");
            Node ip3 = createNode(db, "3.3.3.3", "ip");

            cc1.createRelationshipTo(phone1, RELATED);
            cc1.createRelationshipTo(email1, RELATED);
            cc1.createRelationshipTo(ip1, RELATED);
            phone1.createRelationshipTo(email1, RELATED);
            phone1.createRelationshipTo(ip1, RELATED);
            email1.createRelationshipTo(ip1, RELATED);

            cc2.createRelationshipTo(phone2, RELATED);
            cc2.createRelationshipTo(email2, RELATED);
            cc2.createRelationshipTo(ip2, RELATED);
            phone2.createRelationshipTo(email2, RELATED);
            phone2.createRelationshipTo(ip2, RELATED);
            email2.createRelationshipTo(ip2, RELATED);

            cc2.createRelationshipTo(phone3, RELATED);
            phone2.createRelationshipTo(ip3, RELATED);
            phone2.createRelationshipTo(email3, RELATED);
            email2.createRelationshipTo(ip3, RELATED);
            cc3.createRelationshipTo(ip2, RELATED);

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private Node createNode(GraphDatabaseService db, String value, String type) {
        Index<Node> index = db.index().forNodes(type + "s");
        Node node = db.createNode();
        node.setProperty(type, value);
        index.add(node, type, value);
        return node;
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();

    }

    @Test
    public void shouldRespondToHelloWorld() {
        assertEquals("Hello World!", service.helloWorld());
    }

    @Test
    public void shouldWarmUp() {
        assertEquals("Warmed up and ready to go!", service.warmUp(db));
    }



    static HashMap<String, Object> requestTwo = new HashMap<String, Object>(){{
        put("cc","2");
        put("phone","0987654321");
        put("email","email2@hotmail.com");
        put("ip","2.2.2.2");
    }};

    @Test
    public void crossReference1() throws IOException {
        String requestOne;
        requestOne = "{\"cc\" : \"1\","
                + "\"phone\" : \"1234567890\", "
                + "\"email\" : \"email1@hotmail.com\", "
                + "\"ip\" : \"1.1.1.1\"}";

        Response response = service.crossReference(requestOne, db);
        List<HashMap<String,Integer>> actual = objectMapper.readValue((String) response.getEntity(), List.class);

        List<HashMap<String,Integer>> expected = Arrays.asList(
                new HashMap<String, Integer>(){{
                   put("ccs", 0);
                   put("phones", 1);
                   put("emails", 1);
                   put("ips", 1);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 1);
                    put("phones", 0);
                    put("emails", 1);
                    put("ips", 1);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 1);
                    put("phones", 1);
                    put("emails", 0);
                    put("ips", 1);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 1);
                    put("phones", 1);
                    put("emails", 1);
                    put("ips", 0);
                }}
        );
        assertEquals(expected, actual);
    }

    @Test
    public void crossReference2() throws IOException {
        String requestOne;
        requestOne = "{\"cc\" : \"2\","
                + "\"phone\" : \"0987654321\", "
                + "\"email\" : \"email2@hotmail.com\", "
                + "\"ip\" : \"2.2.2.2\"}";

        Response response = service.crossReference(requestOne, db);
        List<HashMap<String,Integer>> actual = objectMapper.readValue((String) response.getEntity(), List.class);

        List<HashMap<String,Integer>> expected = Arrays.asList(
                new HashMap<String, Integer>(){{
                    put("ccs", 0);
                    put("phones", 2);
                    put("emails", 1);
                    put("ips", 1);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 1);
                    put("phones", 0);
                    put("emails", 2);
                    put("ips", 2);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 1);
                    put("phones", 1);
                    put("emails", 0);
                    put("ips", 2);
                }},
                new HashMap<String, Integer>(){{
                    put("ccs", 2);
                    put("phones", 1);
                    put("emails", 1);
                    put("ips", 0);
                }}
        );
        assertEquals(expected, actual);
    }
}
