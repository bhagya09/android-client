package com.bsb.hike.utils;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.X509HostnameVerifier;

import com.bsb.hike.modules.httpmgr.hikehttp.hostnameverifier.HikeHostNameVerifier;

public class HikeApacheHostNameVerifier implements X509HostnameVerifier{

	private HikeHostNameVerifier hikeHostVerifier;

	public HikeApacheHostNameVerifier() {
		hikeHostVerifier = new HikeHostNameVerifier();
	}

	@Override
	public boolean verify(String host, SSLSession session) {
		try {
			Certificate[] certificates = session.getPeerCertificates();
			verify(host, (X509Certificate) certificates[0]);
			return true;
		} catch (SSLException e) {
		    return false;
		}
	}

	@Override
	public void verify(String host, SSLSocket ssl) throws IOException {
		Certificate[] certificates = ssl.getSession().getPeerCertificates();
		try
		{
			verify(host, (X509Certificate) certificates[0]);
			return;
		}
		catch(SSLException e)
		{
			throw new IOException("Host is not verified");
		}
	}

	@Override
	public void verify(String host, X509Certificate certificate) throws SSLException {
		boolean isVerified = HikeHostNameVerifier.verifyAsIpAddress(host) ? hikeHostVerifier.verifyIpAddress(host, certificate) : hikeHostVerifier.verifyHostName(host, certificate);
		if(!isVerified)
			throw new SSLException("HikeApacheHostNameVerifier :- Host is not verified");
	}

	@Override
	public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
		boolean isVerified = HikeHostNameVerifier.verifyAsIpAddress(host) ? hikeHostVerifier.verifyIpAddress(host, subjectAlts) : verifyHostName(host, subjectAlts, cns);
		if(!isVerified)
			throw new SSLException("Host is not verified");
	}

	private boolean verifyHostName(String hostName, String [] altNames, String [] cn)
	{
		for (int i = 0, size = altNames.length; i < size; i++) {
			if (hikeHostVerifier.verifyHostName(hostName, altNames[i])) {
				return true;
			}
		}

		for (int i = 0, size = cn.length; i < size; i++) {
			if (hikeHostVerifier.verifyHostName(hostName, cn[i])) {
				return true;
			}
		}
		return false;
	}

	/**
  	 * Set FT host to make them white listed
  	 * @param ftHostIps
  	 */
  	public void setFTHostIps(List<String> ftHostIps) {
  		hikeHostVerifier.setFtHostIps(ftHostIps);
	}
}
