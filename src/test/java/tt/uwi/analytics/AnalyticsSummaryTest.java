package tt.uwi.analytics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalyticsSummaryTest {
    
    @Test 
    void calculateSuccessRateForMixedResults() {
        AnalyticsSummary summary = new AnalyticsSummary(5, 2, 1, 1, 1);

        assertEquals(50.0, summary.getCompilationSuccessRate(), 0.001);
    }

    @Test 
    void returnsZeroWhenThereAreNoCompilationAttempts() {
        AnalyticsSummary summary = new AnalyticsSummary(0, 0, 0, 0, 0);

        assertEquals(0.0, summary.getCompilationSuccessRate(), 0.001);
    }

    @Test 
    void returnsOneHundredWhenAllCompilationsSucced() {
        AnalyticsSummary summary = new AnalyticsSummary(3, 3, 0, 0, 0);

        assertEquals(100.0, summary.getCompilationSuccessRate(), 0.001);
    }

    @Test 
    void returnsZeroWhenAllCompilationsFail() {
        AnalyticsSummary summary = new AnalyticsSummary(3, 3, 3, 0, 0);

        assertEquals(0.0, summary.getCompilationSuccessRate(), 0.001);
    }
}
