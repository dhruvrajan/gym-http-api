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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


interface GymRetrofit {
    @POST("/v1/envs/")
    Call<ResponseBody> postEnv(@Body Map<String, String> data);

    @POST("/v1/envs/{id}/step/")
    Call<ResponseBody> envStep(@Path("id") String instanceId);

    @GET("/v1/envs/")
    Call<ResponseBody> getEnv();

    @GET("/v1/envs/{id}/reset/")
    Call<ResponseBody> envReset(@Path("id") String instanceId);

    @GET("/v1/envs/{id}/action_space/sample")
    Call<ResponseBody> envActionSpaceInfo(@Path("id") int instanceId);

    @GET("/v1/envs/{id}/action_space/sample")
    Call<ResponseBody> envActionSpaceSample(@Path("id") int instanceId);

    @GET("/v1/envs/{id}/action_space/contains/{x}")
    Call<ResponseBody> envActionSpaceContains(@Path("id") int instanceId, @Path("x") String x);
}

public class GymClient {
    private Retrofit retrofit;
    private GymRetrofit gymRetrofit;
    private Logger logger = Logger.getLogger("GymClient");



    public static void main(String[] args) throws IOException {
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

    private JsonObject parseResponse(Response response) {
        if (response.isSuccessful()) {
            return new JsonParser().parse(response.message()).getAsJsonObject();
        }

        return null;
    }

    public JsonObject envCreate(String envId) throws IOException {
        Map<String,String> json = new HashMap<String, String>() {{
            put("env_id", envId);
        }};

        logger.info("Create environment: " + envId);
        Response response = gymRetrofit.postEnv(json).execute();
        return parseResponse(response);
    }

    public JsonObject envListAll() throws IOException {
        Response response = gymRetrofit.getEnv().execute();
        return parseResponse(response);
    }

    public JsonObject envReset(String instanceId) throws IOException {
        Response response = gymRetrofit.envReset(instanceId).execute();
        return parseResponse(response);
    }

    public JsonObject envStep(String instanceId, String action, boolean render) throws IOException {
        Map<String, String> data = new HashMap<String, String>() {{
            put("action", action);
            put("render", Boolean.toString(render));
        }};

        Response response = gymRetrofit.envStep(instanceId).execute();
        return parseResponse(response);
    }

    public JsonObject envActionSpaceInfo(String instanceId) throws IOException {
        Map<String, String> data = new HashMap<String, String>() {{
            put("action", action);
            put("render", Boolean.toString(render));
        }};

        Response response = gymRetrofit.envStep(instanceId).execute();
        return parseResponse(response);
    }
}