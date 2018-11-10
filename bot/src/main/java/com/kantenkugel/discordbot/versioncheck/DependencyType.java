package com.kantenkugel.discordbot.versioncheck;

public enum DependencyType
{
    DEFAULT("jar"), POM("pom");

    private final String typeString;

    DependencyType(String typeString)
    {
        this.typeString = typeString;
    }

    public String getTypeString()
    {
        return typeString;
    }
}
