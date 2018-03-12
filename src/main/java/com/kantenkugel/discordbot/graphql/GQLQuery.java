package com.kantenkugel.discordbot.graphql;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.dv8tion.jda.core.utils.JDALogger;
import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GQLQuery<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(GQLQuery.class);
    private static final Gson GSON = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy() {
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

    public GQLQuery(String remoteUrl, String auth, String queryString, Class<T> clazz)
    {
        this.remoteUrl = remoteUrl;
        this.auth = auth;
        this.queryString = queryString;
        this.clazz = clazz;
    }

    public static String readSchema(String schemaName)
    {
        InputStream stream = GQLQuery.class.getClassLoader().getResourceAsStream("gqlqueries/" + schemaName + ".graphql");
        try
        {
            return new String(IOUtils.readFully(stream, -1, false), StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            LOG.error("Could not read query file {}", schemaName, e);
        }
        return null;
    }

    public T execute()
    {
        return doCall(new JSONObject().put("query", queryString));
    }

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
                    if(name.equals("errors"))
                    {
                        readErrors(errors, reader);
                    }
                    else if(name.equals("data"))
                    {
                        if(reader.peek() == JsonToken.BEGIN_OBJECT)
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
                    }
                    else
                    {
                        reader.skipValue();
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
