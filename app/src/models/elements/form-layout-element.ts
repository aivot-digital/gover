import {ElementType} from '../../data/element-type/element-type';
import {type BaseElement} from './base-element';
import {type IntroductionStepElement} from './steps/introduction-step-element';
import {type SummaryStepElement} from './steps/summary-step-element';
import {type SubmitStepElement} from './steps/submit-step-element';
import {AnyElement} from './any-element';
import {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

export interface FormLayoutElement extends BaseElement<ElementType.FormLayout> {
    tabTitle: string | null | undefined;
    children: AnyElement[] | null | undefined;

    offlineSubmissionText: string | null | undefined;
    offlineSignatureNeeded: boolean | null | undefined;

    publicTitle: string | null | undefined;
    showOnFormIndexPage: boolean | null | undefined;

    managingDepartmentId: number | null | undefined;
    responsibleDepartmentId: number | null | undefined;
    legalSupportDepartmentId: number | null | undefined;
    technicalSupportDepartmentId: number | null | undefined;
    imprintDepartmentId: number | null | undefined;
    privacyDepartmentId: number | null | undefined;
    accessibilityDepartmentId: number | null | undefined;
    formSpecificPrivacyStatement: string | null | undefined;
    formSpecificAccessibilityStatement: string | null | undefined;

    themeId: number | null | undefined;

    pdfTemplateKey: string | null | undefined;
}

export function isFormLayoutElement(obj: any): obj is FormLayoutElement {
    return obj != null && obj.type === ElementType.FormLayout;
}

export function resolveFormNodeName(layout: FormLayoutElement | undefined | null, processVersion: ProcessVersionEntity): string {
    if (layout != null && isStringNotNullOrEmpty(layout.publicTitle)) {
        return layout.publicTitle!;
    }

    return processVersion.publicTitle;
}

export function resolveFormNodeTabTitle(layout: FormLayoutElement | undefined | null, processVersion: ProcessVersionEntity): string {
    if (layout != null && isStringNotNullOrEmpty(layout.tabTitle)) {
        return layout.tabTitle!;
    }

    return resolveFormNodeName(layout, processVersion);
}
