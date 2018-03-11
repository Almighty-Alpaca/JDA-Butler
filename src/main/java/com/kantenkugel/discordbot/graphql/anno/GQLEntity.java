package com.kantenkugel.discordbot.graphql.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Has to be present on every entity that should be parsed. otherwise, all non-scalar fields (and non-lists) are ignored
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GQLEntity {}
