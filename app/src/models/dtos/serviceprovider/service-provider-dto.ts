// TODO: Implement this DTO
export interface ServiceProviderDTO {
}
// package de.aivot.GoverBackend.serviceprovider.dtos;
// 
// import de.aivot.GoverBackend.serviceprovider.providers.ServiceProvider;
// 
// /**
//  * Data transfer object for service providers.
//  * This is used to display service providers in the UI.
//  * @param packageName The unique identifier of the provider package
//  * @param label The label of the provider when displayed in the UI
//  * @param description The description of the provider when displayed in the UI
//  */
// public record ServiceProviderDTO(
//         String packageName,
//         String label,
//         String description
// ) {
//     /**
//      * Create a service provider DTO from a service provider interface.
//      * This is used to send the service provider information to the UI via the REST API.
//      * @param spi The service provider interface to create the DTO from
//      * @return The service provider DTO
//      */
//     public static ServiceProviderDTO fromSPI(ServiceProvider spi) {
//         return new ServiceProviderDTO(
//                 spi.getPackageName(),
//                 spi.getLabel(),
//                 spi.getDescription()
//         );
//     }
// }
// 
