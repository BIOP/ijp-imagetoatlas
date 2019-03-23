package ch.epfl.biop.atlas.allen;

import java.net.URL;

import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import bdv.viewer.ViewerOptions;
import bdv.viewer.state.ViewerState;
import ch.epfl.biop.atlas.AtlasMap;
import ch.epfl.biop.bdvslicer.ij2command.BDVSliceToImgPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;

public class AllenMap implements AtlasMap {

	public BigDataViewer bdv;
	URL dataSource;
	public int StructureChannel = 0;
	public int LabelChannel = 1;
	
	@Override
	public void initialize(String atlasName) {
		try {
			String address =  this.getDataSource().toString();
			// Hacky Mac HackFace
			if (address.startsWith("file:")) {
				address = address.substring(5, address.length());
			}
			bdv = BigDataViewer.open( address, atlasName, new ProgressWriterIJ(), ViewerOptions.options() );
		} catch (SpimDataException e) {
			e.printStackTrace();
		}
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
		bdv.getViewerFrame().setVisible(true);
	}

	@Override
	public void hide() {
		bdv.getViewerFrame().setVisible(false);
	}

    @Override
    public void setStructureImageChannel(int channel_index) {
        StructureChannel = channel_index;
    }

    public ImagePlus getImagePlusChannel(int channel) {
		BDVSliceToImgPlus bs = new BDVSliceToImgPlus();
		// Feeds argument
		bs.bdv=this.bdv;
		bs.mipmapLevel = 0;
		bs.xSize=300;
		bs.ySize=300;
		bs.zSize=0;
		bs.samplingInXVoxelUnit=0.05;//1.0;
		bs.interpolate=false;
		bs.sourceIndex = channel;
		bs.run();
		return bs.imp;
	}

	final static public int AvgChannel = 0;
	final static public int NisslChannel = 1;

	@Override
	public ImagePlus getCurrentStructuralImage() {
		ImagePlus imgNissl = this.getImagePlusChannel(NisslChannel).duplicate(); // Virtual images are causing problems
		imgNissl.setTitle("Nissl");
		imgNissl.getProcessor().setMinAndMax(0, 25000);


		ImagePlus imgAvg = this.getImagePlusChannel(AvgChannel).duplicate();
		imgAvg.setTitle("Avg");
		imgAvg.getProcessor().setMinAndMax(0, 255);


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
		imgAvg.show();

		ImagePlus imp_out = new ImagePlus();
		IJ.run(imp_out, "Merge Channels...", "c1=Avg c2=Nissl c3=Label_Edge create keep");

		imp_out = IJ.getImage();

		imgLabel.changes=false;
		imgLabel.close();

		imgNissl.changes=false;
		imgNissl.close();

		imgAvg.changes=false;
		imgAvg.close();

		imgTemp.changes=false;
		imgTemp.close();

		return imp_out;
	}


	@Override
	public ImagePlus getCurrentLabelImage() {
		ImagePlus imgLabel = this.getImagePlusChannel(LabelChannel).duplicate();
		imgLabel.setTitle("Label");
		//imgLabel.show();
		return imgLabel;
	}

	@Override
	public Object getCurrentLocation() {
        ViewerState viewerState = bdv.getViewer().getState();
        AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform
        // 1 - viewer transform
        viewerState.getViewerTransform( transformedSourceToViewer ); // Get current transformation by the viewer state and puts it into sourceToImgPlus
		System.out.println("getCurrentLocation");

		System.out.println(transformedSourceToViewer.toString());
		return transformedSourceToViewer.copy();
	}

	@Override
	public void setCurrentLocation(Object location) {
		// TODO Auto-generated method stub

		AffineTransform3D at3D = (AffineTransform3D) location;
		System.out.println("setCurrentLocation");
		System.out.println(at3D.toString());
		//ViewerState viewerState = bdv.getViewer().getState();
		//viewerState.
		bdv.getViewer().transformChanged(at3D);
		/*viewerState.setViewerTransform(at3D);
		bdv.getViewer()
        bdv.toggleManualTransformation();
        bdv.getManualTransformEditor().transformChanged(at3D);
		bdv.toggleManualTransformation();*/
	}

}