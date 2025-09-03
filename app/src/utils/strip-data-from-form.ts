import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isRootElement} from '../models/elements/root-element';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {LegacySystemIdpKeys} from '../data/legacy-system-idp-key';
import {FormDetailsResponseDTO} from '../modules/forms/dtos/form-details-response-dto';

export function stripDataFromForm(form: FormDetailsResponseDTO): FormDetailsResponseDTO {
    const strippedForm = {...form};

    strippedForm.id = 0;

    // Clear application
    strippedForm.destinationId = null;
    strippedForm.published = null;
    strippedForm.revoked = null;

    strippedForm.developingDepartmentId = 0;

    strippedForm.managingDepartmentId = null;
    strippedForm.responsibleDepartmentId = null;
    strippedForm.imprintDepartmentId = null;
    strippedForm.privacyDepartmentId = null;
    strippedForm.accessibilityDepartmentId = null;
    strippedForm.legalSupportDepartmentId = null;
    strippedForm.technicalSupportDepartmentId = null;

    strippedForm.pdfTemplateKey = null;

    strippedForm.paymentProviderKey = null;

    strippedForm.paymentProducts = [
        ...(strippedForm.paymentProducts ?? []),
    ].map(prd => ({
        ...prd,
        bookingData: [],
    }));

    strippedForm.themeId = null;

    strippedForm.identityVerificationRequired = false;
    strippedForm.identityProviders = [];

    strippedForm.rootElement = recursivelyApply(strippedForm.rootElement, (element) => {
        stripTestProtocol(element);

        if (isAnyInputElement(element)) {
            stripMappings(element);
        }

        return element;
    });

    return strippedForm;
}

function recursivelyApply<T extends AnyElement>(element: T, callback: <C extends AnyElement>(e: C) => C): T {
    const appliedElement = callback({
        ...element,
    });

    if (isRootElement(appliedElement)) {
        if (appliedElement.introductionStep != null) {
            appliedElement.introductionStep = callback({
                ...appliedElement.introductionStep,
            });
        }

        if (appliedElement.summaryStep != null) {
            appliedElement.summaryStep = callback({
                ...appliedElement.summaryStep,
            });
        }

        if (appliedElement.submitStep != null) {
            appliedElement.submitStep = callback({
                ...appliedElement.submitStep,
            });
        }
    }

    if (isAnyElementWithChildren(appliedElement) && appliedElement.children != null) {
        appliedElement.children = appliedElement
            .children
            .map((e) => recursivelyApply(e as any, callback));
    }

    return appliedElement;
}

function stripTestProtocol<T extends AnyElement>(element: T) {
    element.testProtocolSet = undefined;
}

function stripMappings<T extends AnyInputElement>(element: T) {
    const metadata = {
        ...element.metadata,
    };

    const originalMetadata = {
        ...metadata.identityMappings,
    };

    const cleanedMappings: Record<string, string> = {};
    for (const key of LegacySystemIdpKeys) {
        if (key in metadata) {
            cleanedMappings[key] = originalMetadata[key];
        }
    }
    metadata.identityMappings = cleanedMappings;
}