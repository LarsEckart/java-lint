package io.github.larseckart.javalint.checkstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class CheckTestSupport {

    @TempDir
    private Path temporaryDirectory;

    void assertViolations(
            String source,
            Class<? extends AbstractCheck> checkClass,
            String messageKey,
            int... expectedLines) throws Exception {
        DefaultConfiguration checkerConfiguration = new DefaultConfiguration("Checker");
        DefaultConfiguration treeWalker = new DefaultConfiguration("TreeWalker");
        treeWalker.addChild(new DefaultConfiguration(checkClass.getName()));
        checkerConfiguration.addChild(treeWalker);

        List<AuditEvent> violations = runCheck(source, checkerConfiguration);
        assertEquals(
                Arrays.stream(expectedLines).boxed().toList(),
                violations.stream().map(AuditEvent::getLine).toList());
        assertTrue(violations.stream().allMatch(event -> checkClass.getName().equals(event.getSourceName())));
        assertTrue(violations.stream().allMatch(event -> messageKey.equals(event.getViolation().getKey())));
    }

    List<AuditEvent> runCheck(String source, Configuration configuration) throws Exception {
        Path sourceFile = writeSource(source);
        Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());
        CollectingAuditListener listener = new CollectingAuditListener();
        checker.addListener(listener);

        try {
            checker.configure(configuration);
            checker.process(List.of(sourceFile.toFile()));
            return List.copyOf(listener.violations);
        } finally {
            checker.destroy();
        }
    }

    private Path writeSource(String source) throws IOException {
        Path sourceFile = temporaryDirectory.resolve("Example.java");
        Files.writeString(sourceFile, source);
        return sourceFile;
    }

    private static final class CollectingAuditListener implements AuditListener {

        private final List<AuditEvent> violations = new ArrayList<>();

        @Override
        public void auditStarted(AuditEvent event) {
        }

        @Override
        public void auditFinished(AuditEvent event) {
        }

        @Override
        public void fileStarted(AuditEvent event) {
        }

        @Override
        public void fileFinished(AuditEvent event) {
        }

        @Override
        public void addError(AuditEvent event) {
            violations.add(event);
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            throw new AssertionError("Checkstyle audit failed", throwable);
        }
    }
}
