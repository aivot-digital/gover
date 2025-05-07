export enum ShIdAccessLevel {
    Niedrig = 'level1',
    Mittel = 'level3',
    Hoch = 'level4',
}

export const ShIdAccessLevelOptions = [
    {
        value: ShIdAccessLevel.Niedrig,
        label: 'Basisregistrierung (STORK-QAA-Level-1)',
    },
    {
        value: ShIdAccessLevel.Mittel,
        label: 'Substanziell (STORK-QAA-Level-3)',
    },
    {
        value: ShIdAccessLevel.Hoch,
        label: 'Hoch (STORK-QAA-Level-4)',
    },
];
