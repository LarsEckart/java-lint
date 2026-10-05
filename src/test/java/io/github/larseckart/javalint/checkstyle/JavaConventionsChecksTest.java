package io.github.larseckart.javalint.checkstyle;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaConventionsChecksTest extends CheckTestSupport {

    @Test
    void flagsPrivateFieldsAndMethodsOnly() throws Exception {
        String source = """
                class Example {
                    private Object hidden;
                    Object visible;

                    private void hidden() {
                        Object local = hidden;
                    }

                    void visible() {
                    }
                }
                """;

        assertViolations(source, AvoidPrivateMemberCheck.class, AvoidPrivateMemberCheck.MSG_PRIVATE_MEMBER, 2, 5);
    }

    @Test
    void flagsAllExplicitFinalVariablesButAllowsConstantsAndResourceReferences() throws Exception {
        String source = """
                class Example {
                    static final int CONSTANT = 1;
                    final Object field = new Object();

                    void use(final Object parameter) throws Exception {
                        final Object local = parameter;
                        if (parameter instanceof final String text) { }
                        try (final var reader = new java.io.StringReader("")) { }
                        var reusable = new java.io.StringReader("");
                        try (reusable) { }
                    }
                }
                """;

        assertViolations(
                source,
                AvoidFinalVariableCheck.class,
                AvoidFinalVariableCheck.MSG_FINAL_VARIABLE,
                3, 5, 6, 7, 8);
    }

    @Test
    void countsJavaStatementsInLambdaBlocks() throws Exception {
        String source = """
                class Example {
                    void lambdas() {
                        Runnable expression = () -> use();
                        Runnable oneStatement = () -> { use(); };
                        Runnable oneDeclaration = () -> { int first = 1, second = 2; };
                        Runnable multipleStatements = () -> {
                            int first = 1;
                            int second = 2;
                        };
                        Runnable emptyStatement = () -> { use(); ; };
                        Runnable compoundStatement = () -> { if (true) { use(); } };
                    };

                    void use() {
                    }
                }
                """;

        assertViolations(
                source,
                SingleStatementLambdaCheck.class,
                SingleStatementLambdaCheck.MSG_MULTIPLE_STATEMENTS,
                6, 10);
    }

    @Test
    void flagsCollectorsToListOnly() throws Exception {
        String source = """
                import java.util.stream.Collectors;
                import java.util.stream.Stream;

                class Example {
                    Object oldStyle() {
                        return Stream.of("a").collect(Collectors.toList());
                    }

                    Object fullyQualified() {
                        return Stream.of("a").collect(java.util.stream.Collectors.toList());
                    }

                    Object otherCollector() {
                        return Stream.of("a").collect(Collectors.toSet());
                    }

                    Object unrelatedArity() {
                        return Stream.of("a").collect(Collectors.toList(), "extra");
                    }

                    Object modernStyle() {
                        return Stream.of("a").toList();
                    }
                }
                """;

        assertViolations(
                source,
                PreferStreamToListCheck.class,
                PreferStreamToListCheck.MSG_PREFER_TO_LIST,
                6, 10);
    }

    @Test
    void flagsOptionalParametersButAllowsOptionalReturns() throws Exception {
        String source = """
                import java.util.Optional;
                import java.util.List;

                class Example {
                    void consume(Optional<String> value) {
                    }

                    void arrays(Optional<String>[] first, Optional<String> second[], Optional<String>... third) {
                    }

                    void nested(List<Optional<String>> values) {
                    }

                    Optional<String> produce() {
                        return Optional.empty();
                    }
                }
                """;

        assertViolations(
                source,
                AvoidOptionalParameterCheck.class,
                AvoidOptionalParameterCheck.MSG_OPTIONAL_PARAMETER,
                5);
    }

    @Test
    void flagsLambdasWithEquivalentMethodReferences() throws Exception {
        String source = """
                import java.util.function.Function;
                import java.util.function.Predicate;

                class Example {
                    Service service = new Service();
                    Function<String, String> strip = value -> value.strip();
                    Function<String, String> unqualified = value -> process(value);
                    Function<String, String> explicitThis = value -> this.process(value);
                    Function<String, String> computed = value -> createService().process(value);
                    Function<String, String> constructed = value -> new Service().process(value);
                    Function<String, String> mutableField = value -> service.process(value);
                    Function<String, String> substring = value -> value.substring(1);

                    String process(String value) {
                        return value;
                    }

                    Service createService() {
                        return service;
                    }

                    static class Service {
                        String process(String value) {
                            return value;
                        }
                    }
                }
                """;

        assertViolations(
                source,
                PreferMethodReferenceCheck.class,
                PreferMethodReferenceCheck.MSG_PREFER_METHOD_REFERENCE,
                6, 7, 8);
    }

    @Test
    void requiresLoggerFieldsToBeStaticFinalAndUppercase() throws Exception {
        String source = """
                import java.util.function.Supplier;

                class Example {
                    static final System.Logger LOGGER = System.getLogger("good");
                    System.Logger logger = System.getLogger("bad");
                    System.Logger[] loggers;
                    Supplier<System.Logger> loggerSupplier;
                }
                """;

        assertViolations(source, LoggerDeclarationCheck.class, LoggerDeclarationCheck.MSG_LOGGER_DECLARATION, 5);
    }

    @Test
    void requiresCatchBlocksToLogOrThrow() throws Exception {
        String source = """
                class Example {
                    static final System.Logger LOGGER = System.getLogger("example");

                    void ignored() {
                        try {
                            use();
                        } catch (RuntimeException exception) {
                            use();
                        }
                    }

                    void anonymousThrow() {
                        try {
                            use();
                        } catch (RuntimeException exception) {
                            Runnable unused = new Runnable() {
                                public void run() {
                                    throw exception;
                                }
                            };
                        }
                    }

                    void anonymousLog() {
                        try {
                            use();
                        } catch (RuntimeException exception) {
                            Runnable unused = new Runnable() {
                                public void run() {
                                    LOGGER.log(System.Logger.Level.ERROR, "failed", exception);
                                }
                            };
                        }
                    }

                    void logged() {
                        try {
                            use();
                        } catch (RuntimeException exception) {
                            LOGGER.log(System.Logger.Level.ERROR, "failed", exception);
                        }
                    }

                    void rethrown() {
                        try {
                            use();
                        } catch (RuntimeException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }

                    void use() {
                    }
                }
                """;

        assertViolations(
                source,
                CatchHandlingCheck.class,
                CatchHandlingCheck.MSG_CATCH_HANDLING,
                7, 15, 27);
    }

    @Test
    void requiresMarkdownJavadoc() throws Exception {
        String source = """
                /** Traditional JavaDoc. */
                class Example {
                    String text = "/** not JavaDoc */";

                    /// Markdown JavaDoc.
                    void use() {
                    }
                }
                """;

        assertViolations(source, MarkdownJavadocCheck.class, MarkdownJavadocCheck.MSG_MARKDOWN_JAVADOC, 1);
    }

    @Test
    void loadsTheExampleConfiguration() throws Exception {
        String source = """
                class Example {
                    static final System.Logger LOGGER = System.getLogger("example");

                    void use() {
                        Runnable work = this::run;
                        work.run();
                    }

                    void run() {
                    }
                }
                """;
        var configuration = ConfigurationLoader.loadConfiguration(
                Path.of("checkstyle-example.xml").toAbsolutePath().toString(),
                new PropertiesExpander(new Properties()));

        assertTrue(runCheck(source, configuration).isEmpty());
    }
}
