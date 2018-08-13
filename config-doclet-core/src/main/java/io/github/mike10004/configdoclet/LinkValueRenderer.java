package io.github.mike10004.configdoclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class LinkValueRenderer implements CommentRenderer {

    private final VariableElement element;
    private final LinkResolver linkResolver;
    private final RenderMode renderMode;
    private final CommentRenderer labelRenderer;

    public LinkValueRenderer(VariableElement element, LinkResolver linkResolver, RenderMode renderMode) {
        this.element = element;
        this.renderMode = renderMode;
        this.linkResolver = linkResolver;
        labelRenderer = new BasicTextCommentRenderer();
    }

    public enum RenderMode {
        VALUE_ONLY,
        PARENTHESIZED_VALUE
    }

    @Nullable
    private VariableElement findVariableElementForSignature(String signature) {
        signature = qualifySignature(signature, element);
        return linkResolver.resolve(element, signature);
    }

    @Nullable
    private String resolveValue(LinkTree link) {
        String signature = link.getReference().getSignature();
        if (signature != null) {
            @Nullable VariableElement element = findVariableElementForSignature(signature);
            if (element != null) {
                Object constValue = element.getConstantValue();
                String defaultValue = null;
                if (constValue != null) {
                    defaultValue = constValue.toString();
                }
                return defaultValue;
            }
        }
        return null;
    }

    @Override
    public String render(Collection<? extends DocTree> content) {
        List<? extends DocTree> docTrees = asList(content);
        if (!docTrees.isEmpty() && docTrees.get(0) instanceof LinkTree) {
            LinkTree link = (LinkTree) docTrees.get(0);
            @Nullable String value = resolveValue(link);
            if (renderMode == RenderMode.VALUE_ONLY) {
                return value;
            } else {
                List<? extends DocTree> labelTree = link.getLabel();
                String label = labelTree.isEmpty()
                    ? link.getReference().getSignature()
                    : labelRenderer.render(labelTree);
                if (value == null) {
                    return label;
                } else {
                    return String.format("%s (%s)", label, value);
                }
            }
        }
        return null;
    }

    private <T> List<T> asList(Collection<T> items) {
        return items instanceof List
                ? (List<T>) items
                : new ArrayList<>(items);
    }

    static String qualifySignature(String signature, Element element) {
        String fieldPrefix = CollectionLinkResolver.getPrefix(ElementKind.FIELD);
        if (signature.startsWith(fieldPrefix)) {
            String parentSignature = CollectionLinkResolver.constructSignature(element.getEnclosingElement());
            return parentSignature + signature;
        }
        return signature;
    }

}
