import React from 'react';
import {AppConfig} from '../../app-config';
import {ReactComponent as Logo} from './muk-logo.svg';
import {IdInput} from '../id-input/id-input';
import {IdInputProps} from '../id-input/id-input-props';
import {Idp} from '../../data/idp';

export function MukInput(props: Pick<IdInputProps, 'form' | 'allElements' | 'isBusy'>): JSX.Element {
    return (
        <IdInput
            idpQueryId={Idp.Muk}
            host={AppConfig.muk.host}
            realm={AppConfig.muk.realm}
            client={AppConfig.muk.client}
            broker={AppConfig.muk.broker}
            icon={
                <Logo
                    title="MUK-Logo"
                    height="2em"
                />
            }
            callToAction={
                <>
                    Melden Sie sich mit dem <b>Mein Unternehmenskonto</b> an
                </>
            }
            successMessage={
                <>
                    Sie haben sich erfolgreich mit dem <b>Mein Unternehmenskonto</b> angemeldet.
                </>
            }
            form={props.form}
            allElements={props.allElements}
            isBusy={props.isBusy}
        />
    );
}