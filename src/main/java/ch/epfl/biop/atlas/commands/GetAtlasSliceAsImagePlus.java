package ch.epfl.biop.atlas.commands;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.BiopAtlas;
import ij.ImagePlus;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Export Current Atlas Slice as ImagePlus")
public class GetAtlasSliceAsImagePlus implements Command{
	@Parameter
	BiopAtlas atlas;
	
	@Parameter(type = ItemIO.OUTPUT)
	ImagePlus imageLabel;
	
	@Parameter(type = ItemIO.OUTPUT)
	ImagePlus imageStructure;
	
	@Override
	public void run() {
		imageLabel = atlas.map.getCurrentLabelImageAsImagePlus();
		imageStructure = atlas.map.getCurrentStructuralImageAsImagePlus();
	}
	
}
