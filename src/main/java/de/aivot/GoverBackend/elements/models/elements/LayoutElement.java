package de.aivot.GoverBackend.elements.models.elements;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface LayoutElement<T extends BaseElement> {
    @Nonnull
    List<T> getChildren();

    @Nonnull
    LayoutElement<T> setChildren(@Nullable List<T> children);

    default LayoutElement<T> addChild(@Nonnull T child) {
        var newChildren = new LinkedList<T>(getChildren());
        newChildren.add(child);
        setChildren(newChildren);
        return this;
    }

    default Optional<? extends BaseElement> findChild(@Nonnull String childId) {
        var matchingDirectChild = getChildren()
                .stream()
                .filter(s -> Objects.equals(s.getId(), childId))
                .findFirst();

        if (matchingDirectChild.isPresent()) {
            return matchingDirectChild;
        }

        return getChildren()
                .stream()
                .map(s -> {
                    if (s instanceof LayoutElement<?> sC) {
                        return sC.findChild(childId);
                    } else {
                        return Optional.<BaseElement>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
