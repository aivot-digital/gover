import {useEffect, useLayoutEffect} from 'react';
import {
    activeEditor$,
    addComposerChild$,
    editorRootElementRef$,
    editorWrapperElementRef$,
    linkDialogState$,
    realmPlugin,
    useCellValues,
    usePublisher,
} from '@mdxeditor/editor';

function LinkDialogInteraction() {
    const [dialogState, editorWrapperRef, popupContainerRef, activeEditor] = useCellValues(
        linkDialogState$,
        editorWrapperElementRef$,
        editorRootElementRef$,
        activeEditor$,
    );
    const setDialogState = usePublisher(linkDialogState$);
    const isOpen = dialogState.type !== 'inactive';

    useEffect(() => {
        const editorWrapper = editorWrapperRef?.current;
        if (!isOpen || editorWrapper == null) {
            return;
        }

        const ownerDocument = editorWrapper.ownerDocument;
        const dismissOutside = (event: Event) => {
            const target = event.target;
            if (target instanceof Node && (
                editorWrapper.contains(target) || popupContainerRef?.current?.contains(target)
            )) {
                return;
            }

            // Cancelling through cancelLinkEdit$ would refocus the editor and reopen an existing link's preview.
            // An outside interaction must discard the draft without taking focus from its destination.
            setDialogState({type: 'inactive'});
        };
        const allowTabToLeave = (event: KeyboardEvent) => {
            if (event.key === 'Tab' && event.target instanceof Element &&
                popupContainerRef?.current?.contains(event.target) &&
                event.target.closest('[class*="_linkDialogPopoverContent_"]') != null) {
                // Radix loops Tab even in this non-modal popover. Preserve the browser's default tab order
                // so focus can leave the form; the focusin listener then dismisses it.
                event.stopPropagation();
            }
        };

        ownerDocument.addEventListener('pointerdown', dismissOutside, true);
        ownerDocument.addEventListener('focusin', dismissOutside, true);
        ownerDocument.addEventListener('keydown', allowTabToLeave, true);
        return () => {
            ownerDocument.removeEventListener('pointerdown', dismissOutside, true);
            ownerDocument.removeEventListener('focusin', dismissOutside, true);
            ownerDocument.removeEventListener('keydown', allowTabToLeave, true);
        };
    }, [isOpen, editorWrapperRef, popupContainerRef, setDialogState]);

    useLayoutEffect(() => {
        const editorRoot = activeEditor?.getRootElement();
        const anchor = editorWrapperRef?.current?.querySelector<HTMLElement>('[class*="_linkDialogAnchor_"]');
        if (dialogState.type === 'inactive' || editorRoot == null || anchor == null) {
            return;
        }

        const initialRootRect = editorRoot.getBoundingClientRect();
        const initialAnchorRect = anchor.getBoundingClientRect();
        const linkElement = dialogState.linkNodeKey
            ? activeEditor?.getElementByKey(dialogState.linkNodeKey)
            : null;
        if (linkElement != null) {
            // A collapsed selection gives MDXEditor a zero-width anchor at the selected text fragment.
            // Preview and edit should instead share the full link bounds, including formatted/wrapped text.
            const linkRect = linkElement.getBoundingClientRect();
            const rectangle = {
                top: dialogState.rectangle.top + linkRect.top - initialAnchorRect.top,
                left: dialogState.rectangle.left + linkRect.left - initialAnchorRect.left,
                width: linkRect.width,
                height: linkRect.height,
            };
            if (Object.entries(rectangle).some(([key, coordinate]) =>
                Math.abs(coordinate - dialogState.rectangle[key as keyof typeof rectangle]) > 0.5)) {
                setDialogState({...dialogState, rectangle});
                return;
            }
        }

        const offsetTop = initialRootRect.top - editorRoot.scrollTop - initialAnchorRect.top;
        const offsetLeft = initialRootRect.left - editorRoot.scrollLeft - initialAnchorRect.left;
        const followScroll = (event: Event) => {
            // The built-in listener handles window scrolling. Nested scroll containers do not bubble there.
            if (!(event.target instanceof Element) || !event.target.contains(editorRoot)) {
                return;
            }
            const rootRect = editorRoot.getBoundingClientRect();
            const anchorRect = anchor.getBoundingClientRect();
            const deltaTop = rootRect.top - editorRoot.scrollTop - anchorRect.top - offsetTop;
            const deltaLeft = rootRect.left - editorRoot.scrollLeft - anchorRect.left - offsetLeft;
            if (deltaTop === 0 && deltaLeft === 0) {
                return;
            }

            // Updating only the rectangle preserves the unsaved form draft and the edit/preview state.
            setDialogState({
                ...dialogState,
                rectangle: {
                    ...dialogState.rectangle,
                    top: dialogState.rectangle.top + deltaTop,
                    left: dialogState.rectangle.left + deltaLeft,
                },
            });
        };

        editorRoot.ownerDocument.addEventListener('scroll', followScroll, true);
        return () => editorRoot.ownerDocument.removeEventListener('scroll', followScroll, true);
    }, [dialogState, editorWrapperRef, activeEditor, setDialogState]);

    return null;
}

export const linkDialogInteractionPlugin = realmPlugin({
    init(realm) {
        realm.pub(addComposerChild$, LinkDialogInteraction);
    },
});
