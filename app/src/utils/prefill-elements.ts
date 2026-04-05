import {AnyElement} from '../models/elements/any-element';
import {AuthoredElementValues} from '../models/element-data';
import {IdentityData} from '../modules/identity/models/identity-data';
import {mapAuthoredElementValues} from './element-data-utils';
import {isStringNullOrEmpty} from './string-utils';

export function prefillIdentityData(
    element: AnyElement,
    elementData: AuthoredElementValues,
    identityData: IdentityData,
): AuthoredElementValues {
    const {metadataIdentifier, attributes} = identityData;

    const updatedElementData = mapAuthoredElementValues(element, elementData, (element, value) => {
        const identityMapping = getMetadataMapping(element, metadataIdentifier);
        if (identityMapping == null) {
            return value;
        }

        const identityValue = attributes[identityMapping];
        if (identityValue == null) {
            return value;
        }

        return identityValue;
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
