package tt.uwi.analytics;

/**
 * Immutable summary of editor activity within a telemetry session.
 *
 * @param editEvents the number of editor activity events in the session
 * @param estimatedActiveSeconds the estimated amount of active time, in seconds
 * @param estimatedIdleSeconds the estimated amount of idle time, in seconds
 */

public record ActivitySummary(
        long editEvents, 
        double estimatedActiveSeconds,
        double estimatedIdleSeconds ) {
        
}
