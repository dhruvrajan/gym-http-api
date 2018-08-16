import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GymClientTest {
    private GymClient client;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        client = new GymClient().build("http://127.0.0.1:5000");
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
}