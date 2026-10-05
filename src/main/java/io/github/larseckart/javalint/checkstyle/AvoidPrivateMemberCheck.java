package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags private methods and fields. */
public final class AvoidPrivateMemberCheck extends AbstractCheck {

    /** Message key for a private member. */
    public static final String MSG_PRIVATE_MEMBER = "avoid.private.member";

    /** Creates the check. */
    public AvoidPrivateMemberCheck() {
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
        return new int[] {TokenTypes.METHOD_DEF, TokenTypes.VARIABLE_DEF};
    }

    @Override
    public void visitToken(DetailAST definition) {
        boolean field = definition.getType() == TokenTypes.VARIABLE_DEF
                && definition.getParent().getType() == TokenTypes.OBJBLOCK;
        boolean method = definition.getType() == TokenTypes.METHOD_DEF;
        DetailAST modifiers = definition.findFirstToken(TokenTypes.MODIFIERS);

        if ((field || method) && modifiers.findFirstToken(TokenTypes.LITERAL_PRIVATE) != null) {
            log(definition, MSG_PRIVATE_MEMBER);
        }
    }
}
