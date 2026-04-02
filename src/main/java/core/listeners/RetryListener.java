package core.listeners;

import core.retry.RetryAnalyzer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * TestNG Annotation Transformer that globally applies {@link RetryAnalyzer}
 * to every {@code @Test} method — no per-test annotation needed.
 *
 * <p><b>TestNG 7.6+ API notes:</b>
 * <ul>
 *   <li>{@code getRetryAnalyzer()} was removed — use {@code getRetryAnalyzerClass()} instead.</li>
 *   <li>{@code setRetryAnalyzer(Class)} now requires a generic bound:
 *       {@code Class<? extends IRetryAnalyzer>}.</li>
 *   <li>The {@code transform()} signature uses {@code Class<?>} not raw {@code Class}.</li>
 * </ul>
 *
 * <p>Registered in {@code testng.xml}:
 * <pre>
 *   &lt;listeners&gt;
 *     &lt;listener class-name="core.listeners.RetryListener"/&gt;
 *   &lt;/listeners&gt;
 * </pre>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class RetryListener implements IAnnotationTransformer {

    private static final Logger log = LogManager.getLogger(RetryListener.class);

    /**
     * Called by TestNG for every {@code @Test} annotation before execution.
     *
     * <p>Uses {@code getRetryAnalyzerClass()} (TestNG 7.6+ API) instead of the
     * removed {@code getRetryAnalyzer()} to check whether a retry analyzer is
     * already configured, then injects {@link RetryAnalyzer} if not.
     *
     * @param annotation      the @Test annotation being transformed
     * @param testClass       the test class (may be null for method-level tests)
     * @param testConstructor the test constructor (may be null)
     * @param testMethod      the test method (may be null for class-level tests)
     */
//    @Override
//    public void transform(ITestAnnotation annotation,
//                          Class<?> testClass,
//                          Constructor<?> testConstructor,
//                          Method testMethod) {
//
//        // getRetryAnalyzerClass() replaces removed getRetryAnalyzer() in TestNG 7.6+
//        Class<? extends IRetryAnalyzer> existing = annotation.getRetryAnalyzerClass();
//
//        if (existing == null || existing.equals(IRetryAnalyzer.class)) {
//            // Cast is safe — RetryAnalyzer implements IRetryAnalyzer
//            @SuppressWarnings("unchecked")
//            Class<? extends IRetryAnalyzer> analyzerClass =
//                (Class<? extends IRetryAnalyzer>) RetryAnalyzer.class;
//
//            annotation.setRetryAnalyzer(analyzerClass);
//
//            log.trace("RetryAnalyzer injected into: [{}]",
//                testMethod != null ? testMethod.getName() : "<class-level>");
//        }
//    }
}
