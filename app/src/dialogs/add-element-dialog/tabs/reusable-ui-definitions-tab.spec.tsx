import React from 'react';
import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {type TextFieldElement} from '../../../models/elements/form/input/text-field-element';
import {
    type ProcessNodeDefinitionMetadataReusableUiDefinition,
    ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
} from '../../../modules/process/entities/process-node-definition-metadata';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {type ReusableUiDefinitionOption} from '../reusable-ui-definition-utils';
import {ReusableUiDefinitionsTab} from './reusable-ui-definitions-tab';

const {dispatchMock} = vi.hoisted(() => ({
    dispatchMock: vi.fn(),
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatchMock,
}));

describe('ReusableUiDefinitionsTab', () => {
    it('shows understandable category chips and origin descriptions', () => {
        const options = [
            createOption(ProcessNodeDefinitionMetadataReusableUiDefinitionKind.CompleteForm, 'Formular'),
            createOption(ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection, 'Kontaktdaten', true),
            createOption(ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition, 'Bearbeitungsoberfläche'),
            createOption(ProcessNodeDefinitionMetadataReusableUiDefinitionKind.StepperSection, 'Prüfung', true),
            createOption(ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab, 'Historie', true, null),
        ];

        render(<ReusableUiDefinitionsTab options={options} onAddElements={vi.fn()}/>);

        expect(screen.getByText('Gesamtes Formular')).toBeInTheDocument();
        expect(screen.getByText('Formularabschnitt')).toBeInTheDocument();
        expect(screen.getByText('UI-Definition')).toBeInTheDocument();
        expect(screen.getByText('Stepper-Abschnitt')).toBeInTheDocument();
        expect(screen.getByText('Tab')).toBeInTheDocument();
        expect(screen.getAllByText('Ausgangsknoten')).toHaveLength(4);
        expect(screen.getByText('Unbenanntes Prozesselement')).toBeInTheDocument();
        expect(screen.getByText(/Zusätzlicher Kontext/)).toBeInTheDocument();
    });

    it('imports complete definitions directly', () => {
        const onAddElements = vi.fn();
        const option = createOption(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition,
            'Bearbeitungsoberfläche',
        );

        render(<ReusableUiDefinitionsTab options={[option]} onAddElements={onAddElements}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Kopieren und einfügen'}));

        expect(onAddElements).toHaveBeenCalledTimes(1);
        expect(onAddElements.mock.calls[0][0]).toHaveLength(1);
        expect(onAddElements.mock.calls[0][0][0].id).not.toBe(option.definition.uiDefinition.id);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('offers group and flat imports for partial definitions', async () => {
        const onAddElements = vi.fn();
        const option = createOption(
            ProcessNodeDefinitionMetadataReusableUiDefinitionKind.FormSection,
            'Kontaktdaten',
            true,
        );

        const {rerender} = render(
            <ReusableUiDefinitionsTab options={[option]} onAddElements={onAddElements}/>,
        );
        fireEvent.click(screen.getByRole('button', {name: 'Kopieren und einfügen'}));

        expect(screen.getByRole('dialog', {name: 'Teilbereich einfügen'})).toBeInTheDocument();
        expect(screen.getByText(/Überschrift und alle enthaltenen Elemente/)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Flach einfügen'}));

        expect(onAddElements).toHaveBeenCalledTimes(1);
        expect(onAddElements.mock.calls[0][0]).toHaveLength(2);
        expect(onAddElements.mock.calls[0][0].map((element: AnyElement) => element.type)).toEqual([
            ElementType.Headline,
            ElementType.Text,
        ]);

        await waitFor(() => {
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        onAddElements.mockClear();
        rerender(<ReusableUiDefinitionsTab options={[option]} onAddElements={onAddElements}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Kopieren und einfügen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Als Gruppe einfügen'}));

        expect(onAddElements).toHaveBeenCalledTimes(1);
        expect(onAddElements.mock.calls[0][0]).toHaveLength(1);
        expect(onAddElements.mock.calls[0][0][0].type).toBe(ElementType.GroupLayout);
    });

    it('keeps an incompatible import action disabled while allowing the other mode', () => {
        const option = {
            ...createOption(
                ProcessNodeDefinitionMetadataReusableUiDefinitionKind.Tab,
                'Historie',
                true,
            ),
            groupDisabledReason: 'Gruppen sind hier nicht zulässig.',
        };

        const onAddElements = vi.fn();
        render(<ReusableUiDefinitionsTab options={[option]} onAddElements={onAddElements}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Kopieren und einfügen'}));

        expect(screen.getByRole('button', {name: 'Als Gruppe einfügen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Flach einfügen'})).toBeEnabled();
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(onAddElements).not.toHaveBeenCalled();
    });
});

function createOption(
    kind: ProcessNodeDefinitionMetadataReusableUiDefinitionKind,
    label: string,
    partial: boolean = false,
    originName: string | null = 'Ausgangsknoten',
): ReusableUiDefinitionOption {
    const definition: ProcessNodeDefinitionMetadataReusableUiDefinition = {
        label,
        subLabel: kind === ProcessNodeDefinitionMetadataReusableUiDefinitionKind.CompleteForm ?
            'Zusätzlicher Kontext' :
            null,
        uiDefinition: createGroup(`gp_${kind}`, [
            generateElementWithDefaultValues(ElementType.Headline) as AnyElement,
            generateElementWithDefaultValues(ElementType.Text) as TextFieldElement,
        ]),
        origin: {
            id: 21,
            name: originName,
        } as ProcessNodeDefinitionMetadataReusableUiDefinition['origin'],
        kind,
    };

    return {
        definition,
        partial,
    };
}

function createGroup(id: string, children: AnyElement[]): GroupLayout {
    return {
        ...generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
        id,
        children: children as GroupLayout['children'],
        marketplaceLink: null,
    };
}
