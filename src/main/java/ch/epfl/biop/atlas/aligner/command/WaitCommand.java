package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Inspect>ABBA - Wait",
        description = "Waits until the actions queued on the slices are done, at most 'timeout_s' seconds, then tells how many "
                + "are still pending and what failed since the previous 'ABBA - Wait'. Registration and DeepSlice commands return "
                + "as soon as their actions are queued, and their failures come later: follow them with this command, "
                + "and call it again while 'pending_actions' is above 0.")
public class WaitCommand implements Command {

    /** Index of the last failure reported, per session */
    private static final Map<MultiSlicePositioner, Integer> lastFailureReported = Collections.synchronizedMap(new WeakHashMap<>());

    /** Most failures listed, the most recent ones */
    static final int MAX_FAILURES_LISTED = 20;

    @Parameter(label = "ABBA session", description = "The ABBA session to wait for.")
    MultiSlicePositioner mp;

    @Parameter(label = "Timeout (s)", min = "0",
            description = "Longest wait, in seconds. Clients usually give up on a call after about 60 s: keep it under 45, "
                    + "and call again while actions are pending.")
    double timeout_s = 30;

    @Parameter(type = ItemIO.OUTPUT, label = "Pending actions",
            description = "Number of actions still queued or running, 0 once all are done.")
    int pending_actions;

    @Parameter(type = ItemIO.OUTPUT, label = "Failures",
            description = "The failures recorded since the previous 'ABBA - Wait' on this session, one per line "
                    + "('#index hh:mm:ss action on slices: reason'), or empty.")
    String failures;

    @Parameter(type = ItemIO.OUTPUT, label = "Summary",
            description = "One line, for instance 'done: no pending action, no new failure' or '3 actions pending after 30 s; 1 new failure'.")
    String summary;

    @Override
    public void run() {
        pending_actions = mp.waitForTasks((long) (timeout_s * 1000));
        List<MultiSlicePositioner.Failure> newFailures = mp.getFailures(lastFailureReported.getOrDefault(mp, 0));
        if (!newFailures.isEmpty()) lastFailureReported.put(mp, newFailures.get(newFailures.size() - 1).index);
        int skipped = Math.max(0, newFailures.size() - MAX_FAILURES_LISTED);
        failures = (skipped > 0 ? skipped + " earlier failures not listed\n" : "")
                + newFailures.stream().skip(skipped).map(Object::toString).collect(Collectors.joining("\n"));
        String failed = newFailures.isEmpty() ? "no new failure"
                : newFailures.size() + " new failure" + (newFailures.size() > 1 ? "s" : "");
        summary = pending_actions == 0 ? "done: no pending action, " + failed
                : String.format(Locale.ROOT, "%d action%s pending after %.0f s; %s", pending_actions,
                        pending_actions > 1 ? "s" : "", timeout_s, failed);
    }
}
