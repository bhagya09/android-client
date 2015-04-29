package com.bsb.hike.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HikeSSLUtil
{
	public static String algorithmName = "TLSv1";

	// Server certificate is only valid for the following names: *.hike.in , hike.in
	private static String validServerName = "hike.in";
	
	// Added Cloud front server certificate and is only valid for the following names: *.cloudfront.net , cloudfront.net
	private static String validCloudFrontServerName = "cloudfront.net";

	// Create a trust manager that does not validate certificate chains
	public static TrustManager[] trustHikeCerts = new TrustManager[] { new X509TrustManager()
	{

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
		{

//			String cn_recieved = getCN(chain[0].getSubjectDN().getName());
//			if (cn_recieved == null || (!cn_recieved.endsWith(validServerName) && !cn_recieved.endsWith(validCloudFrontServerName)))
//			{
//				throw new CertificateException("Not a valid certificate.");
//			}

		}

		public X509Certificate[] getAcceptedIssuers()
		{
			return null;
		}

	} };

	private static String getCN(String dn)
	{
		int i = 0;
		i = dn.indexOf("CN=");
		if (i == -1)
		{
			return null;
		}

		// get the remaining DN without CN=
		dn = new String(dn.substring(i + 3));
		char[] dncs = dn.toCharArray();
		for (i = 0; i < dncs.length; i++)
		{
			if (dncs[i] == ',' && i > 0 && dncs[i - 1] != '\\')
			{
				break;
			}
		}
		return dn.substring(0, i);
	}

	public static SSLSocketFactory getSSLSocketFactory()
	{
		try
		{
			SSLContext sc = SSLContext.getInstance(HikeSSLUtil.algorithmName);
			sc.init(null, HikeSSLUtil.trustHikeCerts, new java.security.SecureRandom());
			return sc.getSocketFactory();
		}
		catch (Exception e)
		{
			return null;
		}
	}
}