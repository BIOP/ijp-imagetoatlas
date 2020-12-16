package ch.epfl.biop.atlas.aligner;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.awt.*;
import java.util.function.Supplier;

public class SquareGraphicalHandle extends GraphicalHandle{

    Supplier<Integer[]> coords;
    Supplier<Integer> radius;
    Supplier<Integer[]> color;

    public SquareGraphicalHandle(GraphicalHandleListener ghl,
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

    public SquareGraphicalHandle(GraphicalHandleListener ghl,
                                 Behaviour behaviour, String behaviourName, String trigger,
                                 TriggerBehaviourBindings bindings,
                                 Supplier<Integer[]> coords,
                                 Supplier<Integer> radius,
                                 Supplier<Integer[]> color) {
        super(ghl, wrapBehaviours(behaviour, behaviourName, trigger), bindings, behaviour.toString());
        this.radius = radius;
        this.color = color;
        this.coords = coords;
    }

    static Behaviours wrapBehaviours(Behaviour behaviour, String behaviourName, String trigger) {
        Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.behaviour(behaviour, behaviourName, trigger);
        return behaviours;
    }

    @Override
    public synchronized void enabledDraw(Graphics2D g) {
        Integer r = radius.get();
        Integer[] pos = coords.get();
        Integer[] c = color.get();
        g.setColor(new Color(c[0], c[1], c[2], c[3]));
        if (this.mouseAbove) {
            r=(int) (r*1.2);
        }
        g.fillRect(pos[0] - r, pos[1] - r, 2*r, 2*r);
    }

    @Override
    public synchronized void disabledDraw(Graphics2D g) {

    }

    @Override
    synchronized boolean isPresentAt(int x, int y) {
        Integer[] pos = coords.get();
        double r = (double)(radius.get());
        if ((pos == null) || (pos[0] == null) || (pos[1] == null)) return false;
        return (x>pos[0]-r)&&(x<pos[0]+r)&&(y>pos[1]-r)&&(y<pos[1]+r);
    }

    @Override
    public int[] getScreenCoordinates() {
        int[] unboxed = new int[3];
        Integer[] c = coords.get();
        for (int i = 0;i<3;i++) {
            unboxed[i] = c[i];
        }
        return unboxed;
    }
}
