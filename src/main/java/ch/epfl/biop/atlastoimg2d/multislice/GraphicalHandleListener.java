package ch.epfl.biop.atlastoimg2d.multislice;

interface GraphicalHandleListener {

        void hover_in(GraphicalHandle gh);

        void hover_out(GraphicalHandle gh);

        //void clicked(GraphicalHandle gh);

        void created(GraphicalHandle gh);

        void removed(GraphicalHandle gh);

}