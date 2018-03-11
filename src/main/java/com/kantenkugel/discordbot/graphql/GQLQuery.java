package com.kantenkugel.discordbot.graphql;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.graphql.anno.GQLEntity;
import com.kantenkugel.discordbot.graphql.anno.GQLIgnore;
import com.kantenkugel.discordbot.graphql.anno.GQLField;
import com.kantenkugel.discordbot.graphql.anno.GQLOptional;
import net.dv8tion.jda.core.utils.JDALogger;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GQLQuery<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(GQLQuery.class);
    private static final MediaType jsonType = MediaType.parse("application/json");

    private final String remoteUrl;
    private final String auth;
    private final String queryString;
    private final Class<T> clazz;

    public GQLQuery(String remoteUrl, String auth, String queryString, Class<T> clazz)
    {
        if(!clazz.isAnnotationPresent(GQLEntity.class))
            throw new IllegalArgumentException("Provided class has to be annotated with GQLEntity");
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
            return new String(IOUtils.readFully(stream, -1, false), StandardCharsets.UTF_8)
                    .replaceAll("[\r\n]", " ").replaceAll("\\s{2,}", " ");
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
                JSONObject json = new JSONObject(new JSONTokener(body.byteStream()));
                if(json.has("errors"))
                {
                    JSONArray errors = json.getJSONArray("errors");
                    LOG.error("There was at least one GQL error: {}", JDALogger.getLazyString(() ->
                    {
                        StringBuilder b = new StringBuilder();
                        for(int i = 0; i < errors.length(); i++)
                        {
                            b.append(errors.getJSONObject(i).getString("message")).append('\n');
                        }
                        b.setLength(b.length() - 1);
                        return b.toString();
                    }));
                    return null;
                }
                json = json.getJSONObject("data");
                return parseObject(clazz, json.getJSONObject(clazz.getSimpleName().toLowerCase()));
            }
        }
        catch(IOException e)
        {
            LOG.error("Error executing GQL query", e);
        }
        return null;
    }

    private static <T> T parseObject(Class<T> objectClass, JSONObject json)
    {
        String simpleName = objectClass.getSimpleName().toLowerCase();
        T object;
        try
        {
            object = objectClass.newInstance();
        }
        catch(InstantiationException | IllegalAccessException e)
        {
            LOG.error("Could not create new instance of data class {}", objectClass.getName(), e);
            return null;
        }
        for(java.lang.reflect.Field field : objectClass.getDeclaredFields())
        {
            if(field.isAnnotationPresent(GQLIgnore.class))
                continue;

            GQLField fieldAnno = field.getAnnotation(GQLField.class);
            String name = fieldAnno == null || fieldAnno.name().isEmpty() ? field.getName() : fieldAnno.name();
            String path = fieldAnno == null ? "" : fieldAnno.path();

            JSONObject fieldJson = resolvePath(json, path);

            if(fieldJson == null || !fieldJson.has(name))
            {
                if(!field.isAnnotationPresent(GQLOptional.class))
                    LOG.warn("Could not find key {} with path \"{}\" in gql json of object {}", name, path, simpleName);
                continue;
            }

            Class<?> type = field.getType();
            field.setAccessible(true);
            try
            {
                if(type == String.class)
                {
                    field.set(object, fieldJson.getString(name));
                }
                else if(type == Integer.class || type == int.class)
                {
                    int val = fieldJson.getInt(name);
                    if(type == int.class)
                        field.setInt(object, val);
                    else
                        field.set(object, val);
                }
                else if(type == Long.class || type == long.class)
                {
                    long val = fieldJson.getLong(name);
                    if(type == long.class)
                        field.setLong(object, val);
                    else
                        field.set(object, val);
                }
                else if(type == Boolean.class || type == boolean.class)
                {
                    boolean val = fieldJson.getBoolean(name);
                    if(type == boolean.class)
                        field.setBoolean(object, val);
                    else
                        field.set(object, val);
                }
                else if(type == Double.class || type == double.class)
                {
                    double val = fieldJson.getDouble(name);
                    if(type == double.class)
                        field.setDouble(object, val);
                    else
                        field.set(object, val);
                }
                else if(List.class.isAssignableFrom(type))
                {
                    Type genericType = field.getGenericType();
                    if(genericType instanceof ParameterizedType)
                    {
                        ParameterizedType genType = (ParameterizedType) genericType;
                        Class<?> genericClass = (Class<?>) genType.getActualTypeArguments()[0];
                        List value = type == List.class ? new ArrayList() : (List) type.newInstance();
                        JSONArray jsonArray = fieldJson.getJSONArray(name);
                        for(int i = 0; i < jsonArray.length(); i++)
                        {
                            JSONObject collectionJson = resolvePath(jsonArray.getJSONObject(i),
                                    fieldAnno == null ? "" : fieldAnno.collectionPath());
                            if(collectionJson == null)
                            {
                                LOG.warn("Collection path for field {}.{} is wrong", objectClass.getSimpleName(), field.getName());
                                break;
                            }
                            value.add(parseObject(genericClass, collectionJson));
                        }
                        field.set(object, value);
                    }
                    else
                    {
                        LOG.warn("Could not find generic type of collection");
                    }
                }
                else if(type.isAnnotationPresent(GQLEntity.class))
                {
                    field.set(object, parseObject(type, fieldJson.getJSONObject(name)));
                }
                else
                {
                    LOG.warn("Type of field not handled yet!");
                }
            }
            catch(IllegalAccessException | InstantiationException ex)
            {
                LOG.warn("Could not set value for field {}.{}", simpleName, name, ex);
            }
        }
        return object;
    }

    private static JSONObject resolvePath(JSONObject element, String path)
    {
        if(path.length() == 0)
            return element;
        try
        {
            for(String pathSplit : path.split("\\."))
            {
                element = element.getJSONObject(pathSplit);
            }
        }
        catch(JSONException ex)
        {
            return null;
        }
        return element;
    }
}
