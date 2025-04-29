import {IdentityValue} from '../models/identity-value';
import {LegacySystemIdpKey} from '../../../data/legacy-system-idp-key';
import {BayernIdAttribute, BundIdAttribute, MukAttribute, ShIdAttribute} from '../constants/system-identity-provider-attributes';
import parse from 'date-fns/parse';

/**
 * Make sure to keep this in sync with the backend counterpart at src/main/java/de/aivot/GoverBackend/identity/utils/SystemIdentityProviderFormatter.java
 */
export function systemIdentityProviderFormatValues(identityValue: IdentityValue): IdentityValue {
    const {userInfo} = identityValue;

    let mapperFunc = (key: string, value: string) => value;
    switch (identityValue.metadataIdentifier) {
        case LegacySystemIdpKey.BayernId:
            mapperFunc = transformBayernIdAttribute;
            break;
        case LegacySystemIdpKey.BundId:
            mapperFunc = transformBundIdAttribute;
            break;
        case LegacySystemIdpKey.ShId:
            mapperFunc = transformShIdAttribute;
            break;
        case LegacySystemIdpKey.Muk:
            mapperFunc = transformMukAttribute;
            break;
    }

    const transformedUserInfo: typeof userInfo = {};

    for (const [key, value] of Object.entries(userInfo)) {
        transformedUserInfo[key] = mapperFunc(key, value);
    }

    return {
        ...identityValue,
        userInfo: transformedUserInfo,
    };
}

function transformBayernIdAttribute(key: string, value: string): any {
    switch (key) {
        case BayernIdAttribute.DateOfBirth:
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}

function transformBundIdAttribute(key: string, value: string): any {
    switch (key) {
        case BundIdAttribute.DateOfBirth:
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}

function transformMukAttribute(key: string, value: string): any {
    switch (key) {
        case MukAttribute.DateOfBirth:
            return parse(value, 'dd.MM.yyyy', new Date()).toISOString();
        default:
            return value;
    }
}

function transformShIdAttribute(key: string, value: string): any {
    switch (key) {
        case ShIdAttribute.DateOfBirth:
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}