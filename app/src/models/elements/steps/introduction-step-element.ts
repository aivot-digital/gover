import {BaseElement} from '../base-element';
import {ElementType} from '../../../data/element-type/element-type';
import {AnyFormElement} from '../form/any-form-element';

export interface IntroductionStepElement extends BaseElement<ElementType.IntroductionStep> {
    initiativeName: string | null | undefined;
    initiativeLogoLink: string | null | undefined;
    initiativeLink: string | null | undefined;
    teaserText: string | null | undefined;
    organization: string | null | undefined;
    eligiblePersons: string[] | null | undefined;
    supportingDocuments: string[] | null | undefined;
    documentsToAttach: string[] | null | undefined;
    expectedCosts: string | null | undefined;
    privacyText: string | null | undefined;
    children: AnyFormElement[] | null | undefined;
}
