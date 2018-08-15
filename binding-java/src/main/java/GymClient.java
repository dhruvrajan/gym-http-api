import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;


interface GymRetrofit {
    @POST("/v1/envs/")
    Call<ResponseBody> postEnv(@Body JsonObject data);

    @POST("/v1/envs/{id}/step/")
    Call<ResponseBody> envStep(@Body JsonObject data);

    @POST("/v1/envs/{id}/reset/")
    Call<ResponseBody> envReset(@Path("id") String instanceId, @Body JsonObject data);

    @GET("/v1/envs/")
    Call<ResponseBody> getEnv();

    @GET("/v1/envs/{id}/action_space/sample")
    Call<ResponseBody> envActionSpaceInfo(@Path("id") String instanceId);

    @GET("/v1/envs/{id}/action_space/sample")
    Call<ResponseBody> envActionSpaceSample(@Path("id") String instanceId);

    @GET("/v1/envs/{id}/action_space/contains/{x}")
    Call<ResponseBody> envActionSpaceContains(@Path("id") String instanceId, @Path("x") String x);

    @GET("/v1/envs/{id}/observation_space/contains")
    Call<ResponseBody> envObservationSpaceContains(@Path("id") String instanceId);
}

class BadRequestException extends Exception {
    BadRequestException(String message) {
        super(message);
    }
}

class ServerException extends Exception {
    public int status;

    ServerException(String message, int status) {
        super(message);
        this.status = status;
    }
}

public class GymClient {
    private Retrofit retrofit;
    private GymRetrofit gymRetrofit;
    private static final Logger logger = Logger.getLogger(GymClient.class.getName());

    public static void main(String[] args) throws IOException, BadRequestException, ServerException {
        String remoteBase = "http://127.0.0.1:5000";
        GymClient gymClient = new GymClient().build(remoteBase);

        String envId = "CartPole-v0";
        gymClient.envCreate(envId);
    }

    public GymClient build(String remoteBase) throws MalformedURLException {
        URL remoteBase1 = new URL(remoteBase);
        retrofit = new Retrofit.Builder()
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
        Call<ResponseBody> request = gymRetrofit.postEnv(json);

        JsonObject returned = safeExecute(request, "failed to create environment " + envId);
        String instanceId = returned.get("instance_id").getAsString();

        logger.info(String.format("create environment %s with id %s", envId, instanceId));
        return instanceId;
    }

    public List<Double> envReset(String instanceId) throws BadRequestException, ServerException {
        JsonObject json = new JsonObject();
        json.addProperty("instance_id", instanceId);
        Call<ResponseBody> request = gymRetrofit.envReset(instanceId, json);

        JsonObject returned = safeExecute(request, "failed to reset instance " + instanceId);
        JsonArray observation = returned.get("observation").getAsJsonArray();

        List<Double> obs = new ArrayList<>();
        observation.forEach(jsonElement -> obs.add(jsonElement.getAsDouble()));

        logger.info("reset environment instance:" + instanceId);
        return obs;
    }

//    public List<String> envStep(String instanceId, String action, boolean render) throws IOException {
//        JsonObject json = new JsonObject();
//        json.addProperty("action", action);
//        json.addProperty("render", render);
//
//        Response response = gymRetrofit.envStep(json).execute();
//        JsonObject parsed = parseResponse(response, String.format("take action %s on instance %s", action, instanceId));
//        return Arrays.asList(
//                parsed.get("observation").getAsString(),
//                parsed.get("reward").getAsString(),
//                parsed.get("done").getAsString(),
//                parsed.get("info").getAsString()
//        );
//    }
//
//    public JsonElement envListAll() throws IOException {
//        Response response = gymRetrofit.getEnv().execute();
//        return parseResponse(response, "list all environments")
//                .get("all_envs");
//    }
//
//    public JsonElement envActionSpaceInfo(String instanceId) throws IOException {
//        Response response = gymRetrofit.envActionSpaceInfo(instanceId).execute();
//        return parseResponse(response, String.format("action space info for instance %s", instanceId));
//    }
//
//    public JsonElement envActionSpaceSample(String instanceId) throws IOException {
//        Response response = gymRetrofit.envActionSpaceSample(instanceId).execute();
//        return parseResponse(response, String.format("action space sample for instance %s", instanceId));
//    }
//
//    public JsonElement envActionSpaceContains(String instanceId, String x) throws IOException {
//        Response response = gymRetrofit.envActionSpaceContains(instanceId, x).execute();
//        return parseResponse(response, String.format("action space contains query for instance %s, query %s", instanceId, x));
//    }
//
//    public JsonElement envObservationSpaceContains(String instanceId, JsonObject params) throws IOException {
//        Response response = gymRetrofit.envObservationSpaceContains(instanceId).execute();
//        return parseResponse(response, String.format("observation space contains for instance %s", instanceId));
//    }
}