package de.aivot.GoverBackend.data;

public class SpecialCustomerInputKeys {
    // TODO: Capitalize the variable names
    /**
     * @deprecated use {@link de.aivot.GoverBackend.elements.models.steps.IntroductionStepElement#CUSTOMER_IDENTITY_DATA_ID} instead
     */
    @Deprecated
    public static final String IdCustomerInputKey = "__id_data__";

    public static final String UserInfoKey = "userInfo";

    public static final String IdpCustomerInputKey = "idp";

    /**
     * @deprecated use {@link de.aivot.GoverBackend.elements.models.steps.IntroductionStepElement#PRIVACY_CHECKBOX_ID} instead
     */
    @Deprecated
    public static final String PRIVACY_CHECKBOX_ID = "__privacy__";

    /**
     * @deprecated use {@link de.aivot.GoverBackend.elements.models.steps.SummaryStepElement#SUMMARY_CONFIRMATION} instead
     */
    @Deprecated
    public static final String SUMMARY_CONFIRMATION = "__summary__";
}
