package ch.epfl.biop.registration.plugin;

import org.scijava.command.Command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegistrationTypeProperties {

    /**
     * Does the registration required an user input ?
     * @return true if the registration requires user input
     */
    boolean isManual();

    /**
     * Can the registration be edited after it has run ?
     * Considered a manual task by default
     * @return true if the registration can be edited a posteriori
     */
    boolean isEditable();

}

