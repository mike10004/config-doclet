package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

class CollectionLinkResolver implements LinkResolver {

    private final Collection<VariableElement> elements;
    private final Map<VariableElement, String> signatureCache;

    public CollectionLinkResolver(Collection<VariableElement> elements) {
        this.elements = elements;
        signatureCache = new HashMap<>();
    }

    @Nullable
    @Override
    public VariableElement resolve(Element context, String signature) {
        requireNonNull(signature, "signature");
        return elements.stream().filter(element -> {
            String elSignature = getElementSignature(element);
            return signature.equals(elSignature);
        }).findFirst().orElse(null);
    }

    private String getElementSignature(VariableElement el) {
        return signatureCache.computeIfAbsent(el, CollectionLinkResolver::constructSignature);
    }

    static String constructSignature(Element element) {
        List<Element> lineage = new ArrayList<>();
        do {
            lineage.add(element);
            element = element.getEnclosingElement();
        } while (element != null);
        Collections.reverse(lineage);
        StringBuilder sb = new StringBuilder();
        for (Element el : lineage) {
            if (!isSignaturePart(el)) {
                continue;
            }
            sb.append(getPrefix(el));
            Name name = el.getSimpleName();
            sb.append(name.toString());
        }
        return sb.toString();
    }

    private static boolean isSignaturePart(Element el) {
        switch (el.getKind()) {
            case PACKAGE:
            case CLASS:
            case METHOD:
            case FIELD:
                return true;
            default:
                return false;
        }
    }

    private static String getPrefix(Element el) {
        return getPrefix(el.getKind());
    }

    static String getPrefix(ElementKind kind) {
        switch (kind) {
            case PACKAGE:
                return "";
            case CLASS:
                return ".";
            case METHOD:
            case FIELD:
                return "#";
            default:
                return "";
        }
    }

}
