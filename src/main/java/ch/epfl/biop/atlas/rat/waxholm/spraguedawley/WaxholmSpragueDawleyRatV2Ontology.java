package ch.epfl.biop.atlas.rat.waxholm.spraguedawley;

import ch.epfl.biop.atlas.AtlasNode;
import ch.epfl.biop.atlas.AtlasOntology;
import org.scijava.util.TreeNode;

import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class WaxholmSpragueDawleyRatV2Ontology implements AtlasOntology {

    URL dataSource;

    AtlasNode root;

    @Override
    public void initialize() throws Exception {

        Map<String, String> props = new HashMap<>();
        props.put("name", "root");

        List<TreeNode<?>> children = new ArrayList<>();

        root = new AtlasNode() {
            @Override
            public Integer getId() {
                return 1;
            }

            @Override
            public Integer getLabelValue() {
                return 1;
            }

            @Override
            public Map<String, String> data() {
                return props;
            }

            @Override
            public TreeNode<?> parent() {
                return null;
            }

            @Override
            public void setParent(TreeNode<?> treeNode) {

            }

            @Override
            public List<TreeNode<?>> children() {
                return children;
            }
        };
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
    public AtlasNode getRoot() {
        return root;
    }

    @Override
    public Color getColor(AtlasNode node) {
        return new Color(255,128, 50);
    }

    @Override
    public AtlasNode getNodeFromLabelMap(int mapValue) {
        if (mapValue == 1) return root;
        return null;
    }

    @Override
    public AtlasNode getNodeFromId(int id) {
        if (id==1) return root;
        return null;
    }
}
