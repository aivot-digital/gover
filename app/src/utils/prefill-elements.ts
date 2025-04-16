import {Form} from '../models/entities/form';
import {IdentityValue} from '../modules/identity/models/identity-value';
import {CustomerInput} from '../models/customer-input';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {LegacySystemIdpKey} from '../data/legacy-system-idp-key';
import {resolveId} from './id-utils';
import {ElementType} from '../data/element-type/element-type';

type PrefilledCustomerInput = Partial<CustomerInput>;

export function prefillElements(
    form: Form,
    identityValue: IdentityValue,
    customerInput: CustomerInput,
): PrefilledCustomerInput {
    return {
        [IdentityCustomerInputKey]: identityValue,
        ...prefillElement(form.root, identityValue, customerInput, undefined),
    };
}

function prefillElement(
    element: AnyElement,
    identityValue: IdentityValue,
    customerInput: CustomerInput,
    idPrefix: string | undefined,
): PrefilledCustomerInput {
    const resolvedId = resolveId(element.id, idPrefix);
    let prefilledCustomerInput: PrefilledCustomerInput = {};

    if (isAnyInputElement(element)) {
        const userinfoValue = getUserinfoValue(element, identityValue);
        if (userinfoValue != null) {
            prefilledCustomerInput[resolvedId] = userinfoValue;
        }
    }

    if (isAnyElementWithChildren(element)) {
        if (element.type === ElementType.ReplicatingContainer) {
            const childIds: string[] | null | undefined = customerInput[resolvedId];
            if (childIds != null) {
                for (const childId of childIds) {
                    for (const child of element.children) {
                        const resolvedChildId = resolveId(resolvedId, childId);

                        const prefilledChildInput = prefillElement(child, identityValue, customerInput, resolvedChildId);
                        prefilledCustomerInput = {
                            ...prefilledCustomerInput,
                            ...prefilledChildInput,
                        };
                    }
                }
            }
        } else {
            for (const child of element.children) {
                const prefilledChildInput = prefillElement(child, identityValue, customerInput, idPrefix);
                prefilledCustomerInput = {
                    ...prefilledCustomerInput,
                    ...prefilledChildInput,
                };
            }
        }
    }

    return prefilledCustomerInput;
}

function getUserinfoValue(element: AnyInputElement, identityValue: IdentityValue): string | undefined {
    const metadataMapping = getMetadataMapping(element, identityValue.metadataIdentifier);
    if (metadataMapping == null) {
        return undefined;
    }

    const value: string | null | undefined = identityValue.userInfo[metadataMapping];

    return value ?? undefined;
}

export function getMetadataMapping(element: AnyInputElement, idpMetadataIdentifier: string): string | undefined {
    const metadata = element.metadata;

    if (metadata == null) {
        return undefined;
    }

    const {
        bayernIdMapping,
        bundIdMapping,
        shIdMapping,
        mukMapping,
        identityMappings,
    } = metadata;

    const mappings = identityMappings ?? {};

    if (bayernIdMapping != null) {
        mappings[LegacySystemIdpKey.BayernId] = bayernIdMapping;
    }
    if (bundIdMapping != null) {
        mappings[LegacySystemIdpKey.BundId] = bundIdMapping;
    }
    if (shIdMapping != null) {
        mappings[LegacySystemIdpKey.ShId] = shIdMapping;
    }
    if (mukMapping != null) {
        mappings[LegacySystemIdpKey.Muk] = mukMapping;
    }

    const mapping: string | null | undefined = mappings[idpMetadataIdentifier];

    return mapping ?? undefined;
}
