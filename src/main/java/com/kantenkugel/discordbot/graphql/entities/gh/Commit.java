package com.kantenkugel.discordbot.graphql.entities.gh;

import com.google.gson.annotations.SerializedName;

public class Commit
{
    @SerializedName("oid")
    private String hash;
    @SerializedName("abbreviatedOid")
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
