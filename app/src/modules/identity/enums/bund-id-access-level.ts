export enum BundIdAccessLevel {
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
