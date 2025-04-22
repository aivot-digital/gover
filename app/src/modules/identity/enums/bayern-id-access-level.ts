export enum BayernIdAccessLevel {
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