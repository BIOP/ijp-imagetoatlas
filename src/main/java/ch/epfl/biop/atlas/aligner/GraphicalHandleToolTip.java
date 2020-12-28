package ch.epfl.biop.atlas.aligner;

import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public class GraphicalHandleToolTip extends GraphicalHandle implements GraphicalHandleListener {
    final GraphicalHandle gh;
    Supplier<String> toolTipText;

    int shiftX, shiftY;

    public GraphicalHandleToolTip(GraphicalHandle gh, Supplier<String> toolTip, int shiftX, int shiftY) {
        super(null, null, null, null);
        this.gh = gh;
        this.toolTipText = toolTip;
        this.gh.addGraphicalHandleListener(this);
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }

    public GraphicalHandleToolTip(GraphicalHandle gh, String text, int shiftX, int shiftY) {
        super(null, null, null, null);
        this.gh = gh;
        this.toolTipText = () -> text;
        this.gh.addGraphicalHandleListener(this);
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }

    @Override
    public void disabled(GraphicalHandle src) {
        if (src == this.gh) {
            disable();
        }
    }

    @Override
    public void enabled(GraphicalHandle src) {
        if (src == this.gh) {
            enable();
        }
    }

    @Override
    public void hover_in(GraphicalHandle src) {}

    @Override
    public void hover_out(GraphicalHandle src) {}

    @Override
    public void created(GraphicalHandle src) {}

    @Override
    public void removed(GraphicalHandle src) {}

    @Override
    protected void enabledDraw(Graphics2D g) {
        //Integer r = 40;//radius.get();

        Rectangle bounds = g.getClip().getBounds();

        int[] pos = gh.getScreenCoordinates();//coords.get();
        Integer[] c = {255,255,255,128};//color.get();
        g.setColor(new Color(c[0], c[1], c[2], c[3]));
        g.setFont(new Font("TimesRoman", Font.PLAIN, 16));

        int xPos = pos[0] - 40 + shiftX;

        int yPos = pos[1] - 50 + shiftY;

        if (xPos > bounds.width - 70 ) xPos = bounds.width - 70;

        if ( xPos < 0 ) xPos = 0;

        if ( yPos > bounds.height - 30 ) yPos = bounds.height - 30;

        if ( yPos < 30 ) yPos = 30;

        g.drawString(toolTipText.get(), xPos, yPos);
    }

    @Override
    protected void disabledDraw(Graphics2D g) {
    }

    @Override
    boolean isPresentAt(int x, int y) {
        return false;
    }

    @Override
    public int[] getScreenCoordinates() {
        return gh.getScreenCoordinates();
    }

    @Override
    public synchronized void mouseMoved( final MouseEvent e )
    {
        /*final int x = e.getX();
        final int y = e.getY();

        if (isPresentAt(x,y)) {
            if ((!mouseAbove)&&(!isDisabled)) {
                mouseAbove = true;
                ghls.forEach(ghl -> ghl.hover_in(this));
                //behaviours.install(bindings, nameMap);
            }
        } else {
            if ((mouseAbove)&&(!isDisabled)) {
                mouseAbove = false;
                ghls.forEach(ghl -> ghl.hover_out(this));
                //bindings.removeBehaviourMap(nameMap);
                //bindings.removeInputTriggerMap(nameMap);
            }
        }*/

    }
}
