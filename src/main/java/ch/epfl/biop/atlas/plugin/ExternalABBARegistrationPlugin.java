package ch.epfl.biop.atlas.plugin;
import org.scijava.command.Command;

/**
 * An registration plugin interface without any annotation - useful
 * mostly useful in order to add a plugin from PyImageJ
 */
public interface ExternalABBARegistrationPlugin extends IABBARegistrationPlugin {

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
     * // TODO : restrict a bit the Command to facilitate ui writing
     */
    Class<? extends Command>[] userInterface();

}
