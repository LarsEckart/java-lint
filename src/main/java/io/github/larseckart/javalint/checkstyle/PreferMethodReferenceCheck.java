package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags simple lambdas that have an equivalent method-reference form. */
public final class PreferMethodReferenceCheck extends AbstractCheck {

    /** Message key for a replaceable lambda. */
    public static final String MSG_PREFER_METHOD_REFERENCE = "prefer.method.reference";

    /** Creates the check. */
    public PreferMethodReferenceCheck() {
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
        return new int[] {TokenTypes.LAMBDA};
    }

    @Override
    public void visitToken(DetailAST lambda) {
        String parameter = singleParameterName(lambda);
        DetailAST body = unwrapExpression(lambda.getLastChild());

        if (parameter != null
                && body.getType() == TokenTypes.METHOD_CALL
                && isEquivalentMethodCall(body, parameter)) {
            log(lambda, MSG_PREFER_METHOD_REFERENCE);
        }
    }

    private String singleParameterName(DetailAST lambda) {
        DetailAST parameters = lambda.findFirstToken(TokenTypes.PARAMETERS);
        if (parameters == null) {
            DetailAST identifier = lambda.findFirstToken(TokenTypes.IDENT);
            return identifier == null ? null : identifier.getText();
        }

        DetailAST parameter = parameters.findFirstToken(TokenTypes.PARAMETER_DEF);
        if (parameter != null
                && parameter.getNextSibling() == null
                && parameters.findFirstToken(TokenTypes.COMMA) == null) {
            return parameter.findFirstToken(TokenTypes.IDENT).getText();
        }
        DetailAST identifier = parameters.findFirstToken(TokenTypes.IDENT);
        if (identifier != null && parameters.findFirstToken(TokenTypes.COMMA) == null) {
            return identifier.getText();
        }
        return null;
    }

    private boolean isEquivalentMethodCall(DetailAST methodCall, String parameter) {
        DetailAST selector = methodCall.getFirstChild();
        DetailAST arguments = methodCall.findFirstToken(TokenTypes.ELIST);
        int argumentCount = childCount(arguments, TokenTypes.EXPR);

        if (selector.getType() == TokenTypes.DOT
                && isIdentifier(unwrapExpression(selector.getFirstChild()), parameter)
                && argumentCount == 0) {
            return true;
        }

        DetailAST argument = arguments.findFirstToken(TokenTypes.EXPR);
        return argumentCount == 1
                && isIdentifier(unwrapExpression(argument), parameter)
                && isStableForwardingSelector(selector);
    }

    private boolean isStableForwardingSelector(DetailAST selector) {
        if (selector.getType() == TokenTypes.IDENT) {
            return true;
        }
        if (selector.getType() == TokenTypes.DOT) {
            DetailAST receiver = unwrapExpression(selector.getFirstChild());
            return receiver.getType() == TokenTypes.LITERAL_THIS
                    || receiver.getType() == TokenTypes.LITERAL_SUPER;
        }
        return false;
    }

    private int childCount(DetailAST parent, int tokenType) {
        int result = 0;
        for (DetailAST child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == tokenType) {
                result++;
            }
        }
        return result;
    }

    private DetailAST unwrapExpression(DetailAST node) {
        DetailAST result = node;
        while (result.getType() == TokenTypes.EXPR) {
            result = result.getFirstChild();
        }
        return result;
    }

    private boolean isIdentifier(DetailAST node, String name) {
        return node.getType() == TokenTypes.IDENT && name.equals(node.getText());
    }
}
