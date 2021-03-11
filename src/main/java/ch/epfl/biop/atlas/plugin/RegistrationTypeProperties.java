package ch.epfl.biop.atlas.plugin;

import org.scijava.command.Command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegistrationTypeProperties {

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

    /**
     * @return the command class the user has to call in order to start a registration
     * // TODO : restrict a bit the Command to facilitate ui writing
     */
    Class<? extends Command> userInterface();
}

