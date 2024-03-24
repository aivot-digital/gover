export enum BundIdAccessLevel {
    Optional = 'STORK-QAA-Level-0',
    Niedrig = 'STORK-QAA-Level-1',
    Mittel = 'STORK-QAA-Level-3',
    Hoch = 'STORK-QAA-Level-4',
}

export const BundIdAccessLevelOptions = [
    {
        value: BundIdAccessLevel.Optional,
        label: 'Optional',
    },
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
    [BundIdAccessLevel.Optional]: 'level1',
    [BundIdAccessLevel.Niedrig]: 'level1',
    [BundIdAccessLevel.Mittel]: 'level3',
    [BundIdAccessLevel.Hoch]: 'level4',
};