import {useState} from 'react';
import {
    Box,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import {TextFieldComponent} from '../../components/text-field/text-field-component';
import {NumberFieldComponent} from '../../components/number-field/number-field-component';
import {SelectFieldComponent} from '../../components/select-field/select-field-component';
import {DateFieldComponent} from '../../components/date-field/date-field-component';
import {TimeFieldComponent} from '../../components/time-field/time-field-component';
import {DateTimeFieldComponent} from '../../components/date-time-field/date-time-field-component';
import {DateRangeFieldComponent} from '../../components/date-range-field/date-range-field-component';
import {TimeRangeFieldComponent} from '../../components/time-range-field/time-range-field-component';
import {DateTimeRangeFieldComponent} from '../../components/date-time-range-field/date-time-range-field-component';
import {RadioFieldComponent} from '../../components/radio-field/radio-field-component';
import {CheckboxFieldComponent} from '../../components/checkbox-field/checkbox-field-component';
import {MultiCheckboxComponent} from '../../components/multi-checkbox-field/multi-checkbox-component';
import {PhoneNumberFieldComponent} from '../../components/phone-number-field/phone-number-field-component';
import {ChipInputFieldComponent} from '../../components/chip-input-field/chip-input-field-component';
import {DataModelSelectFieldComponent} from '../../components/data-model-select-field/data-model-select-field-component';
import {DataObjectSelectFieldComponent} from '../../components/data-object-select-field/data-object-select-field-component';
import {DomainUserSelectFieldComponent} from '../../components/domain-user-select-field/domain-user-select-field-component';
import type {DomainAndUserSelectOption} from '../../components/domain-user-select-field/domain-user-select-options';
import {AssignmentContextFieldComponent} from '../../components/assignment-context-field/assignment-context-field-component';
import {FileUploadComponent} from '../../components/file-upload-field/file-upload-component';
import {SearchInput} from '../../components/search-input/search-input';
import {AssetSelector} from '../assets/components/asset-selector';
import {ImageSelector} from '../assets/components/image-selector';
import {ThemeColorPicker} from '../themes/components/theme-color-picker';
import {AutocompleteSelect} from '../../components/autocomple-select/autocomplete-select';
import {StringListInput} from '../../components/string-list-input/string-list-input';
import {RichTextInputComponent} from '../../components/rich-text-input-component/rich-text-input-component';
import {CodeInputFieldComponent} from '../../components/code-input-field/code-input-field-component';
import {StoragePathSelectorInputComponent} from '../../components/storage-path-selector-input/storage-path-selector-input-component';
import {UiDefinitionInputFieldComponent} from '../../components/ui-definition-input-field/ui-definition-input-field-component';
import {NoCodeInputFieldComponent} from '../../components/no-code-input-field/no-code-input-field-component';
import {DepartmentSelectField} from '../departments/components/department-select-field';
import {SecretSelectComponent} from '../secrets/components/secret-select-component';
import {OptionListInput} from '../../components/option-list-input/option-list-input';
import type {OptionListInputValue} from '../../components/option-list-input/option-list-input-props';
import {ElementWidthSelector} from '../../components/element-width-selector/element-width-selector';
import {MapPointFieldComponent} from '../../components/map-point-field/map-point-field-component';
import {TableFieldComponent2} from '../../components/table-field/table-field-component-2';
import {
    FormField,
    type FormFieldControlContext,
    getNativeInputAriaProps,
} from '../../components/form-field';
import {
    DynamicTextIndicator,
    DynamicTextIndicatorLabel,
    type InputMode,
    InputModeSelector,
} from '../../components/input-mode-selector';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import type {DateValueIso, InstantIso, LocalTimeIso} from '../../utils/temporal-types';
import type {DateRangeValue} from '../../models/elements/form/input/date-range-field-element';
import type {TimeRangeValue} from '../../models/elements/form/input/time-range-field-element';
import type {DateTimeRangeValue} from '../../models/elements/form/input/date-time-range-field-element';
import type {DomainAndUserSelectItem} from '../../models/elements/form/input/domain-user-select-field-element';
import type {AssignmentContextValue} from '../../models/elements/form/input/assignment-context-field-element';
import type {MapPointValue} from '../../models/elements/form/input/map-point-field-element';
import {ReplicatingContainerView} from '../../components/replicating-container/replicating-container.view';
import type {ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import type {TextFieldElement} from '../../models/elements/form/input/text-field-element';
import type {FormLayoutElement} from '../../models/elements/form-layout-element';
import type {NoCodeInputFieldElementItem} from '../../models/elements/form/input/no-code-input-field-element';
import type {VDepartmentShadowedEntityWithChildren} from '../departments/entities/v-department-shadowed-entity';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ElementType} from '../../data/element-type/element-type';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import type {StoragePathSelectorInputElementValue} from '../../models/elements/form/input/storage-path-selector-input-element';
import {
    createDerivedRuntimeElementData,
    type ReplicatingContainerElementValues,
} from '../../models/element-data';
import {
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../../components/view-dispatcher/view-dispatcher.context';
import {getDepartmentTypeIcons} from '../departments/utils/department-utils';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';

const fieldGridSx = {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 18rem), 1fr))',
    alignItems: 'start',
    gap: 3,
};

const domainAndUserOptions: DomainAndUserSelectOption[] = [
    {
        key: 'orgUnit:10',
        value: {type: 'orgUnit', id: '10'},
        label: 'Fachbereich Anträge',
        subLabel: 'Organisationseinheit',
        group: 'Organisationseinheiten',
        eligibleUserCount: 4,
        icon: getDepartmentTypeIcons(1),
    },
    {
        key: 'team:20',
        value: {type: 'team', id: '20'},
        label: 'Team Leistungsprüfung',
        subLabel: 'Team',
        group: 'Teams',
        eligibleUserCount: 3,
        icon: ModuleIcons.teams,
    },
    {
        key: 'user:user-1',
        value: {type: 'user', id: 'user-1'},
        label: 'Mustermann, Max',
        subLabel: 'max.mustermann@example.org',
        group: 'Mitarbeitende',
        icon: <Person />,
    },
];

const inputModeSummaries: Record<Exclude<InputMode, 'literal'>, {primary: string; secondary: string}> = {
    variable: {
        primary: 'Nachname der antragstellenden Person',
        secondary: 'Vorgangsdaten - $.applicant.lastName',
    },
    noCode: {
        primary: 'Vorname + " " + Nachname',
        secondary: 'Ausdruck (No-Code)',
    },
    lowCode: {
        primary: 'Benutzerdefiniertes Skript',
        secondary: 'return `${$.applicant.firstName} ${$.applicant.lastName}`;',
    },
};

const addressStreetElement: TextFieldElement = {
    type: ElementType.Text,
    id: 'field-layout-gallery-address-street',
    name: null,
    testProtocolSet: null,
    visibility: null,
    override: null,
    metadata: null,
    weight: 6,
    label: 'Straße und Hausnummer',
    hint: null,
    required: true,
    disabled: false,
    technical: false,
    destinationKey: 'street',
    validation: null,
    value: null,
    autocomplete: 'street-address',
    placeholder: null,
    isMultiline: false,
    maxCharacters: null,
    minCharacters: null,
    pattern: null,
    prefix: null,
    copyable: false,
    copyValueTemplate: null,
    suggestions: null,
};

const addressCityElement: TextFieldElement = {
    ...addressStreetElement,
    id: 'field-layout-gallery-address-city',
    label: 'Ort',
    required: false,
    destinationKey: 'city',
    autocomplete: 'address-level2',
};

const addressListElement: ReplicatingContainerLayout = {
    type: ElementType.ReplicatingContainer,
    id: 'field-layout-gallery-addresses',
    name: null,
    testProtocolSet: null,
    visibility: null,
    override: null,
    metadata: null,
    weight: 12,
    label: 'Weitere Anschriften',
    hint: 'Erfassen Sie zusätzliche Anschriften, sofern diese für den Vorgang relevant sind.',
    required: false,
    disabled: false,
    technical: false,
    destinationKey: 'additionalAddresses',
    validation: null,
    value: null,
    minimumRequiredSets: null,
    maximumSets: 3,
    headlineTemplate: 'Anschrift #',
    addLabel: 'Anschrift hinzufügen',
    removeLabel: 'Anschrift löschen',
    children: [addressStreetElement, addressCityElement],
};

const fieldLayoutGalleryDerivedData = createDerivedRuntimeElementData();

const fieldLayoutGalleryRootElement = {
    type: ElementType.FormLayout,
    id: 'field-layout-gallery-root',
    children: [],
} as unknown as FormLayoutElement;

const departmentOptions: VDepartmentShadowedEntityWithChildren[] = [{
    id: 10,
    name: 'Fachbereich Digitalisierung',
    created: '2026-01-01T00:00:00Z',
    updated: '2026-01-01T00:00:00Z',
    depth: 1,
    parentDepartmentId: 1,
    parentNames: ['Bezirksamt Mitte'],
    parentIds: [1],
    children: [],
}];

interface InputModeGalleryFieldProps {
    value: string | null;
    onChange: (value: string | null) => void;
}

function InputModeGalleryField(props: InputModeGalleryFieldProps) {
    const label = 'Bezeichnung';
    const [mode, setMode] = useState<InputMode>('literal');

    return (
        <FormField
            label={label}
            labelAction={(field) => (
                <Stack direction="row" spacing={0.5} sx={{height: '100%', alignItems: 'center'}}>
                    {mode === 'literal' && <DynamicTextIndicator decorative/>}
                    <InputModeSelector
                        fieldLabel={label}
                        controlledFieldId={field.controlId}
                        value={mode}
                        onChange={setMode}
                    />
                </Stack>
            )}
            hint="Eine eindeutige Bezeichnung hilft bei der späteren Zuordnung."
            assistiveText={mode === 'literal' ? DynamicTextIndicatorLabel : undefined}
            required
            margin="none"
        >
            {(field) => mode === 'literal' ? (
                <TextField
                    id={field.controlId}
                    value={props.value ?? ''}
                    onChange={(event) => props.onChange(event.target.value || null)}
                    required={field.required}
                    size="small"
                    margin="none"
                    fullWidth
                    slotProps={{
                        input: {
                            sx: {minHeight: FormFieldTokens.controlMinHeight},
                        },
                        htmlInput: getNativeInputAriaProps(field),
                    }}
                />
            ) : (
                <InputModeSummary mode={mode} field={field}/>
            )}
        </FormField>
    );
}

function InputModeSummary(props: {
    mode: Exclude<InputMode, 'literal'>;
    field: FormFieldControlContext;
}) {
    const summary = inputModeSummaries[props.mode];
    const primaryId = `${props.field.controlId}-summary-primary`;
    const secondaryId = `${props.field.controlId}-summary-secondary`;
    const labelledBy = [props.field.labelId, primaryId].filter(Boolean).join(' ');
    const describedBy = [secondaryId, props.field.ariaProps['aria-describedby']].filter(Boolean).join(' ');

    return (
        <Box
            id={props.field.controlId}
            role="group"
            {...props.field.ariaProps}
            aria-labelledby={labelledBy}
            aria-describedby={describedBy}
            sx={{
                width: '100%',
                minHeight: FormFieldTokens.controlMinHeight,
                px: 1.5,
                py: 0.5,
                border: '1px solid',
                borderColor: props.field.invalid ? 'error.main' : 'divider',
                borderRadius: 1,
                display: 'flex',
                alignItems: 'center',
                textAlign: 'left',
            }}
        >
            <Box sx={{minWidth: 0, flex: 1}}>
                <Typography
                    id={primaryId}
                    variant="body2"
                    noWrap
                    title={summary.primary}
                    sx={{fontSize: '0.875rem', lineHeight: 1.25}}
                >
                    {summary.primary}
                </Typography>
                <Typography
                    id={secondaryId}
                    variant="caption"
                    component="div"
                    color="text.secondary"
                    noWrap
                    title={summary.secondary}
                    sx={{fontSize: '0.75rem', lineHeight: 1.2}}
                >
                    {summary.secondary}
                </Typography>
            </Box>
        </Box>
    );
}

export function FieldLayoutGallery() {
    const [name, setName] = useState<string | null>('Max Mustermann');
    const [description, setDescription] = useState<string | null>('Kurze Beschreibung');
    const [amount, setAmount] = useState<number | null>(1250.5);
    const [category, setCategory] = useState<string | null>('request');
    const [searchableCategory, setSearchableCategory] = useState<string | null>('approval');
    const [delivery, setDelivery] = useState('digital');
    const [consent, setConsent] = useState(false);
    const [reminders, setReminders] = useState(true);
    const [priority, setPriority] = useState<string | null>('normal');
    const [notifications, setNotifications] = useState<string[] | null>(['email']);
    const [shortCode, setShortCode] = useState<string | null>('AB');
    const [limitedText, setLimitedText] = useState<string | null>(
        'Diese Beschreibung überschreitet bewusst die empfohlene Länge.',
    );
    const [searchQuery, setSearchQuery] = useState('Berlin');
    const [date, setDate] = useState<DateValueIso | null>('2026-08-26' as DateValueIso);
    const [time, setTime] = useState<LocalTimeIso | null>('09:30' as LocalTimeIso);
    const [dateTime, setDateTime] = useState<InstantIso | null>('2026-08-26T07:30:00Z' as InstantIso);
    const [dateRange, setDateRange] = useState<DateRangeValue | null>({
        start: '2026-08-01',
        end: '2026-08-31',
    });
    const [timeRange, setTimeRange] = useState<TimeRangeValue | null>({
        start: '08:00',
        end: '16:30',
    });
    const [dateTimeRange, setDateTimeRange] = useState<DateTimeRangeValue | null>({
        start: '2026-08-26T10:00:00Z',
        end: '2026-08-26T09:00:00Z',
    });
    const [phoneNumber, setPhoneNumber] = useState<string | null>('+4915112345678');
    const [tags, setTags] = useState<string[] | null>(['Antrag', 'Priorität']);
    const [dataModelKey, setDataModelKey] = useState<string | null>('persons');
    const [dataObjectId, setDataObjectId] = useState<string | null>('person-1');
    const [participants, setParticipants] = useState<DomainAndUserSelectItem[] | null>([
        {type: 'orgUnit', id: '10'},
        {type: 'user', id: 'user-1'},
    ]);
    const [assignmentContext, setAssignmentContext] = useState<AssignmentContextValue | null>({
        domainAndUserSelection: [{type: 'team', id: '20'}],
        generalAssigneePreference: 'previousProcessStepAssignee',
        repeatExecutionAssigneePreference: 'none',
    });
    const [attachments, setAttachments] = useState<File[] | null>(() => [
        new File(['Beispieldokument'], 'antrag.pdf', {type: 'application/pdf'}),
    ]);
    const [selectedAssetKey, setSelectedAssetKey] = useState<string | null>(null);
    const [selectedImageKey, setSelectedImageKey] = useState<string | null>(null);
    const [accentColor, setAccentColor] = useState('#006E73');
    const [autocomplete, setAutocomplete] = useState<string | undefined>('name');
    const [stringValues, setStringValues] = useState<string[] | undefined>(['lesen', 'schreiben']);
    const [richText, setRichText] = useState<string | null>('## Bearbeitungshinweis\n\nBitte prüfen Sie die Unterlagen vollständig.');
    const [code, setCode] = useState<string | null>('return context.applicant?.age >= 18;');
    const [storagePath, setStoragePath] = useState<StoragePathSelectorInputElementValue | null>(null);
    const [department, setDepartment] = useState<VDepartmentShadowedEntityWithChildren | null>(departmentOptions[0]);
    const [secretKey, setSecretKey] = useState<string | null>(null);
    const [noCodeValue, setNoCodeValue] = useState<NoCodeInputFieldElementItem | null>(null);
    const [selectionOptions, setSelectionOptions] = useState<OptionListInputValue[] | undefined>([
        {label: 'Niedrig', value: 'low'},
        {label: 'Hoch', value: 'high'},
    ]);
    const [elementWidth, setElementWidth] = useState(6);
    const [location, setLocation] = useState<MapPointValue | null>({
        latitude: 52.52,
        longitude: 13.405,
        address: 'Alexanderplatz, 10178 Berlin',
    });
    const [contacts, setContacts] = useState<Array<{name: string; role: string}> | null>([
        {name: 'Max Mustermann', role: 'Ansprechperson'},
        {name: 'Erika Musterfrau', role: ''},
    ]);
    const [additionalAddresses, setAdditionalAddresses] = useState<ReplicatingContainerElementValues | null>([
        {
            id: 'field-layout-gallery-address-1',
            values: {
                [addressStreetElement.id]: 'Musterstraße 1',
                [addressCityElement.id]: 'Musterstadt',
            },
        },
    ]);

    return (
        <Box
            component="section"
            aria-labelledby="field-layout-gallery-title"
        >
            <Typography
                id="field-layout-gallery-title"
                component="h2"
                variant="h5"
                sx={{mb: 3}}
            >
                Neues Feldlayout
            </Typography>

            <Box sx={fieldGridSx}>
                <InputModeGalleryField
                    value={name}
                    onChange={setName}
                />

                <NumberFieldComponent
                    label="Betrag"
                    value={amount}
                    onChange={setAmount}
                    decimalPlaces={2}
                    suffix="EUR"
                    hint="Der Betrag wird kaufmännisch gerundet."
                    margin="none"
                />

                <SelectFieldComponent
                    label="Kategorie"
                    value={category}
                    onChange={setCategory}
                    placeholder="Keine Auswahl"
                    options={[
                        {value: 'request', label: 'Antrag'},
                        {value: 'notification', label: 'Mitteilung'},
                        {value: 'approval', label: 'Freigabe'},
                    ]}
                    margin="none"
                />

                <SelectFieldComponent
                    label="Kategorie (durchsuchbar)"
                    value={searchableCategory}
                    onChange={setSearchableCategory}
                    placeholder="Kategorie suchen"
                    presentation={SelectFieldPresentation.Combobox}
                    options={[
                        {value: 'request', label: 'Antrag', subLabel: 'Neuen Antrag erfassen'},
                        {value: 'notification', label: 'Mitteilung', subLabel: 'Information weitergeben'},
                        {value: 'approval', label: 'Freigabe', subLabel: 'Entscheidung einholen'},
                    ]}
                    margin="none"
                />

                <SearchInput
                    label="Vorgänge durchsuchen"
                    value={searchQuery}
                    onChange={setSearchQuery}
                    placeholder="Aktenzeichen oder Bezeichnung eingeben"
                />

                <TextFieldComponent
                    label="Interne Beschreibung"
                    value={description}
                    onChange={setDescription}
                    multiline
                    margin="none"
                />

                <TextFieldComponent
                    label="Aktenzeichen"
                    value="AZ-2026-0042"
                    onChange={() => undefined}
                    error="Dieses Aktenzeichen ist bereits vergeben."
                    required
                    margin="none"
                />

                <TextFieldComponent
                    label="Systemkennung"
                    value="process-42"
                    onChange={() => undefined}
                    hint="Wird automatisch aus dem Prozess übernommen."
                    readonly
                    margin="none"
                />

                <TextFieldComponent
                    label="Nicht verfügbar"
                    value="Gesperrter Wert"
                    onChange={() => undefined}
                    disabled
                    margin="none"
                />

                <CheckboxFieldComponent
                    label="Einwilligung erteilt"
                    value={consent}
                    onChange={setConsent}
                    hint="Die Einwilligung ist für diesen Vorgang erforderlich."
                    required
                    margin="none"
                />

                <CheckboxFieldComponent
                    label="Automatische Erinnerungen"
                    value={reminders}
                    onChange={setReminders}
                    variant="switch"
                    hint="Erinnert vor Ablauf der Bearbeitungsfrist."
                    showOptionalIndicator
                    margin="none"
                />

                <RadioFieldComponent
                    label="Zustellung"
                    value={delivery}
                    onChange={(value) => setDelivery(value ?? '')}
                    options={[
                        {value: 'digital', label: 'Digital'},
                        {value: 'post', label: 'Per Post'},
                    ]}
                    hint="Legt den bevorzugten Zustellweg fest."
                    required
                    margin="none"
                />

                <MultiCheckboxComponent
                    label="Benachrichtigungen"
                    value={notifications}
                    onChange={setNotifications}
                    options={[
                        {value: 'email', label: 'E-Mail'},
                        {value: 'inbox', label: 'Postfach'},
                        {value: 'sms', label: 'SMS'},
                    ]}
                    hint="Mehrere Kanäle können ausgewählt werden."
                    margin="none"
                />

                <RadioFieldComponent
                    label="Priorität"
                    value={priority}
                    onChange={setPriority}
                    options={[
                        {value: 'low', label: 'Niedrig'},
                        {value: 'normal', label: 'Normal'},
                        {value: 'high', label: 'Hoch'},
                    ]}
                    toggleButtons
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Datum und Zeit
            </Typography>

            <Box sx={fieldGridSx}>
                <DateFieldComponent
                    label="Antragsdatum"
                    value={date}
                    onChange={setDate}
                    mode={DateFieldComponentModelMode.Day}
                    required
                    margin="none"
                />

                <TimeFieldComponent
                    label="Gewünschte Uhrzeit"
                    value={time}
                    onChange={setTime}
                    mode={TimeFieldComponentModelMode.Minute}
                    hint="Die Uhrzeit bezieht sich auf den lokalen Vorgang."
                    margin="none"
                />

                <DateTimeFieldComponent
                    label="Bearbeitungsfrist"
                    value={dateTime}
                    onChange={setDateTime}
                    mode={TimeFieldComponentModelMode.Minute}
                    hint="Datum und Uhrzeit werden in der Anwendungszeitzone angezeigt."
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Zeiträume
            </Typography>

            <Box sx={{display: 'grid', gap: 3}}>
                <DateRangeFieldComponent
                    label="Gültigkeitszeitraum"
                    value={dateRange}
                    onChange={setDateRange}
                    mode={DateFieldComponentModelMode.Day}
                    required
                    margin="none"
                />

                <TimeRangeFieldComponent
                    label="Servicezeit"
                    value={timeRange}
                    onChange={setTimeRange}
                    mode={TimeFieldComponentModelMode.Minute}
                    hint="Start und Ende beziehen sich auf die lokale Uhrzeit."
                    margin="none"
                />

                <DateTimeRangeFieldComponent
                    label="Bearbeitungsfenster"
                    value={dateTimeRange}
                    onChange={setDateTimeRange}
                    mode={TimeFieldComponentModelMode.Minute}
                    error="Das Ende muss nach dem Beginn liegen."
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Textfeldvarianten
            </Typography>

            <Box sx={fieldGridSx}>
                <TextFieldComponent
                    label="Kurzkennung"
                    value={shortCode}
                    onChange={setShortCode}
                    minCharacters={5}
                    maxCharacters={20}
                    hint="Zwischen 5 und 20 Zeichen."
                    required
                    margin="none"
                />

                <TextFieldComponent
                    label="Zusammenfassung"
                    value={limitedText}
                    onChange={setLimitedText}
                    maxCharacters={100}
                    softLimitCharacters={48}
                    hint="Der Text bleibt technisch zulässig, sollte aber möglichst kurz sein."
                    multiline
                    rows={2}
                    margin="none"
                />

                <TextFieldComponent
                    label="E-Mail-Adresse"
                    value="ungueltige-adresse"
                    onChange={() => undefined}
                    pattern={{
                        regex: '^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$',
                        message: 'Bitte geben Sie eine gültige E-Mail-Adresse ein.',
                    }}
                    hint="Die Adresse wird für Rückfragen verwendet."
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Weitere Eingaben
            </Typography>

            <Box sx={fieldGridSx}>
                <PhoneNumberFieldComponent
                    label="Telefonnummer"
                    value={phoneNumber}
                    onChange={setPhoneNumber}
                    hint="Geben Sie eine international erreichbare Nummer an."
                    margin="none"
                />

                <ChipInputFieldComponent
                    label="Schlagwörter"
                    value={tags}
                    onChange={setTags}
                    placeholder="Schlagwort ergänzen"
                    suggestions={['Antrag', 'Priorität', 'Rückfrage', 'Frist']}
                    maxItems={5}
                    hint="Bis zu fünf Schlagwörter können zugeordnet werden."
                    required
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Datenbezüge
            </Typography>

            <Box sx={fieldGridSx}>
                <DataModelSelectFieldComponent
                    label="Datenmodell"
                    value={dataModelKey}
                    onChange={(nextValue) => {
                        setDataModelKey(nextValue);
                        setDataObjectId(null);
                    }}
                    options={[
                        {
                            key: 'persons',
                            value: 'persons',
                            label: 'Personen',
                            subLabel: 'persons',
                        },
                        {
                            key: 'organizations',
                            value: 'organizations',
                            label: 'Organisationen',
                            subLabel: 'organizations',
                        },
                    ]}
                    hint="Legt fest, aus welchem Datenmodell ausgewählt wird."
                    required
                    margin="none"
                />

                <DataObjectSelectFieldComponent
                    label="Datenobjekt"
                    value={dataObjectId}
                    onChange={setDataObjectId}
                    dataModelKey={dataModelKey}
                    dataLabelAttributeKey="name"
                    options={dataModelKey === 'organizations' ? [
                        {
                            key: 'organization-1',
                            value: 'organization-1',
                            label: 'Musterverwaltung',
                            subLabel: 'organizations · organization-1',
                        },
                    ] : [
                        {
                            key: 'person-1',
                            value: 'person-1',
                            label: 'Max Mustermann',
                            subLabel: 'persons · person-1',
                        },
                        {
                            key: 'person-2',
                            value: 'person-2',
                            label: 'Erika Musterfrau',
                            subLabel: 'persons · person-2',
                        },
                    ]}
                    hint="Referenziert einen Datensatz aus dem gewählten Modell."
                    margin="none"
                />

                <DomainUserSelectFieldComponent
                    label="Zugriffsberechtigte"
                    value={participants}
                    onChange={setParticipants}
                    options={domainAndUserOptions}
                    placeholder="Personenkreis ergänzen"
                    hint="Organisationseinheiten, Teams und Mitarbeitende können kombiniert werden."
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Zuweisung
            </Typography>

            <AssignmentContextFieldComponent
                value={assignmentContext}
                onChange={setAssignmentContext}
                options={domainAndUserOptions}
                required
            />

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Dateien, Medien und Farbe
            </Typography>

            <Box sx={fieldGridSx}>
                <AssetSelector
                    label="PDF-Vorlage"
                    selectLabel="PDF-Vorlage auswählen"
                    value={selectedAssetKey}
                    onChange={setSelectedAssetKey}
                    placeholder="Keine PDF-Vorlage ausgewählt"
                    hint="Wählen Sie bei Bedarf eine individuelle Dokumentvorlage aus."
                    mimetype="application/pdf"
                    margin="none"
                />

                <ImageSelector
                    label="Logo"
                    selectLabel="Logo auswählen"
                    value={selectedImageKey}
                    onChange={setSelectedImageKey}
                    size={{aspectRatio: 2}}
                    hint="Das Logo wird auf hellen Hintergründen verwendet."
                    margin="none"
                />

                <ThemeColorPicker
                    label="Akzentfarbe"
                    value={accentColor}
                    onChange={setAccentColor}
                    contrastTextColor="#FFFFFF"
                    contrastBackgroundColor="#FFFFFF"
                    margin="none"
                />
            </Box>

            <Box sx={{mt: 3}}>
                <FileUploadComponent
                    id="field-layout-gallery-attachments"
                    label="Anlagen"
                    value={attachments}
                    onChange={setAttachments}
                    placeholder="Nachweise auswählen oder hier ablegen"
                    extensions={['pdf', 'jpg', 'png']}
                    isMultifile
                    maxFiles={3}
                    hint="Fügen Sie bei Bedarf ergänzende Nachweise hinzu."
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Editoren und Konfigurationsfelder
            </Typography>

            <Box sx={fieldGridSx}>
                <AutocompleteSelect
                    type={ElementType.Text}
                    value={autocomplete}
                    onChange={setAutocomplete}
                    editable
                    margin="none"
                />

                <DepartmentSelectField
                    label="Zuständige Organisationseinheit"
                    value={department}
                    onChange={setDepartment}
                    departments={departmentOptions}
                    hint="Legt fest, welche Organisationseinheit für die Konfiguration verantwortlich ist."
                    margin="none"
                />

                <SecretSelectComponent
                    label="API-Geheimnis"
                    value={secretKey}
                    onChange={setSecretKey}
                    hint="Referenziert ein zentral verwaltetes Geheimnis, ohne dessen Wert anzuzeigen."
                    margin="none"
                />

                <StoragePathSelectorInputComponent
                    label="Ablageort"
                    value={storagePath}
                    onChange={setStoragePath}
                    storageProviderSelectHint="Wählen Sie zuerst den Speicheranbieter und anschließend einen Zielordner."
                    hint="Die Unterlagen werden nach Abschluss des Vorgangs hier abgelegt."
                    margin="none"
                />
            </Box>

            <Box sx={{display: 'grid', gap: 3, mt: 3}}>
                <NoCodeInputFieldComponent
                    rootElement={fieldLayoutGalleryRootElement}
                    label="Freigabebedingung"
                    value={noCodeValue}
                    onChange={setNoCodeValue}
                    desiredReturnType={NoCodeDataType.Boolean}
                    hint="Der Ausdruck entscheidet, ob eine Freigabe erforderlich ist."
                    margin="none"
                />

                <StringListInput
                    label="Berechtigungen"
                    value={stringValues}
                    onChange={setStringValues}
                    addLabel="Berechtigung hinzufügen"
                    noItemsHint="Keine Berechtigungen hinterlegt"
                    hint="Jeder Eintrag bezeichnet eine benötigte Berechtigung."
                    allowEmpty
                    margin="none"
                />

                <OptionListInput
                    label="Auswahloptionen"
                    value={selectionOptions}
                    onChange={setSelectionOptions}
                    addLabel="Option hinzufügen"
                    noItemsHint="Keine Auswahloptionen hinterlegt"
                    hint="Beschriftung und technischer Wert müssen jeweils eindeutig sein."
                    allowEmpty={false}
                    margin="none"
                />

                <ElementWidthSelector
                    label="Elementbreite"
                    elementType={ElementType.Text}
                    value={elementWidth}
                    onChange={setElementWidth}
                    hint="Bestimmt die Breite des Elements im Formularraster."
                    margin="none"
                />

                <RichTextInputComponent
                    label="Bearbeitungshinweis"
                    value={richText}
                    onChange={setRichText}
                    hint="Formatierter Hinweis für die spätere Sachbearbeitung."
                    reducedMode
                    margin="none"
                />

                <CodeInputFieldComponent
                    label="Validierungslogik"
                    value={code}
                    onChange={setCode}
                    hint="Die Funktion gibt zurück, ob die Eingabe zulässig ist."
                    language="javascript"
                    height="220px"
                    margin="none"
                />

                <UiDefinitionInputFieldComponent
                    label="Ergänzende Oberfläche"
                    value={null}
                    onChange={() => undefined}
                    hint="Optional kann hier eine zusätzliche UI-Struktur modelliert werden."
                    displayContext={ElementDisplayContext.StaffFacing}
                    margin="none"
                />
            </Box>

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Ortsangabe
            </Typography>

            <MapPointFieldComponent
                label="Veranstaltungsort"
                value={location}
                onChange={setLocation}
                hint="Suchen Sie eine Adresse oder wählen Sie den Punkt direkt auf der Karte."
                centerLatitude={52.52}
                centerLongitude={13.405}
                margin="none"
            />

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Tabellarische Eingaben
            </Typography>

            <TableFieldComponent2<{name: string; role: string}>
                label="Kontaktpersonen"
                value={contacts}
                onChange={setContacts}
                fields={[
                    {
                        key: 'name',
                        label: 'Name',
                        type: 'string',
                        required: true,
                    },
                    {
                        key: 'role',
                        label: 'Rolle',
                        type: 'string',
                    },
                ]}
                createDefaultRow={() => ({name: '', role: ''})}
                maximumRows={5}
                hint="Erfassen Sie die Kontaktpersonen für diesen Vorgang."
                margin="none"
            />

            <Typography
                component="h3"
                variant="subtitle1"
                sx={{mt: 5, mb: 2}}
            >
                Strukturierte Listen
            </Typography>

            <ViewDispatcherContextProvider
                value={{
                    mode: ViewDispatcherMode.Viewer,
                    rootElement: addressListElement,
                    allElements: [addressListElement, addressStreetElement, addressCityElement],
                    rootAuthoredElementValues: {
                        [addressListElement.id]: additionalAddresses,
                    },
                    rootDerivedData: fieldLayoutGalleryDerivedData,
                }}
            >
                <ReplicatingContainerView
                    element={addressListElement}
                    value={additionalAddresses}
                    setValue={setAdditionalAddresses}
                    onBlur={() => undefined}
                    isBusy={false}
                    isDeriving={false}
                    authoredElementValues={{}}
                    onAuthoredElementValuesChange={() => undefined}
                    onElementBlur={() => undefined}
                    derivedData={fieldLayoutGalleryDerivedData}
                    onDerive={async () => fieldLayoutGalleryDerivedData}
                    onEvent={async () => undefined}
                    onResetErrors={() => undefined}
                    suppressErrors={false}
                    derivationTriggerIdQueue={[]}
                />
            </ViewDispatcherContextProvider>
        </Box>
    );
}
