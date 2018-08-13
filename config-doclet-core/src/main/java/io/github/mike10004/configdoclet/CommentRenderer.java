package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreeScanner;

import java.util.Collection;

interface CommentRenderer {

    String render(Collection<? extends DocTree> docTrees);

    static String concatenateText(Iterable<? extends DocTree> trees) {
        StringBuilder b = new StringBuilder();
        new DocTreeScanner<Void, Void>() {
            @Override
            public Void visitLiteral(LiteralTree node, Void aVoid) {
                visitText(node.getBody(), aVoid);
                return super.visitLiteral(node, aVoid);
            }

            @Override
            public Void visitText(TextTree node, Void aVoid) {
                b.append(node.getBody());
                return super.visitText(node, aVoid);
            }
        }.scan(trees, null);
        return b.toString();
    }

}
