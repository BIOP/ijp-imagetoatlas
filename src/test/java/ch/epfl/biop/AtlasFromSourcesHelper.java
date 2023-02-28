package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasFromSourcesHelper {

    public static AtlasMap fromSources(SourceAndConverter<?>[] sources, SourceAndConverter<?> label, double atlasPixelSizeMm) {
        AtlasMap map = new AtlasMapFromSources(sources, label, atlasPixelSizeMm);
        return map;
    }

    public static Atlas makeAtlas(AtlasMap map, AtlasOntology ontology, String name) {
        return new Atlas() {
            @Override
            public AtlasMap getMap() {
                return map;
            }

            @Override
            public AtlasOntology getOntology() {
                return ontology;
            }

            @Override
            public void initialize(URL mapURL, URL ontologyURL) throws Exception {

            }

            @Override
            public List<String> getDOIs() {
                List<String> dois = new ArrayList<>();
                dois.add("no doi");
                return dois;
            }

            @Override
            public String getURL() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    public static AtlasOntology dummyOntology() {
        return new AtlasOntology() {
            @Override
            public String getName() {
                return "No Ontology";
            }

            @Override
            public void initialize() throws Exception {

            }

            @Override
            public void setDataSource(URL dataSource) {

            }

            @Override
            public URL getDataSource() {
                return null;
            }

            @Override
            public AtlasNode getRoot() {
                return new AtlasNode() {
                    @Override
                    public Integer getId() {
                        return 0;
                    }

                    @Override
                    public int[] getColor() {
                        return new int[]{255,0,0,128};
                    }

                    @Override
                    public Map<String, String> data() {
                        Map<String,String> data = new HashMap<>();
                        data.put("name", "root");
                        return data;
                    }

                    @Override
                    public AtlasNode parent() {
                        return null;
                    }

                    @Override
                    public List<? extends AtlasNode> children() {
                        return new ArrayList<>();
                    }
                };
            }

            @Override
            public AtlasNode getNodeFromId(int id) {
                if (id == 0) {
                    return getRoot();
                } else {
                    return null;
                }
            }

            @Override
            public String getNamingProperty() {
                return "name";
            }

            @Override
            public void setNamingProperty(String namingProperty) {

            }
        };
    }

    public static Atlas fromImagePlus(ImagePlus image, ImagePlus label, double atlasPrecisionMm) {
        AtlasOntology ontology = dummyOntology();

        AbstractSpimData<?> sd = ImagePlusToSpimData.getSpimData(image);

        SourceAndConverterServices.getSourceAndConverterService()
                .register(sd);

        List<SourceAndConverter<?>> structuralImages = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(sd);

        AbstractSpimData<?> sdLabel = ImagePlusToSpimData.getSpimData(image);

        SourceAndConverterServices.getSourceAndConverterService()
                .register(sdLabel);

        List<SourceAndConverter<?>> labelSource = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(sdLabel);


        AtlasMap map = fromSources(structuralImages.toArray(new SourceAndConverter[0]),
                    labelSource.get(0),atlasPrecisionMm
                );

        return makeAtlas(map, ontology, image.getTitle());
    }

    public static class AtlasMapFromSources implements AtlasMap {

        final Map<String, SourceAndConverter> keyToImage = new HashMap<>();
        final List<String> imageKeys = new ArrayList<>();
        final SourceAndConverter<?> labelImage;

        final double atlasPixelSizeInMillimeter;

        public AtlasMapFromSources(SourceAndConverter<?>[] sources,
                                   SourceAndConverter<?> label,
                                   double atlasPixelSizeInMillimeter) {
            for (SourceAndConverter<?> source:sources) {
                imageKeys.add(source.getSpimSource().getName());
                keyToImage.put(source.getSpimSource().getName(), source);
            }
            keyToImage.put("X", AtlasHelper.getCoordinateSac(0, "X"));
            keyToImage.put("Y", AtlasHelper.getCoordinateSac(1, "Y"));
            keyToImage.put("Z", AtlasHelper.getCoordinateSac(2, "Z"));
            keyToImage.put("Left Right", label);

            imageKeys.add("X");
            imageKeys.add("Y");
            imageKeys.add("Z");
            imageKeys.add("Left Right");

            labelImage = label;
            this.atlasPixelSizeInMillimeter = atlasPixelSizeInMillimeter;
        }

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
            return keyToImage;
        }

        @Override
        public List<String> getImagesKeys() {
            return imageKeys;
        }

        @Override
        public SourceAndConverter getLabelImage() {
            return labelImage;
        }

        @Override
        public Double getAtlasPrecisionInMillimeter() {
            return atlasPixelSizeInMillimeter;
        }

        @Override
        public AffineTransform3D getCoronalTransform() {
            return new AffineTransform3D();
        }

        @Override
        public Double getImageMax(String key) {
            return 65535.0;
        }

        @Override
        public int labelRight() {
            return -1;
        }

        @Override
        public int labelLeft() {
            return -1;
        }
    }



}
