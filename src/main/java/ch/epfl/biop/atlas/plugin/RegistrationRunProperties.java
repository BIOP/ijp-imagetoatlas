package ch.epfl.biop.atlas.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegistrationRunProperties {

    /**
     * Does the registration required an user input ?
     * @return
     */
    boolean isManual();

    /**
     * Can the registration be edited after it has run ?
     * Considered a manual task by default
     */
    boolean isEditable() default false;
}

