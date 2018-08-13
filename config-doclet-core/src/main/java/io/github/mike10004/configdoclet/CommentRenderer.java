package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;

import java.util.Collection;

interface CommentRenderer {

    String render(Collection<? extends DocTree> docTrees);

}
