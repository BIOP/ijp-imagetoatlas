package ch.epfl.biop.atlas.allen;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import ch.epfl.biop.atlas.AtlasMap;
import ch.epfl.biop.scijava.command.ExportToImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.net.URL;
import java.util.List;
import java.util.function.BiConsumer;

public class AllenMap implements AtlasMap {

	public BdvHandle bdvh;

	List<SourceAndConverter> sacs;

	URL dataSource;

	public String name;

	// Original source order
	final static private int AraSetupId = 0;
	final static private int LabelBorberSetupId = 1;
	final static private int NisslSetupId = 2;
	final static private int LabelSetupId = 3;
	final static private int LeftRightSetupId = 4;

	// Re ordered channel
	final static public int AraChannel = 0;
	final static public int NisslChannel = 1;
	final static public int LabelBorderChannel = 2;
	final static public int LabelChannel = 3;
	final static public int LabelLeftRight = 4;

	@Override
	public void initialize(String atlasName) {
		this.name = atlasName;


		String address =  this.getDataSource().toString();
		// Hacky Mac HackFace
		if (address.startsWith("file:")) {
			address = address.substring(5);
		}

		SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(address);

		sacs = SourceAndConverterServices
			.getSourceAndConverterService()
			.getSourceAndConverterFromSpimdata(importer.get());

		BiConsumer<RealLocalizable, UnsignedShortType > leftRightIndicator = (l, t ) -> {
			if (l.getFloatPosition(0)>0) {
				t.set(255);
			} else {
				t.set(0);
			}
		};

		FunctionRealRandomAccessible leftRightSource = new FunctionRealRandomAccessible(3,
				leftRightIndicator,	UnsignedShortType::new);


		final Source< UnsignedShortType > s = new RealRandomAccessibleIntervalSource<>( leftRightSource,
				FinalInterval.createMinMax( 0, 0, 0, 1000, 1000, 0),
				new UnsignedShortType(), new AffineTransform3D(), "Left_Right" );

		SourceAndConverter leftRight = SourceAndConverterHelper.createSourceAndConverter(s);

		sacs.add(leftRight);

		SourceAndConverterServices.getSourceAndConverterService().register(leftRight);

		bdvh = SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.getNewBdv();

		((BdvHandleFrame)bdvh).getBigDataViewer().getViewerFrame().setVisible(false);

	}

	@Override
	public void setDataSource(URL dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public URL getDataSource() {
		return dataSource;
	}

	/*@Override
	public void show() {
		SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.show(bdvh, sacs.toArray(new SourceAndConverter[sacs.size()]));
		SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.remove(bdvh,sacs.get(LabelSetupId));

		AffineTransform3D at3D = new AffineTransform3D();
		at3D.translate(6.7, 5.0,0);
		at3D.scale(60);
		bdvh.getViewerPanel().setCurrentViewerTransform(at3D);
		bdvh.getViewerPanel().requestRepaint();
	}

	@Override
	public void hide() {
		SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.remove(bdvh, sacs.toArray(new SourceAndConverter[sacs.size()]));
	}*/

    public ImagePlus getSacChannelAsImagePlus(int channel) {

		ExportToImagePlusCommand export = new ExportToImagePlusCommand();

		export.level=0;
		export.timepointBegin=0;
		export.timepointEnd=0;
		export.sacs = new SourceAndConverter[1];
		export.sacs[0] = getSacChannel(channel);
		export.run();

		return export.imp_out;
	}

	public double getNormTransform(int axis, AffineTransform3D t) {
		double f0 = t.get(axis,0);
		double f1 = t.get(axis,1);
		double f2 = t.get(axis,2);
		return Math.sqrt(f0*f0+f1*f1+f2*f2);
	}

	public SourceAndConverter getSacChannel(int channel) {

		SourceAndConverter model = createModelSource();

		boolean interpolate = true;

		if (channel == LabelSetupId) {
			interpolate = false;
		}

		SourceResampler sampler = new SourceResampler(null, model, true,false, interpolate);

		SourceAndConverter sacIn3D = sampler.apply(sacs.get(channel));

		return sacIn3D;
	}

	private SourceAndConverter createModelSource() {
		// Origin is in fact the point 0,0,0 of the image
		// Get current big dataviewer transformation : source transform and viewer transform
		AffineTransform3D at3D = (AffineTransform3D) this.getCurrentLocation();

		//Center on the display center of the viewer ...
		double w = bdvh.getViewerPanel().getDisplay().getWidth();
		double h = bdvh.getViewerPanel().getDisplay().getHeight();
		// Center on the display center of the viewer ...
		at3D.translate(-w / 2, -h / 2, 0);
		// Getting an image independent of the view scaling unit (not sure)
		double xNorm = getNormTransform(0, at3D);//trans
		at3D.scale(1/xNorm);

		double samplingXYInPhysicalUnit = 0.01; // 10 microns per pixel

		double samplingZInPhysicalUnit = 0.01; // 10 microns but irrelevant - just used for calibration -> slice only

		int xSize = 15; // in millimeter
		int ySize = 15; // in millimeter
		int zSize = 0; // one slice

		at3D.scale(1./samplingXYInPhysicalUnit, 1./samplingXYInPhysicalUnit, 1./samplingZInPhysicalUnit);
		at3D.translate((xSize/(2*samplingXYInPhysicalUnit)), (ySize/(2*samplingXYInPhysicalUnit)), (zSize/(samplingZInPhysicalUnit)));

		long nPx = (long)(xSize / samplingXYInPhysicalUnit);
		long nPy = (long)(ySize / samplingXYInPhysicalUnit);
		long nPz;
		if (samplingZInPhysicalUnit==0) {
			nPz = 1;
		} else {
			nPz = 1+(long)(zSize / (samplingZInPhysicalUnit/2.0)); // TODO : check div by 2
		}

		// At least a pixel in all directions
		if (nPz == 0) nPz = 1;
		if (nPx == 0) nPx = 1;
		if (nPy == 0) nPy = 1;

		return new EmptySourceAndConverterCreator("Model_", at3D.inverse(), nPx, nPy, nPz).get();
	}


	ImagePlus imgNissl, imgAra, imgLabelBorder;

	@Override
	public ImagePlus getCurrentStructuralImageAsImagePlus() {

		Thread getNissl = new Thread(() -> {
			imgNissl = this.getSacChannelAsImagePlus(NisslSetupId);
			imgNissl.setTitle("Nissl");
		});

		Thread getAra = new Thread(() -> {
			imgAra = this.getSacChannelAsImagePlus(AraSetupId);
			imgAra.setTitle("Ara");
		});

		Thread getImgLabelBorder = new Thread(() -> {
			imgLabelBorder = this.getSacChannelAsImagePlus(LabelBorberSetupId);
			imgLabelBorder.setTitle("Label_Edge");
		});

		getNissl.start();
		getAra.start();
		getImgLabelBorder.start();

		try {
			getNissl.join();
			getAra.join();
			getImgLabelBorder.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		imgNissl.show();
		imgAra.show();
		imgLabelBorder.show();

		ImagePlus imp_out = new ImagePlus();
		IJ.run(imp_out, "Merge Channels...", "c1=Ara c2=Nissl c3=Label_Edge create keep");

		imp_out = IJ.getImage();

		imgLabelBorder.changes=false;
		imgLabelBorder.close();

		imgNissl.changes=false;
		imgNissl.close();

		imgAra.changes=false;
		imgAra.close();

		return imp_out;
	}

	@Override
	public ImagePlus getCurrentLabelImageAsImagePlus() {
		//ImagePlus imgLabel = this.getSacChannelAsImagePlus(LabelSetupId).duplicate(); // TODO : solve indexing confusion
		//imgLabel.setTitle("Label");
		//imgLabel.getProcessor().setMinAndMax(0, 65535);
		//imgLabel.show();
		return this.getSacChannelAsImagePlus(LabelSetupId); //imgLabel;
	}

	@Override
	public SourceAndConverter[] getCurrentStructuralImageSliceAsSacs() {
		SourceAndConverter[] out = new SourceAndConverter[5];
		out[AraChannel] = getSacChannel(AraSetupId);
		out[NisslChannel] = getSacChannel(NisslSetupId);
		out[LabelBorderChannel] = getSacChannel(LabelBorberSetupId);
		out[LabelChannel] = getSacChannel(LabelSetupId);
		out[LabelLeftRight] = getSacChannel(LeftRightSetupId);
		return out;
	}

	@Override
	public SourceAndConverter getCurrentLabelImageSliceAsSac() {
		return getSacChannel(LabelSetupId);
	}

	@Override
	public SourceAndConverter[] getStructuralImages() {
		SourceAndConverter[] out = new SourceAndConverter[4];
		out[AraChannel] = sacs.get(AraSetupId);
		out[NisslChannel] = sacs.get(NisslSetupId);
		out[LabelBorderChannel] = sacs.get(LabelBorberSetupId);
		out[LabelLeftRight-1] = sacs.get(LeftRightSetupId); // beurk
		return out;
	}

	@Override
	public SourceAndConverter getLabelImage() {
		return sacs.get(LabelSetupId);
	}

	//@Override
	public int getLabelImageSacIndex() {
		return LabelChannel;
	}

	//@Override
	public int getLeftRightImageSacIndex() {
		return LabelLeftRight;
	}

	//@Override
	public Object getCurrentLocation() {
        SynchronizedViewerState viewerState = bdvh.getViewerPanel().state();
        AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform
        // 1 - viewer transform
        viewerState.getViewerTransform( transformedSourceToViewer ); // Get current transformation by the viewer state and puts it into sourceToImgPlus
		return transformedSourceToViewer.copy();
	}

	/*
	@Override
	public void setCurrentLocation(Object location) {
		// TODO Auto-generated method stub

		AffineTransform3D at3D = (AffineTransform3D) location;
		bdvh.getViewerPanel().state().setViewerTransform(at3D);
	}*/

	@Override
	public String toString() {
		return name;
	}
}