# java-lint

Reusable Java lint rules.

## RequiredConstructorFieldNullCheck

`RequiredConstructorFieldNullCheck` treats a private final field assigned from a constructor parameter as non-null by default. It supports direct assignment and assignment through `Objects.requireNonNull`.

The rule flags `== null` and `!= null` checks on that field outside the constructor. Such a check must lead to one of two fixes:

- Remove fallback behavior for a required field and fail fast during construction.
- Mark an optional field or constructor parameter with an annotation named `Nullable`.

Constructor validation is allowed, as are local variables and parameters that shadow a field.

### Checkstyle configuration

Add Checkstyle and this rules artifact to the Checkstyle classpath:

```kotlin
dependencies {
    checkstyle("com.puppycrawl.tools:checkstyle:13.9.0")
    checkstyle("your.group:java-lint:0.1.0-SNAPSHOT")
}
```

Enable the rule in `checkstyle.xml`:

```xml
<module name="TreeWalker">
    <module name="io.github.larseckart.javalint.checkstyle.RequiredConstructorFieldNullCheck"/>
</module>
```

The library targets Java 17 and is tested with Checkstyle 13.9.0.

## Test through Maven Local

No permanent Maven group is set yet. Supply a temporary group when publishing locally:

```shell
./gradlew publishToMavenLocal -PpublicationGroup=local.java-lint
```

Use this dependency in a local consumer:

```kotlin
checkstyle("local.java-lint:java-lint:0.1.0-SNAPSHOT")
```

Set `publicationGroup` to the final Maven group when publishing elsewhere. Override the version with `-PpublicationVersion=...`.

## Build

```shell
./gradlew build
```

## License

Apache License 2.0.
