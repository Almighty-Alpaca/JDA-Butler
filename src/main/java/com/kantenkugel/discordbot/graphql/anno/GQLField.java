package com.kantenkugel.discordbot.graphql.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation for fields that allow further customization
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GQLField
{
    /**
     * The name of the json key for this entity. If empty (default), the lowercase name of the entity class will be used
     *
     * @return  Custom name for json key
     */
    String name() default "";

    /**
     * Parent path to this fields entry in json.
     * If empty (default), the json key from {@link #name()} is searched at top-level.
     * Multi-level path is delimited with {@code .} (dot) like "path.to.entity"
     * <br>Note: this should not include the actual key name. it is automatically appended
     *
     * @return  Custom path to entity json key
     */
    String path() default "";

    /**
     * Only used for fields of type List.
     * This path denotes a custom path from within the json array to actual entity roots similar to {@link #path()}
     *
     * @return  Custom sub-array path to entity roots used for List fields
     */
    String collectionPath() default "";
}
