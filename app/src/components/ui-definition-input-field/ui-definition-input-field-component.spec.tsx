import React, {type PropsWithChildren} from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {type UiDefinitionInputFieldElementItem} from '../../models/elements/form/input/ui-definition-input-field-element';
import {UiDefinitionInputFieldComponent} from './ui-definition-input-field-component';
import {type ElementDerivationOptions} from '../../modules/elements/elements-api-service';
import {type AuthoredElementValues, getLiteralElementValue, literalAuthoredValue} from '../../models/element-data';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type TextFieldElement} from '../../models/elements/form/input/text-field-element';

vi.mock('../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(),
}));

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../hooks/use-app-selector', () => ({
    useAppSelector: () => false,
}));

vi.mock('../../hooks/use-element-editor-navigation', () => ({
    useElementEditorNavigation: () => ({navigateToElementEditor: vi.fn()}),
}));

vi.mock('../code-editor/code-editor', () => ({
    CodeEditor: () => <div/>,
}));

vi.mock('allotment', () => ({
    Allotment: Object.assign(
        ({children}: PropsWithChildren) => <div>{children}</div>,
        {Pane: ({children}: PropsWithChildren) => <div>{children}</div>},
    ),
}));

vi.mock('../element-tree-2/element-tree', () => ({
    ElementTree: ({value, onChange}: {value: GroupLayout; onChange: (value: GroupLayout) => void}) => (
        <button onClick={() => onChange({
            ...value,
            children: value.children?.map(child => ({...child, minCharacters: 3})),
        })}>
            Mindestlänge ändern
        </button>
    ),
}));

describe('UiDefinitionInputFieldComponent', () => {
    it('validates current inputs and modeling only on request and clears edited errors in both places', async () => {
        const user = userEvent.setup();
        const value: GroupLayout = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'root',
            children: [{
                ...generateElementWithDefaultValues(ElementType.Text),
                id: 'name',
                label: 'Name',
                required: true,
            }],
        };
        const requiredError = 'Bitte geben Sie einen Namen ein.';
        const lengthError = 'Bitte geben Sie mindestens drei Zeichen ein.';
        const derive = vi.fn(async (
            uiDefinition: UiDefinitionInputFieldElementItem,
            authoredElementValues: AuthoredElementValues,
            derivationOptions: ElementDerivationOptions,
        ) => {
            const name = getLiteralElementValue<string>(authoredElementValues, 'name') ?? '';
            const field = (uiDefinition as GroupLayout).children?.[0] as TextFieldElement;
            const validate = !derivationOptions.skipErrorsForElementIds.includes('ALL');
            return {
                effectiveValues: {name},
                elementStates: {
                    root: {visible: true},
                    name: {
                        visible: true,
                        error: !validate ? null : !name ? requiredError
                            : name.length < (field.minCharacters ?? 0) ? lengthError : null,
                    },
                },
            };
        });

        render(<UiDefinitionInputFieldComponent
            label="Testoberfläche"
            value={value}
            onChange={vi.fn()}
            displayContext={ElementDisplayContext.StaffFacing}
            onDerive={derive}
        />);
        await user.click(screen.getByRole('button', {name: 'Bearbeiten'}));
        const input = await screen.findByRole('textbox', {name: 'Name'});
        const validate = screen.getByRole('button', {name: 'Eingaben validieren'});
        await waitFor(() => expect(input).toBeEnabled());
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(input).not.toHaveAttribute('aria-invalid', 'true');

        await user.click(validate);
        expect(await screen.findByRole('listitem')).toHaveTextContent(requiredError);
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAccessibleDescription(requiredError);
        expect(within(screen.getByRole('list')).getAllByRole('listitem')).toHaveLength(1);
        await waitFor(() => expect(validate).toBeEnabled());

        await user.type(input, 'A');
        // Blurring flushes the field's pending debounce before validating or changing the model.
        await user.tab();
        await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByText(lengthError)).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Mindestlänge ändern'}));
        await waitFor(() => expect(input).toBeEnabled());

        await user.click(validate);
        expect(await screen.findByRole('listitem')).toHaveTextContent(lengthError);
        expect(input).toHaveAccessibleDescription('Bitte geben Sie mindestens drei Zeichen ein. Noch mindestens zwei Zeichen');
        expect(derive).toHaveBeenLastCalledWith(
            expect.objectContaining({children: [expect.objectContaining({minCharacters: 3})]}),
            {name: literalAuthoredValue('A')},
            expect.objectContaining({skipErrorsForElementIds: []}),
            undefined,
        );
        await waitFor(() => expect(validate).toBeEnabled());

        await user.type(input, 'da');
        await user.click(validate);
        await waitFor(() => expect(validate).toBeEnabled());
        expect(derive).toHaveBeenLastCalledWith(
            expect.anything(),
            {name: literalAuthoredValue('Ada')},
            expect.objectContaining({skipErrorsForElementIds: []}),
            undefined,
        );
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(input).not.toHaveAttribute('aria-invalid', 'true');

        await user.clear(input);
        fireEvent.blur(input);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        await user.click(validate);
        expect(await screen.findByRole('listitem')).toHaveTextContent(requiredError);
    });

    it('keeps the summary and editor action separate from the field name', () => {
        render(
            <UiDefinitionInputFieldComponent
                label="Ergänzende Oberfläche"
                value={null}
                onChange={vi.fn()}
                hint="Hier kann eine weitere UI-Struktur modelliert werden."
                expectedRootType={ElementType.GroupLayout}
                displayContext={ElementDisplayContext.StaffFacing}
                labelAction={<button type="button">Eingabemodus</button>}
            />,
        );

        const summary = screen.getByRole('group', {name: 'Ergänzende Oberfläche – optional'});
        const action = screen.getByRole('button', {name: 'Bearbeiten'});

        expect(summary).toHaveAccessibleDescription('Hier kann eine weitere UI-Struktur modelliert werden.');
        expect(summary).toHaveTextContent('Keine UI-Definition konfiguriert');
        expect(action).toHaveAttribute('aria-haspopup', 'dialog');
        expect(action).toHaveAttribute('aria-expanded', 'false');
        expect(action).not.toHaveAttribute('aria-controls');
        expect(screen.getByRole('button', {name: 'Eingabemodus'})).toBeInTheDocument();
        expect(action).not.toHaveClass('MuiButton-outlined');
        expect(summary).not.toHaveAccessibleName(/Bearbeiten/);
        expect(getComputedStyle(summary).height).toBe('44px');
    });

    it('keeps a configured one-line summary at the standard control height', () => {
        const value = {
            id: 'root',
            type: ElementType.GroupLayout,
            children: [],
        } as unknown as UiDefinitionInputFieldElementItem;

        render(
            <UiDefinitionInputFieldComponent
                label="Ergänzende Oberfläche"
                value={value}
                onChange={vi.fn()}
                expectedRootType={ElementType.GroupLayout}
                displayContext={ElementDisplayContext.StaffFacing}
            />,
        );

        const summary = screen.getByRole('group', {name: 'Ergänzende Oberfläche – optional'});
        const summaryText = screen.getByText('0 Elemente enthalten');

        expect(getComputedStyle(summary).height).toBe('44px');
        expect(summaryText).toHaveStyle({
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
        });
    });
});
