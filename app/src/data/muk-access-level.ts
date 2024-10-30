export enum MukAccessLevel {
    Optional = 'optional',
    Required = 'required',
}

export const MukAccessLevelOptions = [
    {
        value: MukAccessLevel.Optional,
        label: 'Optional',
    },
    {
        value: MukAccessLevel.Required,
        label: 'Pflicht',
    },
];