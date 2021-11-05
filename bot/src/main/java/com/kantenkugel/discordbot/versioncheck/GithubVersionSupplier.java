package com.kantenkugel.discordbot.versioncheck;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.graphql.GQLQuery;
import com.kantenkugel.discordbot.graphql.entities.gh.Repository;
import org.json.JSONObject;

import java.util.function.Supplier;

/**
 * Returns the latest tag of a GH repository. Useful for simple versioning
 */
public class GithubVersionSupplier implements Supplier<String>
{
    private static final int CHECK_COUNT = 1;
    private static final GQLQuery<Repository> QUERY;

    static
    {
        String ghToken = Bot.config.githubToken();
        if(ghToken.isEmpty())
        {
            VersionChecker.LOG.warn("No GH token set up. GithubVersionSupplier will not work");
            QUERY = null;
        }
        else
        {
            QUERY = new GQLQuery<>(
                    "https://api.github.com/graphql",
                    "Bearer " + ghToken,
                    GQLQuery.readQuery("ghTags"),
                    Repository.class
            );
        }
    }

    private final JSONObject variableMap;

    public GithubVersionSupplier(String ownerName, String repoName)
    {
        this.variableMap = new JSONObject()
                .put("owner", ownerName)
                .put("name", repoName)
                .put("count", CHECK_COUNT);
    }

    @Override
    public String get()
    {
        if(QUERY == null)
            return null;
        Repository repo = QUERY.execute(variableMap);
        if(repo == null || repo.getTags().size() == 0)
            return null;
        return repo.getTags().get(0).getName();
    }
}
