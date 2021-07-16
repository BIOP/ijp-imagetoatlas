package ch.epfl.biop.bdv.gui;

import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Graphical element which can be added on top on a {@link bdv.util.BdvHandle} window
 *
 * Abstract class which is for instance implemented in {@link SquareGraphicalHandle} and
 * {@link CircleGraphicalHandle}.
 *
 * The method {@link GraphicalHandle#isPresentAt(int, int)} can be implemented in various ways
 * in order to account for various graphical elements.
 *
 * The actions which can be listened to are defined in {@link GraphicalHandleListener}
 *
 * This graphical element is either enabled or disabled. If it is disabled, it can be drawn,
 * but it is drawn differently from when it is enabled. Usually a disabled element can simply
 * not be drawn.
 *
 * Generally speaken, the coordinates are those of the screen (not bdv real space ones).
 *
 */
abstract public class GraphicalHandle extends MouseMotionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GraphicalHandle.class);

    /**
     * List of listeners for these elements
     */
    final List<GraphicalHandleListener> ghls = new ArrayList<>();

    /**
     * Behaviours which are enabled when the mouse is detected on the graphical element ( action map )
     */
    final Behaviours behaviours;

    /**
     * Defines which key or mouse action triggers the actions present in behaviours ( input map )
     */
    final TriggerBehaviourBindings bindings;

    /**
     * Name of the action and input map
     */
    final String nameMap;

    /**
     * Constructor
     * @param ghl default listener
     * @param behaviours action map enabled when the mouse is detected over this graphical handle
     * @param bindings input map enabled when the mouse is detected over this graphical handle
     * @param nameMap name of the input and action map pair
     */
    public GraphicalHandle(GraphicalHandleListener ghl, Behaviours behaviours, TriggerBehaviourBindings bindings, String nameMap) {
        addGraphicalHandleListener(ghl);
        this.behaviours = behaviours;
        this.bindings = bindings;
        this.nameMap = nameMap;
        if (ghl!=null) ghl.created(this);
    }

    /**
     * Adds a listener to this graphical element
     * @param ghl listener to add
     */
    public void addGraphicalHandleListener(GraphicalHandleListener ghl) {
        if (ghl!=null) {ghls.add(ghl);}
    }

    /**
     * Removes a listener from this graphical element
     * @param ghl listener to remove
     */
    public void removeGraphicalHandleListener(GraphicalHandleListener ghl) {
        ghls.remove(ghl);
    }

    /**
     * Overriden draw function
     * @param g graphics2d object
     */
    public void draw(Graphics2D g) {
        if (!isDisabled) {
            enabledDraw(g);
        } else {
            disabledDraw(g);
        }
    }

    /**
     * How to draw the graphical handle when it is enabled
     * @param g graphics2d java element
     */
    abstract protected void enabledDraw(Graphics2D g);

    /**
     * How to draw the graphical handle when it is disabled
     * @param g graphics2d java element
     */
    abstract protected void disabledDraw(Graphics2D g);

    /**
     * Removes all listeners to this handle
     */
    public void remove() {
        ghls.forEach(ghl -> ghl.removed(this));
    }

    /**
     *
     * @param x screen coordinate in x
     * @param y screen coordinate in y
     * @return true is the graphical handle is present at these coordinates
     */
    abstract boolean isPresentAt(int x, int y);

    boolean mouseAbove = false;

    /**
     * @return true if the mouse is over the graphical handle (as detected by {@link GraphicalHandle#isPresentAt(int, int)}
     */
    public boolean isMouseOver() {
        return mouseAbove;
    }

    protected boolean isDisabled = false;

    /**
     * Disable the graphical element:
     * - sends {@link GraphicalHandleListener#hover_out(GraphicalHandle)} event to all listener
     * - sends {@link GraphicalHandleListener#disabled(GraphicalHandle)} event to all listener
     * - deactivate action and input maps
     */
    public synchronized void disable() {
        if (!isDisabled) {
            if (mouseAbove) {
                mouseAbove = false;
                ghls.forEach(ghl -> ghl.hover_out(this));
            }
            isDisabled = true;
            ghls.forEach(ghl -> ghl.disabled(this));
            logger.debug("Removing "+nameMap+" from "+this+" (disable called) ");
            bindings.removeBehaviourMap(nameMap);
            bindings.removeInputTriggerMap(nameMap);
        }
    }

    /**
     *
     * @return the current xy screen coordinates of the graphical element
     */
    abstract public int[] getScreenCoordinates();

    /**
     * - sends {@link GraphicalHandleListener#enabled(GraphicalHandle)} event to all listener
     */
    public synchronized void enable() {
        if (isDisabled) {
            isDisabled = false;
            ghls.forEach(ghl -> ghl.enabled(this));
        }
    }

    /**
     * Monitors mouse movements and sends events to listener accordingly (hover_in, hover_out)
     * install and remove input and action maps according to
     * {@link GraphicalHandle#isPresentAt(int, int)} result of the new mouse movement
     *
     * TODO : this means that a movement of a graphical element below the mouse is not detected
     *
     * @param e MouseEvent event
     */
    @Override
    public synchronized void mouseMoved( final MouseEvent e )
    {
        final int x = e.getX();
        final int y = e.getY();

        if (isPresentAt(x,y)) {
            if ((!mouseAbove)&&(!isDisabled)) {
                mouseAbove = true;
                ghls.forEach(ghl -> ghl.hover_in(this));
                behaviours.install(bindings, nameMap);
                logger.debug("Installing "+nameMap+" from "+this+" (x = "+x+" and y = "+y+")");
            }
        } else {
            if ((mouseAbove)){//&&(!isDisabled)) {
                mouseAbove = false;
                ghls.forEach(ghl -> ghl.hover_out(this));
                logger.debug("Removing "+nameMap+" from "+this+" (x = "+x+" and y = "+y+")");
                bindings.removeBehaviourMap(nameMap);
                bindings.removeInputTriggerMap(nameMap);
            }
        }
    }

}
