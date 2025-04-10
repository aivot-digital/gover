import {Form} from '../../models/entities/form';
import {AnyElement} from '../../models/elements/any-element';
import {ReactNode} from 'react';
import {ElementMetadata} from '../../models/elements/element-metadata';
import {Idp} from '../../data/idp';

export interface IdInputProps {
    form: Form;
    allElements: AnyElement[];
    getScope?: (form: Form) => string;

    idpQueryId: Idp;

    host: string;
    realm: string;
    client: string;
    broker: string;

    icon: ReactNode;
    callToAction: ReactNode;
    successMessage: ReactNode;

    isBusy: boolean;
}