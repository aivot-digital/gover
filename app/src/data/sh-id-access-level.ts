import {BayernIdAccessLevel} from './bayern-id-access-level';

export enum ShIdAccessLevel {
    Optional = 'STORK-QAA-Level-0',
    Niedrig = 'STORK-QAA-Level-1',
    Mittel = 'STORK-QAA-Level-3',
    Hoch = 'STORK-QAA-Level-4',
}

export const ShIdAccessLevelOptions = [
    {
        value: ShIdAccessLevel.Optional,
        label: 'Optional',
    },
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