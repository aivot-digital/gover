import {NoCodeDataType as T} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {getInputVariableReference, InputVariableSource} from '../../../models/input-mode';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {parseProcessDataPath} from '../../../utils/process-data-path';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {NoCodePresentationContext} from './humanize-no-code';
import {NoCodeEditorNode} from './no-code-editor-store';
import {getReferencedElementOptions} from './no-code-options';
import {parseNoCodeTestValue, ParsedNoCodeTestValue} from './no-code-test-value';

import {
    NoCodeTestSource,
    NoCodeTestSourceDraft,
    NoCodeTestValueDraft,
} from '../../../models/functions/no-code-editor-metadata';

export type {
    NoCodeTestSource, NoCodeTestSourceDraft, NoCodeTestValueDraft, NoCodeTestValueMode,
} from '../../../models/functions/no-code-editor-metadata';

export interface NoCodeTestField {
    key: string;
    label: string;
    hint?: string;
    reference?: string;
    type: T;
    options: NoCodeParameterOption[];
    segments?: string[];
}

export interface NoCodeTestSourcePlan {
    fields: NoCodeTestField[];
    reason?: string;
}

export function parseTestDraft(draft: NoCodeTestValueDraft): ParsedNoCodeTestValue {
    if (draft.mode === 'missing') return {};
    if (draft.mode === 'null') return {value: null};
    if (draft.mode === 'empty') return {value: ''};
    return parseNoCodeTestValue(draft.text, draft.type, draft.mode === 'json');
}

export function testDraftFromValue(value: unknown, type: T = T.Runtime): NoCodeTestValueDraft {
    const actualType = type !== T.Runtime ? type : typeof value === 'number' ? T.Number : typeof value === 'boolean' ? T.Boolean
        : Array.isArray(value) ? T.List : value != null && typeof value === 'object' ? T.Object : T.String;
    if (value === undefined) return {
        text: '',
        type: actualType,
        mode: 'value',
    };
    if (value === null) return {
        text: '',
        type: actualType,
        mode: 'null',
    };
    if (value === '') return {
        text: '',
        type: actualType,
        mode: 'empty',
    };
    const complex = typeof value === 'object';
    return {
        text: complex ? JSON.stringify(value, null, 2) : String(value),
        type: actualType,
        mode: complex ? 'json' : 'value',
    };
}

export function collectNoCodeTestFields(root: NoCodeEditorNode | null, elements: ElementWithParents[], context: NoCodePresentationContext = {}) {
    const elementFields = new Map<string, NoCodeTestField>();
    const sources: Partial<Record<NoCodeTestSource, NoCodeTestSourcePlan>> = {};
    const visit = (node: NoCodeEditorNode | null) => {
        if (node == null) return;
        if (node.type === 'NoCodeExpression') {
            node.operands.forEach(visit);
            return;
        }
        if (node.type === 'NoCodeReference' && node.elementId) {
            const element = elements.find(({element}) => element.id === node.elementId)?.element;
            elementFields.set(node.elementId, {
                key: node.elementId,
                reference: `Element-ID: ${node.elementId}`,
                label: element ? generateComponentTitle(element) : `Element ${node.elementId}`,
                type: element?.type === ElementType.Checkbox ? T.Boolean : element ? NoCodeDataTypeMap[element.type] : T.Runtime,
                options: element ? getReferencedElementOptions(element, context.codeListOptions ?? new Map()) : [],
            });
            return;
        }
        if (node.type !== 'NoCodeProcessDataReference' && node.type !== 'NoCodeInstanceDataReference' && node.type !== 'NoCodeNodeDataReference') return;
        const source: NoCodeTestSource = node.type === 'NoCodeProcessDataReference' ? 'processData' : node.type === 'NoCodeInstanceDataReference' ? 'instanceData' : 'nodeData';
        const variableSource = source === 'processData' ? InputVariableSource.ProcessData : source === 'instanceData' ? InputVariableSource.ProtectedProcessData : InputVariableSource.ElementData;
        const reference = {
            source: variableSource,
            path: node.path ?? '',
            nodeDataKey: node.type === 'NoCodeNodeDataReference' ? node.nodeDataKey : undefined,
        };
        const displayPath = getInputVariableReference(reference);
        const variable = context.variables?.find(candidate => getInputVariableReference(candidate) === displayPath);
        const plan = sources[source] ??= {fields: []};
        const parsed = parseProcessDataPath(node.path ?? '', source === 'nodeData');
        const segments = parsed == null || !parsed.length ? undefined : source === 'nodeData' ? [reference.nodeDataKey ?? '', ...parsed] : parsed;
        // Indices and wildcards need array editing. Never manufacture sparse arrays or guess their contents.
        if (!segments || segments.some((segment, index) => !segment || /^(?:\d+|\*)$/.test(segment) && !(source === 'nodeData' && index === 0) || ['__proto__', 'constructor', 'prototype'].includes(segment))) {
            plan.reason = 'Diese Daten enthalten Listenpfade oder Pfade, die sich nicht als einzelne Eingabefelder darstellen lassen. Verwenden Sie dafür JSON.';
            return;
        }
        const key = JSON.stringify(segments);
        if (!plan.fields.some(field => field.key === key)) plan.fields.push({
            key,
            segments,
            label: variable?.label ?? displayPath,
            reference: displayPath,
            type: T.Runtime,
            options: source === 'processData' ? context.processOptions?.get(node.path ?? '') ?? [] : [],
        });
    };
    visit(root);
    for (const plan of Object.values(sources)) {
        if (plan.fields.some(first => plan.fields.some(second => first !== second && first.segments!.length < second.segments!.length && first.segments!.every((part, i) => second.segments![i] === part)))) {
            plan.reason = 'Der Ausdruck verwendet sowohl einen vollständigen Datenbereich als auch einzelne Werte daraus. Bearbeiten Sie diese zusammengehörigen Testdaten als JSON.';
        }
    }
    return {
        elements: [...elementFields.values()],
        sources,
    };
}

export function parseTestSourceJson(text?: string): { value: Record<string, unknown>; error?: string } {
    if (!text?.trim()) return {value: {}};
    try {
        const value = JSON.parse(text);
        if (value != null && typeof value === 'object' && !Array.isArray(value)) return {value};
    } catch { /* Report syntax errors and a non-object root consistently below. */
    }
    return {
        value: {},
        error: 'Geben Sie ein JSON-Objekt ein, zum Beispiel {"betrag": 120}.',
    };
}

function readPath(value: Record<string, unknown>, segments: string[]): ParsedNoCodeTestValue {
    let current: unknown = value;
    for (const segment of segments) {
        if (current == null || typeof current !== 'object' || Array.isArray(current)) {
            return {error: 'Ein übergeordneter Wert ist kein Datenbereich. Bearbeiten Sie die Testdaten als JSON, damit keine Werte überschrieben werden.'};
        }
        if (!Object.hasOwn(current, segment)) return {};
        current = (current as Record<string, unknown>)[segment];
    }
    return {value: current};
}

export function testSourceFieldError(field: NoCodeTestField, input: NoCodeTestSourceDraft): string | undefined {
    return readPath(parseTestSourceJson(input.json).value, field.segments!).error;
}

export function testSourceFieldDraft(field: NoCodeTestField, input: NoCodeTestSourceDraft): NoCodeTestValueDraft {
    return input.values?.[field.key] ?? testDraftFromValue(readPath(parseTestSourceJson(input.json).value, field.segments!).value, field.type);
}

export function testSourceMode(plan: NoCodeTestSourcePlan, input: NoCodeTestSourceDraft): 'fields' | 'json' {
    return input.mode ?? (plan.reason ? 'json' : 'fields');
}

export function buildTestSource(plan: NoCodeTestSourcePlan, input: NoCodeTestSourceDraft, mode = testSourceMode(plan, input)) {
    const base = parseTestSourceJson(input.json);
    if (base.error || mode === 'json') return base;
    if (plan.reason) return {
        ...base,
        error: plan.reason,
    };
    const value = structuredClone(base.value);
    for (const field of plan.fields) {
        const previous = readPath(value, field.segments!);
        if (previous.error) return {
            value,
            error: previous.error,
        };
        const draft = input.values?.[field.key];
        if (!draft) continue;
        const parsed = parseTestDraft(draft);
        if (parsed.error) return {
            value,
            error: `Prüfen Sie den Testwert für „${field.label}“.`,
        };
        if (parsed.value === undefined && previous.value === undefined) continue;
        let target = value;
        const segments = field.segments!;
        for (const part of segments.slice(0, -1)) {
            if (!Object.hasOwn(target, part)) {
                Object.defineProperty(target, part, {
                    value: {},
                    writable: true,
                    enumerable: true,
                    configurable: true,
                });
            }
            target = target[part] as Record<string, unknown>;
        }
        if (target == null) continue;
        const leaf = segments[segments.length - 1];
        if (parsed.value === undefined) delete target[leaf];
        else Object.defineProperty(target, leaf, {
            value: parsed.value,
            writable: true,
            enumerable: true,
            configurable: true,
        });
    }
    return {value};
}
