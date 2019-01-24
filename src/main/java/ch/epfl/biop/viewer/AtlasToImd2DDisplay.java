package ch.epfl.biop.viewer;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class AtlasToImd2DDisplay extends AbstractDisplay<AtlasToImg2D> {
    public AtlasToImd2DDisplay() {
        super(AtlasToImg2D.class);
    }
}
