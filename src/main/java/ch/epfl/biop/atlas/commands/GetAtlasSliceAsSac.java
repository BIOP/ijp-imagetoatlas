package ch.epfl.biop.atlas.commands;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Export Current Atlas Slice as SourceAndConverter")
public class GetAtlasSliceAsSac implements Command{
	@Parameter
	BiopAtlas atlas;
	
	@Parameter(type = ItemIO.OUTPUT)
	SourceAndConverter[] imageLabel;
	
	@Parameter(type = ItemIO.OUTPUT)
	SourceAndConverter[] imageStructure;
	
	@Override
	public void run() {
		imageLabel = atlas.map.getCurrentLabelImageAsSacs();
		imageStructure = atlas.map.getCurrentStructuralImageAsSacs();
	}
	
}
