package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;

import java.util.Collections;

class TextCommentRenderer extends BasicTextCommentRenderer {

    private final LinkValueRenderer linkValueRenderer;

    public TextCommentRenderer(LinkValueRenderer linkValueRenderer) {
        this.linkValueRenderer = linkValueRenderer;
    }

    protected boolean isRenderable(DocTree tree) {
        return super.isRenderable(tree)
                ||  tree instanceof LinkTree;
    }

    @Override
    protected String renderOne(DocTree tree) {
        if (tree instanceof LinkTree) {
            return linkValueRenderer.render(Collections.singleton(tree));
        }
        return super.renderOne(tree);
    }

}
