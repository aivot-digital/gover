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
