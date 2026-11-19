import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    FormControlLabel,
    Radio,
    RadioGroup,
    Stack,
    Typography,
} from '@mui/material';
import ExpandMore from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {FormFieldGroup, mergeAriaIds} from '../../components/form-field';
import {SearchInput} from '../../components/search-input/search-input';
import {type MergeChoice, type MergeChoices, type MergeConflictPresentation} from '../../utils/three-way-merge';
import {getInputVariableReference, InputMode, isAuthoredInputValue} from '../../models/input-mode';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export interface MergeConflictDialogConflict {
    id: string;
    label: string;
    group?: MergeConflictPresentation['group'];
    propertyLabel?: string;
    mine: unknown;
    theirs: unknown;
}

/**
 * `restore` offers unsaved changes from an earlier visit of the editor, `save` resolves a save that a newer stored
 * state rejected.
 */
export type MergeConflictDialogMode = 'restore' | 'save';

export type DependentMergeConflicts = (choices: MergeChoices, defaultChoice?: MergeChoice) => MergeConflictDialogConflict[];

interface MergeConflictDialogProps {
    open: boolean;
    mode: MergeConflictDialogMode;
    /** Whether a newer state was stored since the changes were started. */
    storedStateChanged: boolean;
    conflicts: MergeConflictDialogConflict[];
    dependentConflicts?: DependentMergeConflicts;
    onApply: (choices: MergeChoices) => void;
    onDiscard: () => void;
    /** Only in the `save` mode: keeps editing without saving. */
    onCancel?: () => void;
}

interface ConflictPreview extends MergeConflictDialogConflict {
    mineText: string;
    theirsText: string;
}

interface ConflictGroup {
    id: string;
    label: string;
    conflicts: ConflictPreview[];
}

const PREVIEW_MAX_LENGTH = 80;
const ChoiceLabels: Record<MergeChoice, string> = {
    mine: 'Meine Änderung soll behalten werden',
    theirs: 'Gespeicherter Stand soll übernommen werden',
};

export function MergeConflictDialog(props: MergeConflictDialogProps): ReactNode {
    const {open, mode, storedStateChanged, conflicts: primaryConflicts, dependentConflicts, onApply, onDiscard, onCancel} = props;
    const [selected, setSelected] = useState<MergeChoices>({});
    const conflicts = useMemo(() => dependentConflicts == null ? primaryConflicts : [
        ...primaryConflicts,
        ...dependentConflicts(selected),
    ], [primaryConflicts, dependentConflicts, selected]);
    const choices: MergeChoices = Object.fromEntries(conflicts.map(({id}) => [id, selected[id] ?? 'mine']));
    const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
    const [search, setSearch] = useState('');
    const groups = useMemo(() => {
        const grouped = new Map<string, ConflictGroup>();
        for (const conflict of conflicts) {
            const id = conflict.group?.id ?? JSON.stringify(['conflict', conflict.id]);
            const group = grouped.get(id) ?? {id, label: conflict.group?.label ?? conflict.label, conflicts: []};
            group.conflicts.push({...conflict, mineText: formatValue(conflict.mine), theirsText: formatValue(conflict.theirs)});
            grouped.set(id, group);
        }
        return [...grouped.values()];
    }, [conflicts]);

    useEffect(() => {
        if (open) {
            setSelected({});
            const first = primaryConflicts[0];
            setExpandedGroups(first == null ? {} : {[first.group?.id ?? JSON.stringify(['conflict', first.id])]: true});
            setSearch('');
        }
    }, [open, primaryConflicts]);

    const query = search.trim().toLocaleLowerCase('de');
    const visibleGroups = groups.map((group) => ({
        ...group,
        conflicts: group.conflicts.filter((conflict) => [group.label, conflict.propertyLabel ?? conflict.label, conflict.mineText, conflict.theirsText]
            .some((text) => text.toLocaleLowerCase('de').includes(query))),
    })).filter((group) => group.conflicts.length > 0);
    const visibleCount = visibleGroups.reduce((count, group) => count + group.conflicts.length, 0);
    const hasConflicts = conflicts.length > 0;
    const title = mode === 'restore' ? 'Ungespeicherte Änderungen wiederherstellen?' : 'Neuerer Stand gespeichert';
    const chooseAll = (choice: MergeChoice) => {
        // The action explicitly covers every conflict, including collapsed groups and filtered-out entries.
        const primary = Object.fromEntries(primaryConflicts.map(({id}) => [id, choice]));
        const additional = dependentConflicts?.(primary, choice) ?? [];
        setSelected({...primary, ...Object.fromEntries(additional.map(({id}) => [id, choice]))});
    };

    return (
        <Dialog
            open={open}
            // Restoring needs a decision, otherwise the draft would be overwritten by the next edit unnoticed.
            onClose={mode === 'save' ? onCancel : undefined}
            fullWidth={true}
            maxWidth={hasConflicts ? 'md' : 'sm'}
            slotProps={{paper: {sx: hasConflicts ? {height: 'min(820px, calc(100dvh - 64px))', maxHeight: 'calc(100dvh - 64px)'} : undefined}}}
        >
            <DialogTitleWithClose onClose={mode === 'save' ? onCancel : undefined}>
                {title}
            </DialogTitleWithClose>

            <DialogContent
                tabIndex={0}
                sx={hasConflicts ? {display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden', pb: 0} : undefined}
            >
                <Box sx={{flexShrink: 0}}>
                    <Typography>
                        {mode === 'restore' ?
                            (hasConflicts ? 'Aus Ihrer letzten Bearbeitung liegen ungespeicherte Änderungen vor.' :
                                'Aus Ihrer letzten Bearbeitung liegen ungespeicherte Änderungen vor. Sie können diese wiederherstellen und die Bearbeitung fortsetzen.') :
                            'Während Sie Ihre Änderungen vorgenommen haben, wurde an anderer Stelle ein neuerer Stand gespeichert.'}
                    </Typography>

                    {mode === 'restore' && storedStateChanged && !hasConflicts && (
                        <Typography sx={{mt: 2}}>
                            Zwischenzeitlich wurde ein neuerer Stand gespeichert. Ihre Änderungen können mit diesem Stand
                            zusammengeführt werden.
                        </Typography>
                    )}

                    {hasConflicts && (
                        <Typography sx={{mt: 1}}>
                            {mode === 'restore' ?
                                'Zwischenzeitlich wurde ein neuerer Stand gespeichert. Einige Angaben wurden in beiden Fassungen unterschiedlich geändert. Wählen Sie für jede abweichende Einstellung aus, welcher Wert übernommen werden soll. Alle übrigen Änderungen werden zusammengeführt.' :
                                'Die folgenden Angaben wurden an beiden Stellen unterschiedlich geändert. Wählen Sie für jede abweichende Einstellung aus, welcher Wert übernommen werden soll. Alle übrigen Änderungen werden zusammengeführt.'}
                        </Typography>
                    )}

                    {mode === 'restore' && (
                        <Typography sx={{mt: 1}}>
                            Speichern Sie Ihre Änderungen nach der Wiederherstellung. Beim Verwerfen wird die
                            zwischengespeicherte Fassung gelöscht.
                        </Typography>
                    )}

                    {hasConflicts && (
                        <Stack spacing={1} sx={{mt: 2, mb: 1}}>
                            <SearchInput label="Abweichende Angaben suchen" value={search} onChange={setSearch}/>
                            {conflicts.length > 1 && (
                                <Stack direction="row" spacing={1} sx={{flexWrap: 'wrap', rowGap: 0.5}}>
                                    <Button size="small" onClick={() => chooseAll('mine')}>Meine Änderungen für alle behalten</Button>
                                    <Button size="small" onClick={() => chooseAll('theirs')}>Gespeicherten Stand für alle übernehmen</Button>
                                </Stack>
                            )}
                            <Typography variant="body2" role="status">
                                {query.length > 0 ? `${visibleCount} von ${conflicts.length} abweichenden Angaben` :
                                    `${conflicts.length} ${conflicts.length === 1 ? 'abweichende Angabe' : 'abweichende Angaben'}`}
                            </Typography>
                        </Stack>
                    )}
                </Box>

                {hasConflicts && (
                    <Box sx={{minHeight: 0, flex: 1, overflowY: 'auto', pb: 2}}>
                        {visibleGroups.length === 0 && <Typography sx={{py: 2}}>Keine abweichenden Angaben gefunden.</Typography>}
                        {visibleGroups.map((group) => (
                            <ConflictGroupComparison
                                key={group.id}
                                group={group}
                                expanded={query.length > 0 || expandedGroups[group.id] === true}
                                searching={query.length > 0}
                                onExpand={(expanded) => setExpandedGroups((previous) => ({...previous, [group.id]: expanded}))}
                                choices={choices}
                                onChoose={(id, choice) => setSelected((previous) => ({...previous, [id]: choice}))}
                            />
                        ))}
                    </Box>
                )}
            </DialogContent>

            <DialogActions sx={{justifyContent: 'flex-start', flexShrink: 0}}>
                <Button variant="contained" onClick={() => onApply(choices)}>
                    {mode === 'save' ? 'Übernehmen und speichern' : 'Änderungen wiederherstellen'}
                </Button>
                <Button color="error" onClick={onDiscard}>Änderungen verwerfen</Button>
                <Box sx={{flexGrow: 1}}/>
                {mode === 'save' && onCancel != null && <Button onClick={onCancel}>Abbrechen</Button>}
            </DialogActions>
        </Dialog>
    );
}

function ConflictGroupComparison(props: {
    group: ConflictGroup;
    expanded: boolean;
    searching: boolean;
    onExpand: (expanded: boolean) => void;
    choices: MergeChoices;
    onChoose: (id: string, choice: MergeChoice) => void;
}) {
    const id = useNormalizedReactId();
    return (
        <Accordion
            disableGutters
            expanded={props.expanded}
            onChange={(_, expanded) => {
                if (!props.searching) {
                    props.onExpand(expanded);
                }
            }}
            slotProps={{transition: {unmountOnExit: true}}}
            sx={{boxShadow: 'none', borderBottom: '1px solid', borderColor: 'divider', '&::before': {display: 'none'}}}
        >
            <AccordionSummary
                id={`${id}-heading`}
                aria-controls={`${id}-content`}
                expandIcon={props.searching ? undefined : <ExpandMore/>}
                sx={{bgcolor: 'action.hover', '& .MuiAccordionSummary-content': {alignItems: 'center', gap: 1, minWidth: 0}}}
            >
                <Typography component="span" sx={{fontWeight: 600, overflowWrap: 'anywhere'}}>{props.group.label}</Typography>
                <Chip size="small" label={`${props.group.conflicts.length} ${props.group.conflicts.length === 1 ? 'Abweichung' : 'Abweichungen'}`}/>
            </AccordionSummary>
            <AccordionDetails>
                <Stack spacing={2}>
                    {props.group.conflicts.map((conflict) => (
                        <FormFieldGroup
                            key={conflict.id}
                            label={`Abweichende Einstellung: ${conflict.propertyLabel ?? conflict.label}`}
                            required
                            margin="none"
                        >
                            {(context) => (
                                <RadioGroup
                                    role="presentation"
                                    name={`${context.groupId}-options`}
                                    value={props.choices[conflict.id] ?? 'mine'}
                                    onChange={(_, value) => props.onChoose(conflict.id, value === 'theirs' ? 'theirs' : 'mine')}
                                    sx={{display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 1}}
                                >
                                    {(['mine', 'theirs'] as const).map((choice) => (
                                        <ConflictValueOption
                                            key={choice}
                                            choice={choice}
                                            text={choice === 'mine' ? conflict.mineText : conflict.theirsText}
                                            selected={(props.choices[conflict.id] ?? 'mine') === choice}
                                            describedBy={context.describedBy}
                                        />
                                    ))}
                                </RadioGroup>
                            )}
                        </FormFieldGroup>
                    ))}
                </Stack>
            </AccordionDetails>
        </Accordion>
    );
}

function ConflictValueOption(props: {choice: MergeChoice; text: string; selected: boolean; describedBy?: string}) {
    const id = useNormalizedReactId();
    const [expanded, setExpanded] = useState(false);
    const isLong = props.text.length > PREVIEW_MAX_LENGTH;
    const labelId = `${id}-label`;
    const valueId = `${id}-value`;
    return (
        <Box sx={{minWidth: 0, border: '1px solid', borderColor: props.selected ? 'primary.main' : 'divider', borderRadius: 1}}>
            <FormControlLabel
                value={props.choice}
                disableTypography
                control={<Radio slotProps={{input: {'aria-labelledby': labelId, 'aria-describedby': mergeAriaIds(valueId, props.describedBy)}}}/>}
                label={(
                    <Box component="span" sx={{display: 'block', minWidth: 0, py: 1}}>
                        <Typography component="span" id={labelId} sx={{display: 'block'}}>{ChoiceLabels[props.choice]}</Typography>
                        <Typography
                            component="span"
                            variant="body1"
                            id={valueId}
                            sx={{display: 'block', mt: 1, color: 'text.primary', fontWeight: 500, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere'}}
                        >
                            {isLong && !expanded ? `${props.text.slice(0, PREVIEW_MAX_LENGTH)} …` : props.text}
                        </Typography>
                    </Box>
                )}
                sx={{m: 0, pr: 1.5, width: '100%', boxSizing: 'border-box', alignItems: 'flex-start', '& .MuiRadio-root': {mt: 0.125}}}
            />
            {isLong && (
                <Button
                    size="small"
                    aria-expanded={expanded}
                    aria-controls={valueId}
                    onClick={() => setExpanded((previous) => !previous)}
                    sx={{mx: 1, mb: 1}}
                >
                    {expanded ? 'Wert kürzen' : 'Vollständigen Wert anzeigen'}
                </Button>
            )}
        </Box>
    );
}

function formatValue(value: unknown): string {
    if (value === undefined) {
        return 'Entfernt';
    }
    if (value === null || value === '') {
        return 'Leer';
    }
    if (typeof value === 'boolean') {
        return value ? 'Ja' : 'Nein';
    }
    if (typeof value === 'string' || typeof value === 'number') {
        return String(value);
    }
    if (isAuthoredInputValue(value)) {
        switch (value.type) {
            case InputMode.Literal:
                return formatValue(value.value);
            case InputMode.Variable:
                return `Variable: ${getInputVariableReference(value.reference)}`;
            case InputMode.LowCode:
                return `Low-Code-Skript:\n${value.code}`;
            case InputMode.NoCode:
                return `No-Code-Ausdruck:\n${JSON.stringify(value.operand, null, 2)}`;
        }
    }
    return JSON.stringify(value, null, 2) ?? 'Strukturierter Inhalt';
}
