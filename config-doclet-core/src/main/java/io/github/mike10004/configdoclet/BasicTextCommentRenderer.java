package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.TextTree;

import java.util.Collection;
import java.util.Collections;
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
        return tree instanceof TextTree;
    }

    protected String renderOne(DocTree tree) {
        if (tree instanceof TextTree) {
            return ((TextTree)tree).getBody();
        }
        return renderUnsupported(tree);
    }

    String renderUnsupported(DocTree tree) {
        return "";
    }

    @Override
    public String render(Collection<? extends DocTree> docTrees) {
        return joinText(docTrees, " ");
    }
}
