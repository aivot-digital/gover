import {Form} from '../models/entities/form';
import {ApplicationStatus} from '../data/application-status';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isRootElement} from '../models/elements/root-element';

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
    //strippedForm.paymentDescription = undefined;
    //strippedForm.paymentPurpose = undefined;
    strippedForm.paymentEndpointId = undefined;
    strippedForm.paymentOriginatorId = undefined;
    strippedForm.products = [
        ...strippedForm.products ?? []
    ].map(prd => ({
        ...prd,
        bookingData: [],
    }))

    strippedForm.themeId = null;

    strippedForm.root = recursivelyStripTestProtocol(strippedForm.root);

    return strippedForm;
}

function recursivelyStripTestProtocol<T extends AnyElement>(element: T): T {
    const strippedElement = {
        ...element,
        testProtocolSet: undefined,
    };

    if (isRootElement(strippedElement)) {
        strippedElement.introductionStep = {
            ...strippedElement.introductionStep,
            testProtocolSet: undefined,
        };
        strippedElement.summaryStep = {
            ...strippedElement.summaryStep,
            testProtocolSet: undefined,
        };
        strippedElement.submitStep = {
            ...strippedElement.submitStep,
            testProtocolSet: undefined,
        };
    }

    if (isAnyElementWithChildren(strippedElement)) {
        // @ts-ignore
        strippedElement.children = strippedElement
            .children
            .map(recursivelyStripTestProtocol);
    }

    return strippedElement;
}