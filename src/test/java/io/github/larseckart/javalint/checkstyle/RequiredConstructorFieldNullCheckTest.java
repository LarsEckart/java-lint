package io.github.larseckart.javalint.checkstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredConstructorFieldNullCheckTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void flagsNullChecksOnARequiredConstructorField() throws Exception {
        String source = """
                class Example {
                    private final Object dependency;

                    Example(Object dependency) {
                        this.dependency = dependency;
                    }

                    void use() {
                        if (dependency != null) {
                            dependency.toString();
                        }
                        if (null == this.dependency) {
                            throw new IllegalStateException();
                        }
                    }
                }
                """;

        assertEquals(2, runCheck(source));
    }

    @Test
    void flagsNullChecksAfterRequireNonNullAssignment() throws Exception {
        String source = """
                import java.util.Objects;

                class Example {
                    private final Object dependency;

                    Example(Object dependency) {
                        this.dependency = Objects.requireNonNull(dependency);
                    }

                    boolean hasDependency() {
                        return dependency != null;
                    }
                }
                """;

        assertEquals(1, runCheck(source));
    }

    @Test
    void allowsNullChecksWhenTheDependencyIsExplicitlyNullable() throws Exception {
        String source = """
                class Example {
                    private final Object dependency;

                    Example(@Nullable Object dependency) {
                        this.dependency = dependency;
                    }

                    void use() {
                        if (dependency != null) {
                            dependency.toString();
                        }
                    }
                }
                """;

        assertEquals(0, runCheck(source));
    }

    @Test
    void allowsNullChecksWhenTheFieldIsExplicitlyNullable() throws Exception {
        String source = """
                class Example {
                    @Nullable
                    private final Object dependency;

                    Example(Object dependency) {
                        this.dependency = dependency;
                    }

                    void use() {
                        if (dependency != null) {
                            dependency.toString();
                        }
                    }
                }
                """;

        assertEquals(0, runCheck(source));
    }

    @Test
    void doesNotMistakeAShadowingParameterForTheField() throws Exception {
        String source = """
                class Example {
                    private final Object dependency;

                    Example(Object dependency) {
                        this.dependency = dependency;
                    }

                    void use(Object dependency) {
                        if (dependency != null) {
                            dependency.toString();
                        }
                        if (this.dependency != null) {
                            this.dependency.toString();
                        }
                    }
                }
                """;

        assertEquals(1, runCheck(source));
    }

    @Test
    void ignoresFieldsThatAreNotDirectlyAssignedFromAConstructorParameter() throws Exception {
        String source = """
                class Example {
                    private final Object loadedValue = load();
                    private final Object transformedValue;

                    Example(String key) {
                        transformedValue = load(key);
                    }

                    void use() {
                        if (loadedValue != null || transformedValue != null) {
                            loadedValue.toString();
                        }
                    }

                    private Object load() {
                        return new Object();
                    }

                    private Object load(String key) {
                        return key;
                    }
                }
                """;

        assertEquals(0, runCheck(source));
    }

    @Test
    void allowsConstructorValidationToFailFast() throws Exception {
        String source = """
                class Example {
                    private final Object dependency;

                    Example(Object dependency) {
                        this.dependency = dependency;
                        if (this.dependency == null) {
                            throw new IllegalArgumentException("dependency");
                        }
                    }
                }
                """;

        assertEquals(0, runCheck(source));
    }

    private int runCheck(String source) throws Exception {
        Path sourceFile = writeSource(source);
        Checker checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());

        try {
            checker.configure(buildConfiguration());
            return checker.process(List.of(sourceFile.toFile()));
        } finally {
            checker.destroy();
        }
    }

    private Path writeSource(String source) throws IOException {
        Path sourceFile = temporaryDirectory.resolve("Example.java");
        Files.writeString(sourceFile, source);
        return sourceFile;
    }

    private DefaultConfiguration buildConfiguration() {
        DefaultConfiguration checker = new DefaultConfiguration("Checker");
        DefaultConfiguration treeWalker = new DefaultConfiguration("TreeWalker");
        treeWalker.addChild(new DefaultConfiguration(RequiredConstructorFieldNullCheck.class.getName()));
        checker.addChild(treeWalker);
        return checker;
    }
}
