import {useState} from 'react';
import {AnyElement} from '../../../models/elements/any-element';
import {ElementVisibilityFunction} from '../../../models/elements/element-visibility-function';
import {ElementValidationFunction} from '../../../models/elements/element-validation-function';

export type LegacyNoCodeSection = 'visibility' | 'validation';
export type MigratableFunction = ElementVisibilityFunction | ElementValidationFunction;

export interface LegacyNoCodeMigration {
    original: MigratableFunction;
    initial: MigratableFunction;
    draft: MigratableFunction;
}

export type LegacyNoCodeMigrations = Partial<Record<LegacyNoCodeSection, LegacyNoCodeMigration>>;
export const hasLegacyNoCode = (fn: MigratableFunction | null | undefined) => fn?.conditionSet != null || fn?.type === 'ConditionSet';

export function useLegacyNoCodeMigration<T extends AnyElement>(base: T, setBase: (element: T) => void) {
    const [migrations, setMigrations] = useState<LegacyNoCodeMigrations>({});
    const current = {...base, ...Object.fromEntries(Object.entries(migrations).map(([section, value]) => [section, value.draft]))} as T;
    const update = (next: T) => {
        const persisted = {...next};
        const updated = {...migrations};
        for (const section of Object.keys(migrations) as LegacyNoCodeSection[]) {
            const draft = (next as T & Record<LegacyNoCodeSection, MigratableFunction>)[section];
            updated[section] = {
                ...migrations[section]!,
                draft,
            };
            // Requirements are independent of converting the rule and survive a targeted cancellation.
            Object.assign(persisted, {
                [section]: {
                    ...(base as T & Record<LegacyNoCodeSection, MigratableFunction>)[section],
                    requirements: draft.requirements,
                },
            });
        }
        setMigrations(updated);
        setBase(persisted);
    };
    const start = (section: LegacyNoCodeSection) => {
        const original = (base as T & Record<LegacyNoCodeSection, MigratableFunction>)[section];
        if (!hasLegacyNoCode(original) || migrations[section] != null) return;
        const draft: MigratableFunction = {
            ...original,
            type: 'NoCode',
            conditionSet: null,
            javascriptCode: null,
            referencedIds: [],
            ...(section === 'visibility' ? {noCode: null} : {
                noCodeList: [{
                    noCode: null,
                    message: '',
                }],
            }),
        };
        setMigrations({
            ...migrations,
            [section]: {
                original: structuredClone(original),
                initial: draft,
                draft,
            },
        });
    };
    const cancel = (section: LegacyNoCodeSection) => setMigrations((state) => {
        const next = {...state};
        delete next[section];
        return next;
    });
    return {
        migrations,
        current,
        update,
        start,
        cancel,
        reset: () => setMigrations({}),
    };
}
