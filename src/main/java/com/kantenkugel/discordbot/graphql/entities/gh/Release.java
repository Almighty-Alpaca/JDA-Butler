package com.kantenkugel.discordbot.graphql.entities.gh;

public class Release
{
    private Tag tag;
    private String name;
    private String description;
    private boolean isDraft;
    private boolean isPrerelease;

    public Tag getTag()
    {
        return tag;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isDraft()
    {
        return isDraft;
    }

    public boolean isPrerelease()
    {
        return isPrerelease;
    }
}
