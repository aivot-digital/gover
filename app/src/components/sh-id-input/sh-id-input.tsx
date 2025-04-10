import React from 'react';
import {AppConfig} from '../../app-config';
import {ReactComponent as Logo} from './sh-id-logo.svg';
import {IdInput} from '../id-input/id-input';
import {IdInputProps} from '../id-input/id-input-props';
import {ShIdAccessLevel, ShIdAccessLevelEgovScope} from '../../data/sh-id-access-level';
import {Idp} from '../../data/idp';

export function ShIdInput(props: Pick<IdInputProps, 'form' | 'allElements' | 'isBusy'>): JSX.Element {
    return (
        <IdInput
            idpQueryId={Idp.ShId}
            getScope={form => ShIdAccessLevelEgovScope[form.shIdLevel ?? ShIdAccessLevel.Niedrig]}
            host={AppConfig.schleswigHolsteinId.host}
            realm={AppConfig.schleswigHolsteinId.realm}
            client={AppConfig.schleswigHolsteinId.client}
            broker={AppConfig.schleswigHolsteinId.broker}
            icon={
                <Logo
                    title="Servicekonto Schleswig-Holstein Logo"
                    height="2em"
                />
            }
            callToAction={
                <>
                    Melden Sie sich mit dem <b>Servicekonto Schleswig-Holstein</b> an
                </>
            }
            successMessage={
                <>
                    Sie haben sich erfolgreich mit dem <b>Servicekonto Schleswig-Holstein</b> angemeldet.
                </>
            }
            form={props.form}
            allElements={props.allElements}
            isBusy={props.isBusy}
        />
    );
}