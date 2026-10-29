import React, {type PropsWithChildren} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
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

const recalculateReferencedIds = vi.hoisted(() => vi.fn(async (element: unknown) => element));
const validateConfiguration = vi.hoisted(() => vi.fn(async (): Promise<unknown> => ({properties: {}, children: {}})));

vi.mock('../../modules/elements/elements-api-service', () => ({
    ElementsApiService: class {
        recalculateReferencedIds = recalculateReferencedIds;
        validateConfiguration = validateConfiguration;
    },
}));

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

    it('re-derives elements that depend on process data written by an edited field', async () => {
        const user = userEvent.setup();
        const value: GroupLayout = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'root',
            children: [
                {
                    ...generateElementWithDefaultValues(ElementType.Text),
                    id: 'brand',
                    label: 'Marke',
                    destinationKey: 'fahrzeug.marke',
                },
                {
                    ...generateElementWithDefaultValues(ElementType.Text),
                    id: 'audiOnly',
                    label: 'Audi-Modell',
                    visibility: {
                        type: 'NoCode',
                        requirements: undefined,
                        conditionSet: undefined,
                        javascriptCode: undefined,
                        noCode: {
                            type: 'NoCodeExpression',
                            operatorIdentifier: 'equals',
                            operands: [
                                {type: 'NoCodeProcessDataReference', path: 'fahrzeug.marke'},
                                {type: 'NoCodeStaticValue', value: 'Audi'},
                            ],
                        },
                        // The element editor only recalculates the edited element and misses the writing input.
                        referencedIds: [],
                    },
                },
            ],
        };
        recalculateReferencedIds.mockImplementationOnce(async (element: unknown) => {
            const root = element as GroupLayout;
            return {
                ...root,
                children: root.children?.map(child => child.id !== 'audiOnly' || child.visibility == null ? child : {
                    ...child,
                    visibility: {...child.visibility, referencedIds: ['brand']},
                }),
            };
        });
        const derive = vi.fn(async (
            _uiDefinition: UiDefinitionInputFieldElementItem,
            authoredElementValues: AuthoredElementValues,
        ) => {
            const brand = getLiteralElementValue<string>(authoredElementValues, 'brand') ?? null;
            return {
                effectiveValues: {brand},
                elementStates: {
                    root: {visible: true},
                    brand: {visible: true},
                    audiOnly: {visible: brand === 'Audi'},
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

        expect(recalculateReferencedIds).toHaveBeenCalledWith(expect.objectContaining({
            id: 'root',
            children: [expect.objectContaining({id: 'brand'}), expect.objectContaining({id: 'audiOnly'})],
        }));
        const brandInput = await screen.findByRole('textbox', {name: /^Marke/});
        await waitFor(() => expect(brandInput).toBeEnabled());
        expect(screen.queryByRole('textbox', {name: /^Audi-Modell/})).not.toBeInTheDocument();

        await user.type(brandInput, 'Audi');
        await user.tab();

        expect(await screen.findByRole('textbox', {name: /^Audi-Modell/})).toBeInTheDocument();

        // Recalculated referenced IDs are no modeling change, so cancelling must not ask to discard changes.
        await user.click(screen.getByRole('button', {name: 'Abbrechen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('ignores referenced IDs recalculated for an outdated draft', async () => {
        const user = userEvent.setup();
        const value: GroupLayout = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout),
            id: 'root',
            children: [{
                ...generateElementWithDefaultValues(ElementType.Text),
                id: 'name',
                label: 'Name',
            }],
        };
        let resolveOutdatedRecalculation: (element: unknown) => void = () => undefined;
        recalculateReferencedIds.mockImplementationOnce(() => new Promise((resolve) => {
            resolveOutdatedRecalculation = resolve;
        }));
        const onChange = vi.fn();

        render(<UiDefinitionInputFieldComponent
            label="Testoberfläche"
            value={value}
            onChange={onChange}
            displayContext={ElementDisplayContext.StaffFacing}
        />);
        await user.click(screen.getByRole('button', {name: 'Bearbeiten'}));
        await user.click(await screen.findByRole('button', {name: 'Mindestlänge ändern'}));
        await act(async () => {
            resolveOutdatedRecalculation({...value, children: []});
        });
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
            children: [expect.objectContaining({id: 'name', minCharacters: 3})],
        }));
    });

    it('keeps an untouched unconfigured definition empty after recalculating its references', async () => {
        const user = userEvent.setup();
        let completeRecalculation: () => void = () => undefined;
        recalculateReferencedIds.mockImplementationOnce((element: unknown) => new Promise((resolve) => {
            // The backend serializes absent element properties as null.
            completeRecalculation = () => resolve({
                ...(element as GroupLayout),
                name: null,
                visibility: null,
                override: null,
                metadata: null,
            });
        }));
        const onChange = vi.fn();

        render(<UiDefinitionInputFieldComponent
            label="Testoberfläche"
            value={null}
            onChange={onChange}
            displayContext={ElementDisplayContext.StaffFacing}
        />);
        await user.click(screen.getByRole('button', {name: 'Bearbeiten'}));
        await act(async () => {
            completeRecalculation();
        });
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));

        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('marks the root settings button when the root element has configuration errors', async () => {
        const user = userEvent.setup();
        validateConfiguration.mockResolvedValueOnce({
            properties: {title: ['Geben Sie einen Titel ein.']},
            children: {},
        });

        render(<UiDefinitionInputFieldComponent
            label="Testoberfläche"
            value={null}
            onChange={vi.fn()}
            displayContext={ElementDisplayContext.StaffFacing}
        />);
        await user.click(screen.getByRole('button', {name: 'Bearbeiten'}));

        expect(screen.getByRole('button', {name: 'Einstellungen des Wurzelelements öffnen'})).toBeInTheDocument();
        expect(await screen.findByRole(
            'button',
            {name: 'Einstellungen des Wurzelelements öffnen (fehlerhafte Konfiguration)'},
            {timeout: 2000},
        )).toBeInTheDocument();
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
