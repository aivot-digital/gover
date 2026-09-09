import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {literalAuthoredValue, createDerivedRuntimeElementData} from '../../models/element-data';
import type {TextFieldElement} from '../../models/elements/form/input/text-field-element';
import type {BaseViewProps} from '../../views/base-view';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ViewDispatcherComponent} from './view-dispatcher.component';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from './view-dispatcher.context';

vi.mock('../../hooks/use-app-selector', () => ({useAppSelector: () => true}));
vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../element-tree-2/components/element-tree-inline-editor-context', () => ({
    useElementTreeInlineEditorContext: () => null,
}));
vi.mock('../../views', () => ({
    views: {
        [ElementType.Text]: (props: BaseViewProps<TextFieldElement, string>) => <input
            aria-label="Test value"
            value={props.value ?? ''}
            onChange={(event) => props.setValue(event.target.value)}
            onBlur={(event) => props.onBlur(event.target.value)}
        />,
    },
}));

describe('ViewDispatcherComponent authored values', () => {
    it.each([false, true])('keeps unchanged literals stable with input modes enabled: %s', (inputModesEnabled) => {
        const element: TextFieldElement = {
            ...generateElementWithDefaultValues(ElementType.Text),
            id: 'field',
            type: ElementType.Text,
            label: 'Test value',
            inputModePolicy: {allowedModes: ['Literal']},
        };
        const authoredElementValues = {field: literalAuthoredValue('existing')};
        const derivedData = createDerivedRuntimeElementData();
        const onChange = vi.fn();
        const onBlur = vi.fn();
        render(
            <ViewDispatcherContextProvider value={{
                mode: ViewDispatcherMode.Viewer,
                rootElement: element,
                allElements: [element],
                rootAuthoredElementValues: authoredElementValues,
                rootDerivedData: derivedData,
                inputModesEnabled,
            }}>
                <ViewDispatcherComponent
                    element={element}
                    authoredElementValues={authoredElementValues}
                    derivedData={derivedData}
                    onAuthoredElementValuesChange={onChange}
                    onElementBlur={onBlur}
                    onDerive={vi.fn()}
                    onEvent={vi.fn()}
                    onResetErrors={vi.fn()}
                    isBusy={false}
                    isDeriving={false}
                    suppressErrors={false}
                    derivationTriggerIdQueue={[]}
                />
            </ViewDispatcherContextProvider>,
        );

        const input = screen.getByRole('textbox', {name: 'Test value'});
        fireEvent.blur(input);
        expect(onBlur).not.toHaveBeenCalled();
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.change(input, {target: {value: 'changed'}});
        expect(onChange).toHaveBeenCalledExactlyOnceWith({field: literalAuthoredValue('changed')}, ['field']);
        fireEvent.blur(input, {target: {value: 'changed'}});
        expect(onBlur).toHaveBeenCalledExactlyOnceWith({field: literalAuthoredValue('changed')}, ['field']);
    });
});
