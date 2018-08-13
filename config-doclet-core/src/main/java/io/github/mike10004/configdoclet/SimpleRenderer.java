package io.github.mike10004.configdoclet;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocTree;

import java.util.Collection;

class SimpleRenderer implements CommentRenderer {

    @Override
    public String render(Collection<? extends DocTree> docTrees) {
        StringBuilder sb = new StringBuilder();
        for (DocTree node : docTrees) {
            String outcome;
            if (node instanceof BlockTagTree) {
                String stringified = node.toString();
                String expectedPrefix = "@" + ((BlockTagTree)node).getTagName();
                if (stringified.startsWith(expectedPrefix)) {
                    outcome = StringUtils.removeStart(stringified, expectedPrefix);
                } else {
                    outcome = buildReallySimply(node);
                }
            } else {
                outcome = buildReallySimply(node);
            }
            sb.append(outcome);
        }
        return sb.toString().trim();
    }

    private String buildReallySimply(DocTree node) {
        ToStringHelper h = new ToStringHelper(node);
        h.add("toString", node.toString());
        return h.toString();
    }
}
