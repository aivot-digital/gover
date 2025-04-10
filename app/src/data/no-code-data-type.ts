export enum NoCodeDataType {
    Any = 0,

    Boolean = 10,
    Number = 11,
    String = 12,
    Date = 13,

    List = 100,
    Object = 101,
}

export const NoCodeDataTypeLabels: Record<NoCodeDataType, string> = {
    [NoCodeDataType.Any]: 'Beliebiger Wert',
    [NoCodeDataType.Boolean]: 'Wahrheitswert',
    [NoCodeDataType.Number]: 'Zahlenwert',
    [NoCodeDataType.String]: 'Textwert',
    [NoCodeDataType.Date]: 'Datumswert',
    [NoCodeDataType.List]: 'Liste',
    [NoCodeDataType.Object]: 'Objekt',
};