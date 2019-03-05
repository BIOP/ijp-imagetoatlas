package ch.epfl.biop.atlastoimg2d.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.BrowseAtlasCommand;
import ch.epfl.biop.atlastoimg2d.AllenAtlasToImagePlusElastixRegister;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas>Wizard")
public class ImageToAtlasWizard implements Command {

	//@Parameter(required = false)
	//public BiopAtlas ba;
	
	//@Parameter(type = ItemIO.INPUT)
	//public ImagePlus imp;
	
	@Parameter
	public CommandService cs;

	//@Parameter
	//public ObjectService os;
	
	@Parameter(type = ItemIO.OUTPUT)
	public AtlasToImg2D aligner;

	//@Parameter
	//boolean setLocationInteractively = true;
	
	@Override
	public void run() {
		try {
			Future<CommandModule> task = cs.run(ImageToAtlasConstruct.class, true);
			CommandModule cm = task.get();
			aligner = (AtlasToImg2D) cm.getOutput("aligner");

			WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Pick carefully the slice you'd like.");
			dialog.show();
			aligner.setAtlasLocation(
			        aligner.getAtlas().map.getCurrentLocation()
            );

			task = cs.run(ImageToAtlasRegister.class,true,"aligner", aligner );
			cm = task.get();



		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		/*if (ba==null) {
			// Atlas not set -> need to set one
			Future<CommandModule> f = cs.run(BrowseAtlasCommand.class, true);
			try {
				ba = (BiopAtlas) f.get().getOutput("ba");
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return;
			}
		}
		
		if (ba == null) {
			System.out.println("Atlas not initialized");
			return;
		}
		
		aligner = new AllenAtlasToImagePlusElastixRegister();
		aligner.setAtlas(ba);
		// Wait for user to choose its slice
		
		if (setLocationInteractively) {
			WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Pick carefully the slice you'd like.");
			dialog.show();
		}
		
		System.out.println("Starting registration task...");
		
		aligner.setAtlasLocation(ba.map.getCurrentLocation());
		if (os!=null) aligner.setObjectService(os);
		aligner.setImage(imp);
		aligner.register();*/
	}
	
}
