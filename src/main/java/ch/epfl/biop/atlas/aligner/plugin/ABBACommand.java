package ch.epfl.biop.atlas.aligner.plugin;

import org.scijava.command.Command;

/**
 * Empty interface for automatic discovery of extra ABBA commands
 *
 * It should necessarily have an input parameter of class {@link ch.epfl.biop.atlas.aligner.MultiSlicePositioner}
 * named "mp"
 *
 */
public interface ABBACommand extends Command {
}
