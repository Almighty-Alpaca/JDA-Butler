package com.kantenkugel.discordbot.graphql.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells GQLQuery to ignore this field when searching for values in the server response.
 * This is useful for values only used client-side and not being fetched from graqhql server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GQLIgnore {}
