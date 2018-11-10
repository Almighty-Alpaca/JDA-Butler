package com.kantenkugel.discordbot.jenkinsutil;

public class JenkinsUser
{
    public final String fullName;
    public final String id;
    public final String description;

    public JenkinsUser(String fullName, String id, String description)
    {
        this.fullName = fullName;
        this.id = id;
        this.description = description;
    }

    @Override
    public String toString()
    {
        return String.format("CI User %s (%s)", fullName, id);
    }
}
