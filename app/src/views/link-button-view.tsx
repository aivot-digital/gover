import React from 'react';
import {Button} from '@mui/material';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import LinkIcon from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import type {BaseViewProps} from './base-view';
import type {
    LinkButtonElement,
    LinkButtonElementColor,
    LinkButtonElementVariant,
} from '../models/elements/form/content/link-button-element';
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';
import {StepIconsMap} from '../data/step-icons';

const DefaultLabel = 'Link öffnen';

export function LinkButtonView(props: BaseViewProps<LinkButtonElement, void>): React.JSX.Element {
    const {
        element,
        authoredElementValues,
        isBusy,
        onEvent,
    } = props;
    const {readOnly, taskViewMode} = useViewDispatcherContext();

    const label = resolveLabel(element.label);
    const href = resolveHref(element.href);
    const variant = resolveVariant(element.variant);
    const color = resolveColor(element.color);
    const openInNewTab = element.openInNewTab !== false;
    const IconComponent = element.icon ? StepIconsMap[element.icon]?.def : undefined;
    const startIcon = IconComponent ? <IconComponent/> : undefined;

    if (href != null) {
        return (
            <Button
                variant={variant}
                color={color}
                href={href}
                target={openInNewTab ? '_blank' : undefined}
                rel={openInNewTab ? 'noopener noreferrer' : undefined}
                startIcon={startIcon}
                endIcon={openInNewTab ? <OpenInNew sx={{opacity: 0.65}}/> : <LinkIcon sx={{opacity: 0.65}}/>}
                disabled={isBusy}
            >
                {label}
            </Button>
        );
    }

    // A configured link takes precedence, even if its URL is invalid. Do not fall back to a
    // different action from a legacy mixed configuration, matching the backend's inline-event discovery.
    const event = element.href?.trim() ? null : resolveTaskEvent(element, taskViewMode);
    const disabled = isBusy || readOnly === true || event == null;

    return (
        <Button
            variant={variant}
            color={color}
            startIcon={startIcon}
            onClick={() => {
                if (event != null) {
                    void onEvent(authoredElementValues, event);
                }
            }}
            disabled={disabled}
        >
            {label}
        </Button>
    );
}

function resolveLabel(label: string | null | undefined): string {
    const normalizedLabel = label?.trim();
    return normalizedLabel == null || normalizedLabel.length === 0 ? DefaultLabel : normalizedLabel;
}

function resolveHref(href: string | null | undefined): string | null {
    const normalizedHref = href?.trim();
    if (normalizedHref == null || normalizedHref.length === 0) {
        return null;
    }

    if (
        normalizedHref.startsWith('/') && !normalizedHref.startsWith('//') ||
        normalizedHref.startsWith('#') ||
        /^https?:\/\//i.test(normalizedHref) ||
        /^mailto:/i.test(normalizedHref) ||
        /^tel:/i.test(normalizedHref)
    ) {
        return normalizedHref;
    }

    return null;
}

function resolveTaskEvent(element: LinkButtonElement, taskViewMode: 'staff' | 'customer' | null | undefined): string | null {
    if (taskViewMode === 'staff') {
        return normalizeEvent(element.staffTaskEvent);
    }

    if (taskViewMode === 'customer') {
        return normalizeEvent(element.customerTaskEvent);
    }

    return null;
}

function normalizeEvent(event: string | null | undefined): string | null {
    const normalizedEvent = event?.trim();
    return normalizedEvent == null || normalizedEvent.length === 0 ? null : normalizedEvent;
}

function resolveVariant(variant: string | null | undefined): LinkButtonElementVariant {
    if (variant === 'text' || variant === 'outlined') {
        return variant;
    }

    return 'contained';
}

function resolveColor(color: string | null | undefined): LinkButtonElementColor {
    if (color === 'secondary' || color === 'success' || color === 'info' || color === 'warning' || color === 'error') {
        return color;
    }

    return 'primary';
}
