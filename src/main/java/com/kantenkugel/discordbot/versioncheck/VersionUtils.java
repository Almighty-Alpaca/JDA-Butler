package com.kantenkugel.discordbot.versioncheck;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtils
{
    /**
     * Comparator for comparing VersionSplits (older < newer)
     */
    public static final Comparator<VersionSplits> VERSION_COMP = Comparator.<VersionSplits>
             comparingInt(s -> s.major)
            .thenComparingInt(s -> s.minor)
            .thenComparingInt(s -> s.patch)
            .thenComparingInt(s -> s.build)
            .thenComparing((s1, s2)->
                    s1.preReleaseInfo == null && s2.preReleaseInfo == null ? 0
                            : s1.preReleaseInfo != null ? -1 : 1
            );

    /**
     * First parses Strings to {@link VersionSplits} via {@link #parseVersion(String)},
     * then compares those via {@link #VERSION_COMP} (older < newer).
     * <br/><b>Note:</b> This only works on versions in extended SemVer format (optional minor/patch and build)
     */
    public static final Comparator<String> VERSION_STRING_COMP = Comparator.comparing(VersionUtils::parseVersion, VERSION_COMP);

    /**
     * Tries to parse given String to {@link VersionSplits}.
     * <br/><b>Note:</b> This only works on versions in extended SemVer format (optional minor/patch and build)
     */
    public static VersionSplits parseVersion(String version)
    {
        return VersionSplits.parseExtendedSemver(version);
    }

    public static class VersionSplits
    {
        //major is mandatory, others default to 0
        public final int major, minor, patch, build;
        //default to null if not present
        public final String preReleaseInfo, metaData;

        public VersionSplits(int major, int minor, int patch, int build, String preReleaseInfo, String metaData)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.build = build;
            this.preReleaseInfo = preReleaseInfo;
            this.metaData = metaData;
        }

        /**
         * Regex Pattern for parsing extended SemVer.
         *
         * <p>Capture groups:
         * <ul>
         *     <li>Group 1: major version (number, mandatory)</li>
         *     <li>Group 2: minor version (number, optional)</li>
         *     <li>Group 3: patch version (number, optional)</li>
         *     <li>Group 4: build number (number, optional)</li>
         *     <li>Group 5: pre-release tags (String, optional)</li>
         *     <li>Group 6: meta information (String, optional)</li>
         * </ul>
         */
        public static final Pattern EXTENDED_SEMVER_PATTERN =
                Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:_(\\d+))?" +
                        "(?:-([A-Za-z0-9-.]+))?(?:\\+([A-Za-z0-9-.]+))?");

        private static VersionSplits parseExtendedSemver(String versionString)
        {
            Matcher matcher = EXTENDED_SEMVER_PATTERN.matcher(versionString);
            if(!matcher.matches())
                throw new IllegalArgumentException("Given version string is not extended semver");
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            int build = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
            String preReleaseInfo = matcher.group(5);
            String metaData = matcher.group(6);

            return new VersionSplits(major, minor, patch, build, preReleaseInfo, metaData);
        }
    }
}
