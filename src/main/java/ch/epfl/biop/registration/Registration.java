package ch.epfl.biop.registration;

import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import java.util.function.Function;

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);

    void register();

    Function<T,T> getImageRegistration();
    RealPointList getPtsRegistration(RealPointList pts);

}
