export enum ShIdAccessLevel {
    Optional = 'level0',
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

export const ShIdAccessLevelEgovScope: Record<ShIdAccessLevel, string> = {
    [ShIdAccessLevel.Optional]: 'level1',
    [ShIdAccessLevel.Niedrig]: 'level1',
    [ShIdAccessLevel.Mittel]: 'level3',
    [ShIdAccessLevel.Hoch]: 'level4',
};