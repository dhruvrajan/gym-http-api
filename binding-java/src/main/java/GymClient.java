import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


interface GymRetrofit {
    @POST("/v1/envs/")
    Call<ResponseBody> envCreate(@Body JsonObject data);

    @POST("/v1/envs/{id}/step/")
    Call<ResponseBody> envStep(@Body JsonObject data);

    @POST("/v1/envs/{id}/reset/")
    Call<ResponseBody> envReset(@Path("id") String instanceId);

    @GET("/v1/envs/")
    Call<ResponseBody> envListAll();

    @GET("/v1/envs/{id}/action_space/")
    Call<ResponseBody> envActionSpaceInfo(@Path("id") String instanceId);

    @GET("/v1/envs/{id}/action_space/sample")
    Call<ResponseBody> envActionSpaceSample(@Path("id") String instanceId);

    @GET("/v1/envs/{id}/action_space/contains/{x}")
    Call<ResponseBody> envActionSpaceContains(@Path("id") String instanceId, @Path("x") String x);

    @GET("/v1/envs/{id}/observation_space/contains")
    Call<ResponseBody> envObservationSpaceContains(@Path("id") String instanceId);
}

class GymClientException extends Exception {
    GymClientException(String message) {
        super(message);
    }
}


class BadRequestException extends GymClientException {
    BadRequestException(String message) {
        super(message);
    }
}

class ServerException extends GymClientException {
    public int status;

    ServerException(String message, int status) {
        super(message);
        this.status = status;
    }
}

public class GymClient {
    private GymRetrofit gymRetrofit;
    private static final Logger logger = Logger.getLogger(GymClient.class.getName());

    public static void main(String[] args) throws IOException, BadRequestException, ServerException {
        String remoteBase = "http://127.0.0.1:5000";
        GymClient gymClient = new GymClient().build(remoteBase);

        String envId = "CartPole-v0";
        gymClient.envCreate(envId);
    }

    public GymClient build(String remoteBase) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(remoteBase)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        gymRetrofit = retrofit.create(GymRetrofit.class);
        return this;
    }

    private JsonObject safeExecute(Call<ResponseBody> call, String msgOnFail) throws BadRequestException, ServerException {
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                JsonParser parser = new JsonParser();
                ResponseBody body = (ResponseBody) response.body();
                return parser.parse(body.string()).getAsJsonObject();
            } else {
                logger.severe(msgOnFail);
                throw new ServerException(response.message(), response.code());
            }
        } catch (IOException | NullPointerException e) {
            logger.severe(msgOnFail);
            throw new BadRequestException("Request failed to execute.");
        }
    }

    public String envCreate(String envId) throws BadRequestException, ServerException {
        JsonObject json = new JsonObject();
        json.addProperty("env_id", envId);
        Call<ResponseBody> request = gymRetrofit.envCreate(json);

        JsonObject returned = safeExecute(request, "failed to create environment " + envId);
        String instanceId = returned.get("instance_id").getAsString();

        logger.info(String.format("create environment %s with id %s", envId, instanceId));
        return instanceId;
    }

    public Map<String, String> envListAll() throws IOException, BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envListAll();

        JsonObject returned = safeExecute(request, "failed to get list of envs");

        Map<String, String> allEnvs = new HashMap<>();
        returned.getAsJsonObject("all_envs").entrySet()
                .forEach(entry -> allEnvs.put(entry.getKey(), entry.getValue().getAsString()));

        logger.info(String.format("retrieved list of %s environments", allEnvs.size()));
        return allEnvs;
    }

    public List<Double> envReset(String instanceId) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envReset(instanceId);

        JsonObject returned = safeExecute(request, "failed to reset instance " + instanceId);
        JsonArray observation = returned.get("observation").getAsJsonArray();

        List<Double> obs = new ArrayList<>();
        observation.forEach(jsonElement -> obs.add(jsonElement.getAsDouble()));

        logger.info("reset environment instance:" + instanceId);
        return obs;
    }

    public List<String> envStep(String instanceId, String action, boolean render) throws IOException, BadRequestException, ServerException {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("render", render);
        Call<ResponseBody> request = gymRetrofit.envStep(json);

        JsonObject returned = safeExecute(request, "failed to take action " + action + "on instance " + instanceId);

        return Arrays.asList(
                returned.get("observation").getAsString(),
                returned.get("reward").getAsString(),
                returned.get("done").getAsString(),
                returned.get("info").getAsString()
        );
    }
    public Map<String, String> envActionSpaceInfo(String instanceId) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envActionSpaceInfo(instanceId);

        JsonObject returned = safeExecute(request, "failed to retrieve action space info for instance " + instanceId);

        Map<String, String> info = new HashMap<>();
        returned.getAsJsonObject("info").entrySet()
                .forEach(entry -> info.put(entry.getKey(), entry.getValue().getAsString()));

        return info;
    }

    public String envActionSpaceSample(String instanceId) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envActionSpaceSample(instanceId);

        JsonObject returned = safeExecute(request, "failed to retrieve action space sample for instance " + instanceId);
        return returned.get("action").getAsString();
    }

    public Boolean envActionSpaceContains(String instanceId, String x) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envActionSpaceContains(instanceId, x);

        JsonObject returned = safeExecute(request, "failed to run action space contains query for instance " + instanceId + ", query " + x);
        boolean member =  returned.get("member").getAsBoolean();

        logger.info("found action " + x + " in action space of instance " + instanceId);
        return member;
    }
//
//    public JsonElement envObservationSpaceContains(String instanceId, JsonObject params) throws IOException {
//        Response response = gymRetrofit.envObservationSpaceContains(instanceId).execute();
//        return parseResponse(response, String.format("observation space contains for instance %s", instanceId));
//    }
}