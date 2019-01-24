package ch.epfl.biop.viewer;

import ch.epfl.biop.atlas.BiopAtlas;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class AtlasDisplay extends AbstractDisplay<BiopAtlas> {
    public AtlasDisplay() {
        super(BiopAtlas.class);
    }
}
