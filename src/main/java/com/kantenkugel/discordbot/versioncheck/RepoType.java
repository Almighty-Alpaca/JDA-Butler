package com.kantenkugel.discordbot.versioncheck;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RepoType
{
    JCENTER("http://jcenter.bintray.com/", "bintray"),
    MAVENCENTRAL("https://repo.maven.apache.org/maven2/", "central", "maven");


    private final String repoBase;
    private final List<String> aliases;

    RepoType(String repoBase, String... aliases)
    {
        this.repoBase = repoBase;
        this.aliases = Arrays.asList(aliases);
    }

    public String getRepoBase()
    {
        return repoBase;
    }

    public List<String> getAliases()
    {
        return Collections.unmodifiableList(aliases);
    }

    public static RepoType fromString(String value)
    {
        value = value.toLowerCase();
        for (RepoType repoType : RepoType.values())
        {
            if (repoType.toString().equals(value) || repoType.aliases.contains(value))
            {
                return repoType;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return name().toLowerCase();
    }
}
