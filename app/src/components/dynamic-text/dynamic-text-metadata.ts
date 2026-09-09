import {useEffect} from 'react';
import {getDynamicTextSyntaxSegments} from './dynamic-text-syntax';

export interface DynamicTextVariableMetadata {
    category?: string;
    description?: string;
    label: string;
    origin?: string;
    reference: string;
}

export interface DynamicTextInputMethods {
    focus: () => void;
    insertVariableReference: (reference: string) => void;
}

export function getDynamicTextVariableReferences(value: string): string[] {
    return Array.from(new Set(
        getDynamicTextSyntaxSegments(value)
            .filter((segment) => segment.role === 'property')
            .map((segment) => segment.value),
    ));
}

export function getDynamicTextTokenTitle(
    value: string,
    metadata: readonly DynamicTextVariableMetadata[],
): string {
    const references = getDynamicTextVariableReferences(value);
    const metadataByReference = new Map(metadata.map((entry) => [entry.reference, entry]));
    const referenceDetails = references.map((reference) => {
        const entry = metadataByReference.get(reference);
        if (entry == null) {
            return reference;
        }

        return [
            entry.label,
            [entry.category, entry.origin == null ? null : `Erzeugt von ${entry.origin}`]
                .filter((part): part is string => part != null)
                .join(' · '),
            entry.description,
            entry.reference,
        ].filter((part): part is string => part != null && part.length > 0).join('\n');
    });

    return referenceDetails.length > 0
        ? referenceDetails.join('\n\n')
        : value.replaceAll(/\s+/g, ' ').trim();
}

export function useDynamicTextTokenTitles(
    container: HTMLElement | null,
    metadata: readonly DynamicTextVariableMetadata[] | null | undefined,
) {
    // TODO(input-modes): Keep title as pointer enhancement, but expose metadata through a keyboard- and
    // touch-accessible action for the dynamic-text token at the current caret position.
    useEffect(() => {
        if (container == null) {
            return;
        }

        const updateTitles = () => {
            for (const token of Array.from(
                container.querySelectorAll<HTMLElement>('.dynamic-text-token'),
            )) {
                token.title = getDynamicTextTokenTitle(token.textContent ?? '', metadata ?? []);
            }
        };

        updateTitles();
        const observer = new MutationObserver(updateTitles);
        observer.observe(container, {
            childList: true,
            characterData: true,
            subtree: true,
        });

        return () => observer.disconnect();
    }, [container, metadata]);
}
