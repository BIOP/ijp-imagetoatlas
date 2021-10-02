package ch.epfl.biop.atlas;

import org.scijava.util.TreeNode;

import java.util.LinkedHashMap;
import java.util.Map;

public interface AtlasNode extends TreeNode<Map<String, String>> {
    int getId();
    int getLabelValue();
}
