package ch.epfl.biop.atlas.aligner.sourcepreprocessors;

public class SourcesProcessorHelper {

    public static SourcesProcessor Identity() {
        return new SourcesIdentity();
    }

    /**
     * Applies function from right to left; the most right one is the first to be executed:
     * out = f[0](f[1](f[2]( in )))
     * other notation :
     * out = f[0] o f[1] o f[2] (in)
     * @param fs list of functions for the channel processing
     * @return the concatenated functions
     */
    public static SourcesProcessor compose(SourcesProcessor... fs) {
        SourcesProcessor sp = fs[fs.length-1];
        int idx =  fs.length-1;
        while (idx>0) {
            idx--;
            SourcesProcessor f_next = fs[idx];
            SourcesProcessor f_current = sp;
            sp = new SourcesProcessComposer(f_next, f_current);
        }
        return sp;
    }

    /**
     * Gets rid of all channel selectors in the sources processor
     *
     * Used in registration to set the mask
     *
     * Used in editing to override the original channel being selected
     *
     * Limitation : does not work in a general manner...
     *
     * @param processor the source processor to modify
     * @return an identical processor which do not remove any channel
     */
    public static SourcesProcessor removeChannelsSelect(SourcesProcessor processor) {
        if (processor instanceof SourcesChannelsSelect) {
            return new SourcesIdentity();
        } else if (processor instanceof SourcesProcessComposer) {
            SourcesProcessComposer composer_in = (SourcesProcessComposer) processor;
            return new SourcesProcessComposer(
                    removeChannelsSelect(composer_in.f2),
                    removeChannelsSelect(composer_in.f1));
        } else return processor;
    }

}
