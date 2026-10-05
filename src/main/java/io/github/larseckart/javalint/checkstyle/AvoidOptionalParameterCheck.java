package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags Optional parameters. */
public final class AvoidOptionalParameterCheck extends AbstractCheck {

    /** Message key for an Optional parameter. */
    public static final String MSG_OPTIONAL_PARAMETER = "avoid.optional.parameter";

    /** Creates the check. */
    public AvoidOptionalParameterCheck() {
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
        return new int[] {TokenTypes.PARAMETER_DEF};
    }

    @Override
    public void visitToken(DetailAST parameter) {
        DetailAST type = parameter.findFirstToken(TokenTypes.TYPE);
        if (type.findFirstToken(TokenTypes.ARRAY_DECLARATOR) == null
                && parameter.findFirstToken(TokenTypes.ELLIPSIS) == null
                && isOptionalType(type)) {
            log(parameter, MSG_OPTIONAL_PARAMETER);
        }
    }

    private boolean isOptionalType(DetailAST type) {
        for (DetailAST child = type.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.IDENT) {
                return "Optional".equals(child.getText());
            }
            if (child.getType() == TokenTypes.DOT) {
                return "Optional".equals(child.getLastChild().getText());
            }
        }
        return false;
    }
}
