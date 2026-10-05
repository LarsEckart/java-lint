package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Requires Logger fields to be named LOGGER and declared static final. */
public final class LoggerDeclarationCheck extends AbstractCheck {

    /** Message key for an invalid logger field. */
    public static final String MSG_LOGGER_DECLARATION = "logger.declaration";

    /** Creates the check. */
    public LoggerDeclarationCheck() {
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
        return new int[] {TokenTypes.VARIABLE_DEF};
    }

    @Override
    public void visitToken(DetailAST variable) {
        DetailAST type = variable.findFirstToken(TokenTypes.TYPE);
        if (variable.getParent().getType() != TokenTypes.OBJBLOCK
                || type.findFirstToken(TokenTypes.ARRAY_DECLARATOR) != null
                || !isLoggerType(type)) {
            return;
        }

        DetailAST modifiers = variable.findFirstToken(TokenTypes.MODIFIERS);
        String name = variable.findFirstToken(TokenTypes.IDENT).getText();
        if (!"LOGGER".equals(name)
                || modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) == null
                || modifiers.findFirstToken(TokenTypes.FINAL) == null) {
            log(variable, MSG_LOGGER_DECLARATION);
        }
    }

    private boolean isLoggerType(DetailAST type) {
        for (DetailAST child = type.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.IDENT) {
                return "Logger".equals(child.getText());
            }
            if (child.getType() == TokenTypes.DOT) {
                return "Logger".equals(child.getLastChild().getText());
            }
        }
        return false;
    }
}
