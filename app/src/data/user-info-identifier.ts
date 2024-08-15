import {SelectFieldComponentOption} from '../components/select-field/select-field-component-option';

export enum UserInfoIdentifier {
    Salutation = 'salutation',
    Title = 'title',
    FirstName = 'first_name',
    LastName = 'last_name',
    DateOfBirth = 'date_of_birth',
    ContactEmail = 'contact.email',
    ContactPhone = 'contact.phone',
    Street = 'address.street',
    HouseNumber = 'address.house_number',
    StreetHouseNumber = 'address.street_and_house_number',
    ZipCode = 'address.zip_code',
    City = 'address.city',
    Country = 'address.country',
    AddressAddition = 'address.addition',
}

export const UserInfoIdentifierLabels: Record<UserInfoIdentifier, string> = {
    [UserInfoIdentifier.Salutation]: 'Anrede',
    [UserInfoIdentifier.Title]: 'Titel',
    [UserInfoIdentifier.FirstName]: 'Vorname',
    [UserInfoIdentifier.LastName]: 'Nachname',
    [UserInfoIdentifier.DateOfBirth]: 'Geburtsdatum',
    [UserInfoIdentifier.ContactEmail]: 'E-Mail',
    [UserInfoIdentifier.ContactPhone]: 'Telefon',
    [UserInfoIdentifier.Street]: 'Straße',
    [UserInfoIdentifier.HouseNumber]: 'Hausnummer',
    [UserInfoIdentifier.StreetHouseNumber]: 'Straße und Hausnummer',
    [UserInfoIdentifier.ZipCode]: 'PLZ',
    [UserInfoIdentifier.City]: 'Ort',
    [UserInfoIdentifier.Country]: 'Land',
    [UserInfoIdentifier.AddressAddition]: 'Adresszusatz',
};

export const UserInfoIdentifierOptions: SelectFieldComponentOption[] = Object.values(UserInfoIdentifier).map((value) => ({
    value,
    label: UserInfoIdentifierLabels[value],
}));

/* Removed until further notice
export const CompatibleIdFields: Record<UserInfoIdentifier, Record<Idp, string[]> | undefined> = {
    [UserInfoIdentifier.Salutation]: {
        [Idp.BundId]: [
            BundIdAttribute.Salutation,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.Salutation,
        ],
        [Idp.Muk]: [
            MukAttribute.Salutation,
        ],
        [Idp.ShId]: [
            ShIdAttribute.Salutation,
        ],
    },[UserInfoIdentifier.Title]: {
        [Idp.BundId]: [
            BundIdAttribute.Title,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.Title,
        ],
        [Idp.Muk]: [
            MukAttribute.Title,
        ],
        [Idp.ShId]: [
            ShIdAttribute.Title,
        ],
    },
    [UserInfoIdentifier.FirstName]: {
        [Idp.BundId]: [
            BundIdAttribute.GivenName,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.GivenName,
        ],
        [Idp.Muk]: [
            MukAttribute.GivenName,
        ],
        [Idp.ShId]: [
            ShIdAttribute.GivenName,
        ],
    },
    [UserInfoIdentifier.LastName]: {
        [Idp.BundId]: [
            BundIdAttribute.FamilyName,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.FamilyName,
        ],
        [Idp.Muk]: [
            MukAttribute.FamilyName,
        ],
        [Idp.ShId]: [
            ShIdAttribute.FamilyName,
        ],
    },
    [UserInfoIdentifier.DateOfBirth]: {
        [Idp.BundId]: [
            BundIdAttribute.DateOfBirth,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.DateOfBirth,
        ],
        [Idp.Muk]: [
            MukAttribute.DateOfBirth,
        ],
        [Idp.ShId]: [
            ShIdAttribute.DateOfBirth,
        ],
    },
    [UserInfoIdentifier.ContactEmail]: {
        [Idp.BundId]: [
            BundIdAttribute.Email,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.Email,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.Email,
        ],
    },
    [UserInfoIdentifier.ContactPhone]: {
        [Idp.BundId]: [
            BundIdAttribute.Telephone,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.Telephone,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.Telephone,
            ShIdAttribute.Mobilephone,
        ],
    },
    [UserInfoIdentifier.Street]: {
        [Idp.BundId]: [],
        [Idp.BayernId]: [],
        [Idp.Muk]: [],
        [Idp.ShId]: [],
    },
    [UserInfoIdentifier.HouseNumber]: {
        [Idp.BundId]: [],
        [Idp.BayernId]: [],
        [Idp.Muk]: [],
        [Idp.ShId]: [],
    },
    [UserInfoIdentifier.StreetHouseNumber]: {
        [Idp.BundId]: [
            BundIdAttribute.StreetAndBuilding,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.StreetAndBuilding,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.StreetAndBuilding,
        ],
    },
    [UserInfoIdentifier.ZipCode]: {
        [Idp.BundId]: [
            BundIdAttribute.ZipCode,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.ZipCode,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.ZipCode,
        ],
    },
    [UserInfoIdentifier.City]: {
        [Idp.BundId]: [
            BundIdAttribute.City,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.City,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.City,
        ],
    },
    [UserInfoIdentifier.Country]: {
        [Idp.BundId]: [
            BundIdAttribute.Country,
        ],
        [Idp.BayernId]: [
            BayernIdAttribute.Country,
        ],
        [Idp.Muk]: [],
        [Idp.ShId]: [
            ShIdAttribute.Country,
        ],
    },
    [UserInfoIdentifier.AddressAddition]: {
        [Idp.BundId]: [],
        [Idp.BayernId]: [],
        [Idp.Muk]: [],
        [Idp.ShId]: [],
    },
};
 */