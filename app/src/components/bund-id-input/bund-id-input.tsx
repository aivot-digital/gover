import React from 'react';
import {AppConfig} from '../../app-config';
import {ReactComponent as Logo} from './bund-id-logo.svg';
import {IdInput} from '../id-input/id-input';
import {IdInputProps} from '../id-input/id-input-props';
import {BundIdAccessLevel, BundIdAccessLevelEgovScope} from '../../data/bund-id-access-level';
import {Idp} from '../../data/idp';


export function BundIdInput(props: Pick<IdInputProps, 'form' | 'allElements'>): JSX.Element {
    return (
        <IdInput
            idpQueryId={Idp.BundId}
            getScope={form => BundIdAccessLevelEgovScope[form.bundIdLevel ?? BundIdAccessLevel.Niedrig]}
            host={AppConfig.bundId.host}
            realm={AppConfig.bundId.realm}
            client={AppConfig.bundId.client}
            broker={AppConfig.bundId.broker}
            icon={
                <Logo
                    title="BundID-Logo"
                    height="2em"
                />
            }
            callToAction={
                <>
                    Melden Sie sich mit der <b>BundID</b> an
                </>
            }
            successMessage={
                <>
                    Sie haben sich erfolgreich mit der <b>BundID</b> angemeldet.
                </>
            }
            form={props.form}
            allElements={props.allElements}
        />
    );
}