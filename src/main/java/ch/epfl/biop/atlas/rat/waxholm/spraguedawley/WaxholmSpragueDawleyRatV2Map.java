package ch.epfl.biop.atlas.rat.waxholm.spraguedawley;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.AtlasMap;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class WaxholmSpragueDawleyRatV2Map implements AtlasMap {
    @Override
    public void setDataSource(URL dataSource) {

    }

    @Override
    public void initialize(String atlasName) {

    }

    @Override
    public URL getDataSource() {
        return null;
    }

    @Override
    public Map<String, SourceAndConverter> getStructuralImages() {
        return null;
    }

    @Override
    public List<String> getImagesKeys() {
        return null;
    }

    @Override
    public SourceAndConverter getLabelImage() {
        return null;
    }

    @Override
    public double getAtlasPrecisionInMillimeter() {
        return 0;
    }
}
