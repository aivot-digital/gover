import {type ReactNode} from 'react';
import {Box, Link} from '@mui/material';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {CopyToClipboardButton} from '../../../components/copy-to-clipboard-button/copy-to-clipboard-button';
import {
    formatInstantInApplicationTimeZone,
    formatRelativeInstantInApplicationTimeZone,
} from '../../../utils/temporal-utils';

export function ProcessEmptyValue({children}: {children: ReactNode}): ReactNode {
    return (
        <Box
            component="span"
            sx={{color: 'text.secondary'}}
        >
            {children}
        </Box>
    );
}

export function ProcessNodeLabel({name, typeLabel}: {name: string; typeLabel?: string | null}): ReactNode {
    const nodeName = name.trim();
    const nodeType = typeLabel?.trim();
    if (!nodeName) return nodeType || <ProcessEmptyValue>Nicht verfügbar</ProcessEmptyValue>;
    if (!nodeType || nodeName === nodeType) return nodeName;

    return (
        <>
            „{nodeName}“
            <Box
                component="span"
                sx={{color: 'text.secondary'}}
            >
                {' '}
                · {nodeType}
            </Box>
        </>
    );
}

export function ProcessStatusValue({
    systemLabel,
    statusOverride,
}: {
    systemLabel: string;
    statusOverride?: string | null;
}): ReactNode {
    const customLabel = statusOverride?.trim();
    if (!customLabel) return systemLabel;

    return (
        <>
            {customLabel}
            <Box
                component="span"
                sx={{color: 'text.secondary'}}
            >
                {' '}
                (Systemstatus: {systemLabel})
            </Box>
        </>
    );
}

export function formatDateTimeWithRelative(value?: string | null, fallback = 'Nicht hinterlegt'): ReactNode {
    if (value == null || value.trim().length === 0) {
        return <ProcessEmptyValue>{fallback}</ProcessEmptyValue>;
    }

    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm');
    const relative = formatRelativeInstantInApplicationTimeZone(value);
    if (formatted == null || relative == null) {
        return <ProcessEmptyValue>{fallback}</ProcessEmptyValue>;
    }

    return (
        <Box component="span">
            {formatted} Uhr{' '}
            <Box
                component="span"
                sx={{
                    color: 'text.secondary',
                }}
            >
                ({relative})
            </Box>
        </Box>
    );
}

export function renderLinkedValue(label: ReactNode, to: string | null): ReactNode {
    if (to == null) {
        return label;
    }

    return (
        <Link
            href={to}
            target="_blank"
            rel="noopener noreferrer"
            underline="hover"
            color="inherit"
            sx={{
                display: 'inline-flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                columnGap: 0.75,
                rowGap: 0.25,
            }}
        >
            {label}
            <OpenInNewIcon
                fontSize="inherit"
                sx={{
                    fontSize: 16,
                    color: 'text.secondary',
                }}
            />
        </Link>
    );
}

export function renderProcessLabel(name: string, version: number): ReactNode {
    return (
        <Box component="span">
            <Box component="span">{name}</Box>{' '}
            <Box
                component="span"
                sx={{
                    color: 'text.secondary',
                }}
            >
                (v{version})
            </Box>
        </Box>
    );
}

export function CopyableProcessValue({
    value,
    label,
    children,
}: {
    value?: string | null;
    label: string;
    children?: ReactNode;
}): ReactNode {
    if (value == null || value.trim().length === 0) return <ProcessEmptyValue>Nicht hinterlegt</ProcessEmptyValue>;

    return (
        <Box
            component="span"
            sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.75,
                maxWidth: '100%',
            }}
        >
            <Box
                component="span"
                sx={{
                    minWidth: 0,
                    overflowWrap: 'anywhere',
                }}
            >
                {children ?? value}
            </Box>
            <CopyToClipboardButton
                text={value}
                sx={{
                    width: 24,
                    height: 24,
                    p: 0.25,
                    verticalAlign: 'top',
                }}
                tooltip={`${label} kopieren`}
                ariaLabel={`${label} kopieren`}
                errorMessage="Der Wert konnte nicht in die Zwischenablage kopiert werden."
            />
        </Box>
    );
}

export function ProcessFileNumbers({values}: {values?: string[]}): ReactNode {
    const fileNumbers = values?.filter((value) => value.trim().length > 0) ?? [];
    if (fileNumbers.length === 0) return <ProcessEmptyValue>Kein Aktenzeichen hinterlegt</ProcessEmptyValue>;

    return (
        <Box
            sx={{
                display: 'flex',
                flexWrap: 'wrap',
                columnGap: 2,
                rowGap: 0.5,
            }}
        >
            {fileNumbers.map((value, index) => (
                <CopyableProcessValue
                    key={`${index}-${value}`}
                    value={value}
                    label={`Aktenzeichen ${value}`}
                />
            ))}
        </Box>
    );
}
