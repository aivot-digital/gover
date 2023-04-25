import {DataOverview} from '../data-overview/data-overview';
import {Destination} from '../../../models/entities/destination';
import {DestinationsService} from '../../../services/destinations.service';
import {DestinationType} from '../../../data/destination-type/destination-type';
import {DataOverviewProps} from '../data-overview/data-overview-props';
import strings from './destinations-overview-strings.json';
import {Localization} from '../../../locale/localization';
import {isStringNullOrEmpty} from "../../../utils/string-utils";

const _ = Localization(strings);

const DestinationsOverviewConfig: DataOverviewProps<Destination> = {
    title: _.title,
    addLabel: _.addLabel,
    emptySearchHelperText: _.emptySearchHelperText,
    noItemsHelperText: _.noItemsHelperText,
    searchPlaceholder: _.searchPlaceholder,

    exportExtension: 'dstntns',

    list: () => {
        return DestinationsService
            .list()
            .then(response => response._embedded.destinations);
    },

    create: () => {
        return DestinationsService
            .create({
                name: _.addLabel,
                type: DestinationType.Mail,
            });
    },

    import: (destination) => {
        return DestinationsService
            .create(destination);
    },

    update: (dest) => {
        return DestinationsService
            .update(dest.id, dest);
    },

    destroy: (destroyed) => {
        return DestinationsService.destroy(destroyed.id);
    },

    search: (search, item) => {
        return item.name.toLowerCase().includes(search);
    },
    sort: (itemA, itemB) => itemA.name.localeCompare(itemB.name),

    toPrimaryString: destination => destination.name,
    toSecondaryString: destination => {
        let sec: string = destination.type;

        if (destination.type === DestinationType.Mail) {
            if (!isStringNullOrEmpty(destination.mailTo)) {
                sec += `\n${destination.mailTo}`;
            }
            if (!isStringNullOrEmpty(destination.mailCC)) {
                sec += `\nCC: ${destination.mailCC}`;
            }
            if (!isStringNullOrEmpty(destination.mailBCC)) {
                sec += `\nBCC: ${destination.mailBCC}`;
            }
        } else {
            sec += `\n${destination.apiAddress}`;
        }

        return sec;
    },

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
            field: 'type',
            label: _.typeLabel,
            helperText: _.typeHelper,
            placeholder: '',
            isOptions: true,
            options: [
                DestinationType.Mail.toString(),
                DestinationType.HTTP.toString(),
            ],
            required: true,
        },
        _.destinationSettingsTitle,
        {
            label: _.mailToLabel,
            placeholder: _.mailToPlaceholder,
            helperText: _.mailToHelper,
            field: 'mailTo',
            showIf: dest => dest.type === DestinationType.Mail,
            required: true,
        },
        {
            field: 'mailCC',
            label: _.mailCcLabel,
            placeholder: _.mailCcPlaceholder,
            helperText: _.mailCcHelper,
            showIf: dest => dest.type === DestinationType.Mail,
        },
        {
            field: 'mailBCC',
            label: _.mailBccLabel,
            placeholder: _.mailBccPlaceholder,
            helperText: _.mailBccHelper,
            showIf: dest => dest.type === DestinationType.Mail,
        },
        {
            field: 'apiAddress',
            label: _.apiAddressLabel,
            helperText: _.apiAddressHelper,
            placeholder: _.apiAddressPlaceholder,
            showIf: dest => dest.type === DestinationType.HTTP,
            required: true,
        },
        {
            field: 'authorizationHeader',
            label: _.apiKeyLabel,
            placeholder: _.apiKeyPlaceholder,
            helperText: _.apiKeyHelper,
            showIf: dest => dest.type === DestinationType.HTTP,
        },
        {
            field: 'maxAttachmentMegaBytes',
            label: _.maxAttachmentBytesLabel,
            placeholder: _.maxAttachmentBytesPlaceholder,
            helperText: _.maxAttachmentBytesHelper,
        },
    ],
};

export const DestinationsOverview = () => (
    <DataOverview {...DestinationsOverviewConfig}/>
);
