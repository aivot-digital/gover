import {AnyElementWithChildren, isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {AnyElement} from '../../../models/elements/any-element';

export function removeChildElement<T extends AnyElementWithChildren>(element: T, childElement: AnyElement): T {
    if (element.children.includes(childElement as any)) {
        return {
            ...element,
            children: (element.children as AnyElement[]).filter((child: any) => child !== childElement),
        };
    }

    return {
        ...element,
        children: element.children.map((child) => {
            if (isAnyElementWithChildren(child)) {
                return removeChildElement(child, childElement);
            }

            return child;
        }),
    };
}