package io.github.larseckart.javalint.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/** Flags traditional JavaDoc comments in favor of Markdown JavaDoc. */
public final class MarkdownJavadocCheck extends AbstractCheck {

    /** Message key for traditional JavaDoc. */
    public static final String MSG_MARKDOWN_JAVADOC = "javadoc.use.markdown";

    /** Creates the check. */
    public MarkdownJavadocCheck() {
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
        return new int[] {TokenTypes.BLOCK_COMMENT_BEGIN};
    }

    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    @Override
    public void visitToken(DetailAST comment) {
        DetailAST content = comment.findFirstToken(TokenTypes.COMMENT_CONTENT);
        if (content != null && content.getText().startsWith("*")) {
            log(comment, MSG_MARKDOWN_JAVADOC);
        }
    }
}
