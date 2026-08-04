import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {ComputedElementValueSource, createDerivedRuntimeElementData} from '../models/element-data';
import {resolveValueForResolvedOverride} from './element-data-utils';
import {AnyElement} from '../models/elements/any-element';

describe('resolveValueForResolvedOverride', () => {
    const field = {
        id: 'field',
        type: ElementType.Text,
    } as AnyElement;

    it('should prefer explicit authored values over stale derived values', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'derived',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {field: 'authored'}, derivedData)).toBe('authored');
        expect(resolveValueForResolvedOverride(field, {field: null}, derivedData)).toBeNull();
    });

    it('should use derived values while no authored override exists', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'derived',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {}, derivedData)).toBe('derived');
    });

    it('should keep identity values authoritative', () => {
        const derivedData = createDerivedRuntimeElementData({
            effectiveValues: {
                field: 'identity',
            },
            elementStates: {
                field: {
                    valueSource: ComputedElementValueSource.Identity,
                },
            },
        });

        expect(resolveValueForResolvedOverride(field, {field: 'authored'}, derivedData)).toBe('identity');
    });
});
