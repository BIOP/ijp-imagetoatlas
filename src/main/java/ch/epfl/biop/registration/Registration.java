package ch.epfl.biop.registration;

import net.imglib2.Point;

import java.util.List;
import java.util.function.Function;

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);

    void register();

    Function<T,T> getImageRegistration();
    Function<List<Point>, List<Point>> getPtsRegistration();

}
