package tt.uwi.analytics;

public class AnalyticsSummary {

    private int totalEvents;
    private int compilationAttempts;
    private int compilationFailures;
    private int programRuns;
    private int classesCreated;

    public AnalyticsSummary(int totalEvents, int compilationAttempts,
                            int compilationFailures, int programRuns,
                            int classesCreated) {

        this.totalEvents = totalEvents;
        this.compilationAttempts = compilationAttempts;
        this.compilationFailures = compilationFailures;
        this.programRuns = programRuns;
        this.classesCreated = classesCreated;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getCompilationAttempts() {
        return compilationAttempts;
    }

    public int getCompilationFailures() {
        return compilationFailures;
    }

    public int getProgramRuns() {
        return programRuns;
    }

    public int getClassesCreated() {
        return classesCreated;
    }

    public double getCompilationSuccessRate() {
        if (compilationAttempts == 0) {
            return 0.0;
        }

        int successes = compilationAttempts - compilationFailures;
        return (successes * 100.0) / compilationAttempts;
    }
}
