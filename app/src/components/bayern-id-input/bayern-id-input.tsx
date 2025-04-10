import React from 'react';
import {AppConfig} from '../../app-config';
import {ReactComponent as Logo} from './bayern-id-logo.svg';
import {IdInput} from '../id-input/id-input';
import {IdInputProps} from '../id-input/id-input-props';
import {BayernIdAccessLevel, BayernIdAccessLevelEgovScope} from '../../data/bayern-id-access-level';
import {Idp} from '../../data/idp';

export function BayernIdInput(props: Pick<IdInputProps, 'form' | 'allElements' | 'isBusy'>): JSX.Element {
    return (
        <IdInput
            idpQueryId={Idp.BayernId}
            getScope={form => BayernIdAccessLevelEgovScope[form.bayernIdLevel ?? BayernIdAccessLevel.Niedrig]}
            host={AppConfig.bayernId.host}
            realm={AppConfig.bayernId.realm}
            client={AppConfig.bayernId.client}
            broker={AppConfig.bayernId.broker}
            icon={
                <Logo
                    title="BayernID-Logo"
                    height="2em"
                />

            }
            callToAction={
                <>
                    Melden Sie sich mit der <b>BayernID</b> an
                </>
            }
            successMessage={
                <>
                    Sie haben sich erfolgreich mit der <b>BayernID</b> angemeldet.
                </>
            }
            form={props.form}
            allElements={props.allElements}
            isBusy={props.isBusy}
        />
    );

}