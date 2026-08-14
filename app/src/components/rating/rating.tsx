import React from 'react';
import {Rating as MuiRating, useTheme} from '@mui/material';
import {type IconContainerProps} from '@mui/material/Rating';
import MoodBadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/MoodBad';
import SentimentDissatisfiedOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SentimentDissatisfied';
import SentimentNeutralOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SentimentNeutral';
import SentimentSatisfiedAltOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SentimentSatisfied';
import EmojiEmotionsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SentimentVerySatisfied';

interface RatingProps {
    value?: number;
    onChange?: (val: number | null) => void;
}

const customIcons: Record<string, {
    icon: React.ReactElement<any>;
    label: string;
}> = {
    1: {
        icon: <MoodBadOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Sehr Unzufrieden',
    },
    2: {
        icon: <SentimentDissatisfiedOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Unzufrieden',
    },
    3: {
        icon: <SentimentNeutralOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Neutral',
    },
    4: {
        icon: <SentimentSatisfiedAltOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Zufrieden',
    },
    5: {
        icon: <EmojiEmotionsOutlinedIcon
            sx={{
                fontSize: '40px',
                margin: '0 5px',
            }}
        />,
        label: 'Sehr Zufrieden',
    },
};

export function Rating(props: RatingProps) {
    const theme = useTheme();

    return (
        <MuiRating
            sx={{
                color: theme.palette.primary.main,
            }}
            readOnly={props.onChange == null}
            value={props.value}
            name="highlight-selected-only"
            getLabelText={(value) => `${customIcons[value].label}, ${value} von 5`}
            highlightSelectedOnly
            size="large"
            onChange={(_, newValue) => {
                if (props.onChange != null) {
                    props.onChange(newValue);
                }
            }}
            slotProps={{
                icon: {
                    component: IconContainer
                }
            }}
        />
    );
}

function IconContainer(props: IconContainerProps) {
    const {
        value,
        ...other
    } = props;
    return (
        <span {...other} aria-hidden="true">
            {customIcons[value].icon}
        </span>
    );
}
