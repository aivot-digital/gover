import {type AtomicComparisonValue, deepEquals} from './equality-utils';

/**
 * A change that the person and another save made differently to the same value.
 * `path` addresses the value in the merged object; items of keyed arrays appear as `[key]`.
 */
export interface MergeConflict {
    id: string;
    path: string[];
    base: unknown;
    mine: unknown;
    theirs: unknown;
    /** The selected side either keeps the element in this container path or removes it. */
    elementPlacement?: {elementId: string; mine?: string; theirs?: string};
}

export type MergeChoice = 'mine' | 'theirs';

/** The chosen side per conflict, keyed by {@link MergeConflict.id}. Unresolved conflicts keep the stored state. */
export type MergeChoices = Record<string, MergeChoice>;

export interface MergeResult<T> {
    merged: T;
    conflicts: MergeConflict[];
}

interface SubtreeStates {
    base: unknown;
    mine: unknown;
    theirs: unknown;
    interruptedWrite?: {value: unknown};
}

export interface SubtreeMergeInput extends SubtreeStates {
    path: string[];
    choices: MergeChoices;
    /** Merge the remaining values at the same path, preserving regular field and ordering decisions. */
    mergeRest: (states: SubtreeStates) => MergeResult<unknown>;
}

export interface MergeConflictPresentation {
    group: {
        id: string;
        label: string;
    };
    propertyLabel: string;
}

/**
 * Describes how the objects of one type are merged.
 * Each type that can be recovered from a lock version conflict defines its own spec next to its entity.
 */
export interface MergeSpec<T> {
    /** May handle an entire structural conflict at a domain-defined tree root. */
    mergeSubtree?: (input: SubtreeMergeInput) => MergeResult<unknown> | undefined;
    /** Top-level fields that the server maintains, such as the lock version. They are always taken from the stored state. */
    serverManaged?: string[];
    /**
     * The item property that identifies the items of the array at the path, such as `id` for form elements.
     * Arrays without a key, or whose items lack a unique key, are merged as a whole.
     */
    arrayKey?: (path: string[]) => string | undefined;
    /** Compare a domain choice by this value, but always resolve a conflict with the complete original selection. */
    atomicValue?: AtomicComparisonValue;
    /** Applied to all three states first, so that equivalent representations are no conflict. */
    normalize?: (value: T) => T;
    /** Checks invariants of the merged object. A violation turns the whole object into a single conflict. */
    validate?: (merged: T) => boolean;
    /** A readable name for a conflict path, shown in the conflict dialog. */
    label?: (path: string[], states: {base: T; mine: T; theirs: T}) => string;
    /** Stable grouping and a separate property name for the conflict dialog. */
    describeConflict?: (path: string[], states: {base: T; mine: T; theirs: T}) => MergeConflictPresentation;
    /** A domain decision that can be made without asking the person. Explicit choices take precedence. */
    defaultChoice?: (conflict: MergeConflict) => MergeChoice | undefined;
}

export const ORDER_SEGMENT = '#order';

/**
 * Merges the changes of the person (`mine`) and of another save (`theirs`) that both started from `base`.
 * A value changed on one side only takes that change; a value changed differently on both sides is a conflict and
 * keeps the stored state (`theirs`) until a choice for it is passed.
 * If an interrupted write has an unknown outcome, its submitted values are also possible server baselines.
 */
export function threeWayMerge<T>(
    base: T, mine: T, theirs: T, spec: MergeSpec<T> = {}, choices: MergeChoices = {},
    interruptedWrite?: {value: T},
): MergeResult<T> {
    const normalize = spec.normalize ?? ((value: T) => value);
    const normalizedBase = normalize(base);
    const normalizedMine = normalize(mine);
    const normalizedTheirs = normalize(theirs);

    const rootChoice = choices[pathToId([])];
    if (rootChoice != null) {
        return {
            merged: rootChoice === 'mine' ? withServerManaged(normalizedMine, normalizedTheirs, spec) : normalizedTheirs,
            conflicts: [],
        };
    }

    const conflicts: MergeConflict[] = [];
    const merged = mergeValue(normalizedBase, withServerManaged(normalizedMine, normalizedTheirs, spec), normalizedTheirs, [], {
        arrayKey: spec.arrayKey ?? (() => undefined),
        atomicValue: spec.atomicValue,
        mergeSubtree: spec.mergeSubtree,
        choices,
        conflicts,
        interrupted: interruptedWrite != null,
        defaultChoice: spec.defaultChoice,
    }, interruptedWrite == null ? undefined : normalize(interruptedWrite.value)) as T;

    if (spec.validate != null && !spec.validate(merged)) {
        // Merging parts would break the object, e.g. an element that one side moved and the other changed would
        // appear twice. The person then decides for the whole object instead.
        return {
            merged: normalizedTheirs,
            conflicts: [{
                id: pathToId([]),
                path: [],
                base: normalizedBase,
                mine: normalizedMine,
                theirs: normalizedTheirs,
            }],
        };
    }

    return {merged, conflicts};
}

export function pathToId(path: string[]): string {
    return path.join('/');
}

/** Identifies items of arrays whose items carry a unique `id`, such as form elements. */
export const keyById = (): string => 'id';

interface MergeContext {
    mergeSubtree?: MergeSpec<unknown>['mergeSubtree'];
    arrayKey: (path: string[]) => string | undefined;
    atomicValue?: AtomicComparisonValue;
    choices: MergeChoices;
    conflicts: MergeConflict[];
    interrupted: boolean;
    defaultChoice?: MergeSpec<unknown>['defaultChoice'];
}

function mergeValue(
    base: unknown, mine: unknown, theirs: unknown, path: string[], context: MergeContext, submitted?: unknown,
): unknown {
    if (context.mergeSubtree) {
        const result = context.mergeSubtree({
            base, mine, theirs, path, choices: context.choices,
            interruptedWrite: context.interrupted ? {value: submitted} : undefined,
            mergeRest: (states) => {
                const conflicts: MergeConflict[] = [];
                const merged = mergeOrdinaryValue(states.base, states.mine, states.theirs, path,
                    {...context, conflicts, interrupted: states.interruptedWrite != null}, states.interruptedWrite?.value);
                return {merged, conflicts};
            },
        });
        if (result) {
            context.conflicts.push(...result.conflicts);
            return result.merged;
        }
    }
    return mergeOrdinaryValue(base, mine, theirs, path, context, submitted);
}

function mergeOrdinaryValue(
    base: unknown, mine: unknown, theirs: unknown, path: string[], context: MergeContext, submitted?: unknown,
): unknown {
    const {atomicValue} = context;
    if (deepEquals(mine, theirs, atomicValue)) {
        return theirs;
    }
    // After an interrupted PUT, both the original and the submitted value are known server
    // candidates. An edit back to the original value is still deliberate if the PUT committed.
    if (deepEquals(mine, base, atomicValue) && (!context.interrupted || deepEquals(submitted, base, atomicValue))) {
        return theirs;
    }
    if (deepEquals(theirs, base, atomicValue) || (context.interrupted && deepEquals(theirs, submitted, atomicValue))) {
        return mine;
    }

    if (atomicValue?.(mine) !== undefined || atomicValue?.(theirs) !== undefined) {
        return conflict(base, mine, theirs, path, context);
    }

    // A different `type`, such as the input mode of an authored value, gives the other fields their meaning, so the
    // object is decided as a whole and keeps a type that matches its content.
    if (isPlainObject(mine) && isPlainObject(theirs) && mine.type === theirs.type) {
        return mergeObject(isPlainObject(base) ? base : {}, mine, theirs, path, context, isPlainObject(submitted) ? submitted : {});
    }

    if (Array.isArray(mine) && Array.isArray(theirs)) {
        const key = context.arrayKey(path);
        const baseItems = Array.isArray(base) ? base : [];
        if (key != null && [baseItems, mine, theirs].every((items) => hasUniqueKeys(items, key))) {
            return mergeKeyedArray(baseItems, mine, theirs, key, path, context, Array.isArray(submitted) ? submitted : []);
        }
    }

    return conflict(base, mine, theirs, path, context);
}

function mergeObject(
    base: Record<string, unknown>, mine: Record<string, unknown>, theirs: Record<string, unknown>,
    path: string[], context: MergeContext, submitted: Record<string, unknown>,
): Record<string, unknown> {
    const result: Record<string, unknown> = {};
    const keys = new Set([...Object.keys(theirs), ...Object.keys(mine)]);

    for (const key of keys) {
        const merged = mergeValue(base[key], mine[key], theirs[key], [...path, key], context, submitted[key]);
        // Preserve explicit undefined fields common to both editor models. Dropping them would
        // mark an unchanged model dirty, while reintroducing a missing key would undo a removal.
        if (merged !== undefined || (Object.hasOwn(mine, key) && Object.hasOwn(theirs, key))) {
            result[key] = merged;
        }
    }

    return result;
}

function mergeKeyedArray(
    base: unknown[], mine: unknown[], theirs: unknown[], key: string, path: string[], context: MergeContext, submitted: unknown[],
): unknown[] {
    const byKey = (items: unknown[]) => new Map(items.map((item) => [itemKey(item, key), item]));
    const baseItems = byKey(base);
    const mineItems = byKey(mine);
    const theirItems = byKey(theirs);
    const sentItems = context.interrupted && hasUniqueKeys(submitted, key) ? byKey(submitted) : undefined;

    const mergedItems = new Map<string, unknown>();
    for (const itemId of new Set([...theirItems.keys(), ...mineItems.keys()])) {
        const merged = mergeValue(
            baseItems.get(itemId), mineItems.get(itemId), theirItems.get(itemId),
            [...path, `[${itemId}]`], context, sentItems?.get(itemId),
        );
        if (merged !== undefined) {
            mergedItems.set(itemId, merged);
        }
    }

    const order = mergeOrder(
        base.map((item) => itemKey(item, key)),
        mine.map((item) => itemKey(item, key)),
        theirs.map((item) => itemKey(item, key)),
        [...path, ORDER_SEGMENT],
        context,
        sentItems == null ? [] : Array.from(sentItems.keys()),
    );

    return placeItems(order.primary, order.secondary, mergedItems);
}

/**
 * Takes the order of the side that reordered the items. If both sides reordered them differently, the order is a
 * conflict of its own, so that the content of the items can still be merged.
 */
function mergeOrder(
    base: string[], mine: string[], theirs: string[], path: string[], context: MergeContext, submitted: string[],
): {primary: string[]; secondary: string[]} {
    const common = new Set(base.filter((itemId) => mine.includes(itemId) && theirs.includes(itemId)));
    const relative = (order: string[]) => order.filter((itemId) => common.has(itemId));

    const baseOrder = relative(base);
    const mineOrder = relative(mine);
    const theirOrder = relative(theirs);

    const sentOrder = context.interrupted ? relative(submitted) : baseOrder;
    if (deepEquals(mineOrder, theirOrder) || (deepEquals(mineOrder, baseOrder) && deepEquals(sentOrder, baseOrder))) {
        return {primary: theirs, secondary: mine};
    }
    if (deepEquals(theirOrder, baseOrder) || deepEquals(theirOrder, sentOrder)) {
        return {primary: mine, secondary: theirs};
    }

    const choice = conflictChoice(baseOrder, mineOrder, theirOrder, path, context);
    return choice === 'mine' ? {primary: mine, secondary: theirs} : {primary: theirs, secondary: mine};
}

/** Lists the merged items in the primary order and inserts the items only the other side has after their predecessor there. */
function placeItems(primary: string[], secondary: string[], items: Map<string, unknown>): unknown[] {
    const order = primary.filter((itemId) => items.has(itemId));

    secondary.forEach((itemId, index) => {
        if (!items.has(itemId) || order.includes(itemId)) {
            return;
        }

        const predecessor = secondary
            .slice(0, index)
            .reverse()
            .find((candidate) => order.includes(candidate));
        order.splice(predecessor == null ? 0 : order.indexOf(predecessor) + 1, 0, itemId);
    });

    return order.map((itemId) => items.get(itemId));
}

function conflict(base: unknown, mine: unknown, theirs: unknown, path: string[], context: MergeContext): unknown {
    return conflictChoice(base, mine, theirs, path, context) === 'mine' ? mine : theirs;
}

function conflictChoice(base: unknown, mine: unknown, theirs: unknown, path: string[], context: MergeContext): MergeChoice | undefined {
    const id = pathToId(path);
    const conflict = {id, path, base, mine, theirs};
    const choice = context.choices[id] ?? context.defaultChoice?.(conflict);
    if (choice == null) {
        context.conflicts.push(conflict);
    }
    return choice;
}

function withServerManaged<T>(mine: T, theirs: T, spec: MergeSpec<T>): T {
    if (spec.serverManaged == null || !isPlainObject(mine) || !isPlainObject(theirs)) {
        return mine;
    }

    const result: Record<string, unknown> = {...mine};
    for (const field of spec.serverManaged) {
        if (field in theirs) {
            result[field] = theirs[field];
        } else {
            delete result[field];
        }
    }
    return result as T;
}

function hasUniqueKeys(items: unknown[], key: string): boolean {
    const keys = items.map((item) => isPlainObject(item) ? item[key] : undefined);
    return keys.every((value) => typeof value === 'string' || typeof value === 'number') && new Set(keys).size === keys.length;
}

function itemKey(item: unknown, key: string): string {
    return String((item as Record<string, unknown>)[key]);
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object' && !Array.isArray(value);
}
