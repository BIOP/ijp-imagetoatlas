package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.registration.Registration;

import java.awt.*;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RegisterSlice extends CancelableAction {
    final SliceSources slice;
    Registration<SourceAndConverter[]> registration;
    final SourcesProcessor preprocessFixed;
    final SourcesProcessor preprocessMoving;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public Registration<SourceAndConverter[]>  getRegistration() {
        return registration;
    }

    public void setRegistration(Registration<SourceAndConverter[]> registration) {
        this.registration = registration;
    }

    public RegisterSlice(MultiSlicePositioner mp,
                         SliceSources slice,
                         Registration<SourceAndConverter[]> registration,
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

    @Override
    public boolean run() { //
        if (registration.isRegistrationDone()) {
            slice.appendRegistration(registration);
        } else {
            slice.runRegistration(registration, preprocessFixed, preprocessMoving);
        }
        return true;
    }

    public String toString() {
        return "Registration [" + registration.getClass().getSimpleName() + "] " + slice.getActionState(this);//getRegistrationState(registration);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)){//.getRegistrationState(registration)) {
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
        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("R", (int) px - 4, (int) py + 5);
    }

    @Override
    public boolean cancel() {
        return slice.removeRegistration(registration);
    }
}