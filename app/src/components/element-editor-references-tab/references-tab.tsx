import React, {useMemo} from 'react';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import CallMadeIcon from '@mui/icons-material/CallMade';
import CallReceivedIcon from '@mui/icons-material/CallReceived';
import {DataGrid, GridColDef} from '@mui/x-data-grid';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {flattenElements} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {getElementIcon} from '../../data/element-type/element-icons';
import {AlertComponent} from '../alert/alert-component';

interface ReferencesTabProps {
    rootElement: AnyElement;
    element: AnyElement;
}

const SourceLabels: Record<SourceType, string> = {
    'visibility': 'Sichtbarkeit',
    'override': 'Dynamische Struktur',
    'validation': 'Validierung',
    'value': 'Dynamischer Wert',
};

const columns: GridColDef[] = [
    {
        field: 'type',
        headerName: 'Referenztyp',
        flex: 1,
        renderCell: (params) => {
            const {type} = params.row;
            return (
                <Chip
                    size="small"
                    icon={type === 'outgoing' ? <CallMadeIcon fontSize="small" /> : <CallReceivedIcon fontSize="small" />}
                    label={type === 'outgoing' ? 'Ausgehend' : 'Eingehend'}
                />
            );
        },
    },
    {
        field: 'source',
        headerName: 'Referenzquelle',
        flex: 1,
        renderCell: (params) => {
            const {source} = params.row;
            return (
                <Typography
                    variant="body2"
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        height: '100%',
                    }}
                >
                    {SourceLabels[source as SourceType]}
                </Typography>
            );
        },
    },
    {
        field: 'element',
        headerName: 'Referenziertes Element',
        flex: 3,
        renderCell: (params) => {
            const {element} = params.row;
            const Icon = getElementIcon(element);
            return (
                <Typography
                    variant="body2"
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        height: '100%',
                    }}
                >
                    <Icon fontSize="small" />
                    <span>{generateComponentTitle(element)} ({element.id})</span>
                </Typography>
            );
        },
    },
];

export function ReferencesTab(props: ReferencesTabProps) {
    const {
        rootElement,
        element,
    } = props;

    const references = useMemo(() => {
        const allElements = flattenElements(rootElement, false);
        return determineReferences(allElements, element);
    }, [rootElement, element]);

    return (
        <Box sx={{p: 4}}>
            <ElementEditorSectionHeader
                title="Referenzen"
                disableMarginTop
            >
                Hier können Sie zentral alle Elemente einsehen, die mit diesem Element in Verbindung stehen, entweder als Quelle (ausgehend) oder als Ziel (eingehend) einer Referenz.
            </ElementEditorSectionHeader>

            {
                references.length === 0 && (
                    <AlertComponent
                        color="info"
                        sx={{
                            mt: 3,
                        }}
                        title="Keine Referenzen gefunden"
                    >
                        Für dieses Element existieren noch keine eingehenden oder ausgehenden Referenzen. Wenn Sie auf dieses Element in den Sichtbarkeits-, Validierungs- oder Wert-Einstellungen verweisen, werden diese Referenzen hier aufgelistet.
                    </AlertComponent>
                )
            }

            {
                references.length > 0 && (
                    <DataGrid
                        sx={{
                            mt: 4,
                        }}
                        columns={columns}
                        rows={references}
                        autoHeight={true}
                    />
                )
            }
        </Box>
    );
}

type SourceType = 'visibility' | 'override' | 'validation' | 'value';

interface Reference {
    id: string;
    element: AnyElement;
    type: 'outgoing' | 'incoming';
    source: SourceType;
}

function determineReferences(
    allElements: AnyElement[],
    element: AnyElement,
): Reference[] {
    const references: Reference[] = [];

    for (const otherElement of allElements) {
        if (element.visibility?.referencedIds?.includes(otherElement.id)) {
            references.push({
                id: `${element.id}-${otherElement.id}-visibility`,
                element: otherElement,
                type: 'outgoing',
                source: 'visibility',
            });
        }

        if (element.override?.referencedIds?.includes(otherElement.id)) {
            references.push({
                id: `${element.id}-${otherElement.id}-override`,
                element: otherElement,
                type: 'outgoing',
                source: 'override',
            });
        }

        if (isAnyInputElement(element)) {
            if (element.validation?.referencedIds?.includes(otherElement.id)) {
                references.push({
                    id: `${element.id}-${otherElement.id}-validation`,
                    element: otherElement,
                    type: 'outgoing',
                    source: 'validation',
                });
            }

            if (element.value?.referencedIds?.includes(otherElement.id)) {
                references.push({
                    id: `${element.id}-${otherElement.id}-value`,
                    element: otherElement,
                    type: 'outgoing',
                    source: 'value',
                });
            }
        }

        if (otherElement.visibility?.referencedIds?.includes(element.id)) {
            references.push({
                id: `${otherElement.id}-${element.id}-visibility`,
                element: otherElement,
                type: 'incoming',
                source: 'visibility',
            });
        }

        if (otherElement.override?.referencedIds?.includes(element.id)) {
            references.push({
                id: `${otherElement.id}-${element.id}-override`,
                element: otherElement,
                type: 'incoming',
                source: 'override',
            });
        }

        if (isAnyInputElement(otherElement)) {
            if (otherElement.validation?.referencedIds?.includes(element.id)) {
                references.push({
                    id: `${otherElement.id}-${element.id}-validation`,
                    element: otherElement,
                    type: 'incoming',
                    source: 'validation',
                });
            }

            if (otherElement.value?.referencedIds?.includes(element.id)) {
                references.push({
                    id: `${otherElement.id}-${element.id}-value`,
                    element: otherElement,
                    type: 'incoming',
                    source: 'value',
                });
            }
        }
    }

    return references;
}
