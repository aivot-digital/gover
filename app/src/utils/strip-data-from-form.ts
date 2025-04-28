import {Form} from '../models/entities/form';
import {ApplicationStatus} from '../data/application-status';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isRootElement} from '../models/elements/root-element';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {LegacySystemIdpKeys} from '../data/legacy-system-idp-key';

export function stripDataFromForm(form: Form): Form {
    const strippedForm = {...form};

    strippedForm.id = 0;

    // Clear application
    strippedForm.destinationId = null;
    strippedForm.status = ApplicationStatus.Drafted;

    strippedForm.developingDepartmentId = 0;

    strippedForm.managingDepartmentId = null;
    strippedForm.responsibleDepartmentId = null;
    strippedForm.imprintDepartmentId = null;
    strippedForm.privacyDepartmentId = null;
    strippedForm.accessibilityDepartmentId = null;
    strippedForm.legalSupportDepartmentId = null;
    strippedForm.technicalSupportDepartmentId = null;

    strippedForm.pdfBodyTemplateKey = null;

    strippedForm.paymentProvider = undefined;

    strippedForm.products = [
        ...strippedForm.products ?? [],
    ].map(prd => ({
        ...prd,
        bookingData: [],
    }));

    strippedForm.themeId = null;

    strippedForm.identityRequired = false;
    strippedForm.identityProviders = [];

    strippedForm.root = recursivelyApply(strippedForm.root, (element) => {
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
        appliedElement.introductionStep = callback({
            ...appliedElement.introductionStep,
        });

        appliedElement.summaryStep = callback({
            ...appliedElement.summaryStep,
        });

        appliedElement.submitStep = callback({
            ...appliedElement.submitStep,
        });
    }

    if (isAnyElementWithChildren(appliedElement)) {
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