import {useState} from 'react';
import {Box, Typography} from '@mui/material';
import {CheckboxFieldComponent} from '../../components/checkbox-field/checkbox-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {type AuthoredElementValues, literalAuthoredValue} from '../../models/element-data';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementDerivationContext} from '../elements/components/element-derivation-context';
import {AssetVisibility} from '../assets/models/asset-visibility';

const resourceLayout = {
    ...generateElementWithDefaultValues(ElementType.GroupLayout),
    id: 'resource-input-gallery',
    children: [{
        ...generateElementWithDefaultValues(ElementType.AssetSelectInput),
        id: 'certificate',
        label: 'Client-Zertifikat',
        dialogTitle: 'Client-Zertifikat auswählen',
        placeholder: 'Kein Zertifikat ausgewählt',
        hint: 'Private Datei für die Anmeldung beim externen Dienst.',
        allowedMimeTypes: ['application/x-pem-file', 'application/pkix-cert'],
        assetVisibility: AssetVisibility.Private,
        weight: 6,
    }, {
        ...generateElementWithDefaultValues(ElementType.SecretSelectInput),
        id: 'clientSecret',
        label: 'Zugangsschlüssel',
        hint: 'Der geheime Wert wird erst bei der Ausführung verwendet.',
        required: true,
        weight: 6,
    }],
};

export function ResourceInputGallery() {
    const [values, setValues] = useState<AuthoredElementValues>({
        certificate: literalAuthoredValue(null),
        clientSecret: literalAuthoredValue(null),
    });
    const [readOnly, setReadOnly] = useState(false);

    return (
        <Box component="section" aria-labelledby="resource-input-gallery-title" sx={{mt: 5, minWidth: 0}}>
            <Typography id="resource-input-gallery-title" component="h3" variant="subtitle1" sx={{mb: 2}}>
                Ressourcenreferenzen
            </Typography>
            {/* Exercise the real dispatcher and authored-value boundary, not a separate picker-only demo.
                This static layout has no derivation functions; selecting resources still uses the real APIs. */}
            <ElementDerivationContext
                element={resourceLayout}
                authoredElementValues={values}
                onAuthoredElementValuesChange={setValues}
                deriveOnMount={false}
                readOnly={readOnly}
            />
            <CheckboxFieldComponent
                label="Schreibgeschützt"
                variant="switch"
                value={readOnly}
                onChange={value => setReadOnly(value ?? false)}
                margin="none"
            />
        </Box>
    );
}
