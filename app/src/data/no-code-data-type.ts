export enum NoCodeDataType {
    Runtime = 'Runtime',

    Boolean = 'Boolean',
    Number = 'Number',
    String = 'String',
    Date = 'Date',

    List = 'List',
    Object = 'Object',
}

export const NoCodeDataTypeLabels: Record<NoCodeDataType, string> = {
    [NoCodeDataType.Runtime]: 'Beliebiger Wert',
    [NoCodeDataType.Boolean]: 'Wahrheitswert',
    [NoCodeDataType.Number]: 'Zahlenwert',
    [NoCodeDataType.String]: 'Textwert',
    [NoCodeDataType.Date]: 'Datumswert',
    [NoCodeDataType.List]: 'Liste',
    [NoCodeDataType.Object]: 'Objekt',
};