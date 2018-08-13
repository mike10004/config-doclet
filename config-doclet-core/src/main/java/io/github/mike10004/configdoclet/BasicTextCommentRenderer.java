package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.InlineTagTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.TextTree;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class BasicTextCommentRenderer implements CommentRenderer {

    public BasicTextCommentRenderer() {
    }

    @SuppressWarnings("SameParameterValue")
    String joinText(Collection<? extends DocTree> fullBody, String delimiter) {
        return fullBody.stream()
                .filter(this::isRenderable)
                .map(this::renderOne)
                .collect(Collectors.joining(delimiter)).trim();
    }

    protected boolean isRenderable(DocTree tree) {
        return tree instanceof TextTree
                || isInlineCode(tree);
    }

    protected String renderOne(DocTree tree) {
        if (tree instanceof TextTree) {
            return ((TextTree)tree).getBody();
        }
        if (isInlineCode(tree)) {
             TextTree body = ((LiteralTree) tree).getBody();
             return body.getBody();
        }
        return renderUnsupported(tree);
    }

    private boolean isInlineCode(DocTree tree) {
        if (tree instanceof LiteralTree) {
            String tagName = ((InlineTagTree) tree).getTagName();
            if ("code".equalsIgnoreCase(tagName)) {
                return true;
            }
        }
        return false;
    }

    protected String renderUnsupported(DocTree tree) {
        return "";
    }

    @Override
    public String render(Collection<? extends DocTree> docTrees) {
        return joinText(docTrees, " ");
    }
}
