import {Form} from '../models/entities/form';
import {ApplicationStatus} from '../data/application-status';

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
    strippedForm.paymentDescription = undefined;
    strippedForm.paymentPurpose = undefined;
    strippedForm.paymentEndpointId = undefined;
    strippedForm.paymentOriginatorId = undefined;
    strippedForm.products = [];

    return strippedForm;
}