package tt.uwi.analytics;

public record ActivitySummary(
        long editEvents, 
        double estimatedActiveSeconds,
        double estimatedIdleSeconds ) {
        
}
