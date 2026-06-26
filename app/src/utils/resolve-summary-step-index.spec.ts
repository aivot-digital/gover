import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {type FormLayoutElement} from '../models/elements/form-layout-element';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type StepElement} from '../models/elements/steps/step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {resolveSummaryStepIndex} from './resolve-summary-step-index';

function createStepElement(id: string): StepElement {
    return {
        type: ElementType.Step,
        id,
        name: null,
        testProtocolSet: null,
        visibility: null,
        override: null,
        metadata: null,
        title: id,
        icon: null,
        children: [],
    };
}

function createIntroductionStepElement(id: string): IntroductionStepElement {
    return {
        type: ElementType.IntroductionStep,
        id,
        name: null,
        testProtocolSet: null,
        visibility: null,
        override: null,
        metadata: null,
        initiativeName: null,
        initiativeLogoLink: null,
        initiativeLink: null,
        teaserText: null,
        organization: null,
        eligiblePersons: null,
        supportingDocuments: null,
        documentsToAttach: null,
        expiring: null,
        expectedCosts: null,
        privacyText: null,
        children: [],
    };
}

function createSummaryStepElement(id: string): SummaryStepElement {
    return {
        type: ElementType.SummaryStep,
        id,
        name: null,
        testProtocolSet: null,
        visibility: null,
        override: null,
        metadata: null,
    };
}

describe('resolveSummaryStepIndex', () => {
    it('should resolve the index from the visible root step order', () => {
        const intro = createIntroductionStepElement('intro');
        const hiddenStep = createStepElement('hidden');
        const visibleStep = createStepElement('visible');
        const summary = createSummaryStepElement('summary');
        const root: FormLayoutElement = {
            type: ElementType.FormLayout,
            id: 'root',
            name: null,
            testProtocolSet: null,
            visibility: null,
            override: null,
            metadata: null,
            tabTitle: null,
            children: [intro, hiddenStep, visibleStep, summary],
            offlineSubmissionText: null,
            offlineSignatureNeeded: null,
            publicTitle: null,
            showOnFormIndexPage: null,
            managingDepartmentId: null,
            responsibleDepartmentId: null,
            legalSupportDepartmentId: null,
            technicalSupportDepartmentId: null,
            imprintDepartmentId: null,
            privacyDepartmentId: null,
            accessibilityDepartmentId: null,
            themeId: null,
            pdfTemplateKey: null,
        };

        const derivedData = createDerivedRuntimeElementData({
            elementStates: {
                hidden: {
                    visible: false,
                },
            },
        });

        expect(resolveSummaryStepIndex(root, derivedData, visibleStep.id)).toBe(1);
    });

    it('should return -1 when the summary is rendered outside a form layout', () => {
        const derivedData = createDerivedRuntimeElementData();
        const groupRoot = {
            type: ElementType.GroupLayout,
            id: 'group-root',
            name: null,
            testProtocolSet: null,
            visibility: null,
            override: null,
            metadata: null,
            children: [],
        };

        expect(resolveSummaryStepIndex(groupRoot as any, derivedData, 'step-1')).toBe(-1);
    });
});
