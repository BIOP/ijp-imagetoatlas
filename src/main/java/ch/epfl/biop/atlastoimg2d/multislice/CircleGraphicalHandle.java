package ch.epfl.biop.atlastoimg2d.multislice;

import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.awt.*;
import java.util.function.Supplier;

public class CircleGraphicalHandle extends GraphicalHandle{

    Supplier<Integer[]> coords;
    Supplier<Integer> radius;
    Supplier<Integer[]> color;

    public CircleGraphicalHandle(GraphicalHandleListener ghl,
                                 Behaviours behaviours,
                                 TriggerBehaviourBindings bindings,
                                 String nameMap,
                                 Supplier<Integer[]> coords,
                                 Supplier<Integer> radius,
                                 Supplier<Integer[]> color) {
        super(ghl, behaviours, bindings, nameMap);
        this.radius = radius;
        this.color = color;
        this.coords = coords;
    }

    Integer[] pos;
    Integer r;

    @Override
    public synchronized void enabledDraw(Graphics2D g) {
        r = radius.get();
        pos = coords.get();
        Integer[] c = color.get();
        g.setColor(new Color(c[0], c[1], c[2], c[3]));
        if (!this.mouseAbove) {
            r=(int) (r*0.75);
        }
        g.fillOval(pos[0] - r / 2, pos[1] - r / 2, r, r);
    }

    @Override
    public synchronized void disabledDraw(Graphics2D g) {

    }

    @Override
    synchronized boolean isPresentAt(int x, int y) {
        double d = (pos[0]-x)*(pos[0]-x)+(pos[1]-y)*(pos[1]-y);
        return d*d<4*r*r;
    }
}
