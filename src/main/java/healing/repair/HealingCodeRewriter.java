package healing.repair;

import healing.ai.DiagnosisResult;
import healing.ai.HealingException;
import healing.core.HealingConfig;
import healing.core.HealingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Applies an AI-generated patch to a step definition source file.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Locate the .java file using {@link SourceCodeLocator}</li>
 *   <li>Create a timestamped backup of the original</li>
 *   <li>Validate the patch size (reject patches that are too large)</li>
 *   <li>Replace the broken method with the AI-generated fix</li>
 *   <li>Write the patched source back to disk</li>
 * </ol>
 *
 * <p>Uses a simple but reliable string-replacement strategy: find the original
 * method block (located via the same extraction logic as {@link SourceCodeLocator})
 * and replace it wholesale. This avoids the need for a full AST parser while
 * being safe — it only replaces a uniquely identifiable method block.
 *
 * @author Enterprise QA Team — Self-Healing Engine
 * @version 1.0
 */
public class HealingCodeRewriter {

    private static final Logger log = LogManager.getLogger(HealingCodeRewriter.class);

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Applies the AI-generated patch to the step definition source file.
     *
     * @param ctx      the failure context (identifies the file and method)
     * @param diagnosis the AI diagnosis containing the fixed code
     * @throws HealingException if the patch cannot be applied
     */
    public void applyPatch(HealingContext ctx, DiagnosisResult diagnosis) {
        String fixedCode = diagnosis.getFixedCode();

        if (fixedCode == null || fixedCode.isBlank()) {
            throw new HealingException("AI returned empty fixedCode — cannot patch.");
        }

        // Validate patch size
        long patchLines = fixedCode.lines().count();
        if (patchLines > HealingConfig.getMaxPatchLines()) {
            throw new HealingException(
                "Patch rejected: " + patchLines + " lines exceeds max allowed " +
                HealingConfig.getMaxPatchLines() + ". Escalating to human.");
        }

        Path sourceFile = SourceCodeLocator.resolveSourceFile(ctx.getStepClass());
        log.info("[CodeRewriter] Applying patch to: {}", sourceFile);

        try {
            String originalSource = Files.readString(sourceFile);

            // Back up original before any modification
            backup(sourceFile, originalSource);

            // Extract original method block from source
            String originalMethod = SourceCodeLocator.extractMethod(
                originalSource, ctx.getStepMethod());

            if (originalMethod.startsWith("// Method") || originalMethod.startsWith("// Source")) {
                throw new HealingException(
                    "Cannot locate method '" + ctx.getStepMethod() + "' in " + sourceFile +
                    " — cannot apply patch safely.");
            }

            // Validate that the replacement method keeps the same Cucumber annotation
            validateAnnotationPreserved(originalMethod, fixedCode);

            // Replace original method with fixed code
            if (!originalSource.contains(originalMethod)) {
                throw new HealingException(
                    "Original method block not found verbatim in source file — " +
                    "source may have been modified. Aborting patch.");
            }

            String patchedSource = originalSource.replace(originalMethod, fixedCode.stripTrailing());
            Files.writeString(sourceFile, patchedSource);

            log.info("[CodeRewriter] ✅ Patch applied successfully to: {}", sourceFile);
            log.info("[CodeRewriter] Method: {} ({} lines rewritten)",
                ctx.getStepMethod(), patchLines);

        } catch (IOException e) {
            throw new HealingException(
                "I/O error writing patch to " + sourceFile + ": " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Backup
    // ─────────────────────────────────────────────────────────

    /**
     * Creates a timestamped backup of the source file before patching.
     * Backup is placed alongside the original with a {@code .bak_<timestamp>} suffix.
     */
    private void backup(Path sourceFile, String originalContent) throws IOException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        Path backupPath = sourceFile.resolveSibling(
            sourceFile.getFileName().toString() + ".bak_" + timestamp);
        Files.writeString(backupPath, originalContent);
        log.debug("[CodeRewriter] Backup created: {}", backupPath);
    }

    // ─────────────────────────────────────────────────────────
    // Safety validation
    // ─────────────────────────────────────────────────────────

    /**
     * Verifies that the AI has preserved the Cucumber step annotation.
     *
     * <p>Rejects any patch where the AI changed the annotation pattern
     * (e.g., changed @When to @Given, or modified the step expression).
     * This prevents the healed step from de-registering from Cucumber.
     */
    private void validateAnnotationPreserved(String originalMethod, String fixedCode) {
        String[] cucumberAnnotations = { "@When", "@Given", "@Then", "@And", "@But" };
        for (String annotation : cucumberAnnotations) {
            if (originalMethod.contains(annotation)) {
                // Extract the annotation expression from original
                int origStart = originalMethod.indexOf(annotation);
                int origEnd   = originalMethod.indexOf(")", origStart);
                String origAnnotation = originalMethod.substring(origStart, origEnd + 1);

                if (!fixedCode.contains(origAnnotation)) {
                    throw new HealingException(
                        "Safety check failed: AI modified the Cucumber annotation. " +
                        "Original: [" + origAnnotation + "]. " +
                        "This would break step registration — patch rejected.");
                }
                break;
            }
        }
    }
}
