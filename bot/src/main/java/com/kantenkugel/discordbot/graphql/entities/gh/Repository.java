package com.kantenkugel.discordbot.graphql.entities.gh;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.kantenkugel.discordbot.graphql.adapters.gh.GHListAdapter;

import java.util.List;

public class Repository
{
    @JsonAdapter(GHListAdapter.class)
    private List<Release> releases;

    @SerializedName("refs")
    @JsonAdapter(GHListAdapter.class)
    private List<Tag> tags;

    @SerializedName("ref")
    @JsonAdapter(GHListAdapter.class)
    private List<Commit> commits;

    public List<Release> getReleases()
    {
        return releases;
    }

    public List<Tag> getTags()
    {
        return tags;
    }

    public List<Commit> getCommits()
    {
        return commits;
    }
}
