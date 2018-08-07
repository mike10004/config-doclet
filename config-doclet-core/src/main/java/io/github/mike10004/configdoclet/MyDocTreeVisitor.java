package io.github.mike10004.configdoclet;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTreeScanner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

abstract class MyDocTreeVisitor<R, D> extends DocTreeScanner<R, D> {

    private final Set<String> actionableTags;

    public MyDocTreeVisitor(Collection<String> actionableTags) {
        this.actionableTags = Collections.unmodifiableSet(new HashSet<>(actionableTags));
    }

    @Override
    public final R visitUnknownBlockTag(UnknownBlockTagTree node, D d) {
        if (isActionable(node)) {
            return processActionableTag(node, d);
        }
        return null;
    }

    protected abstract R processActionableTag(BlockTagTree node, D value);

    @Override
    public R visitOther(DocTree node, D d) {
        if (node instanceof BlockTagTree) {
            BlockTagTree btt = (BlockTagTree) node;
            if (isActionable(btt)) {
                processActionableTag(btt, d);
            }
        }
        return null;
    }

    protected boolean isActionable(BlockTagTree node) {
        return actionableTags.contains(node.getTagName());
    }
}
