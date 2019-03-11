package ch.epfl.biop.atlas;

import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import org.scijava.Priority;

/**
 * Enables Atlas to be garbage collected if the Big Data Viewer window is closed by the user
 */

@Plugin(type = PostprocessorPlugin.class, priority = Priority.VERY_LOW - 1)
public class AtlasProcessorPlugin extends AbstractPostprocessorPlugin {

	@Parameter(required = false)
	private UIService ui;
	
	@Parameter
	ObjectService os;
	
	@Override
	public void process(Module module) {
		if (ui == null) {
			// no UIService available for displaying results
			return;
		} else {

		}
		
		module.getInfo().outputs().forEach(output -> {
			if ((output.getGenericType()==BiopAtlas.class)&&(output.isOutput())) {
				final String name = output.getName();
				BiopAtlas ba = (BiopAtlas) module.getOutput(name);
				if (!os.getObjects(BiopAtlas.class).contains(ba)) { // Avoids double addition
					os.addObject(ba);
				}
				ba.runOnClose(() -> os.removeObject(ba)); // removes the object when the atlas window is closed
			}
		});
	}

}
