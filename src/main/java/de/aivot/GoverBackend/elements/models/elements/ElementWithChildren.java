package de.aivot.GoverBackend.elements.models.elements;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ElementWithChildren<T extends BaseElement> {
    List<T> getChildren();
    ElementWithChildren<T> setChildren(List<T> children);

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
                    if (s instanceof ElementWithChildren<?> sC) {
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
