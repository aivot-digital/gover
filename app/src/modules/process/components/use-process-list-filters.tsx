import {useCallback, useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {Alert, Button, Chip} from '@mui/material';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {SelectFieldPresentation} from '../../../models/elements/form/input/select-field-presentation';
import {ProcessListApiService} from '../services/process-list-api-service';
import {ProcessListOptions} from '../entities/process-list';

export function useProcessListFilters(tasks: boolean, instanceId?: number) {
    const [params, setParams] = useSearchParams();
    const defaultAssignee = tasks && instanceId == null ? 'mine' : 'all';
    const assignee = params.get('assignee') || defaultAssignee;
    const processId = params.get('processId') || undefined;
    const processVersion = params.get('processVersion') || undefined;
    const [options, setOptions] = useState<ProcessListOptions>({
        processes: [],
        assignees: [],
    });
    const [busy, setBusy] = useState(true);
    const [failed, setFailed] = useState(false);
    const [reload, setReload] = useState(0);
    const refreshOptions = useCallback(() => setReload((value) => value + 1), []);
    useEffect(() => {
        let cancelled = false;
        setBusy(true);
        setFailed(false);
        new ProcessListApiService()
            .options(tasks, instanceId)
            .then((value) => {
                if (!cancelled) setOptions(value);
            })
            .catch(() => {
                if (!cancelled) setFailed(true);
            })
            .finally(() => {
                if (!cancelled) setBusy(false);
            });
        return () => {
            cancelled = true;
        };
    }, [tasks, instanceId, reload]);

    const change = (key: string, value: string | null) =>
        setParams((current) => {
            const next = new URLSearchParams(current);
            if (value) next.set(key, value);
            else next.delete(key);
            if (key === 'processId') next.delete('processVersion');
            next.set('page', '1');
            return next;
        });
    const assignees = [
        {
            value: 'all',
            label: 'Alle Mitarbeiter:innen',
        },
        {
            value: 'mine',
            label: 'Mir zugewiesen',
        },
        {
            value: 'unassigned',
            label: 'Nicht zugewiesen',
        },
        ...options.assignees,
    ];
    if (!assignees.some((option) => option.value === assignee))
        assignees.push({
            value: assignee,
            label: 'Ausgewählte Person',
        });
    const processes = [
        {
            value: 'all',
            label: 'Alle Prozesse',
        },
        ...options.processes,
    ];
    if (processId && !processes.some((option) => option.value === processId))
        processes.push({
            value: processId,
            label: `Prozess #${processId}`,
        });
    return {
        assignee,
        processId: processId == null ? undefined : Number(processId),
        processVersion: processVersion == null ? undefined : Number(processVersion),
        refreshOptions,
        hasActiveAdditionalFilters: assignee !== defaultAssignee || processId != null || processVersion != null,
        preSearchElements: [
            <SelectFieldComponent<string>
                key="assignee"
                label="Zugewiesen an"
                value={assignee}
                options={assignees}
                onChange={(value) => change('assignee', value ?? defaultAssignee)}
                presentation={SelectFieldPresentation.Combobox}
                busy={busy}
                showOptionalIndicator={false}
                includeEmptyOption={false}
                margin="none"
            />,
            ...(instanceId == null
                ? [
                      <SelectFieldComponent<string>
                          key="process"
                          label="Prozess"
                          value={processId ?? 'all'}
                          options={processes}
                          onChange={(value) => change('processId', value === 'all' ? null : value)}
                          presentation={SelectFieldPresentation.Combobox}
                          busy={busy}
                          showOptionalIndicator={false}
                          includeEmptyOption={false}
                          margin="none"
                      />,
                  ]
                : []),
        ],
        listContextElements: [
            ...(processVersion
                ? [
                      <Chip
                          key="version"
                          label={`Prozessversion ${processVersion}`}
                          onDelete={() => change('processVersion', null)}
                      />,
                  ]
                : []),
            ...(failed
                ? [
                      <Alert
                          key="error"
                          severity="error"
                          action={<Button onClick={refreshOptions}>Erneut laden</Button>}
                      >
                          Die Filterauswahl konnte nicht geladen werden.
                      </Alert>,
                  ]
                : []),
        ],
    };
}
