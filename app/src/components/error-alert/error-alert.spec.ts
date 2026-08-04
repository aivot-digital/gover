import {describe, expect, it} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {collectErrors} from './error-alert';

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

describe('collectErrors', () => {
    it('should prefer the backend-provided public label for introduction step validation errors', () => {
        const element = createIntroductionStepElement('intro');
        const derivedData = createDerivedRuntimeElementData({
            elementStates: {
                intro: {
                    error: 'Bitte akzeptieren Sie die Hinweise zum Datenschutz.',
                    errorDetails: {
                        label: 'Datenschutzrechtliche Einwilligung',
                    },
                },
            },
        });

        expect(collectErrors(element, {}, derivedData)).toEqual([
            {
                id: 'intro',
                label: 'Datenschutzrechtliche Einwilligung',
                error: 'Bitte akzeptieren Sie die Hinweise zum Datenschutz.',
            },
        ]);
    });
});
