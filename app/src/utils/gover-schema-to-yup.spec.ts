import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {applyComputedErrors, type ComputedElementStates} from '../models/element-data';
import * as yup from 'yup';
import {
    applyYupErrorsToElementData,
    goverSchemaToYup,
    mapFormManagerErrorsToComputedErrors,
} from './gover-schema-to-yup';
import {DateFieldComponentModelMode} from '../models/elements/form/input/date-field-element';

describe('mapFormManagerErrorsToComputedErrors', () => {
    it('should validate children inside replicating container row values', async () => {
        const street = createTextField('street');
        street.required = true;
        const rootElement = createGroupLayout([
            createReplicatingContainer('addresses', [
                street,
            ]),
        ]);
        const schema = yup.object().shape(goverSchemaToYup(rootElement, {}));

        await expect(schema.validate({
            addresses: [
                {
                    id: 'row-1',
                    values: {
                        street: '',
                    },
                },
            ],
        }, {abortEarly: false})).rejects.toMatchObject({
            inner: [
                expect.objectContaining({
                    path: 'addresses[0].values.street',
                }),
            ],
        });
    });

    it('should map top-level form manager errors to computed element errors', () => {
        const rootElement = createGroupLayout([
            createTextField('apiKey'),
        ]);

        expect(mapFormManagerErrorsToComputedErrors(
            rootElement,
            {},
            {
                name: 'Name is required',
                'config.apiKey': 'API key is required',
            },
            {rootPath: 'config'},
        )).toEqual({
            apiKey: {
                error: 'API key is required',
            },
        });
    });

    it('should map descendant paths to the owning composite input element', () => {
        const rootElement = createGroupLayout([
            createMapPointField('location'),
        ]);

        expect(mapFormManagerErrorsToComputedErrors(
            rootElement,
            {},
            {
                'config.location.latitude': 'Latitude is invalid',
            },
            {rootPath: 'config'},
        )).toEqual({
            location: {
                error: 'Latitude is invalid',
            },
        });
    });

    it('should build full replicating container sub state arrays so unaffected rows remain mergeable', () => {
        const rootElement = createGroupLayout([
            createReplicatingContainer('addresses', [
                createTextField('street'),
            ]),
        ]);
        const computedErrors = mapFormManagerErrorsToComputedErrors(
            rootElement,
            {
                addresses: [
                    {
                        id: 'row-1',
                        values: {
                            street: 'A',
                        },
                    },
                    {
                        id: 'row-2',
                        values: {
                            street: '',
                        },
                    },
                ],
            },
            {
                'config.addresses[1].values.street': 'Street is required',
            },
            {rootPath: 'config'},
        );
        const existingStates: ComputedElementStates = {
            addresses: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            street: {
                                visible: false,
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            street: {
                                visible: true,
                            },
                        },
                    },
                ],
            },
        };

        expect(computedErrors).toEqual({
            addresses: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {},
                    },
                    {
                        id: 'row-2',
                        states: {
                            street: {
                                error: 'Street is required',
                            },
                        },
                    },
                ],
            },
        });
        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            addresses: {
                subStates: [
                    {
                        id: 'row-1',
                        states: {
                            street: {
                                visible: false,
                            },
                        },
                    },
                    {
                        id: 'row-2',
                        states: {
                            street: {
                                visible: true,
                                error: 'Street is required',
                            },
                        },
                    },
                ],
            },
        });
    });
});

describe('applyYupErrorsToElementData', () => {
    it('should use the same mapping logic for default data paths', () => {
        const rootElement = createGroupLayout([
            createTextField('title'),
        ]);

        expect(applyYupErrorsToElementData(
            rootElement,
            {},
            {
                'data.title': 'Title is required',
            },
        ).elementStates).toEqual({
            title: {
                error: 'Title is required',
            },
        });
    });
});

describe('temporal range validation', () => {
    it('should validate canonical local date and time ranges', async () => {
        const dateSchema = goverSchemaToYup(createRangeField('date', ElementType.DateRange), {}).date;
        const timeSchema = goverSchemaToYup(createRangeField('time', ElementType.TimeRange), {}).time;

        await expect(dateSchema.validate({
            start: '2026-07-29',
            end: '2026-07-30',
        })).resolves.toBeDefined();
        await expect(dateSchema.validate({
            start: '2026-07-30',
            end: '2026-07-29',
        })).rejects.toThrow('Der Startwert darf nicht größer als der Endwert sein.');
        await expect(timeSchema.validate({
            start: '09:30:15',
            end: '09:30:14',
        })).rejects.toThrow('Der Startwert darf nicht größer als der Endwert sein.');
    });

    it('should compare datetime ranges as explicit instants', async () => {
        const schema = goverSchemaToYup(createRangeField('dateTime', ElementType.DateTimeRange), {}).dateTime;

        await expect(schema.validate({
            start: '2026-07-29T09:30:00+02:00',
            end: '2026-07-29T07:30:01Z',
        })).resolves.toBeDefined();
        await expect(schema.validate({
            start: '2026-07-29T09:30:00',
            end: '2026-07-29T09:31:00',
        })).rejects.toThrow('Der Wert besitzt kein gültiges Datums- oder Zeitformat.');
    });

    it('should validate date ranges according to their configured precision', async () => {
        const monthSchema = goverSchemaToYup(
            createRangeField('month', ElementType.DateRange, DateFieldComponentModelMode.Month),
            {},
        ).month;
        const yearSchema = goverSchemaToYup(
            createRangeField('year', ElementType.DateRange, DateFieldComponentModelMode.Year),
            {},
        ).year;

        await expect(monthSchema.validate({
            start: '2026-07',
            end: '2026-08',
        })).resolves.toBeDefined();
        await expect(monthSchema.validate({
            start: '2026-08',
            end: '2026-07',
        })).rejects.toThrow('Der Startwert darf nicht größer als der Endwert sein.');
        await expect(yearSchema.validate({
            start: '2026',
            end: '2027',
        })).resolves.toBeDefined();
        await expect(yearSchema.validate({
            start: '2026-01-01',
            end: '2027-01-01',
        })).rejects.toThrow('Der Wert besitzt kein gültiges Datums- oder Zeitformat.');
    });

    it('should compare datetime range boundaries below millisecond precision', async () => {
        const schema = goverSchemaToYup(createRangeField('dateTime', ElementType.DateTimeRange), {}).dateTime;

        await expect(schema.validate({
            start: '2026-07-29T07:30:00.000000002Z',
            end: '2026-07-29T09:30:00.000000001+02:00',
        })).rejects.toThrow('Der Startwert darf nicht größer als der Endwert sein.');
    });
});

function createGroupLayout(children: any[]): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: children,
        storeLink: null,
    };
}

function createTextField(id: string): any {
    return {
        id: id,
        type: ElementType.Text,
        label: id,
        required: false,
    };
}

function createMapPointField(id: string): any {
    return {
        id: id,
        type: ElementType.MapPoint,
        label: id,
        required: false,
    };
}

function createReplicatingContainer(id: string, children: any[]): any {
    return {
        id: id,
        type: ElementType.ReplicatingContainer,
        label: id,
        required: false,
        children: children,
    };
}

function createRangeField(
    id: string,
    type: ElementType,
    mode?: DateFieldComponentModelMode,
): any {
    return {
        id,
        type,
        label: id,
        required: false,
        mode,
    };
}
