import React from 'react';
import {DepartmentsService} from '../../../services/departments.service';
import {DataOverview} from '../data-overview/data-overview';
import {DataOverviewProps} from '../data-overview/data-overview-props';
import {Department} from '../../../models/department';
import {Localization} from '../../../locale/localization';
import strings from './departments-overview-strings.json';

const _ = Localization(strings);

const DepartmentsOverviewConfig: DataOverviewProps<Department> = {
    title: _.title,
    addLabel: _.addLabel,
    emptySearchHelperText: _.emptySearchHelperText,
    noItemsHelperText: _.noItemsHelperText,
    searchPlaceholder: _.searchPlaceholder,

    exportExtension: 'dprtmnts',

    list: () => {
        return DepartmentsService
            .list()
            .then(data => data._embedded.departments);
    },

    create: () => {
        return DepartmentsService
            .create({
                name: _.addLabel,
                address: '',
                accessibility: '',
                imprint: '',
                privacy: '',
                technicalSupportAddress: '',
                specialSupportAddress: '',
            });
    },
    import: (department) => {
        return DepartmentsService
            .create(department);
    },
    update: (dest) => {
        return DepartmentsService
            .update(dest.id, dest);
    },

    destroy: (destroyed) => {
        return DepartmentsService
            .destroy(destroyed.id);
    },

    search: (search, item) => {
        return item.name.toLowerCase().includes(search);
    },
    sort: (itemA, itemB) => itemA.name.localeCompare(itemB.name),

    toPrimaryString: department => department.name,
    toSecondaryString: department => department.address,

    fieldsToEdit: [
        _.aboutTitle,
        {
            label: _.nameLabel,
            placeholder: _.namePlaceholder,
            field: 'name',
            helperText: _.nameHelper,
            required: true,
        },
        {
            label: _.addressLabel,
            placeholder: _.addressPlaceholder,
            field: 'address',
            isMultiline: true,
            helperText: _.addressHelper,
            required: true,
        },
        _.supportTitle,
        {
            label: _.supportSpecialLabel,
            placeholder: _.supportSpecialPlaceholder,
            field: 'specialSupportAddress',
            helperText: _.supportSpecialHelper,
            required: true,
        },
        {
            label: _.supportTechnicalLabel,
            placeholder: _.supportTechnicalPlaceholder,
            field: 'technicalSupportAddress',
            helperText: _.supportTechnicalHelper,
            required: true,
        },
        _.legalTitle,
        {
            label: _.imprint,
            field: 'imprint',
            placeholder: '',
            isRichtext: true,
            helperText: _.imprintHelper,
        },
        {
            label: _.privacy,
            field: 'privacy',
            placeholder: '',
            isRichtext: true,
            helperText: _.privacyHelper,
        },
        {
            label: _.accessibility,
            field: 'accessibility',
            placeholder: '',
            isRichtext: true,
            helperText: _.accessibilityHelper,
        },
    ],
};

export const DepartmentsOverview = () => (
    <DataOverview {...DepartmentsOverviewConfig}/>
);
