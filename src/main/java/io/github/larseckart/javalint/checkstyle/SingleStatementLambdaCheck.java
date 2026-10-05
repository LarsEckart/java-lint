package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags lambda blocks containing more than one statement. */
public final class SingleStatementLambdaCheck extends AbstractCheck {

    /** Message key for a multi-statement lambda. */
    public static final String MSG_MULTIPLE_STATEMENTS = "lambda.multiple.statements";

    /** Creates the check. */
    public SingleStatementLambdaCheck() {
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
        DetailAST body = lambda.getLastChild();
        if (body.getType() == TokenTypes.SLIST && statementCount(body) > 1) {
            log(lambda, MSG_MULTIPLE_STATEMENTS);
        }
    }

    private int statementCount(DetailAST block) {
        int result = 0;
        boolean variableDeclaration = false;
        for (DetailAST child = block.getFirstChild(); child != null; child = child.getNextSibling()) {
            int type = child.getType();
            if (type == TokenTypes.VARIABLE_DEF) {
                if (!variableDeclaration) {
                    result++;
                    variableDeclaration = true;
                }
            } else if (type == TokenTypes.SEMI) {
                variableDeclaration = false;
            } else if (type != TokenTypes.LCURLY
                    && type != TokenTypes.RCURLY
                    && type != TokenTypes.COMMA) {
                result++;
                variableDeclaration = false;
            }
        }
        return result;
    }
}
