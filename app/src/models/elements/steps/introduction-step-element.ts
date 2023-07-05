import {BaseElement} from '../base-element';
import {ElementType} from '../../../data/element-type/element-type';

export interface IntroductionStepElement extends BaseElement<ElementType.IntroductionStep> {
    initiativeName?: string;
    initiativeLogoLink?: string;
    initiativeLink?: string;

    teaserText?: string;

    organization?: string;

    eligiblePersons?: string[];
    supportingDocuments?: string[];
    documentsToAttach?: string[];
    expectedCosts?: string;
}
