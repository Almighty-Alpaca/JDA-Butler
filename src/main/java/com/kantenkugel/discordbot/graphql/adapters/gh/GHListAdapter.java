package com.kantenkugel.discordbot.graphql.adapters.gh;

import com.google.gson.*;
import com.kantenkugel.discordbot.graphql.GQLQuery;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GHListAdapter implements JsonDeserializer<List>
{
    @Override
    public List deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        if(!(typeOfT instanceof ParameterizedType)
                || ((ParameterizedType) typeOfT).getActualTypeArguments().length == 0
                || List.class != ((ParameterizedType) typeOfT).getRawType())
        {
            GQLQuery.LOG.warn("Did get non-parameterized list for GHListAdapter");
            return null;
        }
        Type subType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        List l = new ArrayList();
        JsonObject root = json.getAsJsonObject();
        while(!root.has("nodes") && root.size() == 1)
        {
            JsonElement jsonElement = root.get(root.keySet().iterator().next());
            if(!jsonElement.isJsonObject())
            {
                GQLQuery.LOG.warn("Could not find single path to \"nodes\" array for GHListAdapter");
                return null;
            }
            root = jsonElement.getAsJsonObject();
        }
        if(!root.has("nodes"))
        {
            GQLQuery.LOG.warn("Could not find \"nodes\" array for GHListAdapter");
            return null;
        }
        root.getAsJsonArray("nodes").forEach(element -> l.add(context.deserialize(element, subType)));
        return l;
    }
}
