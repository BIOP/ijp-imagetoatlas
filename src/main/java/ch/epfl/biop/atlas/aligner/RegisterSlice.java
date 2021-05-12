package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.atlas.plugin.RegistrationPluginHelper;
import ch.epfl.biop.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RegisterSlice extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(RegisterSlice.class);

    final SliceSources slice;
    Registration<SourceAndConverter<?>[]> registration;
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

    public RegisterSlice(MultiSlicePositioner mp,
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

    public SourcesProcessor getFixedSourcesProcessor() {
        return preprocessFixed;
    }

    public SourcesProcessor getMovingSourcesProcessor() {
        return preprocessMoving;
    }

    boolean isValid = true;

    @Override
    public boolean run() { //
        if (registration.isRegistrationDone()&&isValid()) {
            slice.appendRegistration(registration);
            return true;
        } else {
            isValid = slice.runRegistration(registration, preprocessFixed, preprocessMoving);
            return isValid;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public String toString() {
        return registration.toString() + " "+preprocessFixed.toString()+".Fixed // "+preprocessMoving.toString()+".Moving)] " + slice.getActionState(this);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        if (isValid) {
            switch (slice.getActionState(this)) {//.getRegistrationState(registration)) {
                case "(done)":
                    g.setColor(new Color(0, 255, 0, 200));
                    break;
                case "(locked)":
                    g.setColor(new Color(255, 0, 0, 200));
                    break;
                case "(pending)":
                    g.setColor(new Color(255, 255, 0, 200));
                    break;
            }
            if (RegistrationPluginHelper.isManual(registration)) {
                g.fillRect((int) (px - 7), (int) (py - 7), 14, 14);
            } else {
                g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.drawString("R", (int) px - 4, (int) py + 5);
        } else {
            g.setColor(new Color(255, 0, 0, 200));
            g.drawString("X", (int) px - 4, (int) py + 5);
        }
    }

    @Override
    public boolean cancel() {
        if (registration!=null) {
            logger.debug("Registration action "+this+" cancelled");
            registration.abort(); // Probably not necessary
        }
        return slice.removeRegistration(registration);
    }
}