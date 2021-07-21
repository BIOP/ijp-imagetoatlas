package ch.epfl.biop.atlas.aligner;

import bdv.util.*;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.aligner.commands.*;
import ch.epfl.biop.atlas.aligner.serializers.*;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.*;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationPluginHelper;
import ch.epfl.biop.bdv.gui.GraphicalHandle;
import ch.epfl.biop.bdv.gui.GraphicalHandleListener;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.registration.Registration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.cache.CacheService;
import org.scijava.command.Command;
import org.scijava.convert.ConvertService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.*;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.behaviour.EditorBehaviourUnInstaller;
import sc.fiji.bdvpg.scijava.BdvScijavaHelper;
import sc.fiji.bdvpg.scijava.ScijavaSwingUI;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.persist.RuntimeTypeAdapterFactory;
import sc.fiji.persist.ScijavaGsonHelper;

import javax.swing.*;
import java.awt.Point;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.*;
import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * All specific methods and fields dedicated to the multislice positioner
 *
 * There is:
 *
 * - a positioning mode
 *      This is mosly useful at the beginning of the registration
 *      Slices can be moved along the axis / stretched and shrunk
 *      Only certain sections of the atlas are shown to improve global overview, based on the user need
 *
 * - a review mode
 *      This is mostly useful for reviewing the quality of registration
 *      Only one slice is visible at a time
 *      The atlas is fully displayed
 */

public class MultiSlicePositionerNoGUI extends MultiSlicePositioner {
    /**
     * Starts ABBA
     *
     * @param biopAtlas     an atlas
     * @param reslicedAtlas a resliced atlas
     * @param ctx           a scijava context
     */
    public MultiSlicePositionerNoGUI(BiopAtlas biopAtlas, ReslicedAtlas reslicedAtlas, Context ctx) {
        super(null, biopAtlas, reslicedAtlas, ctx);
        this.log = (message) -> {
            logger.info("Multipositioner : "+message);
        };
    }

    public void updateDisplay() {}

    public void recenterBdvh() {}

    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {}

    protected void draw(Graphics2D g) {}

}