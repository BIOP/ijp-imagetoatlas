package ch.epfl.biop.atlastoimg2d.multislice;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

abstract public class GraphicalHandle extends MouseMotionAdapter {

    final GraphicalHandleListener ghl;

    public GraphicalHandle(GraphicalHandleListener ghl) {
        this.ghl = ghl;
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
            if (!mouseAbove) {
                mouseAbove = true;
                ghl.hover_in(this);
            }
        } else {
            if (mouseAbove) {
                mouseAbove = false;
                ghl.hover_out(this);
            }
        }

    }

}
