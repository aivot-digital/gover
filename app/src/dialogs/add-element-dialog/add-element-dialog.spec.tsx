import {type ComponentProps, useState} from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';
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
    it('offers one single-choice element and creates it with radio presentation', async () => {
        const user = userEvent.setup();
        const onAddElements = vi.fn();
        render(<AddElementDialog show parentType={ElementType.GroupLayout} allParents={[]}
            displayContext={ElementDisplayContext.CustomerFacing}
            limitElementTypes={[ElementType.Select]}
            onClose={vi.fn()} onAddElements={onAddElements}/>);

        expect(screen.getAllByText('Einzelauswahl', {exact: true})).toHaveLength(1);
        await user.click(screen.getByRole('button', {name: 'Einzelauswahl: Hinzufügen'}));
        expect(onAddElements).toHaveBeenCalledWith([expect.objectContaining({
            type: ElementType.Select, presentation: SelectFieldPresentation.Radio,
        })]);
    });

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
            expect(within(dialog).getByText('Texteingabe', {exact: true})).toBeInTheDocument();
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
        expect(within(dialog).getByRole('button', {name: 'Gruppierung: Hinzufügen'})).toBeEnabled();
        expect(within(dialog).queryByText('Funktionseingabe')).not.toBeInTheDocument();
        expect(within(dialog).queryByText('Sonstige')).not.toBeInTheDocument();
    });

    it.each([
        ['steps', ElementType.Step],
        ['groups', ElementType.GroupLayout],
        ['repeating sections', ElementType.ReplicatingContainer],
        ['the introduction', ElementType.IntroductionStep],
        ['summaries', ElementType.SummaryLayout],
    ])('offers formatted text input in customer forms inside %s', async (_, parentType) => {
        const user = userEvent.setup();
        render(
            <AddElementDialog
                show
                parentType={parentType}
                allParents={[]}
                displayContext={ElementDisplayContext.CustomerFacing}
                onClose={vi.fn()}
                onAddElements={vi.fn()}
            />,
        );

        const dialog = screen.getByRole('dialog', {name: 'Formularelement hinzufügen'});
        await user.type(within(dialog).getByRole('searchbox', {name: 'Element suchen'}), 'Formatierung');
        expect(await within(dialog).findByText('Texteingabe mit Formatierung', {exact: true})).toBeInTheDocument();
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

    it('opens another element description at the top while keeping search and focus in the list', async () => {
        const user = userEvent.setup();
        const onAddElements = vi.fn();
        render(<AddElementDialog show parentType={ElementType.GroupLayout} allParents={[]}
                                 displayContext={ElementDisplayContext.CustomerFacing}
                                 limitElementTypes={[ElementType.Text, ElementType.Number]}
                                 onClose={vi.fn()} onAddElements={onAddElements}/>);

        const dialog = screen.getByRole('dialog', {name: 'Formularelement hinzufügen'});
        const search = within(dialog).getByRole('searchbox', {name: 'Element suchen'});
        await user.type(search, 'Erfasst');
        const detailsButtonFor = (name: string) => within(dialog).getByRole('button', {
            name: `${name}: Details anzeigen`,
        });
        const textDetails = detailsButtonFor('Texteingabe');
        const numberDetails = detailsButtonFor('Zahl');
        const listBody = textDetails.closest('.MuiAccordion-root')!.parentElement as HTMLElement;
        listBody.scrollTop = 160;
        expect(textDetails).toHaveAttribute('aria-expanded', 'false');
        await user.click(textDetails);
        expect(textDetails).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('region', {name: 'Details: Texteingabe'}))
            .toHaveAttribute('id', textDetails.getAttribute('aria-controls'));
        const detailsBody = () => within(dialog).getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;
        detailsBody().scrollTop = 320;
        expect(detailsBody().scrollTop).toBe(320);

        await user.click(numberDetails);

        expect(within(dialog).getByRole('heading', {name: 'Zahl', level: 3})).toBeVisible();
        expect(detailsBody().scrollTop).toBe(0);
        expect(numberDetails).toHaveFocus();
        expect(numberDetails).toHaveAttribute('aria-expanded', 'true');
        expect(textDetails).toHaveAttribute('aria-expanded', 'false');
        expect(textDetails).not.toHaveAttribute('aria-controls');
        expect(search).toHaveValue('Erfasst');
        expect(listBody.scrollTop).toBe(160);
        expect(textDetails).toBeVisible();
        expect(numberDetails).toBeVisible();

        detailsBody().scrollTop = 200;
        await user.click(numberDetails);
        expect(detailsBody().scrollTop).toBe(200);
        await user.click(textDetails);
        expect(detailsBody().scrollTop).toBe(0);
        expect(onAddElements).not.toHaveBeenCalled();
    });

    it('opens details with the keyboard and returns focus to the opening button when closing', async () => {
        const user = userEvent.setup();
        const onAddElements = vi.fn();
        render(<AddElementDialog show parentType={ElementType.GroupLayout} allParents={[]}
                                 displayContext={ElementDisplayContext.CustomerFacing}
                                 limitElementTypes={[ElementType.Text, ElementType.Number]}
                                 onClose={vi.fn()} onAddElements={onAddElements}/>);

        const trigger = screen.getByRole('button', {name: 'Texteingabe: Details anzeigen'});
        trigger.focus();
        await user.keyboard('{Enter}');

        const details = screen.getByRole('region', {name: 'Details: Texteingabe'});
        expect(details).toHaveAttribute('id', trigger.getAttribute('aria-controls'));
        expect(trigger).toHaveAttribute('aria-expanded', 'true');
        await user.click(within(details).getByRole('button', {name: 'Details schließen'}));

        expect(trigger).toHaveFocus();
        expect(trigger).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Details: Texteingabe'})).not.toBeInTheDocument();
        expect(onAddElements).not.toHaveBeenCalled();
    });

    it('returns focus to search when the details trigger was removed by filtering', async () => {
        const user = userEvent.setup();
        render(<AddElementDialog show parentType={ElementType.GroupLayout} allParents={[]}
                                 displayContext={ElementDisplayContext.CustomerFacing}
                                 limitElementTypes={[ElementType.Text, ElementType.Number]}
                                 onClose={vi.fn()} onAddElements={vi.fn()}/>);

        await user.click(screen.getByRole('button', {name: 'Texteingabe: Details anzeigen'}));
        const search = screen.getByRole('searchbox', {name: 'Element suchen'});
        await user.type(search, 'Zahl');
        await waitFor(() => expect(screen.queryByRole('button', {
            name: 'Texteingabe: Details anzeigen',
        })).not.toBeInTheDocument());
        await user.click(within(screen.getByRole('region', {name: 'Details: Texteingabe'}))
            .getByRole('button', {name: 'Details schließen'}));

        expect(search).toHaveFocus();
        expect(search).toHaveValue('Zahl');
    });

    it('returns focus to search when the opening button remains inside a collapsed group', async () => {
        const user = userEvent.setup();
        const onAddElements = vi.fn();
        render(<AddElementDialog show parentType={ElementType.GroupLayout} allParents={[]}
                                 displayContext={ElementDisplayContext.CustomerFacing}
                                 limitElementTypes={[ElementType.Text]}
                                 onClose={vi.fn()} onAddElements={onAddElements}/>);
        const trigger = screen.getByRole('button', {name: 'Texteingabe: Details anzeigen'});
        const group = trigger.closest('.MuiAccordion-root') as HTMLElement;
        const groupToggle = within(within(group).getByRole('heading')).getByRole('button');
        const search = screen.getByRole('searchbox', {name: 'Element suchen'});
        await user.click(trigger);
        await user.click(groupToggle);
        await waitFor(() => expect(trigger).not.toBeVisible());
        expect(trigger.isConnected).toBe(true);
        // JSDOM allows focusing hidden elements, unlike the browser.
        const focusTrigger = vi.spyOn(trigger, 'focus').mockImplementation(() => undefined);

        await user.click(screen.getByRole('button', {name: 'Details schließen'}));

        expect(focusTrigger).toHaveBeenCalledWith({preventScroll: true});
        expect(search).toHaveFocus();
        expect(groupToggle).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Details: Texteingabe'})).not.toBeInTheDocument();
        expect(onAddElements).not.toHaveBeenCalled();
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
