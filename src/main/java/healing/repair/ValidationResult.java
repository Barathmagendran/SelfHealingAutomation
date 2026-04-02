package healing.repair;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Result of sandbox compilation and validation of an AI-generated patch.
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
@Data
@Builder
public class ValidationResult {

    /** Whether the patched source compiles without errors */
    private final boolean compilationPassed;

    /** Whether all structural checks passed */
    private final boolean structureChecksPassed;

    /** Individual check results */
    private final List<CheckResult> checks;

    /** Compiler error output, if any */
    private final String compilerOutput;

    /** @return true if compilation succeeded and all structure checks passed */
    public boolean isAllPassed() {
        return compilationPassed && structureChecksPassed;
    }

    /** @return count of passed checks */
    public long getPassedCount() {
        if (checks == null) return 0;
        return checks.stream().filter(CheckResult::isPassed).count();
    }

    /** @return count of failed checks */
    public long getFailedCount() {
        if (checks == null) return 0;
        return checks.stream().filter(c -> !c.isPassed()).count();
    }

    @Data
    @Builder
    public static class CheckResult {
        private final String  name;
        private final boolean passed;
        private final String  detail;
    }
}
