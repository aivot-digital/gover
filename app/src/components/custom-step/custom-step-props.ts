import {type StepElement} from '../../models/elements/steps/step-element';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {type SummaryStepElement} from '../../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {type SubmittedStepElement} from '../../models/elements/steps/submitted-step-element';
import type React from 'react';

export interface CustomStepProps {
    stepIndex: number;
    isFirstStep: boolean;
    isLastStep: boolean;

    step: StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement | SubmittedStepElement;
    children: React.ReactNode;

    onNext?: () => void;
    onPrevious?: () => void;

    navDirection?: 'next' | 'previous';
    setNavDirection?: (direction: 'next' | 'previous') => void;
    stepRefs: React.MutableRefObject<React.RefObject<HTMLDivElement>[]>;
    scrollContainerRef?: React.RefObject<HTMLDivElement>;

    isBusy: boolean;
    isDeriving: boolean;
}
