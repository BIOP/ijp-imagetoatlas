package ch.epfl.biop.atlas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasOntologyHelper {

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
            p = (AtlasNode) origin.parent();
        }
        return listOfParentLabels;
    }
}
