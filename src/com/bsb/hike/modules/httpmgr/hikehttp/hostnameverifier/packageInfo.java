/**
 * This package contains two classes which are duplicate of {@link OkHostnameVerifier} and {@link DistinguishedNameParser} with minor change in verifyIpAddress method of
 * {@link OkHostnameVerifier} if ip verification fails we check in hardcoded ips If present then return true otherwise false 
 * 
 * These were added while fallback to ips in case of {@link UnknownHostException} was implemented and was failing when ssl was on So we added custom {@link HikeHostNameVerifier} to handle this case
 */
