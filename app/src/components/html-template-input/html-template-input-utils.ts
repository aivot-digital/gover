import {AssetsApiService} from '../../modules/assets/assets-api-service';

export const SupportedHtmlTemplateSlotTypes = ['image', 'text', 'richtext'] as const;

export type HtmlTemplateSlotType = typeof SupportedHtmlTemplateSlotTypes[number];

export interface HtmlTemplateSlot {
    id: string;
    type: HtmlTemplateSlotType;
    label: string;
    defaultValue: string | null;
}

export interface UnsupportedHtmlTemplateSlot {
    id: string;
    type: string | null;
}

export interface ParsedHtmlTemplate {
    slots: HtmlTemplateSlot[];
    unsupportedSlots: UnsupportedHtmlTemplateSlot[];
}

function isSupportedSlotType(value: string | null): value is HtmlTemplateSlotType {
    return SupportedHtmlTemplateSlotTypes.includes(value as HtmlTemplateSlotType);
}

function formatSlotLabel(slotId: string): string {
    const label = slotId
        .replace(/[_-]+/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

    if (label.length === 0) {
        return slotId;
    }

    return label.substring(0, 1).toUpperCase() + label.substring(1);
}

function getSlotDefaultValue(element: Element, type: HtmlTemplateSlotType): string | null {
    if (type === 'image') {
        const src = element.getAttribute('src')?.trim();
        return src != null && src.length > 0 ? src : null;
    }

    const text = element.textContent?.trim();
    return text != null && text.length > 0 ? text : null;
}

export function parseHtmlTemplateSlots(templateHtml: string): ParsedHtmlTemplate {
    const document = new DOMParser().parseFromString(templateHtml, 'text/html');
    const slotElements = Array.from(document.querySelectorAll('[data-slot]'));
    const slotsById = new Map<string, HtmlTemplateSlot>();
    const unsupportedSlots: UnsupportedHtmlTemplateSlot[] = [];

    for (const element of slotElements) {
        const id = element.getAttribute('data-slot')?.trim();
        if (id == null || id.length === 0) {
            continue;
        }

        const type = element.getAttribute('data-slot-type')?.trim().toLowerCase() ?? null;
        if (!isSupportedSlotType(type)) {
            if (!unsupportedSlots.some((slot) => slot.id === id)) {
                unsupportedSlots.push({id, type});
            }
            continue;
        }

        if (slotsById.has(id)) {
            continue;
        }

        const configuredLabel = element.getAttribute('data-slot-label')?.trim();
        slotsById.set(id, {
            id,
            type,
            label: configuredLabel != null && configuredLabel.length > 0 ? configuredLabel : formatSlotLabel(id),
            defaultValue: getSlotDefaultValue(element, type),
        });
    }

    return {
        slots: Array.from(slotsById.values()),
        unsupportedSlots,
    };
}

export function applyHtmlTemplateSlotValues(
    templateHtml: string,
    slotValues: Record<string, string | null>,
    renderRichText: (markdown: string) => string,
): string {
    const document = new DOMParser().parseFromString(templateHtml, 'text/html');
    const slotElements = Array.from(document.querySelectorAll('[data-slot]'));

    for (const element of slotElements) {
        const id = element.getAttribute('data-slot')?.trim();
        if (id == null || id.length === 0) {
            continue;
        }

        const rawValue = slotValues[id];
        if (rawValue == null || rawValue.trim().length === 0) {
            continue;
        }

        const type = element.getAttribute('data-slot-type')?.trim().toLowerCase() ?? null;
        if (type === 'image') {
            element.setAttribute('src', AssetsApiService.useAssetLink(rawValue));
        } else if (type === 'text') {
            element.textContent = rawValue;
        } else if (type === 'richtext') {
            element.innerHTML = renderRichText(rawValue);
        }
    }

    return `<!doctype html>\n${document.documentElement.outerHTML}`;
}
