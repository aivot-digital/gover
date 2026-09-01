import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import {FieldLayoutGallery} from './field-layout-gallery';
import {DynamicTextIndicatorLabel} from '../../components/input-mode-selector';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../hooks/use-app-selector', () => ({
    useAppSelector: () => true,
}));

vi.mock('../../components/code-editor/code-editor', () => ({
    CodeEditor: (props: {
        id?: string;
        ariaLabel?: string;
        ariaLabelledBy?: string;
        ariaDescribedBy?: string;
        value?: string | null;
    }) => (
        <textarea
            id={props.id}
            aria-label={props.ariaLabelledBy == null ? props.ariaLabel : undefined}
            aria-labelledby={props.ariaLabelledBy}
            aria-describedby={props.ariaDescribedBy}
            value={props.value ?? ''}
            readOnly
        />
    ),
}));

vi.mock('../../components/rich-text-input-component/rich-text-input-component', () => ({
    RichTextInputComponent: (props: {label: string; value?: string | null}) => (
        <textarea aria-label={`${props.label} – optional`} value={props.value ?? ''} readOnly/>
    ),
}));

vi.mock('../../components/storage-path-selector-input/storage-path-selector-input-component', () => ({
    StoragePathSelectorInputComponent: (props: {label: string}) => (
        <div role="group" aria-label={`${props.label} – optional`}>
            <input aria-label="Speicheranbieter" readOnly/>
            <input aria-label="Pfad" readOnly/>
        </div>
    ),
}));

vi.mock('../../components/ui-definition-input-field/ui-definition-input-field-component', () => ({
    UiDefinitionInputFieldComponent: (props: {label: string}) => (
        <div role="group" aria-label={`${props.label} – optional`}/>
    ),
}));

vi.mock('../../components/no-code-input-field/no-code-input-field-component', () => ({
    NoCodeInputFieldComponent: (props: {label: string; hint?: string}) => (
        <div role="group" aria-label={`${props.label} – optional`} aria-description={props.hint}>
            <button type="button">Bearbeiten</button>
        </div>
    ),
}));

vi.mock('../../dialogs/select-asset-dialog/select-asset-dialog', () => ({
    SelectAssetDialog: ({id, show, title}: {id?: string; show: boolean; title: string}) => show
        ? <div id={id} role="dialog" aria-label={title}/>
        : null,
}));

vi.mock('../../components/view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: (props: {
        element: {
            id: string;
            label?: string | null;
            required?: boolean | null;
        };
        authoredElementValues: Record<string, unknown>;
        isBusy: boolean;
    }) => (
        <input
            aria-label={`${props.element.label ?? 'Unterfeld'}${props.element.required ? '' : ' – optional'}`}
            value={String(props.authoredElementValues[props.element.id] ?? '')}
            disabled={props.isBusy}
            readOnly
        />
    ),
}));

vi.mock('@mui/x-data-grid', async () => {
    const React = await import('react');

    return {
        DataGrid: (props: {
            columns: Array<{
                field: string;
                headerName?: string;
                renderHeader?: (params: unknown) => React.ReactNode;
            }>;
            'aria-labelledby'?: string;
            'aria-describedby'?: string;
            'aria-invalid'?: boolean;
            'aria-busy'?: boolean;
        }) => (
            <div
                role="grid"
                aria-labelledby={props['aria-labelledby']}
                aria-describedby={props['aria-describedby']}
                aria-invalid={props['aria-invalid']}
                aria-busy={props['aria-busy']}
            >
                <div role="row">
                    {props.columns.map((column) => (
                        <div role="columnheader" key={column.field}>
                            {column.renderHeader?.({}) ?? column.headerName}
                        </div>
                    ))}
                </div>
            </div>
        ),
    };
});

vi.mock('../../components/map-point-field/leaflet-point-picker-map', async () => {
    const React = await import('react');

    return {
        LeafletPointPickerMap: React.forwardRef((props: {
            ariaLabel: string;
            ariaDescribedBy?: string;
        }, ref) => {
            React.useImperativeHandle(ref, () => ({
                panTo: () => undefined,
                setView: () => undefined,
            }));

            return (
                <div
                    role="region"
                    aria-label={props.ariaLabel}
                    aria-describedby={props.ariaDescribedBy}
                />
            );
        }),
    };
});

describe('FieldLayoutGallery accessibility', () => {
    it('keeps values, field labels and label actions as separate accessible concepts', () => {
        const {container} = render(<FieldLayoutGallery/>);

        expect(screen.getByRole('region', {name: 'Neues Feldlayout'})).not.toHaveClass('MuiPaper-root');

        const label = screen.getByTitle('Bezeichnung');
        const input = document.getElementById(label.getAttribute('for')!);
        if (!(input instanceof HTMLInputElement)) {
            throw new Error('Expected the Bezeichnung control to be an input.');
        }
        const modeSelector = container.querySelector(`[aria-controls="${input.id}"]`);

        expect(input).toHaveAccessibleName('Bezeichnung');
        expect(input).toHaveValue('Max Mustermann');
        expect(input).not.toHaveAttribute('aria-labelledby');
        expect(input).toHaveAccessibleDescription(
            `Eine eindeutige Bezeichnung hilft bei der späteren Zuordnung. ${DynamicTextIndicatorLabel}`,
        );
        expect(modeSelector).toHaveAccessibleName('Wert: Eingabemodus für Bezeichnung ändern');
        expect(modeSelector).toHaveAttribute('aria-controls', input.id);
        expect(input).not.toHaveAccessibleName(/Wert|Max Mustermann/);
        const field = input.closest('[data-form-field]');
        expect(field).not.toHaveAttribute('role');
        expect(Array.from(field!.querySelectorAll('button, input')).slice(0, 2))
            .toEqual([modeSelector, input]);
    });

    it('exposes each adapter and the native radio fieldset with an exact label', () => {
        const {container} = render(<FieldLayoutGallery/>);

        const amountLabel = screen.getByTitle('Betrag');
        const amountInput = document.getElementById(amountLabel.getAttribute('for')!);
        expect(amountInput).toHaveAccessibleName('Betrag – optional');
        expect(amountInput).toHaveValue('1.250,50');

        const categoryLabel = screen.getByTitle('Kategorie');
        const categorySelect = document.getElementById(categoryLabel.getAttribute('for')!);
        expect(categorySelect).toHaveAccessibleName('Kategorie – optional');
        expect(categorySelect).toHaveTextContent('Antrag');

        const searchLabel = screen.getByTitle('Vorgänge durchsuchen');
        const searchInput = document.getElementById(searchLabel.getAttribute('for')!);
        expect(searchInput).toHaveAccessibleName('Vorgänge durchsuchen');
        expect(searchInput).toHaveAttribute('type', 'search');
        expect(searchInput).toHaveValue('Berlin');
        expect(searchInput).not.toHaveAccessibleName(/optional/);

        const referenceLabel = screen.getByTitle('Aktenzeichen');
        const referenceInput = document.getElementById(referenceLabel.getAttribute('for')!);
        expect(referenceInput).toHaveAccessibleName('Aktenzeichen');
        expect(referenceInput).toHaveAccessibleDescription('Dieses Aktenzeichen ist bereits vergeben.');
        expect(referenceInput).not.toHaveAttribute('aria-labelledby');
        expect(container.querySelector('[role="alert"]'))
            .toHaveTextContent('Dieses Aktenzeichen ist bereits vergeben.');

        const deliveryLabel = screen.getByTitle('Zustellung');
        const deliveryGroup = deliveryLabel.closest('fieldset');
        expect(deliveryGroup).toHaveAccessibleDescription('Legt den bevorzugten Zustellweg fest.');
        expect(deliveryGroup).toHaveAccessibleName('Zustellung');
        expect(deliveryGroup?.querySelector('[role="radiogroup"]')).not.toBeInTheDocument();
        const radios = container.querySelectorAll<HTMLInputElement>('input[type="radio"]');
        expect(radios[0]).toHaveAccessibleName('Digital');
        expect(radios[0]).toBeChecked();
        expect(radios[1]).toHaveAccessibleName('Per Post');
        expect(radios[1]).not.toBeChecked();

        const notificationLabel = screen.getByTitle('Benachrichtigungen');
        const notificationGroup = notificationLabel.closest('fieldset');
        expect(notificationGroup).toHaveAccessibleName('Benachrichtigungen – optional');
        expect(notificationGroup).toHaveAccessibleDescription('Mehrere Kanäle können ausgewählt werden.');
        expect(screen.getByLabelText('E-Mail')).toBeChecked();

        const priorityLabel = screen.getByTitle('Priorität');
        const priorityGroup = priorityLabel.closest('fieldset');
        expect(priorityGroup).toHaveAccessibleName('Priorität – optional');
        expect(priorityGroup?.querySelector('button[value="normal"]')).toHaveAttribute('aria-pressed', 'true');

        const consent = screen.getByTitle('Einwilligung erteilt').closest('label')?.querySelector('input');
        if (!(consent instanceof HTMLInputElement)) {
            throw new Error('Expected the consent control to be an input.');
        }
        expect(consent).toBeRequired();
        expect(consent).toHaveAccessibleDescription('Die Einwilligung ist für diesen Vorgang erforderlich.');

        const reminders = screen.getByTitle('Automatische Erinnerungen').closest('label')?.querySelector('input');
        if (!(reminders instanceof HTMLInputElement)) {
            throw new Error('Expected the reminder control to be an input.');
        }
        expect(reminders).toHaveAttribute('role', 'switch');
        expect(reminders).toBeChecked();
        expect(reminders).toHaveAccessibleDescription('Erinnert vor Ablauf der Bearbeitungsfrist.');

        const rangeLabel = screen.getByTitle('Gültigkeitszeitraum');
        const rangeGroup = rangeLabel.closest('fieldset');
        expect(rangeGroup).toHaveAccessibleName('Gültigkeitszeitraum');
        expect(rangeGroup?.querySelectorAll('[data-form-field]')).toHaveLength(2);
        expect(rangeGroup?.textContent).not.toContain('optional');

        const phoneLabel = screen.getByTitle('Telefonnummer');
        const phoneInput = document.getElementById(phoneLabel.getAttribute('for')!);
        expect(phoneInput).toHaveAccessibleName('Telefonnummer – optional');
        expect(phoneInput).toHaveAccessibleDescription('Geben Sie eine international erreichbare Nummer an.');

        const tagsLabel = screen.getByTitle('Schlagwörter');
        const tagsInput = document.getElementById(tagsLabel.getAttribute('for')!);
        expect(tagsInput).toHaveAccessibleName('Schlagwörter');
        expect(tagsInput).toHaveAccessibleDescription(/Bis zu fünf Schlagwörter können zugeordnet werden/);
        expect(tagsInput).toHaveAttribute('role', 'combobox');

        const dataModelLabel = screen.getByTitle('Datenmodell');
        const dataModelInput = document.getElementById(dataModelLabel.getAttribute('for')!);
        expect(dataModelInput).toHaveAccessibleName('Datenmodell');
        expect(dataModelInput).toHaveAccessibleDescription('Legt fest, aus welchem Datenmodell ausgewählt wird.');
        expect(dataModelInput).toHaveValue('Personen');

        const dataObjectLabel = screen.getByTitle('Datenobjekt');
        const dataObjectInput = document.getElementById(dataObjectLabel.getAttribute('for')!);
        expect(dataObjectInput).toHaveAccessibleName('Datenobjekt – optional');
        expect(dataObjectInput).toHaveAccessibleDescription(
            'Referenziert einen Datensatz aus dem gewählten Modell.',
        );
        expect(dataObjectInput).toHaveValue('Max Mustermann');

        const participantsLabel = screen.getByTitle('Zugriffsberechtigte');
        const participantsInput = document.getElementById(participantsLabel.getAttribute('for')!);
        expect(participantsInput).toHaveAccessibleName('Zugriffsberechtigte – optional');
        expect(participantsInput).toHaveAccessibleDescription(
            'Organisationseinheiten, Teams und Mitarbeitende können kombiniert werden.',
        );

        const assignmentLabel = screen.getByTitle('Personenkreis');
        const assignmentInput = document.getElementById(assignmentLabel.getAttribute('for')!);
        expect(assignmentInput).toHaveAccessibleName('Personenkreis');
        expect(screen.getByText('Verantwortlicher Personenkreis').parentElement).toHaveAttribute('role', 'group');

        const assetLabel = screen.getByTitle('PDF-Vorlage');
        const assetButton = document.getElementById(assetLabel.getAttribute('for')!);
        expect(assetButton).toHaveAccessibleName('PDF-Vorlage – optional Keine PDF-Vorlage ausgewählt');
        expect(assetButton).toHaveAttribute('aria-haspopup', 'dialog');
        expect(assetButton).toHaveAttribute('aria-expanded', 'false');
        expect(assetButton).toHaveAccessibleDescription(
            'Wählen Sie bei Bedarf eine individuelle Dokumentvorlage aus.',
        );
        expect(container.querySelector('button[aria-label="PDF-Vorlage: Auswahl entfernen"]')).toBeDisabled();

        const imageLabel = screen.getByTitle('Logo');
        const imageButton = document.getElementById(imageLabel.getAttribute('for')!);
        expect(imageButton).toHaveAccessibleName('Logo – optional Kein Bild ausgewählt');
        expect(imageButton).toHaveAccessibleDescription('Das Logo wird auf hellen Hintergründen verwendet.');
        expect(container.querySelector('button[aria-label="Logo: Auswahl entfernen"]')).toBeDisabled();

        const departmentLabel = screen.getByTitle('Zuständige Organisationseinheit');
        const departmentButton = document.getElementById(departmentLabel.getAttribute('for')!);
        expect(departmentButton).toHaveAccessibleName(
            'Zuständige Organisationseinheit – optional Fachbereich Digitalisierung',
        );
        expect(departmentButton).toHaveAttribute('aria-haspopup', 'dialog');

        const secretLabel = screen.getByTitle('API-Geheimnis');
        const secretButton = document.getElementById(secretLabel.getAttribute('for')!);
        expect(secretButton).toHaveAccessibleName('API-Geheimnis – optional Kein Geheimnis ausgewählt');
        expect(secretButton).toHaveAccessibleDescription(
            'Referenziert ein zentral verwaltetes Geheimnis, ohne dessen Wert anzuzeigen.',
        );

        expect(screen.getByRole('group', {name: 'Freigabebedingung – optional'}))
            .toHaveAccessibleDescription('Der Ausdruck entscheidet, ob eine Freigabe erforderlich ist.');

        const colorLabel = screen.getByTitle('Akzentfarbe');
        const colorInput = document.getElementById(colorLabel.getAttribute('for')!);
        expect(colorInput).toHaveAccessibleName('Akzentfarbe');
        expect(colorInput).toHaveValue('#006E73');
        expect(colorInput).toBeRequired();

        const attachmentsLabel = screen.getByTitle('Anlagen');
        const attachmentsInput = document.getElementById(attachmentsLabel.getAttribute('for')!);
        expect(attachmentsInput).toHaveAccessibleName('Anlagen – optional');
        expect(attachmentsInput).toHaveAccessibleDescription(
            'Fügen Sie bei Bedarf ergänzende Nachweise hinzu. 1 von max. 3 Dateien',
        );
        expect(screen.getByRole('button', {name: 'antrag.pdf entfernen'})).toBeInTheDocument();

        const locationGroup = screen.getByRole('group', {name: 'Veranstaltungsort – optional'});
        expect(locationGroup).toHaveAccessibleDescription(
            'Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte.',
        );
        expect(screen.getByRole('textbox', {name: 'Adresse oder Ort'})).toBeInTheDocument();
        expect(screen.getByRole('region', {name: 'Karte zur Auswahl von Veranstaltungsort'}))
            .toHaveAccessibleDescription(
                'Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte.',
            );

        const contactsGroup = screen.getByRole('group', {name: 'Kontaktpersonen – optional'});
        const contactsGrid = screen.getByRole('grid', {name: 'Kontaktpersonen – optional'});
        const addContactButton = container.querySelector<HTMLButtonElement>(
            'button[aria-label="Eintrag hinzufügen"]',
        );
        if (addContactButton == null) {
            throw new Error('Expected the table add action to be rendered.');
        }
        expect(contactsGroup).toHaveAccessibleDescription(
            'Erfassen Sie die Kontaktpersonen für diesen Vorgang.',
        );
        expect(contactsGrid).toHaveAccessibleDescription(
            'Erfassen Sie die Kontaktpersonen für diesen Vorgang.',
        );
        expect(addContactButton).toHaveAccessibleName('Eintrag hinzufügen');
        expect(addContactButton).toHaveTextContent('Hinzufügen');
        expect(addContactButton.closest('[data-form-field-label-action]')).not.toBeNull();
        expect(Array.from(contactsGrid.querySelectorAll('[role="columnheader"]'))[1])
            .toHaveTextContent('Rolle – optional');

        const addressesGroup = screen.getByRole('group', {name: 'Weitere Anschriften – optional'});
        expect(addressesGroup).toHaveAccessibleDescription(
            'Erfassen Sie zusätzliche Anschriften, sofern diese für den Vorgang relevant sind.',
        );
        expect(screen.getByRole('group', {name: 'Anschrift 1'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Straße und Hausnummer'})).toHaveValue('Musterstraße 1');
        expect(screen.getByRole('textbox', {name: 'Ort – optional'})).toHaveValue('Musterstadt');
        expect(screen.getByText('Anschrift hinzufügen').closest('button')).toBeInTheDocument();
    }, 30_000);
});
