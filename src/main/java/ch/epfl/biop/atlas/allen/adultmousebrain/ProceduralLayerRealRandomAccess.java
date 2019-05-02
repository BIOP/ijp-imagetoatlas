package ch.epfl.biop.atlas.allen.adultmousebrain;

import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class ProceduralLayerRealRandomAccess extends RealPoint
        implements RealRandomAccess< UnsignedByteType >
{
    final UnsignedByteType t;

    public ProceduralLayerRealRandomAccess()
    {
        super( 3 ); // number of dimensions is 2
        t = new UnsignedByteType();
    }

    public static final int mandelbrot( final double re0, final double im0,
                                        final int maxIterations )
    {
        double re = re0;
        double im = im0;
        int i = 0;
        for ( ; i < maxIterations; ++i )
        {
            final double squre = re * re;
            final double squim = im * im;
            if ( squre + squim > 4 )
                break;
            im = 2 * re * im + im0;
            re = squre - squim + re0;
        }
        return i;
    }

    @Override
    public UnsignedByteType get()
    {
        t.set( mandelbrot( position[ 0 ], position[ 1 ], 255 ) );
        return t;
    }

    @Override
    public ProceduralLayerRealRandomAccess copyRealRandomAccess()
    {
        return copy();
    }

    @Override
    public ProceduralLayerRealRandomAccess copy()
    {
        final ProceduralLayerRealRandomAccess a = new ProceduralLayerRealRandomAccess();
        a.setPosition( this );
        return a;
    }
}
