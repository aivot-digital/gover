import {DataOverview} from '../data-overview/data-overview';
import {ProviderLink} from '../../../models/entities/provider-link';
import {ProviderLinksService} from '../../../services/provider-links.service';
import {DataOverviewProps} from '../data-overview/data-overview-props';
import {Localization} from '../../../locale/localization';
import strings from './provider-links-overview-strings.json';


const _ = Localization(strings);

const ProviderLinksOverviewConfig: DataOverviewProps<ProviderLink> = {
    title: _.title,
    addLabel: _.addLabel,
    emptySearchHelperText: _.emptySearchHelperText,
    noItemsHelperText: _.noItemsHelperText,
    searchPlaceholder: _.searchPlaceholder,

    exportExtension: 'lnks',

    list: () => {
        return ProviderLinksService
            .list()
            .then(response => response._embedded.providerLinks);
    },

    create: () => {
        return ProviderLinksService
            .create({
                text: _.addLabel,
                link: 'https://aivot.de/gover',
            });
    },

    import: (link) => {
        return ProviderLinksService
            .create(link);
    },

    update: (dest) => {
        return ProviderLinksService
            .update(dest.id, dest);
    },

    destroy: (destroyed) => {
        return ProviderLinksService.destroy(destroyed.id);
    },

    search: (search, item) => {
        return item.text.toLowerCase().includes(search);
    },
    sort: (itemA, itemB) => itemA.text.localeCompare(itemB.text),

    toPrimaryString: destination => destination.text,

    fieldsToEdit: [
        _.aboutTitle,
        {
            field: 'text',
            label: _.nameLabel,
            placeholder: _.namePlaceholder,
            helperText: _.nameHelper,
            isMultiline: true,
            required: true,
        },
        {
            field: 'link',
            label: _.linkLabel,
            placeholder: _.linkPlaceholder,
            helperText: _.linkHelper,
            required: true,
        },
    ],
};

export const ProviderLinksOverview = () => (
    <DataOverview {...ProviderLinksOverviewConfig}/>
);
