package com.kantenkugel.discordbot.graphql.entities.gh;

import com.kantenkugel.discordbot.graphql.anno.GQLEntity;

@GQLEntity
public class Tag
{
    private String name;

    public String getName()
    {
        return name;
    }
}
