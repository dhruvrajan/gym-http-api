import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class GymClientTest {
    private GymClient client;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        client = new GymClient().build("http://127.0.0.1:5000");
    }

    @Test
    public void testCreateDestroy() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        assertTrue(client.envListAll().containsKey(instanceId));
        client.envClose(instanceId);
        assertFalse(client.envListAll().containsKey(instanceId));
    }

    @Test
    public void testActionSpaceDiscrete() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, String> info = client.envActionSpaceInfo(instanceId);

        assertEquals("Discrete", info.get("name"));
        assertEquals(2, Integer.parseInt(info.get("n")));
    }

    @Test
    public void testActionSpaceSample() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        int action = Integer.parseInt(client.envActionSpaceSample(instanceId));
        assertTrue(0 <= action && action < 2);
    }

    @Test
    public void testActionSpaceContains() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, String> info = client.envActionSpaceInfo(instanceId);

        assertEquals(2, Integer.parseInt(info.get("n")));
        assertTrue(client.envActionSpaceContains(instanceId, "0"));
        assertTrue(client.envActionSpaceContains(instanceId, "1"));
        assertFalse(client.envActionSpaceContains(instanceId, "2"));
    }

    @Test
    public void testObservationSpaceBox() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, Object> info = client.envObservationSpaceInfo(instanceId);

        assertEquals("Box", info.get("name"));
        assertEquals(1, ((List) info.get("shape")).size());
        assertEquals(4.0, ((List) info.get("shape")).get(0));
        assertEquals(4, ((List) info.get("low")).size());
        assertEquals(4, ((List) info.get("high")).size());
    }

    @Test
    public void testObservationSpaceContains() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, Object> info = client.envObservationSpaceInfo(instanceId);

        assertEquals("Box", info.get("name"));

        JsonObject json1 = new JsonObject();
        json1.addProperty("name", "Box");

        JsonArray arr = new JsonArray();
        arr.add(4);

        JsonObject json2 = new JsonObject();
        json2.add("shape", arr);

        JsonObject json3 = new JsonObject();
        json3.addProperty("name", "Box");
        json3.add("shape", arr);

        assertTrue(client.envObservationSpaceContains(instanceId, json1));
        assertTrue(client.envObservationSpaceContains(instanceId, json2));
        assertTrue(client.envObservationSpaceContains(instanceId, json3));
    }

    @Test
    public void testReset() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        List<Double> initObs = client.envReset(instanceId);
        assertEquals(4, initObs.size());

        instanceId = client.envCreate("FrozenLake-v0");
        initObs = client.envReset(instanceId);
        assertEquals(Collections.singletonList(0.0), initObs);
    }

    @Test
    public void testStep() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        client.envReset(instanceId);

        Map<String, Object> step = client.envStep(instanceId, "0", false);

        assertTrue(step.get("observation") instanceof List);
        assertTrue(step.get("reward") instanceof Double);
        assertTrue(step.get("done") instanceof Boolean);
        assertTrue(step.get("info") instanceof Map);
        assertEquals(4, ((List) step.get("observation")).size());

        instanceId = client.envCreate("FrozenLake-v0");
        client.envReset(instanceId);
        step = client.envStep(instanceId, "0", false);
        assertTrue(step.get("observation") instanceof List);
        assertEquals(1, ((List) step.get("observation")).size());
    }

    @Test
    public void testEnvCreate() throws Exception {
        String id = client.envCreate("CartPole-v0");
        assertEquals(8, id.length());

        expected.expect(ServerException.class);
        client.envCreate("asdf");
    }

    @Test
    public void envReset() throws BadRequestException, ServerException {
        String id = client.envCreate("CartPole-v0");
        List<Double> observation = client.envReset(id);

        // initial observation has 4 measurements
        assertEquals(4, observation.size());
    }

    @Test
    public void envListAll() throws ServerException, BadRequestException, IOException {
        Map<String, String> envs = client.envListAll();

        for (String instanceId : envs.keySet()) {
            assertEquals(8, instanceId.length());
        }
    }

    @Test
    public void envActionSpaceInfo() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, String> info = client.envActionSpaceInfo(instanceId);

        assertEquals(2, info.size());
    }

    @Test
    public void envActionSpaceSample() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, String> info = client.envActionSpaceInfo(instanceId);
        String getAction = client.envActionSpaceSample(instanceId);

        int n = Integer.parseInt(info.get("n"));
        int action = Integer.parseInt(getAction);

        assertTrue(action < n);
    }

    @Test
    public void envStep() throws BadRequestException, ServerException {
        String id = client.envCreate("CartPole-v0");

    }

    @Test
    public void envActionSpaceContains() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");

        assertTrue(client.envActionSpaceContains(instanceId, "1"));
        assertFalse(client.envActionSpaceContains(instanceId, "2"));
    }

    @Test
    public void envObservationSpaceInfo() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, String> f= new HashMap<>();
        Map<String, Object> info = client.envObservationSpaceInfo(instanceId);

        assertEquals(4, info.size());
        assertTrue(info.get("name") instanceof String);
        assertTrue(info.get("high") instanceof List);
        assertTrue(info.get("low") instanceof List);
        assertTrue(info.get("shape") instanceof List);
    }

    @Test
    public void testEnvObservationSpaceContains() throws BadRequestException, ServerException {
        String instanceId = client.envCreate("CartPole-v0");
        Map<String, Object> query = new HashMap<>();
        query.put("name", "Box");
    }

}