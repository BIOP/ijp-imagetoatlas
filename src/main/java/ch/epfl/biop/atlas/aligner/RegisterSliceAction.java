package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RegisterSliceAction extends CancelableAction {

    protected static final Logger logger = LoggerFactory.getLogger(RegisterSliceAction.class);

    final SliceSources slice;
    Registration<SourceAndConverter<?>[]> registration = null;
    Supplier<Registration<SourceAndConverter<?>[]>> registrationSupplier = null;
    final SourcesProcessor preprocessFixed;
    final SourcesProcessor preprocessMoving;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public Registration<SourceAndConverter<?>[]>  getRegistration() {
        return registration;
    }

    public void setRegistration(Registration<SourceAndConverter<?>[]> registration) {
        this.registration = registration;
    }

    public RegisterSliceAction(MultiSlicePositioner mp,
                               SliceSources slice,
                               Registration<SourceAndConverter<?>[]> registration,
                               SourcesProcessor preprocessFixed,
                               SourcesProcessor preprocessMoving) {
        super(mp);
        this.slice = slice;
        this.registration = registration;
        this.preprocessFixed = preprocessFixed;
        this.preprocessMoving = preprocessMoving;
    }

    public RegisterSliceAction(MultiSlicePositioner mp,
                               SliceSources slice,
                               Supplier<Registration<SourceAndConverter<?>[]>> registrationSupplier,
                               SourcesProcessor preprocessFixed,
                               SourcesProcessor preprocessMoving) {
        super(mp);
        this.slice = slice;
        this.registrationSupplier = registrationSupplier;
        this.preprocessFixed = preprocessFixed;
        this.preprocessMoving = preprocessMoving;
    }

    public SourcesProcessor getFixedSourcesProcessor() {
        return preprocessFixed;
    }

    public SourcesProcessor getMovingSourcesProcessor() {
        return preprocessMoving;
    }

    boolean isValid = true;

    @Override
    protected boolean run() { //
        if (registration == null) {
            registration = registrationSupplier.get();
        }
        if (registration.isRegistrationDone()&&isValid()) {
            slice.appendRegistration(registration);
            slice.sourcesChanged();
            getMP().stateHasBeenChanged();
            return true;
        } else {
            isValid = slice.runRegistration(registration, preprocessFixed, preprocessMoving);
            slice.sourcesChanged();
            getMP().stateHasBeenChanged();
            return isValid;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public String toString() {
        if (registration == null) {
            return "Future<Registration>";
        }
        return registration.toString() + " "+preprocessFixed.toString()+".Atlas // "+preprocessMoving.toString()+".Section)] " + slice.getActionState(this);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        double size = 7.0*scale;
        if (isValid) {
            if (scale<0.9) {
                g.setColor(new Color(128, 128, 128, 200));
            } else
            switch (slice.getActionState(this)) {
                case "(done)":
                    g.setColor(new Color(0, 255, 0, 200));
                    break;
                case "(locked)":
                    g.setColor(new Color(205, 1, 106, 200));
                    break;
                case "(pending)":
                    g.setColor(new Color(255, 255, 0, 200));
                    break;
            }
            if ((registration != null) && (RegistrationPluginHelper.isManual(registration))) {
                g.fillRect((int) (px - size), (int) (py - size), (int) (2.0*size), (int) (2.0*size));
            } else {
                g.fillOval((int) (px - size), (int) (py - size), (int) (2.0*size), (int) (2.0*size));
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.drawString("R", (int) px - 4, (int) py + 5);
        } else {
            g.setColor(new Color(205, 1, 106, 199));
            g.drawString("X", (int) px - 4, (int) py + 5);
        }
    }

    @Override
    protected boolean cancel() {
        if (registration!=null) {
            logger.debug("Registration action "+this+" cancelled");
            registration.abort(); // Probably not necessary
        }
        boolean result = slice.removeRegistration(registration);
        getMP().stateHasBeenChanged();
        return result;
    }

}