package ch.epfl.biop.bdvslicer.ij2command;
import java.net.URL;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BDVServerUtils {

    public static Map<String,String> getDatasetList( final String remoteUrl ) throws IOException {

        Map< String, String > datasetUrlMap = new HashMap<>();

        // Get JSON string from the server
        final URL url = new URL( remoteUrl + "/json/" );
        final InputStream is = url.openStream();
        final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );

        reader.beginObject();
        while ( reader.hasNext() )
        {
            // skipping id
            reader.nextName();
            reader.beginObject();
            String id = null, description = null, thumbnailUrl = null, datasetUrl = null;
            while ( reader.hasNext() )
            {
                final String name = reader.nextName();
                if ( name.equals( "id" ) )
                    id = reader.nextString();
                else if ( name.equals( "description" ) )
                    description = reader.nextString();
                else if ( name.equals( "thumbnailUrl" ) )
                    thumbnailUrl = reader.nextString();
                else if ( name.equals( "datasetUrl" ) )
                    datasetUrl = reader.nextString();
                else
                    reader.skipValue();
            }
            if ( id != null )
            {
                datasetUrlMap.put( id, datasetUrl );
            }
            reader.endObject();
        }
        reader.endObject();
        reader.close();
        return datasetUrlMap;
    }
}
