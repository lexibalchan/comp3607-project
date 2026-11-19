package tt.uwi.analytics;

/**
 * Stores aggregate analytics for a telemetry session.
 * This class summarizes event volume, compilation activity, program runs,
 * and class creation activity.
 */

public class AnalyticsSummary {

    /** The total number of events in the telemetry session. */
    private int totalEvents;

    /** The number of compilation events, including successes and failures. */
    private int compilationAttempts;

    /** The number of compilation failures. */
    private int compilationFailures;

    /** The number of program executions. */
    private int programRuns;

    /** The number of classes created during the session. */
    private int classesCreated;

    /**
     * Constructs an analytics summary from session activity counts.
     *
     * @param totalEvents the total number of telemetry events
     * @param compilationAttempts the total number of compilation attempts
     * @param compilationFailures the number of failed compilations
     * @param programRuns the number of program executions
     * @param classesCreated the number of classes created
     */

    public AnalyticsSummary(int totalEvents, int compilationAttempts,
                            int compilationFailures, int programRuns,
                            int classesCreated) {

        this.totalEvents = totalEvents;
        this.compilationAttempts = compilationAttempts;
        this.compilationFailures = compilationFailures;
        this.programRuns = programRuns;
        this.classesCreated = classesCreated;
    }

    /**
     * Returns the total number of telemetry events.
     *
     * @return the total event count
     */
    public int getTotalEvents() {
        return totalEvents;
    }

    /**
     * Returns the number of compilation attempts.
     *
     * @return the compilation attempt count
     */
    public int getCompilationAttempts() {
        return compilationAttempts;
    }

    /**
     * Returns the number of compilation failures.
     *
     * @return the compilation failure count
     */
    public int getCompilationFailures() {
        return compilationFailures;
    }

    /**
     * Returns the number of program executions.
     *
     * @return the program run count
     */
    public int getProgramRuns() {
        return programRuns;
    }

    /**
     * Returns the number of classes created.
     *
     * @return the classes-created count
     */
    public int getClassesCreated() {
        return classesCreated;
    }

    /**
     * Calculates the compilation success rate as a percentage.
     *
     * @return the successful compilation percentage, or {@code 0.0} when
     *         there have been no compilation attempts
     */
    public double getCompilationSuccessRate() {
        if (compilationAttempts == 0) {
            return 0.0;
        }

        int successes = compilationAttempts - compilationFailures;
        return (successes * 100.0) / compilationAttempts;
    }
}
