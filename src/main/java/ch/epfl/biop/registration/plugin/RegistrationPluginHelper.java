package ch.epfl.biop.registration.plugin;

import ch.epfl.biop.registration.Registration;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.PluginService;

import java.util.Arrays;

// Facilitates accessing annotation values
public class RegistrationPluginHelper {

    /**
     * Does the registration required an user input ?
     * @return true if user input is required
     */
    public static boolean isManual(Registration<?> reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.isManual();
        } else {
            if (reg instanceof ExternalRegistrationPlugin) {
                return ((ExternalRegistrationPlugin) reg).isManual();
            } else {
                return false; // Default value if no annotation is present
            }
        }
    }

    /**
     * Can the registration be edited after it has run ?
     * Considered a manual task by default
     */
    public static boolean isEditable(Registration<?> reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.isEditable();
        } else {
            if (reg instanceof ExternalRegistrationPlugin) {
                return ((ExternalRegistrationPlugin) reg).isEditable();
            } else {
                return false; // Default value if no annotation is present
            }
        }
    }

    /**
     * @return the command class the user has to call in order to start a registration
     */
    public static Class<? extends Command>[] userInterfaces(Registration<?> reg) {
        if (reg.getClass().isAnnotationPresent(RegistrationTypeProperties.class)) {
            final RegistrationTypeProperties annotation = reg.getClass()
                    .getAnnotation(RegistrationTypeProperties.class);
            return annotation.userInterface();
        } else {
            if (reg instanceof ExternalRegistrationPlugin) {
                return ((ExternalRegistrationPlugin) reg).userInterface();
            } else {
                return new Class[0]; // Default value if no annotation is present
            }
        }
    }

    /**
     * Assumes unicity! Find the registration class from a UI class
     * @param queryUIClass the ui class from which the registration is supposed to be found
     * @return null is nothing is found
     */
    public static Class<? extends IRegistrationPlugin> registrationFromUI (Context ctx, Class<? extends Command> queryUIClass) {
        PluginService pluginService = ctx.getService(PluginService.class);

        // OK... intellij found this alone, let's hope it works
        return pluginService
                .getPluginsOfType(IRegistrationPlugin.class)
                .stream().map(pluginService::createInstance)
                .filter(plugin -> Arrays.asList(RegistrationPluginHelper.userInterfaces(plugin)).contains(queryUIClass))
                .findFirst()
                .map(IRegistrationPlugin::getClass)
                .orElse(null);

    }

}
