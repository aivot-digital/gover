import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {synchronizeAuthoredElementValuesByDestinationPath} from './element-data-utils';

describe('synchronizeAuthoredElementValuesByDestinationPath', () => {
    it('should mirror authored values to inputs with the same destination path', () => {
        const rootElement = createGroupLayout([
            createTextField('firstNameA'),
            createTextField('firstNameB'),
            createTextField('lastName'),
        ]);

        const result = synchronizeAuthoredElementValuesByDestinationPath(
            rootElement,
            {
                firstNameA: 'Ada',
            },
            {
                firstNameA: 'Grace',
            },
            createDerivedRuntimeElementData({
                elementStates: {
                    firstNameA: {
                        destinationPath: 'person.first_name',
                        valueSource: 'Authored',
                    },
                    firstNameB: {
                        destinationPath: 'person.first_name',
                        valueSource: 'Authored',
                    },
                    lastName: {
                        destinationPath: 'person.last_name',
                        valueSource: 'Authored',
                    },
                },
            }),
            ['firstNameA'],
        );

        expect(result.authoredElementValues).toEqual({
            firstNameA: 'Grace',
            firstNameB: 'Grace',
        });
        expect(result.triggeringElementIds).toEqual(expect.arrayContaining(['firstNameA', 'firstNameB']));
    });

    it('should keep replicated rows isolated when their resolved destination paths differ', () => {
        const rootElement = createGroupLayout([
            createReplicatingContainer('rows', [
                createTextField('name'),
            ]),
        ]);

        const result = synchronizeAuthoredElementValuesByDestinationPath(
            rootElement,
            {
                rows: [
                    {name: 'Ada'},
                    {name: 'Grace'},
                ],
            },
            {
                rows: [
                    {name: 'Marie'},
                    {name: 'Grace'},
                ],
            },
            createDerivedRuntimeElementData({
                elementStates: {
                    rows: {
                        subStates: [
                            {
                                name: {
                                    destinationPath: 'members.0.first_name',
                                    valueSource: 'Authored',
                                },
                            },
                            {
                                name: {
                                    destinationPath: 'members.1.first_name',
                                    valueSource: 'Authored',
                                },
                            },
                        ],
                    },
                },
            }),
            ['name'],
        );

        expect(result.authoredElementValues).toEqual({
            rows: [
                {name: 'Marie'},
                {name: 'Grace'},
            ],
        });
    });

    it('should mirror values between different row-local fields that resolve to the same destination path', () => {
        const rootElement = createGroupLayout([
            createReplicatingContainer('rows', [
                createTextField('firstName'),
                createTextField('alias'),
            ]),
        ]);

        const result = synchronizeAuthoredElementValuesByDestinationPath(
            rootElement,
            {
                rows: [
                    {firstName: 'Ada'},
                ],
            },
            {
                rows: [
                    {firstName: 'Grace'},
                ],
            },
            createDerivedRuntimeElementData({
                elementStates: {
                    rows: {
                        subStates: [
                            {
                                firstName: {
                                    destinationPath: 'members.0.first_name',
                                    valueSource: 'Authored',
                                },
                                alias: {
                                    destinationPath: 'members.0.first_name',
                                    valueSource: 'Authored',
                                },
                            },
                        ],
                    },
                },
            }),
            ['firstName'],
        );

        expect(result.authoredElementValues).toEqual({
            rows: [
                {
                    firstName: 'Grace',
                    alias: 'Grace',
                },
            ],
        });
        expect(result.triggeringElementIds).toEqual(expect.arrayContaining(['firstName', 'alias']));
    });

    it('should not overwrite derived targets even if they share the same destination path', () => {
        const rootElement = createGroupLayout([
            createTextField('firstNameA'),
            createTextField('firstNameB'),
        ]);

        const result = synchronizeAuthoredElementValuesByDestinationPath(
            rootElement,
            {
                firstNameA: 'Ada',
            },
            {
                firstNameA: 'Grace',
            },
            createDerivedRuntimeElementData({
                elementStates: {
                    firstNameA: {
                        destinationPath: 'person.first_name',
                        valueSource: 'Authored',
                    },
                    firstNameB: {
                        destinationPath: 'person.first_name',
                        valueSource: 'Derived',
                    },
                },
            }),
            ['firstNameA'],
        );

        expect(result.authoredElementValues).toEqual({
            firstNameA: 'Grace',
        });
        expect(result.triggeringElementIds).toEqual(['firstNameA']);
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

function createReplicatingContainer(id: string, children: any[]): any {
    return {
        id: id,
        type: ElementType.ReplicatingContainer,
        label: id,
        required: false,
        children: children,
    };
}
