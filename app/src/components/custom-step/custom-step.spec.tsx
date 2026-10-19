import React, {createRef} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {act, render, screen} from '@testing-library/react';
import {Stepper} from '@mui/material';
import {CustomStep} from './custom-step';
import {ElementType} from '../../data/element-type/element-type';

const settings = vi.hoisted(() => ({disableAutoScroll: false, reducedMotion: false}));

vi.mock('../../hooks/use-app-selector', () => ({
    useAppSelector: () => settings.disableAutoScroll,
}));
vi.mock('../view-dispatcher/view-dispatcher.context', () => ({
    useViewDispatcherContext: () => ({highlightedElementId: null}),
}));

function renderSteps(withContainer = true) {
    const scrollContainerRef = createRef<HTMLDivElement>();
    const stepRefs = {current: [createRef<HTMLDivElement>(), createRef<HTMLDivElement>(), createRef<HTMLDivElement>()]};
    const scroll = vi.fn();
    const tree = (active: number, direction?: 'next' | 'previous') => (
        <div ref={scrollContainerRef} style={{height: 400, overflowY: 'auto'}}>
            <div style={{position: 'relative'}}>
                <Stepper activeStep={active} orientation="vertical">
                    {[0, 1, 2].map((index) => (
                        <CustomStep
                            key={index}
                            step={{
                                id: `step-${index}`,
                                type: ElementType.Step,
                                title: `Step ${index}`,
                                children: [],
                                icon: null,
                                name: null,
                                testProtocolSet: null,
                                visibility: null,
                                override: null,
                                metadata: null,
                            }}
                            stepIndex={index}
                            isFirstStep={index === 0}
                            isLastStep={index === 2}
                            isSubmitStep={false}
                            active={active === index}
                            navDirection={direction}
                            stepRefs={stepRefs}
                            scrollContainerRef={withContainer ? scrollContainerRef : undefined}
                            isBusy={false}
                            isDeriving={false}
                        >Content {index}</CustomStep>
                    ))}
                </Stepper>
            </div>
        </div>
    );
    const result = render(tree(1));
    const container = scrollContainerRef.current!;
    container.scrollTo = scroll;
    container.scrollTop = 500;
    vi.spyOn(container, 'getBoundingClientRect').mockReturnValue({top: 100} as DOMRect);
    Object.defineProperty(container, 'clientTop', {value: 2});
    stepRefs.current.forEach((ref, index) => {
        // Layout is supplied explicitly because jsdom does not calculate geometry.
        // Deliberately differ from offsetTop to cover positioned form wrappers.
        Object.defineProperty(ref.current, 'offsetTop', {value: 10 + index});
        vi.spyOn(ref.current!, 'getBoundingClientRect').mockReturnValue({top: 200 + 100 * index} as DOMRect);
    });
    return {
        ...result,
        scroll,
        container,
        stepRefs,
        navigate: (active: number, direction: 'next' | 'previous') => result.rerender(tree(active, direction)),
    };
}

describe('CustomStep scrolling', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        settings.disableAutoScroll = false;
        settings.reducedMotion = false;
        vi.stubGlobal('matchMedia', () => ({matches: settings.reducedMotion}));
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it.each([false, true])('scrolls the container in both directions (reduced motion: %s)', (reducedMotion) => {
        settings.reducedMotion = reducedMotion;
        const windowScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
        const focus = vi.spyOn(HTMLElement.prototype, 'focus');
        const {navigate, scroll, container, stepRefs} = renderSteps();
        const behavior = reducedMotion ? 'auto' : 'smooth';
        expect(scroll).not.toHaveBeenCalled();

        navigate(2, 'next');
        // Forward scrolling waits until the collapsing step has updated its layout.
        expect(scroll).not.toHaveBeenCalled();
        vi.mocked(stepRefs.current[1].current!.getBoundingClientRect).mockReturnValue({top: 320} as DOMRect);
        act(() => {vi.runAllTimers();});
        expect(scroll).toHaveBeenLastCalledWith({top: 718, behavior});
        expect(screen.getByRole('heading', {name: 'Step 2'})).toHaveFocus();
        expect(focus).toHaveBeenLastCalledWith({preventScroll: true});

        scroll.mockClear();
        container.scrollTop = 700;
        navigate(1, 'previous');
        // Preserve the preceding-section anchor while the previous step expands.
        expect(scroll).toHaveBeenCalledExactlyOnceWith({top: 798, behavior});
        expect(screen.getByRole('heading', {name: 'Step 1'})).toHaveFocus();
        expect(focus).toHaveBeenLastCalledWith({preventScroll: true});
        act(() => {vi.runAllTimers();});
        expect(scroll).toHaveBeenCalledTimes(1);

        scroll.mockClear();
        navigate(0, 'previous');
        expect(scroll).toHaveBeenCalledExactlyOnceWith({top: 798, behavior});
        expect(windowScroll).not.toHaveBeenCalled();
    });

    it('uses document coordinates when no scroll container is supplied', () => {
        vi.spyOn(window, 'scrollY', 'get').mockReturnValue(600);
        const windowScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
        const {navigate, scroll} = renderSteps(false);
        navigate(2, 'next');
        act(() => {vi.runAllTimers();});
        expect(windowScroll).toHaveBeenLastCalledWith({top: 900, behavior: 'smooth'});
        navigate(1, 'previous');
        expect(windowScroll).toHaveBeenLastCalledWith({top: 800, behavior: 'smooth'});
        expect(scroll).not.toHaveBeenCalled();
    });

    it('respects disabled automatic scrolling', () => {
        settings.disableAutoScroll = true;
        const windowScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
        const focus = vi.spyOn(HTMLElement.prototype, 'focus');
        const {navigate, scroll} = renderSteps();
        navigate(2, 'next');
        act(() => {vi.runAllTimers();});
        navigate(1, 'previous');
        act(() => {vi.runAllTimers();});
        expect(scroll).not.toHaveBeenCalled();
        expect(windowScroll).not.toHaveBeenCalled();
        expect(focus).not.toHaveBeenCalled();
    });

    it('cancels deferred scrolling when the form unmounts', () => {
        const windowScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
        const {navigate, unmount, scroll} = renderSteps();
        navigate(2, 'next');
        unmount();
        act(() => {vi.runAllTimers();});
        expect(scroll).not.toHaveBeenCalled();
        expect(windowScroll).not.toHaveBeenCalled();
    });
});
