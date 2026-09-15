import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {FormField} from '../../../../components/form-field';
import {type UiDefinitionInputFieldComponentProps} from '../../../../components/ui-definition-input-field/ui-definition-input-field-component';
import {ElementType} from '../../../../data/element-type/element-type';
import {type GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {type TextFieldElement} from '../../../../models/elements/form/input/text-field-element';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {type DataObjectSchema, ID_GEN_CUSTOM, ID_GEN_UUID} from '../../models/data-object-schema';
import {DataObjectSchemaDetailsPageIndex, YupSchema} from './data-object-schema-details-page-index';

vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => ({})}));
vi.mock('../../../../hooks/use-change-blocker', () => ({useChangeBlocker: () => ({dialog: null})}));
vi.mock('../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => true}));
vi.mock('../../../../components/ui-definition-input-field/ui-definition-input-field-component', () => ({
    UiDefinitionInputFieldComponent: (props: UiDefinitionInputFieldComponentProps) => (
        <FormField label={props.label} required={props.required ?? false} error={props.error}>
            {field => (
                <div id={field.controlId} role="group" {...field.ariaProps}>
                    <button onClick={() => props.onChange(null)}>Schema leeren</button>
                    <button onClick={() => props.onChange(group(textField('name')))}>Datenfeld hinzufügen</button>
                    <button onClick={() => props.onChange(JSON.parse(JSON.stringify(props.value)))}>Schema unverändert übernehmen</button>
                    <button onClick={() => {
                        const schema = props.value as GroupLayout;
                        props.onChange({
                            ...schema,
                            children: schema.children.map(child => child.id === '$id' ? {...child, label: 'Aktennummer'} : child),
                        });
                    }}>ID-Feld anpassen</button>
                </div>
            )}
        </FormField>
    ),
}));

function textField(id: string): TextFieldElement {
    return {...generateElementWithDefaultValues(ElementType.Text), id, label: 'Name'} as TextFieldElement;
}

function group(...children: GroupLayout['children']): GroupLayout {
    return {...generateElementWithDefaultValues(ElementType.GroupLayout), children} as GroupLayout;
}

function model(schema: GroupLayout = group()): DataObjectSchema {
    return {
        key: 'test', name: 'Testmodell', description: 'Eine Beschreibung', idGen: ID_GEN_UUID,
        schema, displayFields: [], created: '', updated: '',
    };
}

function renderPage(item = model(), isNewItem = true) {
    const create = vi.spyOn(DataObjectSchemasApiService.prototype, 'create').mockImplementation(() => new Promise(() => {}));
    const update = vi.spyOn(DataObjectSchemasApiService.prototype, 'update').mockImplementation(() => new Promise(() => {}));
    render(
        <MemoryRouter>
            <GenericDetailsPageContext.Provider value={{
                item, isNewItem, isEditable: true, isBusy: false,
                setItem: vi.fn(), setIsBusy: vi.fn(), setAdditionalData: vi.fn(), refresh: vi.fn(),
            }}>
                <DataObjectSchemaDetailsPageIndex/>
            </GenericDetailsPageContext.Provider>
        </MemoryRouter>,
    );
    return {create, update};
}

describe('DataObjectSchemaDetailsPageIndex', () => {
    it.each(['UUID', 'Seriell fortlaufend', 'Formatvorlage'])('removes only the unchanged generated ID field when switching to %s', alternative => {
        const original = model(group(textField('name')));
        const {create} = renderPage(original);
        fireEvent.change(screen.getByRole('textbox', {name: 'Name'}), {target: {value: 'Kopie'}});
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('button', {name: 'Schema unverändert übernehmen'}));

        fireEvent.click(screen.getByRole('radio', {name: alternative}));
        if (alternative === 'Formatvorlage') {
            fireEvent.change(screen.getByRole('textbox', {name: 'ID-Formatvorlage'}), {target: {value: 'TEST-%I4'}});
        }
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).toHaveBeenCalledTimes(1);
        expect(create.mock.calls[0][0].schema).toEqual(original.schema);
    });

    it('preserves a generated field after customization across repeated ID type changes', () => {
        const {create} = renderPage();
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('button', {name: 'ID-Feld anpassen'}));

        for (const name of ['Seriell fortlaufend', 'Manuell festgelegt', 'UUID']) {
            fireEvent.click(screen.getByRole('radio', {name}));
        }
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).toHaveBeenCalledTimes(1);
        expect(create.mock.calls[0][0].schema.children).toHaveLength(1);
        expect(create.mock.calls[0][0].schema.children[0]).toMatchObject({id: '$id', label: 'Aktennummer', required: true});
    });

    it('preserves a pre-existing field even when it exactly matches the generated default', () => {
        const original = model(group({
            ...generateElementWithDefaultValues(ElementType.Text),
            id: '$id', name: 'ID', label: 'ID', hint: 'Eindeutige ID des Datenobjekts', required: true,
        } as TextFieldElement));
        const {create} = renderPage(original);
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('radio', {name: 'Seriell fortlaufend'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).toHaveBeenCalledTimes(1);
        expect(create.mock.calls[0][0].schema).toEqual(original.schema);
    });

    it('rejects saving an empty schema after removing the generated ID field', () => {
        const {create} = renderPage();
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('radio', {name: 'Seriell fortlaufend'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).not.toHaveBeenCalled();
        expect(screen.getByRole('group', {name: 'Datenschema'}))
            .toHaveAccessibleDescription('Das Datenschema muss mindestens ein Datenfeld enthalten.');
    });

    it.each(['UUID', 'Seriell fortlaufend', 'Formatvorlage'])('can return to manual IDs after switching to %s without duplicating the ID field', alternative => {
        const {create} = renderPage();

        for (let iteration = 0; iteration < 2; iteration++) {
            fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
            expect(screen.getByRole('radio', {name: 'Manuell festgelegt'})).toBeChecked();
            fireEvent.click(screen.getByRole('radio', {name: alternative}));
            expect(screen.getByRole('radio', {name: alternative})).toBeChecked();
        }
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).toHaveBeenCalledTimes(1);
        const saved = create.mock.calls[0][0];
        expect(saved.idGen).toBe(ID_GEN_CUSTOM);
        expect(saved.schema.children).toHaveLength(1);
        expect(saved.schema.children[0]).toMatchObject({id: '$id', type: ElementType.Text, required: true});
    });

    it('preserves a pre-existing customized ID field when selecting manual IDs', () => {
        const idField = {...textField('$id'), label: 'Aktennummer', hint: 'Eigene Kennung', required: true};
        const original = model(group(idField, textField('name')));
        const {create} = renderPage(original);

        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        expect(screen.getByRole('radio', {name: 'Manuell festgelegt'})).toBeChecked();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create.mock.calls[0][0].schema).toEqual(original.schema);
    });

    it('marks the schema required and rejects an empty group on save', () => {
        const {create} = renderPage();
        expect(screen.getByRole('group', {name: 'Datenschema'})).toHaveAttribute('aria-required', 'true');
        fireEvent.change(screen.getByRole('textbox', {name: 'Name'}), {target: {value: 'Neuer Name'}});

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create).not.toHaveBeenCalled();
        expect(screen.getByRole('group', {name: 'Datenschema'}))
            .toHaveAccessibleDescription('Das Datenschema muss mindestens ein Datenfeld enthalten.');
    });

    it.each([true, false])('validates clearing and correcting the schema (new: %s)', isNew => {
        const {create, update} = renderPage(model(group(textField('original'))), isNew);
        fireEvent.click(screen.getByRole('button', {name: 'Schema leeren'}));
        expect(screen.getByRole('group', {name: 'Datenschema'})).toHaveAttribute('aria-invalid', 'true');
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(create).not.toHaveBeenCalled();
        expect(update).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Datenfeld hinzufügen'}));
        expect(screen.getByRole('group', {name: 'Datenschema'})).not.toHaveAttribute('aria-invalid', 'true');
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(isNew ? create : update).toHaveBeenCalledTimes(1);
    });

    it('can select manual IDs after the schema has been cleared', () => {
        const {create} = renderPage();
        fireEvent.click(screen.getByRole('button', {name: 'Schema leeren'}));
        fireEvent.click(screen.getByRole('radio', {name: 'Manuell festgelegt'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(create.mock.calls[0][0]).toMatchObject({
            idGen: ID_GEN_CUSTOM,
            schema: {type: ElementType.GroupLayout, children: [{id: '$id', required: true}]},
        });
    });
});

describe('data model schema validation', () => {
    it.each([undefined, null, group(), group(group()), group({
        ...generateElementWithDefaultValues(ElementType.ReplicatingContainer), children: [],
    } as GroupLayout['children'][number])])('rejects a missing schema or empty layout: %j', schema => {
        expect(() => YupSchema.validateSync({...model(), schema})).toThrow();
    });

    it.each([
        group(textField('name')),
        group(group(textField('name'))),
        group({
            ...generateElementWithDefaultValues(ElementType.ReplicatingContainer), children: [textField('name')],
        } as GroupLayout['children'][number]),
        group(generateElementWithDefaultValues(ElementType.Table) as GroupLayout['children'][number]),
    ])('accepts a schema with data fields: %j', schema => {
        expect(() => YupSchema.validateSync(model(schema))).not.toThrow();
    });
});
