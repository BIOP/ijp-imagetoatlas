package ch.epfl.biop.atlas.plugin;

import ch.epfl.biop.registration.Registration;
import org.scijava.command.Command;

import java.lang.reflect.Method;

// Facilitates accessing annotation values
public class RegistrationPluginHelper {

    /**
     * Does the registration required an user input ?
     * @return
     */
    public static boolean isManual(Registration reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.isManual();
        } else {
            return false; // Default value if no annotation is present
        }
    }

    /**
     * Can the registration be edited after it has run ?
     * Considered a manual task by default
     */
    public static boolean isEditable(Registration reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.isEditable();
        } else {
            return false; // Default value if no annotation is present
        }
    }

    /**
     * @return the command class the user has to call in order to start a registration
     * // TODO : restrict a bit the Command to facilitate ui writing
     */
    public static Class<? extends Command>[] userInterfaces(Registration reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.userInterface();
        } else {
            return null; // Default value if no annotation is present
        }
    }


}
