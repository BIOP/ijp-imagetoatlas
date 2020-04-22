package ch.epfl.biop.atlas.allen;

import java.net.URL;
import java.util.List;
import bdv.util.*;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import ch.epfl.biop.atlas.AtlasMap;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

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

	// Re ordered channel
	final static public int AraChannel = 0;
	final static public int NisslChannel = 1;
	final static public int LabelBorderChannel = 2;
	final static public int LabelChannel = 3;
	
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

		bdvh = SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.getNewBdv();

		// Following line should be done with show
		/*SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.show(bdvh, sacs.toArray(new SourceAndConverter[sacs.size()]));*/

	}

	@Override
	public void setDataSource(URL dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public URL getDataSource() {
		return dataSource;
	}

	@Override
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
	}

    public ImagePlus getImagePlusChannel(int channel) {
		/*FastBDVSliceToImgPlus bs = new FastBDVSliceToImgPlus();
		// Feeds argument
		bs.bdvh=this.bdvh;
		bs.mipmapLevel = 0;
		bs.xSize=300;
		bs.ySize=300;
		bs.zSize=0;
		bs.samplingInXVoxelUnit=0.05e3;//1.0;
		bs.interpolate=false;
		bs.sourceIndex = channel;
		bs.run();
		return bs.imp;*/
		return null;
	}

	public double getNormTransform(int axis, AffineTransform3D t) {
		double f0 = t.get(axis,0);
		double f1 = t.get(axis,1);
		double f2 = t.get(axis,2);
		return Math.sqrt(f0*f0+f1*f1+f2*f2);
	}

	public SourceAndConverter getSacChannel(int channel) {

		SourceAndConverter model = createModelSource();

		SourceResampler sampler = new SourceResampler(null, model, true, true);

		SourceAndConverter sacIn3D = sampler.apply(sacs.get(channel));

		// Now restricted to a 2D registration problem

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

		double samplingXYInPhysicalUnit = 0.02; // 20 microns per pixel

		double samplingZInPhysicalUnit = 0.01; // 10 microns but irrelevant -> slice only

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

		// Dummy ImageFactory
		final int[] cellDimensions = new int[] { 32, 32, 32 };

		// Cached Image Factory Options
		final DiskCachedCellImgOptions factoryOptions = options()
				.cellDimensions( cellDimensions )
				.cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
				.maxCacheSize( 1 );

		// Creates cached image factory of Type UnsignedShort
		final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>( new UnsignedShortType(), factoryOptions );

		// At least a pixel in all directions
		if (nPz == 0) nPz = 1;
		if (nPx == 0) nPx = 1;
		if (nPy == 0) nPy = 1;

		return new EmptySourceAndConverterCreator("Model_", at3D.inverse(), nPx, nPy, nPz, factory).get();
	}

	@Override
	public ImagePlus getCurrentStructuralImageAsImagePlus() {
		ImagePlus imgNissl = this.getImagePlusChannel(NisslChannel).duplicate(); // Virtual images are causing problems
		imgNissl.setTitle("Nissl");
		imgNissl.getProcessor().setMinAndMax(0, 25000);


		ImagePlus imgAra = this.getImagePlusChannel(AraChannel).duplicate();
		imgAra.setTitle("Ara");
		imgAra.getProcessor().setMinAndMax(0, 255);


		ImagePlus imgLabel = this.getImagePlusChannel(LabelChannel).duplicate();
		imgLabel.setTitle("Label_Edge");
		imgLabel.getProcessor().setMinAndMax(0, 25000);

		imgLabel.show();

		IJ.run(imgLabel, "Find Edges", "");
		ImagePlus imgTemp = imgLabel;
		imgLabel = IJ.getImage();

		IJ.setRawThreshold(imgLabel, 1, 65535, null);

		Prefs.blackBackground = true;
		IJ.run(imgLabel,"Make Binary", "thresholded remaining black");
		IJ.run(imgLabel, "16-bit", "");
		IJ.run(imgLabel, "Smooth", "");

		imgNissl.show();
		imgAra.show();

		ImagePlus imp_out = new ImagePlus();
		IJ.run(imp_out, "Merge Channels...", "c1=Ara c2=Nissl c3=Label_Edge create keep");

		imp_out = IJ.getImage();

		imgLabel.changes=false;
		imgLabel.close();

		imgNissl.changes=false;
		imgNissl.close();

		imgAra.changes=false;
		imgAra.close();

		imgTemp.changes=false;
		imgTemp.close();

		return imp_out;
	}

	@Override
	public ImagePlus getCurrentLabelImageAsImagePlus() {
		ImagePlus imgLabel = this.getImagePlusChannel(LabelSetupId).duplicate(); // TODO : solve indexing confusion
		imgLabel.setTitle("Label");
		imgLabel.getProcessor().setMinAndMax(0, 65535);
		//imgLabel.show();
		return imgLabel;
	}

	@Override
	public SourceAndConverter[] getCurrentStructuralImageAsSacs() {
		SourceAndConverter[] out = new SourceAndConverter[4];
		out[AraChannel] = getSacChannel(AraSetupId);
		out[NisslChannel] = getSacChannel(NisslSetupId);
		out[LabelBorderChannel] = getSacChannel(LabelBorberSetupId);
		out[LabelChannel] = getSacChannel(LabelSetupId);
		return out;
	}

	@Override
	public SourceAndConverter[] getCurrentLabelImageAsSacs() {
		return new SourceAndConverter[0];
	}

	@Override
	public Object getCurrentLocation() {
        SynchronizedViewerState viewerState = bdvh.getViewerPanel().state();
        AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform
        // 1 - viewer transform
        viewerState.getViewerTransform( transformedSourceToViewer ); // Get current transformation by the viewer state and puts it into sourceToImgPlus
		return transformedSourceToViewer.copy();
	}

	@Override
	public void setCurrentLocation(Object location) {
		// TODO Auto-generated method stub

		AffineTransform3D at3D = (AffineTransform3D) location;
		bdvh.getViewerPanel().transformChanged(at3D);
	}

	@Override
	public String toString() {
		return name;
	}
}