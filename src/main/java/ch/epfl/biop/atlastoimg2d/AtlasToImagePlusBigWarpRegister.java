package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ij.ImagePlus;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import mpicbg.spim.data.SpimDataException;

// Big question : should I add ROIs linked to that once the registration is done ?

// Probably yes, Or I should launch a command asking for manual improvements ?

public class AtlasToImagePlusBigWarpRegister extends AtlasToImagePlus2D_Core {
	
	Thread registerThread;
	
	@Override
	public void register() {
		if (this.atlasLocation==null) {
			this.atlasLocation = this.ba.map.getCurrentLocation();
		} else {
			this.ba.map.setCurrentLocation(this.atlasLocation);
		}

		ImagePlus imgAtlas = this.ba.map.getCurrentStructuralImage();

		try
		{
			//new RepeatingReleasedEventsFixer().install();
			final BigWarp bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( imgAtlas, this.imageUsedForRegistration ), "Big Warp",  null );
			bw.getViewerFrameP().getViewerPanel().requestRepaint();
			bw.getViewerFrameQ().getViewerPanel().requestRepaint();
			bw.getLandmarkFrame().repaint();
		}
		catch (final SpimDataException e)
		{
			e.printStackTrace();
			return;
		}

		//imageUsedForRegistration

		// BigWarp between imgAtlas = imageUsedForRegistration



		// First : select the location


		/*if ((registerThread!=null)&&(registerThread.isAlive())) {
			System.out.println("Registration in progress... cancel it before re running it");
			return;
		}
		if (this.registrationSet) {
			System.out.println("Registration already set. Please reset it before continuing.");
			return;
		}
		if (!this.isInitialized()) {
			System.out.println("Atlas and / or image not set.");
			return;
		}
		
		if (this.atlasLocation==null) {
			this.atlasLocation = this.ba.map.getCurrentLocation();
		} else {
			this.ba.map.setCurrentLocation(this.atlasLocation);
		}

		ImagePlus imgAtlas = this.ba.map.getCurrentStructuralImage();
		
		er = new Elastix_Register();
		er.movingImage=this.imageUsedForRegistration;
		er.fixedImage=imgAtlas;
		er.rigid=false;//true;
		er.fast_affine=true;
		er.affine=false;
		er.spline=false;//true;
		
		// Actually Perform the registration

		er.run();
		/*System.out.println("Registration in progress ... this can take up to 2 minutes.");
		registerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				er.run();
				notifyRegistrationDone(er);
			}
		});
		registerThread.start();*/

		//notifyRegistrationDone(er);

		// elastix registration done

	}

	@Override
	public Object getRegistration() {
		return null;
	}

	//public Elastix_Register er;

	/*void notifyRegistrationDone(Elastix_Register er) {
		this.er=er;
		this.registrationSet=true;
		//os.addObject(er.rh);
		//os.getContext().getService(DisplayService.class).createDisplay(er.rh);
	}*/
	
	void cancelRegistration() {
	/*	if ((registerThread!=null)&&(registerThread.isAlive())) {
			registerThread.interrupt();
		}*/
	}

	@Override
	public void putRoisToImageJ(ConvertibleRois cr) {
		/*if (this.registrationSet) {
			Transformix_TransformROIs ttr = new Transformix_TransformROIs();
			ttr.cr_in = cr;
			ttr.roisFromRoiManager = false;
			ttr.rh = er.rh;
			ttr.run();
			ttr.cr_out.to(RoiManager.class);
		} else {
			System.out.println("Registration not set.");
		}*/
	}

	@Override
	public void putRoisToQuPath(ConvertibleRois cr) {
		if (this.registrationSet) {
			System.out.println("Unsupported operation");
		} else {
			System.out.println("Registration not set.");
		}
	}

    /*public RegisterHelper getRegistration() {
		return this.er.rh;
	}*/

}
