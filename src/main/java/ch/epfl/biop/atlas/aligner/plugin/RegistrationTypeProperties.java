package ch.epfl.biop.atlas.aligner.plugin;

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
    boolean isEditable();

    /**
     * @return the command class the user has to call in order to start a registration
     */
    Class<? extends Command>[] userInterface();
}

