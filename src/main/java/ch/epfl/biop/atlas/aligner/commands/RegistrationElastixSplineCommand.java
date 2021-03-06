package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Spline)")
public class RegistrationElastixSplineCommand extends RegistrationCommand {

    @Parameter(label = "Number of control points along X, minimum 2.", validater = "checkNumberOfPoints")
    int nbControlPointsX = 10;

    @Parameter(label = "Show registration results as ImagePlus")
    boolean showIJ1Result;

    public void runValidated() {
        mp.registerElastixSpline(getFixedFilter(), getMovingFilter(), nbControlPointsX, showIJ1Result);
    }

    public void checkNumberOfPoints() {
        if (nbControlPointsX<2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
        }
    }

}
