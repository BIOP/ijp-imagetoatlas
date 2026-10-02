package ch.epfl.biop.atlas.aligner;

/**
 * Builds the message shown to the user when an operation failed.
 */
public class ErrorMessage {

    private ErrorMessage() {}

    private static final int MAX_CAUSES = 5;

    /**
     * Appends what actually went wrong to a message meant for the user.
     * <p>
     * A failure surfaces as a generic exception ("Module threw error"), while its cause says what
     * happened, so a message built from the outermost exception alone sends the user looking in the
     * wrong place: an import that failed because a Java class could not initialize reads as a
     * problem with their image.
     *
     * @param message what was being done, in the words of the user
     * @param error the exception that ended it
     * @return the message, followed by the exception and its causes
     */
    public static String withCauses(String message, Throwable error) {
        StringBuilder text = new StringBuilder(message);
        text.append("\nCause:");
        Throwable cause = error;
        for (int depth = 0; (cause != null) && (depth < MAX_CAUSES); depth++) {
            text.append("\n ").append(cause.getClass().getName());
            if (cause.getMessage() != null) text.append(": ").append(cause.getMessage());
            cause = (cause.getCause() == cause) ? null : cause.getCause();
        }
        return text.toString();
    }
}
