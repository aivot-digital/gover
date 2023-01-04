import {useSelector} from 'react-redux';
import React, {useEffect} from 'react';
import {UserInputService} from '../../../services/user-input.service';
import {Application} from '../../../models/application';
import {selectCustomerInput} from '../../../slices/customer-input-slice';

export function InputWatcher({application}: { application: Application }) {
    const customerInput = useSelector(selectCustomerInput);

    useEffect(() => {
        if (application && customerInput) {
            UserInputService.storeUserInput(application, customerInput);
        }
    }, [application, customerInput]);

    return <React.Fragment/>;
}
