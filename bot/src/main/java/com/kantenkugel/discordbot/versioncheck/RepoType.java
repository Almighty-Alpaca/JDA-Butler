package com.kantenkugel.discordbot.versioncheck;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RepoType
{
    JCENTER("https://jcenter.bintray.com/", Pair.of("jcenter()", null), "bintray"),
    MAVENCENTRAL("https://repo.maven.apache.org/maven2/", null, "central", "maven"),
    M2_DV8TION("https://m2.dv8tion.net/releases/", Pair.of("m2-dv8tion", "https://m2.dv8tion.net/releases"));


    private final String repoBase;
    private final Pair<String, String> gradleImport;
    private final List<String> aliases;

    RepoType(String repoBase, Pair<String, String> gradleImport, String... aliases)
    {
        this.repoBase = repoBase;
        this.gradleImport = gradleImport;
        this.aliases = Arrays.asList(aliases);
    }

    public String getRepoBase()
    {
        return repoBase;
    }

    public Pair<String, String> getGradleImport()
    {
        return gradleImport;
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
