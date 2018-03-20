package com.kantenkugel.discordbot.graphql;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.dv8tion.jda.core.utils.JDALogger;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to query a GraphQL API via http and map its json response to a POJO using the Gson lib
 *
 * @param <T>
 *      The type of POJO to use as root for parsing
 */
public class GQLQuery<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(GQLQuery.class);
    private static final Gson GSON = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy()
    {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {return f.getAnnotation(JsonIgnore.class) != null;}
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {return false;}
    }).create();
    private static final MediaType jsonType = MediaType.parse("application/json");

    private final String remoteUrl;
    private final String auth;
    private final String queryString;
    private final Class<T> clazz;

    /**
     * Creates a new (reusable) GQLQuery instance with given configuration
     *
     * @param remoteUrl
     *          The url of the remote server (root of graphql api)
     * @param auth
     *          Bearer token for the api or {@code null} if not needed
     * @param queryString
     *          The graphql query to send to the server.
     *          {@link #readQuery(String)} can be used to read the query from a file (see its jdocs).
     * @param clazz
     *          The class object of the root POJO for parsing.
     *
     * @see     #readQuery(String)
     */
    public GQLQuery(String remoteUrl, String auth, String queryString, Class<T> clazz)
    {
        this.remoteUrl = remoteUrl;
        this.auth = auth;
        this.queryString = queryString;
        this.clazz = clazz;
    }

    /**
     * Reads a GraphQL query from a {@code .graphql} file located in the {@code gqlqueries} resource directory
     * and returns its content as String.
     *
     * @param queryName
     *          The name of the graphql file inside the gqlqueries directory (excluding file ending)
     *
     * @return  {@code null}, if the file didn't exist or there was an error reading it,
     *          otherwise the full content of the file as String
     */
    public static String readQuery(String queryName)
    {
        InputStream stream = GQLQuery.class.getClassLoader().getResourceAsStream("gqlqueries/" + queryName + ".graphql");
        if(stream == null)
            return null;
        try
        {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            LOG.error("Could not read query file {}", queryName, e);
        }
        return null;
    }

    /**
     * Executes the GraphQL query without variables and returns the created POJO or {@code null} in case of an error.
     * <br><b>Note:</b> This should only be used with queries that do not have any variables or only optional variables.
     *
     * @return  The parsed POJO or {@code null} in case of an error.
     *
     * @see     #execute(JSONObject) execute(JSONObject) for when variables are needed
     */
    public T execute()
    {
        return doCall(new JSONObject().put("query", queryString));
    }

    /**
     * Executes the GraphQL query with the given variables and returns the created POJO or {@code null} in case of an error.
     * <br><b>Note:</b> If the query contains no variables, {@link #execute()} should be used instead.
     *
     * @return  The parsed POJO or {@code null} in case of an error.
     *
     * @see     #execute() execute() for when no variables are needed
     */
    public T execute(JSONObject variables)
    {
        return doCall(new JSONObject().put("query", queryString).put("variables", variables));
    }

    private T doCall(JSONObject data)
    {
        Request.Builder post = new Request.Builder().url(remoteUrl).post(RequestBody.create(jsonType, data.toString().getBytes()));
        if(auth != null)
            post.header("Authorization", auth);
        try
        {
            Response execute = Bot.httpClient.newCall(post.build()).execute();
            try(ResponseBody body = execute.body())
            {
                if(body == null)
                {
                    LOG.error("Got empty body from GQL query");
                    return null;
                }
                List<String> errors = new ArrayList<>();
                T object = null;
                JsonReader reader = GSON.newJsonReader(body.charStream());
                reader.beginObject();
                while(reader.hasNext())
                {
                    String name = reader.nextName();
                    switch (name)
                    {
                        case "errors":
                            readErrors(errors, reader);
                            break;
                        case "data":
                            if (reader.peek() == JsonToken.BEGIN_OBJECT)
                            {
                                reader.beginObject();
                                reader.nextName();
                                object = GSON.fromJson(reader, clazz);
                                reader.endObject();
                            }
                            else
                            {
                                reader.skipValue();
                            }
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();
                reader.close();
                if(!errors.isEmpty())
                {
                    LOG.error("There was at least one GQL error:\n{}", JDALogger.getLazyString(() ->
                    {
                        StringBuilder b = new StringBuilder();
                        for(String error : errors)
                        {
                            b.append(error).append('\n');
                        }
                        b.setLength(b.length() - 1);
                        return b.toString();
                    }));
                    return null;
                }
                return object;
            }
        }
        catch(IOException e)
        {
            LOG.error("Error executing GQL query", e);
        }
        return null;
    }

    private static void readErrors(List<String> errorList, JsonReader reader)
    {
        try
        {
            reader.beginArray();
            while(reader.hasNext())
            {
                reader.beginObject();
                while(reader.hasNext())
                {
                    String name = reader.nextName();
                    if(name.equals("message"))
                        errorList.add(reader.nextString());
                    else
                        reader.skipValue();
                }
                reader.endObject();
            }
            reader.endArray();
        }
        catch(IOException e)
        {
            LOG.error("There was an error parsing GQL errors", e);
        }
    }
}
