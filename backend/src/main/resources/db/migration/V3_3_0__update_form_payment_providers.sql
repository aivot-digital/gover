-- Create payment providers for ePayBL forms
insert into payment_providers (key, provider_key, name, description, is_test_provider, config)
select gen_random_uuid()                             as key,
       epaybl_forms.payment_provider                 as provider_key,
       epaybl_forms.payment_provider                 as name,
       'Automatisch generierter Bezahldienstleister' as description,
       false                                         as is_test_provider,
       jsonb_build_object(
               'endpointId', epaybl_forms.payment_endpoint_id,
               'originatorId', epaybl_forms.payment_originator_id,
               'certificate', '',
               'certificatePassword', '',
               'paymentTransactionUrl', 'https://epayment.dataport.de/konnektor/epayment/'
       )                                             as config
from (select distinct payment_provider, payment_endpoint_id, payment_originator_id
      from ((select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from submissions)
            union all
            (select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from forms)) as payment_providers
      where payment_provider = 'epaybl') as epaybl_forms;

-- Create payment providers for pmPayment forms

insert into payment_providers (key, provider_key, name, description, is_test_provider, config)
select gen_random_uuid()                             as key,
       pmpayment_forms.payment_provider              as provider_key,
       pmpayment_forms.payment_provider              as name,
       'Automatisch generierter Bezahldienstleister' as description,
       false                                         as is_test_provider,
       jsonb_build_object(
               'endpointId', pmpayment_forms.payment_endpoint_id,
               'originatorId', pmpayment_forms.payment_originator_id,
               'clientId', '',
               'clientSecret', '',
               'oauthUrl', 'https://payment.govconnect.de/oauth2/token',
               'paymentTransactionUrl', 'https://payment.govconnect.de'
       )                                             as config
from (select distinct payment_provider, payment_endpoint_id, payment_originator_id
      from ((select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from submissions)
            union all
            (select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from forms)) as payment_providers
      where payment_provider = 'pmpayment') as pmpayment_forms;

-- Create payment providers for girocheckout forms

insert into payment_providers (key, provider_key, name, description, is_test_provider, config)
select gen_random_uuid()                             as key,
       pmpayment_forms.payment_provider              as provider_key,
       pmpayment_forms.payment_provider              as name,
       'Automatisch generierter Bezahldienstleister' as description,
       false                                         as is_test_provider,
       jsonb_build_object(
               'sellerId', pmpayment_forms.payment_originator_id,
               'projectId', pmpayment_forms.payment_endpoint_id,
               'projectPasswordSecret', ''
       )                                             as config
from (select distinct payment_provider, payment_endpoint_id, payment_originator_id
      from ((select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from submissions)
            union all
            (select payment_provider,
                    payment_originator_id,
                    payment_endpoint_id
             from forms)) as payment_providers
      where payment_provider = 'giropay') as pmpayment_forms;

-- Update forms payment provider column to be larger

alter table forms
    alter column payment_provider type varchar(36);

-- Update all forms with new references to the ePayBL payment providers

update forms
set payment_provider = (select key from payment_providers where provider_key = 'epaybl' and config ->> 'endpointId' = payment_endpoint_id and config ->> 'originatorId' = payment_originator_id)
where payment_provider = 'epaybl';

-- Update all forms with new references to the pmPayment payment providers

update forms
set payment_provider = (select key from payment_providers where provider_key = 'pmpayment' and config ->> 'endpointId' = payment_endpoint_id and config ->> 'originatorId' = payment_originator_id)
where payment_provider = 'pmpayment';

-- Update all forms with new references to the pmPayment payment providers

update forms
set payment_provider = (select key from payment_providers where provider_key = 'giropay' and config ->> 'endpointId' = payment_endpoint_id and config ->> 'originatorId' = payment_originator_id)
where payment_provider = 'giropay';

-- Add foreign key constraints to the forms table

alter table forms
    add constraint forms_payment_provider_fkey foreign key (payment_provider) references payment_providers (key) on delete restrict;

-- Drop originatorId and endpointId columns from forms table

alter table forms
    drop column payment_originator_id,
    drop column payment_endpoint_id;