export enum BundIdAccessLevel {
    Optional = 'level0',
    Niedrig = 'level1',
    Mittel = 'level3',
    Hoch = 'level4',
}

export const BundIdAccessLevelOptions = [
    {
        value: BundIdAccessLevel.Niedrig,
        label: 'Basisregistrierung (STORK-QAA-Level-1)',
    },
    {
        value: BundIdAccessLevel.Mittel,
        label: 'Substanziell (STORK-QAA-Level-3)',
    },
    {
        value: BundIdAccessLevel.Hoch,
        label: 'Hoch (STORK-QAA-Level-4)',
    },
];

export const BundIdAccessLevelEgovScope: Record<BundIdAccessLevel, string> = {
    [BundIdAccessLevel.Optional]: 'level0',
    [BundIdAccessLevel.Niedrig]: 'level1',
    [BundIdAccessLevel.Mittel]: 'level3',
    [BundIdAccessLevel.Hoch]: 'level4',
};