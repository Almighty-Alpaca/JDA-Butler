package com.kantenkugel.discordbot.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fields annotated with this annotation should not be attempted to be deserialized from json.
 * <p>
 * This should normally not be needed as Gson doesn't throw warnings if some field was not present in json.
 * <br>It instead assigns default values to fields which weren't present in json
 * (null for objects, zero for number primitives and false for boolean primitive).
 * If that is not desired for whatever reason (or to be extra sure),
 * use this annotation to let Gson completely ignore the field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonIgnore {}
