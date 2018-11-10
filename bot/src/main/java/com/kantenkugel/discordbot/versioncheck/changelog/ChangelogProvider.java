package com.kantenkugel.discordbot.versioncheck.changelog;

import java.util.List;

public interface ChangelogProvider
{
    /**
     * Returns the non-null url for a changelog overview.
     * This url is used for no-arg command call as well as if this doesn't support individual changelogs.
     *
     * @return  non-null url for a changelog overview
     */
    String getChangelogUrl();

    /**
     * Indicates, whether or not this ChangelogProvider supports changes to individual versions.
     * If this is {@code true}, {@link #getChangelog(String)} and {@link #getChangelogs(String, String)} are used.
     * If this is {@code false}, the url from {@link #getChangelogUrl()} is returned.
     *
     * @return  boolean indicating if changes for individual versions are supported
     */
    boolean supportsIndividualLogs();

    /**
     * Retrieves the changelog for a specific version.
     * <br/>Note: this is only called, if {@link #supportsIndividualLogs()} returns true.
     *
     * @param version
     *          The version to retrieve the Changelog for
     * @return  The Changelog for given version or null if it doesn't exist
     */
    Changelog getChangelog(String version);

    /**
     * Retrieves the changelog for a range of versions.
     * <br/>Note: this is only called, if {@link #supportsIndividualLogs()} returns true.
     *
     * @param startVersion
     *          The first version to retrieve the Changelog for
     * @param endVersion
     *          The last version to retrieve the Changelog for
     * @return  The Changelogs for given version range or empty list if there aren't any
     */
    List<Changelog> getChangelogs(String startVersion, String endVersion);

    /**
     * Class represeting a changelog (all changes for a single version)
     */
    class Changelog
    {
        private final String title;
        private final List<String> changeset;
        private final String changelogUrl;

        public Changelog(String title, List<String> changeset, String changelogUrl)
        {
            this.title = title;
            this.changeset = changeset;
            this.changelogUrl = changelogUrl;
        }

        public Changelog(String title, List<String> changeset)
        {
            this(title, changeset, null);
        }

        /**
         * Identifier of the changelog. This should contain the version.
         *
         * @return  The title of this changelog
         */
        public String getTitle()
        {
            return title;
        }

        /**
         * The list of individual changes contained in this changelog
         *
         * @return  List of individual changes.
         */
        public List<String> getChangeset()
        {
            return changeset;
        }

        /**
         * An optional url pointing to this specific changelog.
         * This is used if the changelog message is too long to show.
         *
         * @return  null-able url for this changelog
         */
        public String getChangelogUrl()
        {
            return changelogUrl;
        }
    }
}
