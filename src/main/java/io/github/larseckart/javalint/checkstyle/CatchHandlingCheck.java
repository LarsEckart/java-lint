package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Requires catch blocks to log or throw. */
public final class CatchHandlingCheck extends AbstractCheck {

    /** Message key for an unhandled exception. */
    public static final String MSG_CATCH_HANDLING = "catch.must.log.or.throw";

    /** Creates the check. */
    public CatchHandlingCheck() {
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
        return new int[] {TokenTypes.LITERAL_CATCH};
    }

    @Override
    public void visitToken(DetailAST catchClause) {
        DetailAST body = catchClause.findFirstToken(TokenTypes.SLIST);
        if (!containsHandling(body, body)) {
            log(catchClause, MSG_CATCH_HANDLING);
        }
    }

    private boolean containsHandling(DetailAST node, DetailAST root) {
        if (node.getType() == TokenTypes.LITERAL_THROW) {
            return true;
        }
        if (node.getType() == TokenTypes.METHOD_CALL && "log".equals(methodName(node))) {
            return true;
        }
        if (node != root
                && (node.getType() == TokenTypes.LAMBDA
                    || node.getType() == TokenTypes.METHOD_DEF
                    || isType(node))) {
            return false;
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (containsHandling(child, root)) {
                return true;
            }
        }
        return false;
    }

    private String methodName(DetailAST methodCall) {
        DetailAST selector = methodCall.getFirstChild();
        DetailAST identifier = selector.getType() == TokenTypes.IDENT
                ? selector
                : selector.getLastChild();
        return identifier.getText();
    }

    private boolean isType(DetailAST node) {
        int type = node.getType();
        return type == TokenTypes.CLASS_DEF
                || type == TokenTypes.ENUM_DEF
                || type == TokenTypes.INTERFACE_DEF
                || type == TokenTypes.ANNOTATION_DEF
                || type == TokenTypes.RECORD_DEF;
    }
}
