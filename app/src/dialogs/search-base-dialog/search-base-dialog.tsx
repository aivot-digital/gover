import {SearchBaseDialogProps} from './search-base-dialog-props';
import {Dialog, DialogContent, Tab, Tabs} from '@mui/material';
import {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {SearchBaseDialogTab} from './search-base-dialog-tab';

export function SearchBaseDialog<T>(props: SearchBaseDialogProps<T>) {
    const [currentTab, setCurrentTab] = useState(0);

    useEffect(() => {
        setCurrentTab(0);
    }, [props.open]);

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth={true}
            maxWidth="lg"
        >
            <DialogTitleWithClose
                onClose={props.onClose}
                bordered
            >
                {props.title}

                {
                    props.tabs.length > 1 &&
                    <Tabs
                        value={currentTab}
                        onChange={(_, newValue) => {
                            setCurrentTab(newValue);
                        }}
                    >
                        {
                            props.tabs.map((tab, index) => (
                                <Tab
                                    key={tab.title}
                                    label={tab.title}
                                    value={index}
                                />
                            ))
                        }
                    </Tabs>
                }
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    padding: 0,
                    height: 'max(100vh - 480px, 480px)',
                }}
            >
                <SearchBaseDialogTab
                    {...props.tabs[currentTab]}
                />
            </DialogContent>
        </Dialog>
    );
}