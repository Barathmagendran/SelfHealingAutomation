package healing.repair;

import healing.ai.HealingException;
import healing.core.HealingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Locates and extracts the source code for a failing step definition method.
 *
 * <p>Searches the project's {@code src/main/java} tree to find the .java file
 * matching the step class, then extracts the specific method body.
 *
 * <p>The extracted source is embedded in the Claude AI prompt so it has
 * the exact code to diagnose and fix.
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class SourceCodeLocator {

    private static final Logger log = LogManager.getLogger(SourceCodeLocator.class);

    /** Source roots to search — relative to the working directory (project root). */
    private static final String[] SOURCE_ROOTS = {
        "src/main/java",
        "src/test/java"
    };

    private SourceCodeLocator() {}

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Extracts the source code of the method that failed.
     *
     * @param ctx the failure context containing class and method name
     * @return source code of the failing method, or a placeholder if not found
     */
    public static String getSource(HealingContext ctx) {
        try {
            Path sourceFile = resolveSourceFile(ctx.getStepClass());
            String fullSource = Files.readString(sourceFile);
            String methodSource = extractMethod(fullSource, ctx.getStepMethod());
            log.debug("[SourceCodeLocator] Extracted method [{}.{}]: {} chars",
                ctx.getStepClass(), ctx.getStepMethod(), methodSource.length());
            return methodSource;
        } catch (Exception e) {
            log.warn("[SourceCodeLocator] Could not extract source for [{}.{}]: {}",
                ctx.getStepClass(), ctx.getStepMethod(), e.getMessage());
            return "// Source not available: " + e.getMessage();
        }
    }

    /**
     * Resolves the absolute path to the .java source file for the given fully qualified class.
     *
     * @param fullyQualifiedClassName e.g. "stepdefinitions.web.LoginSteps"
     * @return absolute path to the .java file
     */
    public static Path resolveSourceFile(String fullyQualifiedClassName) {
        String relativePath = fullyQualifiedClassName.replace('.', '/') + ".java";

        for (String root : SOURCE_ROOTS) {
            Path candidate = Paths.get(root, relativePath);
            if (Files.exists(candidate)) {
                log.debug("[SourceCodeLocator] Found source: {}", candidate.toAbsolutePath());
                return candidate;
            }
        }

        // Fallback: deep search by simple class name
        String simpleName = fullyQualifiedClassName.substring(
            fullyQualifiedClassName.lastIndexOf('.') + 1) + ".java";

        for (String root : SOURCE_ROOTS) {
            Path rootPath = Paths.get(root);
            if (!Files.exists(rootPath)) continue;
            try (Stream<Path> walk = Files.walk(rootPath)) {
                Path found = walk
                    .filter(p -> p.getFileName().toString().equals(simpleName))
                    .findFirst()
                    .orElse(null);
                if (found != null) {
                    log.debug("[SourceCodeLocator] Found via deep search: {}", found.toAbsolutePath());
                    return found;
                }
            } catch (IOException e) {
                log.warn("[SourceCodeLocator] Error walking {}: {}", root, e.getMessage());
            }
        }

        throw new HealingException(
            "Source file not found for class: " + fullyQualifiedClassName +
            ". Searched: src/main/java and src/test/java");
    }

    // ─────────────────────────────────────────────────────────
    // Method extraction
    // ─────────────────────────────────────────────────────────

    /**
     * Extracts a method and its preceding annotations from a Java source file.
     *
     * <p>Finds the method declaration line then walks braces to capture the
     * complete method body including closing brace.
     *
     * @param fullSource  complete source file content
     * @param methodName  simple method name to find
     * @return method source including Cucumber annotation and body
     */
    static String extractMethod(String fullSource, String methodName) {
        String[] lines = fullSource.split("\n");

        int methodLineIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            // Match: "public void theUserClicksLoginButton(" or with parameters
            if (trimmed.contains("void " + methodName + "(") ||
                trimmed.contains("public " + methodName + "(") ||
                trimmed.contains("private " + methodName + "(")) {
                methodLineIdx = i;
                break;
            }
        }

        if (methodLineIdx == -1) {
            return "// Method '" + methodName + "' not found in source";
        }

        // Walk back to collect Cucumber annotations (@When, @Given, @Then, @And)
        int startIdx = methodLineIdx;
        for (int i = methodLineIdx - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("@") || trimmed.isEmpty()) {
                startIdx = i;
            } else {
                break;
            }
        }
        // Remove the blank line step-back
        while (startIdx < methodLineIdx && lines[startIdx].trim().isEmpty()) startIdx++;

        // Walk braces forward to find the closing brace
        int braceCount = 0;
        int endIdx = methodLineIdx;
        boolean started = false;

        for (int i = methodLineIdx; i < lines.length; i++) {
            for (char c : lines[i].toCharArray()) {
                if (c == '{') { braceCount++; started = true; }
                if (c == '}') { braceCount--; }
            }
            if (started && braceCount == 0) {
                endIdx = i;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i <= endIdx; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
