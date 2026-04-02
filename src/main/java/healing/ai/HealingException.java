package healing.ai;

/**
 * Typed exception for all self-healing engine failures.
 *
 * <p>Thrown when any phase of the healing cycle cannot complete:
 * API call failure, compilation error, source file not found, etc.
 *
 * <p>Caught by {@link healing.core.HealingEngine} which logs and escalates
 * rather than propagating the exception to the Cucumber runner.
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class HealingException extends RuntimeException {

    public HealingException(String message) {
        super(message);
    }

    public HealingException(String message, Throwable cause) {
        super(message, cause);
    }
}
