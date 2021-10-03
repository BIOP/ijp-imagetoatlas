package ch.epfl.biop.atlas;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Static atlas ontology helper functions
 */
public class BiopAtlasHelper {

    public static Map<Integer, AtlasNode> buildLabelToAtlasNodeMap(AtlasNode rootNode) {
        Map<Integer, AtlasNode> result = new HashMap<>();
        return appendToLabelToAtlasNodeMap(result, rootNode);
    }

    private static Map<Integer, AtlasNode> appendToLabelToAtlasNodeMap(Map<Integer, AtlasNode> map, AtlasNode node) {
        map.put(node.getLabelValue(), node);
        node.children().forEach(child -> appendToLabelToAtlasNodeMap(map, (AtlasNode) child));
        return map;
    }

    public static List<Integer> getAllParentLabels(AtlasOntology ontology, int label) {
        AtlasNode origin = ontology.getNodeFromLabelMap(label);
        ArrayList listOfParentLabels = new ArrayList();
        if (origin == null) {
            return listOfParentLabels;
        }
        AtlasNode p = (AtlasNode) origin.parent();
        while (p!=null) {
            listOfParentLabels.add(p.getLabelValue());
            p = (AtlasNode) p.parent();
        }
        return listOfParentLabels;
    }

    public static Map<Integer, AtlasNode> buildIdToAtlasNodeMap(AtlasNode root) {
        Map<Integer, AtlasNode> result = new HashMap<>();
        return appendToIdToAtlasNodeMap(result, root);
    }

    private static Map<Integer, AtlasNode> appendToIdToAtlasNodeMap(Map<Integer, AtlasNode> map, AtlasNode node) {
        map.put(node.getId(), node);
        node.children().forEach(child -> appendToIdToAtlasNodeMap(map, (AtlasNode) child));
        return map;
    }

    public static SourceAndConverter<FloatType> getCoordinateSac(final int axis, String name) {
        BiConsumer<RealLocalizable, FloatType > coordIndicator = (l, t ) -> {
            t.set(l.getFloatPosition(axis));
        };

        FunctionRealRandomAccessible coordSource = new FunctionRealRandomAccessible(3,
                coordIndicator,	FloatType::new);

        final Source<UnsignedShortType> s = new RealRandomAccessibleIntervalSource<>( coordSource,
                FinalInterval.createMinMax( 0, 0, 0, 1320, 800, 1140),
                new FloatType(), new AffineTransform3D(), name );

        return SourceAndConverterHelper.createSourceAndConverter(s);
    }
}
