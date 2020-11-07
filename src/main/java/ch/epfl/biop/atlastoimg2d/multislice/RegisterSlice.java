package ch.epfl.biop.atlastoimg2d.multislice;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.Registration;

import java.awt.*;
import java.util.function.Function;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RegisterSlice extends CancelableAction {
    final SliceSources slice;
    Registration<SourceAndConverter[]> registration;
    final Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed;
    final Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving;

    boolean alreadyRun = false;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public Registration<SourceAndConverter[]>  getRegistration() {
        return registration;
    }

    public void  setRegistration(Registration<SourceAndConverter[]> registration) {
        this.registration = registration;
    }

    public RegisterSlice(MultiSlicePositioner mp,
                         SliceSources slice,
                         Registration<SourceAndConverter[]> registration,
                         Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                         Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
        super(mp);
        this.slice = slice;
        this.registration = registration;
        this.preprocessFixed = preprocessFixed;
        this.preprocessMoving = preprocessMoving;
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