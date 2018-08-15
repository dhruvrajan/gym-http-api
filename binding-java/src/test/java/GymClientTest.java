import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GymClientTest {
    GymClient client;

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
    public void envStep() {
    }

    @Test
    public void envListAll() {
    }

    @Test
    public void envActionSpaceInfo() {
    }

    @Test
    public void envActionSpaceSample() {
    }

    @Test
    public void envActionSpaceContains() {
    }

    @Test
    public void envObservationSpaceContains() {
    }
}