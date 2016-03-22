package com.bsb.hike.models;

public class AccountInfo
{
	private String token;

	private String msisdn;

	private String uid;

	private int smsCredits;

	private int all_invitee;

	private int all_invitee_joined;

	private String country_code;

	private String backupToken;

	private String serverName;

	private String serverGender;

	private Birthday serverDOB;

	private AccountInfo(Builder builder)
	{
		this.token = builder.token;
		this.msisdn = builder.msisdn;
		this.uid = builder.uid;
		this.smsCredits = builder.smsCredits;
		this.all_invitee = builder.all_invitee;
		this.all_invitee_joined = builder.all_invitee_joined;
		this.country_code = builder.country_code;
		this.backupToken = builder.backupToken;
		this.serverName = builder.serverName;
		this.serverGender = builder.serverGender;
		this.serverDOB = builder.serverDOB;
	}

	public String getToken()
	{
		return token;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public String getUid()
	{
		return uid;
	}

	public int getSmsCredits()
	{
		return smsCredits;
	}

	public int getAllInvitee()
	{
		return all_invitee;
	}

	public int getAllInviteeJoined()
	{
		return all_invitee_joined;
	}

	public String getCountryCode()
	{
		return country_code;
	}

	public String getServerName()
	{
		return serverName;
	}

	public String getServerGender()
	{
		return serverGender;
	}

	public Birthday getServerDOB()
	{
		return serverDOB;
	}

	public String getBackUpToken()
	{
		return backupToken;
	}

	public static class Builder
	{
		private String token;

		private String msisdn;

		private String uid;

		private int smsCredits;

		private int all_invitee;

		private int all_invitee_joined;

		private String country_code;

		private String backupToken;

		private String serverName;

		private String serverGender;

		private Birthday serverDOB;

		public AccountInfo build()
		{
			return new AccountInfo(this);
		}

		public Builder setToken(String token)
		{
			this.token = token;
			return this;
		}

		public Builder setMsisdn(String msisdn)
		{
			this.msisdn = msisdn;
			return this;
		}

		public Builder setUid(String uid)
		{
			this.uid = uid;
			return this;
		}

		public Builder setSmsCredits(int smsCredits)
		{
			this.smsCredits = smsCredits;
			return this;
		}

		public Builder setAllInvitee(int all_invitee)
		{
			this.all_invitee = all_invitee;
			return this;
		}

		public Builder setAllInviteJoined(int all_invitee_joined)
		{
			this.all_invitee_joined = all_invitee_joined;
			return this;
		}

		public Builder setCountryCode(String country_code)
		{
			this.country_code = country_code;
			return this;
		}

		public Builder setBackupToken(String backupToken)
		{
			this.backupToken = backupToken;
			return this;
		}

		public Builder setServerName(String serverName)
		{
			this.serverName = serverName;
			return this;
		}

		public Builder setServerGender(String serverGender)
		{
			this.serverGender = serverGender;
			return this;
		}

		public Builder setServerBirthday(Birthday serverDOB)
		{
			this.serverDOB = serverDOB;
			return this;
		}
	}
}