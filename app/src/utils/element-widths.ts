import {ElementType} from '../data/element-type/element-type';
import {AnyElement} from '../models/elements/any-element';

export const DEFAULT_ELEMENT_WEIGHT = 12;
export const COMPACT_DEFAULT_ELEMENT_WEIGHT = 6;

export interface ElementWidthOption {
    value: number;
    label: string;
}

export interface ElementWidthChoice extends ElementWidthOption {
    disabled: boolean;
    disabledReason?: string;
    fractionLabel: string;
    widthPercent: number;
}

export const ElementWidthOptions: readonly ElementWidthOption[] = [
    {
        label: '25 %',
        value: 3,
    },
    {
        label: '33 %',
        value: 4,
    },
    /* 37,5 and 62,5 are odd additional options which are probably not necessary as 33 and 66 % give a similar result
       todo: remove at some time if nobody misses these options */
    /*{
        label: '37,5 %',
        value: 4.5,
    },*/
    {
        label: '50 %',
        value: 6,
    },
    /*{
        label: '62,5 %',
        value: 7.5,
    },*/
    {
        label: '66 %',
        value: 8,
    },
    {
        label: '75 %',
        value: 9,
    },
    {
        label: '100 %',
        value: 12,
    },
] as const;

const minimumElementWeightMap: Partial<Record<ElementType, number>> = {
    [ElementType.DateTimeRange]: 6,
    [ElementType.DateRange]: 6,
    [ElementType.TimeRange]: 6,
    [ElementType.MapPoint]: 6,
    [ElementType.ReplicatingContainer]: 4,
    [ElementType.Table]: 4,
};

const compactDefaultElementTypes = new Set<ElementType>([
    ElementType.Date,
    ElementType.Number,
    ElementType.Select,
    ElementType.Radio,
    ElementType.Checkbox,
    ElementType.MultiCheckbox,
    ElementType.Text,
    ElementType.Time,
    ElementType.ChipInput,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.DataModelSelect,
    ElementType.DataObjectSelect,
]);

function getResolvedParentElementWeight(parentElement?: AnyElement): number {
    if (parentElement == null || !('weight' in parentElement)) {
        return DEFAULT_ELEMENT_WEIGHT;
    }

    return parentElement.weight ?? DEFAULT_ELEMENT_WEIGHT;
}

export function getMinimumElementWeight(type: ElementType): number {
    return minimumElementWeightMap[type] ?? 3;
}

export function getDefaultElementWeight(type: ElementType, parentElement?: AnyElement): number {
    if (!compactDefaultElementTypes.has(type)) {
        return DEFAULT_ELEMENT_WEIGHT;
    }

    if (getResolvedParentElementWeight(parentElement) < DEFAULT_ELEMENT_WEIGHT) {
        return DEFAULT_ELEMENT_WEIGHT;
    }

    return COMPACT_DEFAULT_ELEMENT_WEIGHT;
}

export function formatElementWeight(weight: number): string {
    return ElementWidthOptions.find(option => option.value === weight)?.label ?? `${(weight / DEFAULT_ELEMENT_WEIGHT) * 100}%`;
}

export function normalizeElementWeight(type: ElementType, weight: number | null | undefined): number {
    const resolvedWeight = weight ?? DEFAULT_ELEMENT_WEIGHT;
    return Math.max(getMinimumElementWeight(type), resolvedWeight);
}

export function getElementWidthChoices(type: ElementType): ElementWidthChoice[] {
    const minimumWeight = getMinimumElementWeight(type);
    const minimumWeightLabel = formatElementWeight(minimumWeight);

    return ElementWidthOptions.map(option => ({
        ...option,
        disabled: option.value < minimumWeight,
        disabledReason: option.value < minimumWeight
            ? `Für diesen Elementtyp sind mindestens ${minimumWeightLabel} Breite erforderlich.`
            : undefined,
        fractionLabel: `${option.value}/12`,
        widthPercent: option.value / DEFAULT_ELEMENT_WEIGHT * 100,
    }));
}

export function getElementWidthRestrictionHint(type: ElementType): string | undefined {
    const minimumWeight = getMinimumElementWeight(type);
    if (minimumWeight <= 3) {
        return undefined;
    }

    return `Für diesen Elementtyp ist mindestens eine Breite von ${formatElementWeight(minimumWeight)} erforderlich.`;
}
