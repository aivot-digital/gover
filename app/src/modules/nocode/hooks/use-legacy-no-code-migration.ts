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

interface ControlledLegacyNoCodeMigration<T extends AnyElement> {
    migrations: LegacyNoCodeMigrations;
    original: T;
    onChange: (element: T, migrations: LegacyNoCodeMigrations) => void;
}

export function useLegacyNoCodeMigration<T extends AnyElement>(
    base: T,
    setBase: (element: T) => void,
    controlled?: ControlledLegacyNoCodeMigration<T>,
) {
    const [localMigrations, setMigrations] = useState<LegacyNoCodeMigrations>({});
    // The panel's element is authoritative after draft recovery or a concurrent form save.
    // Keep migration metadata only while an old rule is still being replaced by this edit.
    const migrations: LegacyNoCodeMigrations = controlled == null ? localMigrations : Object.fromEntries(
        Object.entries(controlled.migrations).flatMap(([key, migration]) => {
            const section = key as LegacyNoCodeSection;
            const original = (controlled.original as T & Record<LegacyNoCodeSection, MigratableFunction>)[section];
            const draft = (base as T & Record<LegacyNoCodeSection, MigratableFunction>)[section];
            return hasLegacyNoCode(original) && draft?.type === 'NoCode'
                ? [[section, {...migration, original, draft}]] : [];
        }),
    );
    const current = controlled == null
        ? {...base, ...Object.fromEntries(Object.entries(migrations).map(([section, value]) => [section, value.draft]))} as T
        : base;
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
        if (controlled != null) controlled.onChange(next, updated);
        else {
            setMigrations(updated);
            setBase(persisted);
        }
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
        const nextMigrations = {
            ...migrations,
            [section]: {
                original: structuredClone(original),
                initial: draft,
                draft,
            },
        };
        if (controlled != null) controlled.onChange({...base, [section]: draft}, nextMigrations);
        else setMigrations(nextMigrations);
    };
    const cancel = (section: LegacyNoCodeSection) => {
        const migration = migrations[section];
        if (migration == null) return;
        const next = {...migrations};
        delete next[section];
        if (controlled != null) {
            controlled.onChange({
                ...base,
                [section]: {...migration.original, requirements: migration.draft.requirements},
            }, next);
        } else setMigrations(next);
    };
    return {
        migrations,
        current,
        update,
        start,
        cancel,
        // Controlled drafts live with the form editor and survive unmounting this drawer.
        reset: () => { if (controlled == null) setMigrations({}); },
    };
}
