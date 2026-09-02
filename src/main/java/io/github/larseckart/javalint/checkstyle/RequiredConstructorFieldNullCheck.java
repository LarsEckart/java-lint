package io.github.larseckart.javalint.checkstyle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Flags null checks on required constructor-assigned fields.
 *
 * <p>A private final field assigned from a constructor parameter is treated as required unless
 * either the field or parameter has an annotation named {@code Nullable}. A null check outside the
 * constructor must make that nullable contract explicit.</p>
 */
public final class RequiredConstructorFieldNullCheck extends AbstractCheck {

    /** Message key for a null check without a nullable contract. */
    public static final String MSG_NULL_CHECK = "required.constructor.field.null.check";

    /** Creates the check. */
    public RequiredConstructorFieldNullCheck() {
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
                TokenTypes.CLASS_DEF,
                TokenTypes.ENUM_DEF,
                TokenTypes.RECORD_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST typeDefinition) {
        DetailAST objectBlock = typeDefinition.findFirstToken(TokenTypes.OBJBLOCK);
        if (objectBlock == null) {
            return;
        }

        Set<String> requiredFields = findRequiredConstructorAssignedFields(objectBlock);
        if (!requiredFields.isEmpty()) {
            findNullChecks(objectBlock, requiredFields, typeDefinition);
        }
    }

    private Set<String> findRequiredConstructorAssignedFields(DetailAST objectBlock) {
        Set<String> candidateFields = findCandidateFields(objectBlock);
        Set<String> requiredFields = new HashSet<>();
        Set<String> nullableFields = new HashSet<>();

        for (DetailAST member = objectBlock.getFirstChild(); member != null; member = member.getNextSibling()) {
            if (member.getType() == TokenTypes.CTOR_DEF) {
                Map<String, Boolean> constructorParameters = findConstructorParameters(member);
                findConstructorAssignments(
                        member,
                        candidateFields,
                        constructorParameters,
                        requiredFields,
                        nullableFields,
                        member);
            }
        }

        requiredFields.removeAll(nullableFields);
        return requiredFields;
    }

    private Set<String> findCandidateFields(DetailAST objectBlock) {
        Set<String> fields = new HashSet<>();

        for (DetailAST member = objectBlock.getFirstChild(); member != null; member = member.getNextSibling()) {
            if (member.getType() != TokenTypes.VARIABLE_DEF) {
                continue;
            }

            DetailAST modifiers = member.findFirstToken(TokenTypes.MODIFIERS);
            if (modifiers != null
                    && modifiers.findFirstToken(TokenTypes.LITERAL_PRIVATE) != null
                    && modifiers.findFirstToken(TokenTypes.FINAL) != null
                    && modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) == null
                    && !hasNullableAnnotation(member)) {
                fields.add(member.findFirstToken(TokenTypes.IDENT).getText());
            }
        }

        return fields;
    }

    private Map<String, Boolean> findConstructorParameters(DetailAST constructor) {
        Map<String, Boolean> parameters = new HashMap<>();
        DetailAST parameterList = constructor.findFirstToken(TokenTypes.PARAMETERS);

        for (DetailAST child = parameterList.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.PARAMETER_DEF) {
                String name = child.findFirstToken(TokenTypes.IDENT).getText();
                parameters.put(name, hasNullableAnnotation(child));
            }
        }

        return parameters;
    }

    private void findConstructorAssignments(
            DetailAST node,
            Set<String> candidateFields,
            Map<String, Boolean> constructorParameters,
            Set<String> requiredFields,
            Set<String> nullableFields,
            DetailAST rootConstructor) {
        if (node != rootConstructor && isNestedType(node)) {
            return;
        }

        if (node.getType() == TokenTypes.ASSIGN) {
            DetailAST left = node.getFirstChild();
            DetailAST right = left == null ? null : left.getNextSibling();
            String assignedField = getFieldName(left, candidateFields);
            String sourceParameter = getDirectParameterName(right, constructorParameters.keySet());

            if (assignedField != null && sourceParameter != null) {
                if (constructorParameters.get(sourceParameter)) {
                    nullableFields.add(assignedField);
                } else {
                    requiredFields.add(assignedField);
                }
            }
        }

        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            findConstructorAssignments(
                    child,
                    candidateFields,
                    constructorParameters,
                    requiredFields,
                    nullableFields,
                    rootConstructor);
        }
    }

    private void findNullChecks(DetailAST node, Set<String> requiredFields, DetailAST rootType) {
        if (node.getType() == TokenTypes.CTOR_DEF || (node != rootType && isNestedType(node))) {
            return;
        }

        if (node.getType() == TokenTypes.EQUAL || node.getType() == TokenTypes.NOT_EQUAL) {
            DetailAST left = node.getFirstChild();
            DetailAST right = left == null ? null : left.getNextSibling();
            String fieldName = null;

            if (isNullLiteral(left)) {
                fieldName = getCheckedFieldName(right, requiredFields, node);
            } else if (isNullLiteral(right)) {
                fieldName = getCheckedFieldName(left, requiredFields, node);
            }

            if (fieldName != null) {
                log(node, MSG_NULL_CHECK, fieldName);
            }
        }

        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            findNullChecks(child, requiredFields, rootType);
        }
    }

    private String getCheckedFieldName(DetailAST expression, Set<String> requiredFields, DetailAST useSite) {
        DetailAST unwrapped = unwrapExpression(expression);
        if (unwrapped == null) {
            return null;
        }

        if (unwrapped.getType() == TokenTypes.IDENT) {
            String name = unwrapped.getText();
            if (requiredFields.contains(name) && !isShadowedInExecutable(useSite, name)) {
                return name;
            }
        } else if (unwrapped.getType() == TokenTypes.DOT && isThisFieldReference(unwrapped)) {
            String name = unwrapped.getLastChild().getText();
            if (requiredFields.contains(name)) {
                return name;
            }
        }

        return null;
    }

    private String getFieldName(DetailAST expression, Set<String> candidateFields) {
        DetailAST unwrapped = unwrapExpression(expression);
        if (unwrapped == null) {
            return null;
        }

        if (unwrapped.getType() == TokenTypes.IDENT && candidateFields.contains(unwrapped.getText())) {
            return unwrapped.getText();
        }
        if (unwrapped.getType() == TokenTypes.DOT && isThisFieldReference(unwrapped)) {
            String name = unwrapped.getLastChild().getText();
            return candidateFields.contains(name) ? name : null;
        }
        return null;
    }

    private DetailAST unwrapExpression(DetailAST expression) {
        DetailAST result = expression;
        while (result != null && result.getType() == TokenTypes.EXPR) {
            result = result.getFirstChild();
        }
        return result;
    }

    private boolean isNullLiteral(DetailAST expression) {
        DetailAST unwrapped = unwrapExpression(expression);
        return unwrapped != null && unwrapped.getType() == TokenTypes.LITERAL_NULL;
    }

    private boolean isThisFieldReference(DetailAST dot) {
        DetailAST first = dot.getFirstChild();
        DetailAST last = dot.getLastChild();
        return first != null
                && first.getType() == TokenTypes.LITERAL_THIS
                && last != null
                && last.getType() == TokenTypes.IDENT;
    }

    private String getDirectParameterName(DetailAST expression, Set<String> parameterNames) {
        DetailAST unwrapped = unwrapExpression(expression);
        if (unwrapped == null) {
            return null;
        }
        if (unwrapped.getType() == TokenTypes.IDENT && parameterNames.contains(unwrapped.getText())) {
            return unwrapped.getText();
        }
        if (unwrapped.getType() == TokenTypes.METHOD_CALL && isRequireNonNullCall(unwrapped)) {
            DetailAST arguments = unwrapped.findFirstToken(TokenTypes.ELIST);
            if (arguments != null) {
                return getDirectParameterName(arguments.getFirstChild(), parameterNames);
            }
        }
        return null;
    }

    private boolean isRequireNonNullCall(DetailAST methodCall) {
        DetailAST methodSelector = methodCall.getFirstChild();
        DetailAST methodName = null;
        if (methodSelector != null) {
            methodName = methodSelector.getType() == TokenTypes.IDENT
                    ? methodSelector
                    : methodSelector.getLastChild();
        }
        return methodName != null
                && methodName.getType() == TokenTypes.IDENT
                && "requireNonNull".equals(methodName.getText());
    }

    private boolean isShadowedInExecutable(DetailAST useSite, String fieldName) {
        DetailAST executable = useSite;
        while (executable != null
                && executable.getType() != TokenTypes.METHOD_DEF
                && executable.getType() != TokenTypes.CTOR_DEF) {
            executable = executable.getParent();
        }
        return executable != null && containsDeclaration(executable, fieldName, executable);
    }

    private boolean containsDeclaration(DetailAST node, String name, DetailAST rootExecutable) {
        if (node != rootExecutable && isNestedType(node)) {
            return false;
        }
        if (node.getType() == TokenTypes.PARAMETER_DEF || node.getType() == TokenTypes.VARIABLE_DEF) {
            DetailAST identifier = node.findFirstToken(TokenTypes.IDENT);
            if (identifier != null && name.equals(identifier.getText())) {
                return true;
            }
        }

        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (containsDeclaration(child, name, rootExecutable)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNullableAnnotation(DetailAST definition) {
        return containsAnnotationNamed(definition, "Nullable");
    }

    private boolean containsAnnotationNamed(DetailAST node, String annotationName) {
        if (node.getType() == TokenTypes.ANNOTATION && containsIdentifier(node, annotationName)) {
            return true;
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (containsAnnotationNamed(child, annotationName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIdentifier(DetailAST node, String identifier) {
        if (node.getType() == TokenTypes.IDENT && identifier.equals(node.getText())) {
            return true;
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (containsIdentifier(child, identifier)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNestedType(DetailAST node) {
        return node.getType() == TokenTypes.CLASS_DEF
                || node.getType() == TokenTypes.ENUM_DEF
                || node.getType() == TokenTypes.INTERFACE_DEF
                || node.getType() == TokenTypes.ANNOTATION_DEF
                || node.getType() == TokenTypes.RECORD_DEF;
    }
}
