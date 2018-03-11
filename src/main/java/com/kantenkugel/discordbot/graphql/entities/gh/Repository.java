package com.kantenkugel.discordbot.graphql.entities.gh;

import com.kantenkugel.discordbot.graphql.anno.GQLEntity;
import com.kantenkugel.discordbot.graphql.anno.GQLField;
import com.kantenkugel.discordbot.graphql.anno.GQLOptional;

import java.util.List;

@GQLEntity
public class Repository
{
    @GQLOptional
    @GQLField(path = "releases", name = "nodes")
    private List<Release> releases;

    @GQLOptional
    @GQLField(path = "refs", name = "nodes")
    private List<Tag> tags;

    @GQLOptional
    @GQLField(path = "ref.target.history", name = "nodes")
    private List<Commit> commits;

    public List<Release> getReleases()
    {
        return releases;
    }

    public List<Tag> getTags()
    {
        return tags;
    }
}
