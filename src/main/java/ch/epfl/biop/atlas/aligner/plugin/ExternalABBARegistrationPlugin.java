package ch.epfl.biop.atlas.aligner.plugin;
import org.scijava.command.Command;

/**
 * An registration plugin interface without any annotation - useful
 * mostly useful in order to add a plugin from PyImageJ
 */
public interface ExternalABBARegistrationPlugin extends IABBARegistrationPlugin {

    /**
     * Does the registration required an user input ?
     * @return true if some user action is required
     */
    boolean isManual();

    /**
     * Can the registration be edited after it has run ?
     * Considered a manual task by default
     * @return true if the registration can be edited a posteriori
     */
    boolean isEditable();

    /**
     * @return the command class the user has to call in order to start a registration
     */
    Class<? extends Command>[] userInterface();

}
