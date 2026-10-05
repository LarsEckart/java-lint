package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags {@code collect(Collectors.toList())} in favor of {@code toList()}. */
public final class PreferStreamToListCheck extends AbstractCheck {

    /** Message key for collecting to a list. */
    public static final String MSG_PREFER_TO_LIST = "prefer.stream.to.list";

    /** Creates the check. */
    public PreferStreamToListCheck() {
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
        return new int[] {TokenTypes.METHOD_CALL};
    }

    @Override
    public void visitToken(DetailAST methodCall) {
        if ("collect".equals(methodName(methodCall)) && collectsWithCollectorsToList(methodCall)) {
            log(methodCall, MSG_PREFER_TO_LIST);
        }
    }

    private boolean collectsWithCollectorsToList(DetailAST collectCall) {
        DetailAST arguments = collectCall.findFirstToken(TokenTypes.ELIST);
        if (childCount(arguments, TokenTypes.EXPR) != 1) {
            return false;
        }
        DetailAST expression = arguments == null ? null : arguments.findFirstToken(TokenTypes.EXPR);
        DetailAST argument = expression == null ? null : expression.getFirstChild();

        if (argument == null
                || argument.getType() != TokenTypes.METHOD_CALL
                || !"toList".equals(methodName(argument))) {
            return false;
        }

        DetailAST selector = argument.getFirstChild();
        return selector.getType() == TokenTypes.DOT
                && isCollectors(selector.getFirstChild())
                && argument.findFirstToken(TokenTypes.ELIST).getChildCount() == 0;
    }

    private boolean isCollectors(DetailAST receiver) {
        if (receiver.getType() == TokenTypes.IDENT) {
            return "Collectors".equals(receiver.getText());
        }
        return receiver.getType() == TokenTypes.DOT
                && "java.util.stream.Collectors".equals(qualifiedName(receiver));
    }

    private String qualifiedName(DetailAST node) {
        if (node.getType() == TokenTypes.IDENT) {
            return node.getText();
        }
        return qualifiedName(node.getFirstChild()) + "." + node.getLastChild().getText();
    }

    private int childCount(DetailAST parent, int tokenType) {
        int result = 0;
        if (parent != null) {
            for (DetailAST child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getType() == tokenType) {
                    result++;
                }
            }
        }
        return result;
    }

    private String methodName(DetailAST methodCall) {
        DetailAST selector = methodCall.getFirstChild();
        DetailAST identifier = selector.getType() == TokenTypes.IDENT
                ? selector
                : selector.getLastChild();
        return identifier.getText();
    }
}
