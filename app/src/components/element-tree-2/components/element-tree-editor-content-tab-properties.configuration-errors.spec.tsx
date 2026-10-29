import React, {useState} from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {type AnyElement} from '../../../models/elements/any-element';
import {type TextFieldElement} from '../../../models/elements/form/input/text-field-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {type ElementConfigurationErrors, ElementsApiService} from '../../../modules/elements/elements-api-service';
import {ElementTreeContextProvider} from '../element-tree-context';
import {ElementTreeEditorContextProvider} from './element-tree-editor-context';
import {ElementTreeEditorContentTabProperties} from './element-tree-editor-content-tab-properties';

vi.mock('../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn().mockResolvedValue(true)}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const TEXT_ERROR = 'Sie fordern mehr Zeichen, als Sie maximal zulassen.';

describe('ElementTreeEditorContentTabProperties configuration errors', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('shows the server errors of the unapplied draft at the affected fields', async () => {
        const validate = vi.spyOn(ElementsApiService.prototype, 'validateConfiguration')
            .mockImplementation(async (element): Promise<ElementConfigurationErrors> => {
                const text = element as TextFieldElement;
                const invalid = (text.minCharacters ?? 0) > (text.maxCharacters ?? 0);
                return {
                    properties: invalid ? {minCharacters: [TEXT_ERROR], maxCharacters: [TEXT_ERROR]} : {},
                    children: {},
                };
            });
        render(<TestProperties element={{
            ...generateElementWithDefaultValues(ElementType.Text),
            label: 'Vornamen',
            minCharacters: 6,
            maxCharacters: 5,
        } as AnyElement}/>);

        const minimum = screen.getByRole('textbox', {name: /^Minimalanzahl an Zeichen/});
        const maximum = screen.getByRole('textbox', {name: /^Maximalanzahl an Zeichen/});
        await waitFor(() => expect(minimum).toHaveAccessibleDescription(new RegExp(TEXT_ERROR)));
        expect(maximum).toHaveAccessibleDescription(new RegExp(TEXT_ERROR));

        fireEvent.change(minimum, {target: {value: '4'}});
        fireEvent.blur(minimum);

        await waitFor(() => expect(minimum).not.toHaveAccessibleDescription(new RegExp(TEXT_ERROR)));
        expect(maximum).not.toHaveAccessibleDescription(new RegExp(TEXT_ERROR));
        expect(validate).toHaveBeenLastCalledWith(expect.objectContaining({minCharacters: 4}), expect.any(AbortSignal));
    });
});

function TestProperties({element}: {element: AnyElement}) {
    const [currentElement, setCurrentElement] = useState(element);
    const [root] = useState<AnyElement>(() => generateElementWithDefaultValues(ElementType.FormLayout));

    return (
        <ElementTreeContextProvider value={{
            root,
            editable: true,
            allElements: [{element: currentElement, parents: [root]}],
            allowElementIdEditing: false,
            displayContext: ElementDisplayContext.CustomerFacing,
            scrollToElement: () => undefined,
            canDropElement: () => false,
            moveElement: () => undefined,
            expandCommand: {type: null, version: 0},
            initiallyExpandedSectionIds: new Set(),
        }}>
            <ElementTreeEditorContextProvider value={{
                currentElement,
                parents: [root],
                onChangeCurrentElement: setCurrentElement,
            }}>
                <ElementTreeEditorContentTabProperties/>
            </ElementTreeEditorContextProvider>
        </ElementTreeContextProvider>
    );
}
