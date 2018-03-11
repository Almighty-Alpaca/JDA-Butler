package com.kantenkugel.discordbot.graphql.entities.gh;

import com.kantenkugel.discordbot.graphql.anno.GQLEntity;
import com.kantenkugel.discordbot.graphql.anno.GQLIgnore;

import java.time.OffsetDateTime;

@GQLEntity
public class Actor
{
    private String name;
    private String avatarUrl;
    private String email;
    private String date;

    @GQLIgnore
    private OffsetDateTime dateTime;

    public String getName()
    {
        return name;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public String getEmail()
    {
        return email;
    }

    public String getDateString()
    {
        return date;
    }

    public OffsetDateTime getDate()
    {
        synchronized(this)
        {
            if(dateTime == null)
                dateTime = OffsetDateTime.parse(date);
        }
        return dateTime;
    }
}
