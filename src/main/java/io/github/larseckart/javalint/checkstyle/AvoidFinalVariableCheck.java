package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags explicit final modifiers on variables and parameters, except constants. */
public final class AvoidFinalVariableCheck extends AbstractCheck {

    /** Message key for a final variable. */
    public static final String MSG_FINAL_VARIABLE = "avoid.final.variable";

    /** Creates the check. */
    public AvoidFinalVariableCheck() {
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
                TokenTypes.VARIABLE_DEF,
                TokenTypes.PARAMETER_DEF,
                TokenTypes.RESOURCE,
                TokenTypes.PATTERN_VARIABLE_DEF,
        };
    }

    @Override
    public void visitToken(DetailAST definition) {
        DetailAST modifiers = definition.findFirstToken(TokenTypes.MODIFIERS);
        boolean isFinal = modifiers != null && modifiers.findFirstToken(TokenTypes.FINAL) != null;
        boolean isConstant = definition.getType() == TokenTypes.VARIABLE_DEF
                && definition.getParent().getType() == TokenTypes.OBJBLOCK
                && modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null;

        if (isFinal && !isConstant) {
            log(definition, MSG_FINAL_VARIABLE);
        }
    }
}
