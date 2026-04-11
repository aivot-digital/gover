import {ElementType} from '../data/element-type/element-type';
import {applyComputedErrors, type ComputedElementStates} from '../models/element-data';
import {applyYupErrorsToElementData, mapFormManagerErrorsToComputedErrors} from './gover-schema-to-yup';

describe('mapFormManagerErrorsToComputedErrors', () => {
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
                    {street: 'A'},
                    {street: ''},
                ],
            },
            {
                'config.addresses[1].street': 'Street is required',
            },
            {rootPath: 'config'},
        );
        const existingStates: ComputedElementStates = {
            addresses: {
                subStates: [
                    {
                        street: {
                            visible: false,
                        },
                    },
                    {
                        street: {
                            visible: true,
                        },
                    },
                ],
            },
        };

        expect(computedErrors).toEqual({
            addresses: {
                subStates: [
                    {},
                    {
                        street: {
                            error: 'Street is required',
                        },
                    },
                ],
            },
        });
        expect(applyComputedErrors(computedErrors, existingStates)).toEqual({
            addresses: {
                subStates: [
                    {
                        street: {
                            visible: false,
                        },
                    },
                    {
                        street: {
                            visible: true,
                            error: 'Street is required',
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
