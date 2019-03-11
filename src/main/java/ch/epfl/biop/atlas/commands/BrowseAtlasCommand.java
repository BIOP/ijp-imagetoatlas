package ch.epfl.biop.atlas.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.BiopAtlas;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Open Atlas...")
public class BrowseAtlasCommand implements Command {

	@Parameter(type = ItemIO.OUTPUT)
	BiopAtlas ba;
	
	@Parameter(choices={"Adult Mouse Allen Brain CCF 2017"})
	String atlasId;
	
	@Parameter
	CommandService cs;
	
	@Override
	public void run() {
		Future<CommandModule> f=null;
		switch(atlasId) {
		case "Adult Mouse Allen Brain CCF 2017":
			f = cs.run(AllenBrainAdultMouseAtlasCCF2017.class, true);
			break;
		}
		if (f!=null) {
			try {
				ba = (BiopAtlas) (f.get().getOutput("ba"));
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}

}
