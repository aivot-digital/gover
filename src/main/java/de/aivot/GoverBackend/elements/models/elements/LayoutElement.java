package de.aivot.GoverBackend.elements.models.elements;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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

    default Optional<? extends BaseElement> findChild(Predicate<BaseElement> pred) {
        var matchingDirectChild = getChildren()
                .stream()
                .filter(pred)
                .findFirst();

        if (matchingDirectChild.isPresent()) {
            return matchingDirectChild;
        }

        return getChildren()
                .stream()
                .map(s -> {
                    if (s instanceof LayoutElement<?> sC) {
                        return sC.findChild(pred);
                    } else {
                        return Optional.<BaseElement>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    default <C extends BaseElement> Optional<C> findChild(@Nonnull String childId, @Nonnull Class<C> clazz) {
        var child = findChild(childId);
        if (child.isPresent() && clazz.isAssignableFrom(child.get().getClass())) {
            return Optional.of(clazz.cast(child.get()));
        }
        return Optional.empty();
    }

    default LayoutElement<T> insertChildBefore(@Nonnull T child, @Nonnull String beforeChildId) {
        var newChildren = new LinkedList<T>();
        for (var c : getChildren()) {
            if (Objects.equals(c.getId(), beforeChildId)) {
                newChildren.add(child);
            }
            newChildren.add(c);
        }
        setChildren(newChildren);
        return this;
    }

    default LayoutElement<T> insertChildAfter(@Nonnull T child, @Nonnull String afterChildId) {
        var newChildren = new LinkedList<T>();
        for (var c : getChildren()) {
            newChildren.add(c);
            if (Objects.equals(c.getId(), afterChildId)) {
                newChildren.add(child);
            }
        }
        setChildren(newChildren);
        return this;
    }
}
