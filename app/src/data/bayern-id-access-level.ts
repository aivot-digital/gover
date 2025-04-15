export enum BayernIdAccessLevel {
    Optional = 'level0',
    Niedrig = 'level1',
    Mittel = 'level3',
    Hoch = 'level4',
}

export const BayernIdAccessLevelOptions = [
    {
        value: BayernIdAccessLevel.Niedrig,
        label: 'Basisregistrierung (STORK-QAA-Level-1)',
    },
    {
        value: BayernIdAccessLevel.Mittel,
        label: 'Substanziell (STORK-QAA-Level-3)',
    },
    {
        value: BayernIdAccessLevel.Hoch,
        label: 'Hoch (STORK-QAA-Level-4)',
    },
];

export const BayernIdAccessLevelEgovScope: Record<BayernIdAccessLevel, string> = {
    [BayernIdAccessLevel.Optional]: 'level0',
    [BayernIdAccessLevel.Niedrig]: 'level1',
    [BayernIdAccessLevel.Mittel]: 'level3',
    [BayernIdAccessLevel.Hoch]: 'level4',
};