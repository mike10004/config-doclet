package io.github.mike10004.configdoclet;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

interface LinkResolver {
    @Nullable
    VariableElement resolve(Element context, String signature);
}
