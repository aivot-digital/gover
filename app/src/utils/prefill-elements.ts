import {Form} from '../models/entities/form';
import {IdentityValue} from '../modules/identity/models/identity-value';
import {CustomerInput} from '../models/customer-input';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {LegacySystemIdpKey} from '../data/legacy-system-idp-key';

export function prefillElements(form: Form, identityValue: IdentityValue): {
    input: CustomerInput,
    disabled: Record<string, boolean>
} {
    return prefillElement(form.root, identityValue);
}

function prefillElement(element: AnyElement, identityValue: IdentityValue): {
    input: CustomerInput,
    disabled: Record<string, boolean>
} {
    const input: CustomerInput = {
        [IdentityCustomerInputKey]: identityValue,
    };
    const disabled: Record<string, boolean> = {};

    if (isAnyInputElement(element)) {
        // TODO: Handle replicating list container elements
        const metadata = element.metadata;

        if (metadata != null) {
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

            const mapping = mappings[identityValue.idp];
            if (mapping != null) {
                const value = identityValue.userInfo[mapping];
                if (value != null) {
                    input[element.id] = value;
                }
            }
        }
    }

    if (isAnyElementWithChildren(element)) {
        for (const child of element.children) {
            const {
                input: childInput,
                disabled: childDisabled,
            } = prefillElement(child, identityValue);

            Object.assign(input, childInput);
            Object.assign(disabled, childDisabled);
        }
    }

    return {
        input,
        disabled,
    };
}
