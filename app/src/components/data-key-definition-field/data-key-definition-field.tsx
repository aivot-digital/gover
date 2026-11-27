import {useEffect, useId, useRef, useState} from 'react';
import {Stack, Typography} from '@mui/material';
import WandStars from '@aivot/mui-material-symbols-400-n25-outlined/WandStars';
import Lock from '@aivot/mui-material-symbols-400-n25-outlined/Lock';
import LockOpen from '@aivot/mui-material-symbols-400-n25-outlined/LockOpen';
import {useConfirm} from '../../providers/confirm-provider';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type EndAction} from '../text-field/text-field-component-props';
import {ProcessDataKeyInputComponent} from '../../views/process-data-key-input-field-view';
import {generateDataKey, normalizeDataKeyInput, validateNodeDataKey} from '../../utils/data-key-input';
import {Hint} from '../hint/hint';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

interface DataKeyDefinitionFieldProps {
    value: string | null | undefined;
    persistedValue: string | null | undefined;
    onChange: (value: string | null) => void;
    source: string | null | undefined;
    kind?: 'process-data-path' | 'node-key';
    prefix?: string;
    disabled?: boolean;
    error?: string | string[];
    validationRequested?: boolean;
    unlocked: boolean;
    onUnlock: () => void;
    onLock: () => void;
    /** Resolves collisions in the caller's scope without changing manually chosen paths. */
    resolveGeneratedKey?: (key: string) => string;
}

export function DataKeyDefinitionField(props: DataKeyDefinitionFieldProps) {
    const confirm = useConfirm();
    const lockStatusId = useId();
    const isNodeKey = props.kind === 'node-key';
    const [confirming, setConfirming] = useState(false);
    const mounted = useRef(true);
    useEffect(() => {
        mounted.current = true;
        return () => { mounted.current = false; };
    }, []);
    const generated = generateDataKey(props.source, isNodeKey ? 32 : undefined);
    const proposal = generated == null ? null : (props.resolveGeneratedKey?.(generated) ?? generated);

    const requestEdit = async (generate: boolean) => {
        if (props.disabled || confirming) return;
        if (!props.unlocked && props.persistedValue?.trim()) {
            setConfirming(true);
            const accepted = await confirm({
                title: 'Datenschlüssel bearbeiten?',
                confirmButtonText: 'Bearbeitung freigeben',
                cancelButtonText: 'Abbrechen',
                children: <>
                    <Typography>
                        Wenn Sie den Datenschlüssel ändern, müssen Sie betroffene Datenbindungen, Ausdrücke und
                        Skripte entsprechend anpassen. Verweise auf den bisherigen Schlüssel werden nicht automatisch
                        aktualisiert.
                    </Typography>
                    {generate && <Typography sx={{mt: 2, overflowWrap: 'anywhere'}}>
                        Vorgeschlagene Änderung: <strong>{props.value}</strong> → <strong>{proposal}</strong>
                    </Typography>}
                </>,
            });
            if (!mounted.current) return;
            setConfirming(false);
            if (!accepted) return;
        }
        props.onUnlock();
        if (generate && proposal != null) props.onChange(proposal);
    };

    const actions: EndAction[] = props.disabled ? [] : [
        {
            icon: <WandStars/>,
            tooltip: isNodeKey ? 'Aus Name erzeugen' : 'Aus Titel erzeugen',
            disabled: confirming || proposal == null || proposal === props.value,
            onClick: () => { void requestEdit(true); },
        },
        {
            icon: props.unlocked ? <Lock/> : <LockOpen/>,
            tooltip: props.unlocked ? 'Datenschlüssel sperren' : 'Datenschlüssel entsperren',
            disabled: confirming,
            onClick: () => {
                if (props.unlocked) props.onLock();
                else void requestEdit(false);
            },
        },
    ];
    const hint = <Hint
        ariaLabel="Hilfe zum Datenschlüssel"
        summary={isNodeKey ? 'Zugriff auf die Daten dieses Prozesselements.' : 'Zuordnung des Feldwerts zu den Vorgangsdaten.'}
        detailsTitle="Datenschlüssel"
        details={isNodeKey ? <>
            <Typography>Der Datenschlüssel identifiziert dieses Prozesselement innerhalb der Prozessversion.
                Er muss eindeutig sein und darf maximal 32 Zeichen lang sein. Über die Elementdaten (_) greifen Sie
                auf seine Ergebnisse zu.</Typography>
            <Typography sx={{mt: 2}}>Der Zauberstab erzeugt einen Vorschlag aus dem Namen des Prozesselements.</Typography>
            <Typography sx={{mt: 2}}>Verwenden Sie Buchstaben von A bis Z, Zahlen, Unterstriche oder $.
                Der Schlüssel darf nicht mit einer Zahl beginnen und keine Punkte oder Klammern enthalten.
                Bei der Eingabe werden Leerzeichen und Bindestriche zu Unterstrichen sowie Umlaute umgeschrieben.</Typography>
        </> : <>
            <Typography>Der Feldwert wird unter diesem Schlüssel in den Vorgangsdaten gespeichert und aus vorhandenen
                Vorgangsdaten vorbelegt. Ohne Datenschlüssel bleibt der Wert ausschließlich in den Elementdaten.</Typography>
            <Typography sx={{mt: 2}}>Verwenden Sie Namen wie vorname oder Pfade wie person.vorname und personen[0].vorname.
                Namen dürfen Buchstaben von A bis Z, Zahlen, Unterstriche und $ enthalten, aber nicht mit einer Zahl
                beginnen. Groß- und Kleinschreibung werden unterschieden.</Typography>
            <Typography sx={{mt: 2}}>Bei der Eingabe werden Leerzeichen und Bindestriche zu Unterstrichen sowie Umlaute
                umgeschrieben. Der Zauberstab erzeugt einen Vorschlag aus dem Titel. Innerhalb strukturierter Listen
                wird der übergeordnete Pfad automatisch ergänzt.</Typography>
        </>}
    />;

    const commonProps = {
        label: 'Datenschlüssel',
        value: props.value,
        onChange: props.onChange,
        disabled: props.disabled,
        error: props.error,
        labelAction: <Stack direction="row" spacing={1} sx={{alignItems: 'center'}}>
            {!props.unlocked && <Typography id={lockStatusId} component="span" variant="caption" color="text.secondary">
                Gesperrt
            </Typography>}
            {hint}
        </Stack>,
        ariaDescribedBy: props.unlocked ? undefined : lockStatusId,
        controlSx: {backgroundColor: props.unlocked ? undefined : getDisabledFieldBackground},
        endAction: actions,
        validationRequested: props.validationRequested,
    };
    return isNodeKey ? <TextFieldComponent
        {...commonProps}
        readonly={!props.unlocked}
        required
        maxCharacters={32}
        startIcon="_."
        hint="Der Schlüssel muss innerhalb dieser Prozessversion eindeutig sein. Für die Übersicht empfehlen wir einen sprechenden Schlüssel."
        validate={validateNodeDataKey}
        normalizeInput={normalizeDataKeyInput}
        copyable
        copyValueTemplate="_.{value}"
    /> : <ProcessDataKeyInputComponent
        {...commonProps}
        readOnly={!props.unlocked}
        normalizeInput={normalizeDataKeyInput}
        debounce={0}
        prefix={props.prefix}
        hint="Unter diesem Schlüssel ist der Feldwert in den Vorgangsdaten verfügbar."
    />;
}
