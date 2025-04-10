import {Secret} from '../models/secret';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';

export interface SecretFormProps {
    secret: Secret;
    onChange: (secret: Secret) => void;
    isDisabled?: boolean;
}

export function SecretForm(props: SecretFormProps) {
    return (
        <>
            <TextFieldComponent
                label="Name"
                value={props.secret.name}
                onChange={value => {
                    props.onChange({
                        ...props.secret,
                        name: value ?? '',
                    });
                }}
                disabled={props.isDisabled}
            />

            <TextFieldComponent
                label="Beschreibung"
                value={props.secret.description}
                onChange={value => {
                    props.onChange({
                        ...props.secret,
                        description: value ?? '',
                    });
                }}
                multiline={true}
                disabled={props.isDisabled}
            />

            <TextFieldComponent
                label="Wert"
                value={props.secret.value}
                onChange={value => {
                    props.onChange({
                        ...props.secret,
                        value: value ?? '',
                    });
                }}
                disabled={props.isDisabled}
            />
        </>
    );
}