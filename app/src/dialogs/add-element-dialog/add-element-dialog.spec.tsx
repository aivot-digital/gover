import {type ComponentProps, useState} from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {ProcessNodeDefinitionMetadataReusableUiDefinitionKind} from '../../modules/process/entities/process-node-definition-metadata';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {AddElementDialog} from './add-element-dialog';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../modules/process/pages/details/components/process-node-editor/process-node-editor-context', () => ({
    useOptionalProcessNodeEditorContext: () => ({
        incomingMetadata: {
            reusableUiDefinitions: [{
                label: 'Kontaktdaten',
                subLabel: null,
                uiDefinition: generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
                origin: {id: 21, name: 'Formular'} as ProcessNodeEntity,
                kind: ProcessNodeDefinitionMetadataReusableUiDefinitionKind.UiDefinition,
            }],
        },
    }),
}));

describe('AddElementDialog', () => {
    it.each([ElementType.ConfigLayout, ElementType.GroupLayout])(
        'does not offer secret selectors for staff-facing parent type %s',
        async (parentType) => {
            const user = userEvent.setup();
            render(
                <AddElementDialog
                    show
                    parentType={parentType}
                    allParents={[]}
                    displayContext={ElementDisplayContext.StaffFacing}
                    limitElementTypes={[ElementType.Text, ElementType.SecretSelectInput]}
                    onClose={vi.fn()}
                    onAddElements={vi.fn()}
                />,
            );

            const dialog = screen.getByRole('dialog', {name: 'Formularelement hinzufügen'});
            expect(within(dialog).getByText('Text', {exact: true})).toBeInTheDocument();
            expect(within(dialog).queryByText('Geheimnis-Auswahl')).not.toBeInTheDocument();

            await user.type(within(dialog).getByRole('searchbox', {name: 'Element suchen'}), 'Geheimnis');
            expect(await within(dialog).findByText(
                'Es wurden keine Formularelemente gefunden, die zu Ihrer Suche passen.',
            )).toBeInTheDocument();
        },
    );

    it('keeps internal function and layout types out of the element selection', () => {
        render(
            <AddElementDialog
                show
                parentType={ElementType.ConfigLayout}
                allParents={[]}
                displayContext={ElementDisplayContext.StaffFacing}
                limitElementTypes={[
                    ElementType.GroupLayout,
                    ElementType.FunctionInput,
                    ElementType.DialogLayout,
                    ElementType.StepperLayout,
                    ElementType.ConfigLayout,
                    ElementType.TabLayout,
                ]}
                onClose={vi.fn()}
                onAddElements={vi.fn()}
            />,
        );

        const dialog = screen.getByRole('dialog', {name: 'Formularelement hinzufügen'});
        expect(within(dialog).getByRole('button', {name: 'Hinzufügen'})).toBeEnabled();
        expect(within(dialog).queryByText('Funktionseingabe')).not.toBeInTheDocument();
        expect(within(dialog).queryByText('Sonstige')).not.toBeInTheDocument();
    });

    it('focuses search on every opening and restores focus after closing with Escape', async () => {
        const user = userEvent.setup();
        const onAddElements = vi.fn();

        render(<Harness onAddElements={onAddElements}/>);
        const opener = screen.getByRole('button', {name: 'Formularelement hinzufügen'});
        await user.click(opener);

        const dialog = screen.getByRole('dialog', {name: 'Formularelement hinzufügen'});
        const search = within(dialog).getByRole('searchbox', {name: 'Element suchen'});
        expect(search).toHaveFocus();
        await user.keyboard('Text');
        expect(search).toHaveValue('Text');
        expect(onAddElements).not.toHaveBeenCalled();

        await user.tab({shift: true});
        expect(within(dialog).getByRole('tab', {name: 'Elemente'})).toHaveFocus();
        await user.tab();
        expect(search).toHaveFocus();

        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(opener).toHaveFocus();

        await user.click(opener);
        const reopenedSearch = screen.getByRole('searchbox', {name: 'Element suchen'});
        expect(reopenedSearch).toHaveFocus();
        expect(reopenedSearch).toHaveValue('');
    });

    it('keeps focus on the selected tab when returning to the element list with the keyboard', async () => {
        const user = userEvent.setup();
        render(<Harness onAddElements={vi.fn()}/>);
        await user.click(screen.getByRole('button', {name: 'Formularelement hinzufügen'}));

        await user.click(screen.getByRole('tab', {name: 'UI-Definitionen'}));
        expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'UI-Definitionen'})).toHaveFocus();

        await user.keyboard('{ArrowLeft}{Enter}');
        const elementsTab = screen.getByRole('tab', {name: 'Elemente'});
        expect(elementsTab).toHaveAttribute('aria-selected', 'true');
        expect(elementsTab).toHaveFocus();
        expect(screen.getByRole('searchbox', {name: 'Element suchen'})).not.toHaveFocus();
    });
});

function Harness({onAddElements}: Pick<ComponentProps<typeof AddElementDialog>, 'onAddElements'>) {
    const [show, setShow] = useState(false);

    return <>
        <button onClick={() => setShow(true)}>Formularelement hinzufügen</button>
        <AddElementDialog
            show={show}
            parentType={ElementType.GroupLayout}
            allParents={[]}
            displayContext={ElementDisplayContext.CustomerFacing}
            limitElementTypes={[ElementType.Text, ElementType.Number, ElementType.GroupLayout]}
            onClose={() => setShow(false)}
            onAddElements={onAddElements}
        />
    </>;
}
