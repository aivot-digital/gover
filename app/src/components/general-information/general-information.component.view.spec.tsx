import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../view-dispatcher/view-dispatcher.context';
import {GeneralInformationComponentView} from './general-information.component.view';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('GeneralInformationComponentView', () => {
    it('requires the privacy acknowledgment without showing the former required-field legend', () => {
        const element = generateElementWithDefaultValues(ElementType.IntroductionStep);
        const rootElement = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
        rootElement.children = [element];
        const derivedData = createDerivedRuntimeElementData();

        render(
            <ViewDispatcherContextProvider value={{
                mode: ViewDispatcherMode.Viewer,
                rootElement,
                allElements: [rootElement, element],
                rootAuthoredElementValues: {},
                rootDerivedData: derivedData,
            }}>
                <GeneralInformationComponentView
                    element={element}
                    isBusy={false}
                    isDeriving={false}
                    setValue={vi.fn()}
                    onBlur={vi.fn()}
                    authoredElementValues={{}}
                    onAuthoredElementValuesChange={vi.fn()}
                    derivedData={derivedData}
                    onDerive={vi.fn()}
                    onEvent={vi.fn()}
                    onResetErrors={vi.fn()}
                    suppressErrors={false}
                    derivationTriggerIdQueue={[]}
                />
            </ViewDispatcherContextProvider>,
        );

        expect(screen.getByRole('checkbox', {
            name: 'Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen.',
        })).toBeRequired();
        expect(screen.queryByText(/mit Stern.*gekennzeichneten Felder/)).not.toBeInTheDocument();
    });
});
