import com.google.gson.*;
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
    Call<ResponseBody> envStep(@Path("id") String instanceId, @Body JsonObject data);

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

    @GET("/v1/envs/{id}/observation_space/")
    Call<ResponseBody> envObservationSpaceInfo(@Path("id") String instanceId);

    @POST("/v1/envs/{id}/observation_space/contains")
    Call<ResponseBody> envObservationSpaceContains(@Path("id") String instanceId, @Body JsonObject params);

    @POST("v1/envs/{id}/monitor/start/")
    Call<ResponseBody> envMonitorStart(@Path("id") String instanceId, @Body JsonObject data);

    @POST("/v1/envs/{id}/monitor/close/")
    Call<ResponseBody> envMonitorClose(@Path("id") String instanceId);

    @POST("/v1/envs/{id}/close/")
    Call<ResponseBody> envClose(@Path("id") String instanceId);

    @POST("/v1/upload/")
    Call<ResponseBody> upload(@Body JsonObject data);

    @POST("/v1/shutdown/")
    Call<ResponseBody> shutdownServer();
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
                if (response.body() != null) {
                    JsonParser parser = new JsonParser();
                    ResponseBody body = (ResponseBody) response.body();
                    return parser.parse(body.string()).getAsJsonObject();
                } else {
                    return new JsonObject();
                }
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

    public Map<String, String> envListAll() throws BadRequestException, ServerException {
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
        JsonElement observed = returned.get("observation");
        List<Double> list = new ArrayList<>();

        if (observed instanceof JsonArray) {
            observed.getAsJsonArray().forEach(jsonElement -> list.add(jsonElement.getAsDouble()));
        } else if (observed instanceof JsonPrimitive) {
            list.add(observed.getAsDouble());
        }

        logger.info("reset environment instance:" + instanceId);
        return list;
    }

    public Map<String, Object> envStep(String instanceId, String action, boolean render) throws  BadRequestException, ServerException {
        JsonObject json = new JsonObject();
        json.addProperty("action", Integer.parseInt(action));
        json.addProperty("render", render);
        Call<ResponseBody> request = gymRetrofit.envStep(instanceId, json);

        JsonObject returned = safeExecute(request, "failed to take action " + action + " on instance " + instanceId);

        Map<String, Object> step = new HashMap<>();
        List<Double> observation = new ArrayList<>();

        if (returned.get("observation") instanceof JsonArray) {
            returned.get("observation").getAsJsonArray().forEach(jsonElement ->
                    observation.add(jsonElement.getAsDouble()));
        } else if (returned.get("observation") instanceof JsonPrimitive){
            observation.add(returned.get("observation").getAsDouble());
        }

        Map<String, String> info = new HashMap<>();
        returned.getAsJsonObject("info").entrySet()
                .forEach(entry -> info.put(entry.getKey(), entry.getValue().getAsString()));

        step.put("observation", observation);
        step.put("reward", returned.get("reward").getAsDouble());
        step.put("done", returned.get("done").getAsBoolean());
        step.put("info", info);

        return step;
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

    public Map<String, Object> envObservationSpaceInfo(String instanceId) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envObservationSpaceInfo(instanceId);

        JsonObject returned = safeExecute(request, "failed to retrieve observation space info for instance " + instanceId);

        Map<String, Object> info = new HashMap<>();


        for (Map.Entry<String, JsonElement> entry : returned.getAsJsonObject("info").entrySet()) {
            if (entry.getKey().equals("name")) {
                info.put(entry.getKey(), entry.getValue().getAsString());
            } else {
                List<Double> arr = new ArrayList<>();
                entry.getValue().getAsJsonArray().forEach(d -> arr.add(d.getAsDouble()));
                info.put(entry.getKey(), arr);
            }
        }

        return info;
    }

    public Boolean envObservationSpaceContains(String instanceId, JsonObject params) throws BadRequestException, ServerException {
        Call<ResponseBody> request = gymRetrofit.envObservationSpaceContains(instanceId, params);
        JsonObject returned = safeExecute(request, "failed to run action space contains query for instance " + instanceId);
        return returned.get("member").getAsBoolean();
    }

    public void envMonitorStart(String instanceId, String directory, boolean force, boolean resume, boolean videoCallable) throws BadRequestException, ServerException {
        JsonObject data = new JsonObject();
        data.addProperty("directory", directory);
        data.addProperty("force", force);
        data.addProperty("resume", resume);
        data.addProperty("video_callable", videoCallable);

        JsonObject returned = safeExecute(gymRetrofit.envMonitorStart(instanceId, data), "failed to start monitor at " + directory + " for instance " + instanceId);
        System.out.print(returned);
    }

    public void envMonitorClose(String instanceId) throws BadRequestException, ServerException {
        safeExecute(gymRetrofit.envMonitorClose(instanceId), "close monitor for id " + instanceId + "failed");
    }

    public void envClose(String instanceId) throws BadRequestException, ServerException {
        safeExecute(gymRetrofit.envClose(instanceId), "failed to close monitor for instance " + instanceId);
    }

    public void upload(String trainingDirectory, String algorithmId, String apiKey) throws BadRequestException, ServerException {
        JsonObject data = new JsonObject();
        data.addProperty("training_dir", trainingDirectory);
        data.addProperty("algorithm_id", algorithmId);
        data.addProperty("api_key", apiKey);

        safeExecute(gymRetrofit.upload(data), "failed to upload to directory " + trainingDirectory);
    }

    public void shutdownServer() throws BadRequestException, ServerException {
        safeExecute(gymRetrofit.shutdownServer(), "failed to shutdown server");
    }
}