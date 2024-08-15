export enum BayernIdAccessLevel {
    Optional = 'STORK-QAA-Level-0',
    Niedrig = 'STORK-QAA-Level-1',
    Mittel = 'STORK-QAA-Level-3',
    Hoch = 'STORK-QAA-Level-4',
}

export const BayernIdAccessLevelOptions = [
    {
        value: BayernIdAccessLevel.Optional,
        label: 'Optional',
    },
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
    [BayernIdAccessLevel.Optional]: 'level1',
    [BayernIdAccessLevel.Niedrig]: 'level1',
    [BayernIdAccessLevel.Mittel]: 'level3',
    [BayernIdAccessLevel.Hoch]: 'level4',
};