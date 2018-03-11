package com.kantenkugel.discordbot.graphql.entities.gh;

import com.kantenkugel.discordbot.graphql.anno.GQLEntity;
import com.kantenkugel.discordbot.graphql.anno.GQLField;

@GQLEntity
public class Commit
{
    @GQLField(name = "oid")
    private String hash;
    @GQLField(name = "abbreviatedOid")
    private String shortHash;
    private String message;
    private Actor author;

    public String getHash()
    {
        return hash;
    }

    public String getShortHash()
    {
        return shortHash;
    }

    public String getMessage()
    {
        return message;
    }

    public Actor getAuthor()
    {
        return author;
    }
}
