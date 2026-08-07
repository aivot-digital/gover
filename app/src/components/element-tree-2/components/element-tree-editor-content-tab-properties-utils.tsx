import {Typography} from '@mui/material';
import React, {type ReactNode} from 'react';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {createElementEditorNavigationLink} from '../../../hooks/use-element-editor-navigation';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';
import {type ElementWithParents, generateElementNameWithParent} from '../../../utils/flatten-elements';
import {isStringNullOrEmpty} from '../../../utils/string-utils';

export function collectHttpMappingProblems(current: ElementWithParents, allElements: ElementWithParents[]): ReactNode[] {
    const {element} = current;

    if (!isAnyInputElement(element)) {
        return [];
    }

    const elementDestinationKey = resolveEffectiveDestinationKey(current);
    if (elementDestinationKey == null) {
        return [];
    }

    const problems: ReactNode[] = [];

    for (const ot of allElements) {
        const {
            element: otherElement,
        } = ot;

        if (element.id === otherElement.id) {
            continue;
        }

        if (!isAnyInputElement(otherElement)) {
            continue;
        }

        const otherElementDestinationKey = resolveEffectiveDestinationKey(ot);
        if (otherElementDestinationKey == null) {
            continue;
        }

        if (isReplicatingContainerAncestorPair(current, ot)) {
            continue;
        }

        if (otherElementDestinationKey === elementDestinationKey) {
            const otherElementName = generateElementNameWithParent(ot);

            problems.push(
                <>
                    <Typography>
                        Der Datenschlüssel <strong>„{elementDestinationKey}”</strong> wird bereits von
                        dem Formularelement <a href={createElementEditorNavigationLink(otherElement.id, DefaultTabs.properties)}>„{otherElementName}”</a> verwendet.
                        Dies führt dazu, dass die Daten gegebenenfalls überschrieben werden. Stellen Sie sicher, dass
                        dies ein beabsichtigtes Verhalten ist.
                    </Typography>
                </>,
            );
            continue;
        }

        if (destinationKeysOverlap(otherElementDestinationKey, elementDestinationKey)) {
            const otherElementName = generateElementNameWithParent(ot);

            const overlapDescription = describeDestinationKeyOverlap(elementDestinationKey, otherElementDestinationKey);

            problems.push(
                <>
                    <Typography gutterBottom>
                        Der Datenschlüssel <strong>„{elementDestinationKey}”</strong> überschneidet sich
                        mit dem Datenschlüssel <strong>„{otherElementDestinationKey}”</strong> des
                        Formularelements <a href={createElementEditorNavigationLink(otherElement.id, DefaultTabs.metadata)}>„{otherElementName}”</a>.
                        {overlapDescription}
                    </Typography>
                    <Typography>
                        Dies kann zu Problemen bei der Datenverarbeitung führen. Bitte passen Sie die Schlüssel an, um
                        Überschneidungen zu vermeiden oder stellen Sie sicher, dass nicht beide Elemente gleichzeitig
                        verwendet werden.
                    </Typography>
                </>,
            );
        }
    }

    return problems;
}

export function resolveEffectiveDestinationKey({element, parents}: ElementWithParents): string | null {
    if (!isAnyInputElement(element)) {
        return null;
    }

    const destinationKey = normalizeDestinationKey(element.destinationKey);
    if (destinationKey == null) {
        return null;
    }

    const parentPrefix = resolveReplicatingParentDestinationKeyPrefix(parents);
    if (parentPrefix == null) {
        return null;
    }

    return parentPrefix.length > 0 ? `${parentPrefix}.${destinationKey}` : destinationKey;
}

export function destinationKeysOverlap(firstKey: string, secondKey: string): boolean {
    const firstSegments = firstKey.split('.');
    const secondSegments = secondKey.split('.');
    const comparedSegmentCount = Math.min(firstSegments.length, secondSegments.length);

    for (let index = 0; index < comparedSegmentCount; index++) {
        if (!destinationKeySegmentsOverlap(firstSegments[index], secondSegments[index])) {
            return false;
        }
    }

    return true;
}

function resolveReplicatingParentDestinationKeyPrefix(parents: ElementWithParents['parents']): string | null {
    const segments: string[] = [];

    for (const parent of parents) {
        if (!isReplicatingContainerLayout(parent)) {
            continue;
        }

        const destinationKey = normalizeDestinationKey(parent.destinationKey);
        if (destinationKey == null) {
            return null;
        }

        segments.push(destinationKey, '*');
    }

    return segments.join('.');
}

function normalizeDestinationKey(destinationKey: string | null | undefined): string | null {
    if (isStringNullOrEmpty(destinationKey)) {
        return null;
    }

    return destinationKey!.trim();
}

function isReplicatingContainerAncestorPair(first: ElementWithParents, second: ElementWithParents): boolean {
    return isReplicatingContainerAncestor(first, second) || isReplicatingContainerAncestor(second, first);
}

function isReplicatingContainerAncestor(candidate: ElementWithParents, descendant: ElementWithParents): boolean {
    return isReplicatingContainerLayout(candidate.element) &&
        descendant.parents.some((parent) => parent.id === candidate.element.id);
}

function destinationKeySegmentsOverlap(firstSegment: string, secondSegment: string): boolean {
    return firstSegment === secondSegment || firstSegment === '*' || secondSegment === '*';
}

function describeDestinationKeyOverlap(elementDestinationKey: string, otherElementDestinationKey: string): string {
    if (otherElementDestinationKey.startsWith(elementDestinationKey + '.')) {
        return ' Das andere Element schreibt in ein Unterattribut des aktuellen Elements.';
    }

    if (elementDestinationKey.startsWith(otherElementDestinationKey + '.')) {
        return ' Das aktuelle Element schreibt in ein Unterattribut des anderen Elements.';
    }

    return ' Die Schlüssel können auf denselben Pfad zeigen.';
}
