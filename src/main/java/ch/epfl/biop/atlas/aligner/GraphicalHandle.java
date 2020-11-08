package ch.epfl.biop.atlas.aligner;

import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

abstract public class GraphicalHandle extends MouseMotionAdapter {

    final GraphicalHandleListener ghl;

    final Behaviours behaviours;

    final TriggerBehaviourBindings bindings;

    final String nameMap;

    public GraphicalHandle(GraphicalHandleListener ghl, Behaviours behaviours, TriggerBehaviourBindings bindings, String nameMap) {
        this.ghl = ghl;
        this.behaviours = behaviours;
        this.bindings = bindings;
        this.nameMap = nameMap;
        ghl.created(this);
    }

    public void draw(Graphics2D g) {
        if (!isDisabled) {
            enabledDraw(g);
        } else {
            disabledDraw(g);
        }
    }

    abstract protected void enabledDraw(Graphics2D g);
    abstract protected void disabledDraw(Graphics2D g);

    public void remove() {
        ghl.removed(this);
    }

    abstract boolean isPresentAt(int x, int y);

    boolean mouseAbove = false;

    public boolean isMouseOver() {
        return mouseAbove;
    }

    protected boolean isDisabled = false;

    public synchronized void disable() {
        if (!isDisabled) {
            if (mouseAbove) {
                mouseAbove = false;
                ghl.hover_out(this);
            }
            isDisabled = true;
        }
    }

    public synchronized void enable() {
        if (isDisabled) {
            isDisabled = false;
        }
    }

    @Override
    public synchronized void mouseMoved( final MouseEvent e )
    {
        final int x = e.getX();
        final int y = e.getY();

        if (isPresentAt(x,y)) {
            if ((!mouseAbove)&&(!isDisabled)) {
                mouseAbove = true;
                ghl.hover_in(this);
                behaviours.install(bindings, nameMap);
            }
        } else {
            if ((mouseAbove)&&(!isDisabled)) {
                mouseAbove = false;
                ghl.hover_out(this);
                bindings.removeBehaviourMap(nameMap);
                bindings.removeInputTriggerMap(nameMap);
            }
        }

    }

}
