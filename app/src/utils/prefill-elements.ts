import {AnyElement} from '../models/elements/any-element';
import {ElementData, ElementDataObject} from '../models/element-data';
import {IdentityData} from '../modules/identity/models/identity-data';
import {mapElementData} from './element-data-utils';
import {isStringNullOrEmpty} from './string-utils';

export function prefillIdentityData(
    element: AnyElement,
    elementData: ElementData,
    identityData: IdentityData,
): ElementData {
    const {
        metadataIdentifier,
        attributes,
    } = identityData;

    const updatedElementData = mapElementData(element, elementData, (element, elementDataObject) => {
        if (elementDataObject == null) {
            return null;
        }

        const identityMapping = getMetadataMapping(element, metadataIdentifier);
        if (identityMapping == null) {
            return elementDataObject;
        }

        const identityValue = attributes[identityMapping];
        if (identityValue == null) {
            return elementDataObject;
        }

        return {
            ...elementDataObject,
            isPrefilled: true,
            inputValue: identityValue,
        } as ElementDataObject;
    });

    return updatedElementData;
}

export function getMetadataMapping(element: AnyElement, idpMetadataIdentifier: string): string | undefined {
    const metadata = element.metadata;

    if (metadata == null) {
        return undefined;
    }

    const {
        identityMappings,
    } = metadata;

    if (identityMappings == null) {
        return undefined;
    }

    const mapping: string | null | undefined = identityMappings[idpMetadataIdentifier];

    if (isStringNullOrEmpty(mapping)) {
        return undefined;
    }

    return mapping ?? undefined;
}
