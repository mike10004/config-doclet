package io.github.mike10004.configdoclet;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

class SimpleRenderer implements CommentRenderer {

    public SimpleRenderer() {
    }

    @Override
    public String render(Collection<? extends DocTree> docTrees) {
        StringBuilder sb = new StringBuilder();
        for (DocTree node : docTrees) {
            String outcome;
            if (node instanceof BlockTagTree) {
                String stringified = node.toString();
                String tagName = ((BlockTagTree)node).getTagName();
                if (ConfigDoclet.TAG_CFG_EXAMPLE.equals(tagName)) {
                    outcome = CommentRenderer.concatenateText(((UnknownBlockTagTree)node).getContent());
                } else {
                    String expectedPrefix = "@" + tagName;
                    if (stringified.startsWith(expectedPrefix)) {
                        outcome = StringUtils.removeStart(stringified, expectedPrefix);
                    } else {
                        outcome = buildReallySimply(node);
                    }
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
