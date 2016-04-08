package com.bsb.hike.utils;

import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public final class Telephony {

	private static Telephony telephonyInfo;
	private static Context mContext;
	private static TelephonyManager telephonyManager;
	private boolean isSIM1Ready;
	private boolean isSIM2Ready;
	private boolean isSIM3Ready;

	static String[] operators = new String[3];
	static String[] imei = new String[3];
	static String[] number = new String[3];
	static int[] roaming = { -1, -1, -1 };
	static String[] countryISO = new String[3];
	static int[] simSlot = { -1, -1, -1 };
	static Sim[] sims = new Sim[3];

	public static Sim[] getSims() {
		return sims;
	}

	public static void setSims(Sim[] sims) {
		Telephony.sims = sims;
	}

	private static int activeSimCount;
	private static boolean defaultCheckDone;
	private static boolean needTocheck;

	public static int getActiveSimCount() {
		return activeSimCount;
	}

	public static void setActiveSimCount(int activeSimCount) {
		Telephony.activeSimCount = activeSimCount;
	}

	public String[] getOperators() {
		return operators;
	}

	public boolean isSIM1Ready() {
		return isSIM1Ready;
	}

	public boolean isSIM2Ready() {
		return isSIM2Ready;
	}

	private Telephony() {
	}

	public static Telephony getInstance(Context context)
	{
		telephonyInfo = new Telephony();

		telephonyManager = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));

		mContext = context;
		checkForMultiSim(context);
		activeSimCount = getSimReadyCount();

		getIMEI();

		if (isLollipopMR1OrHigher())
		{
			buildSimObjects();
		}
		else
		{
			needTocheck = true;
		}

		// printTelephonyManagerMethodNamesForThisDevice(context);
		// Check for default case
		if (needTocheck)
		{
			if (!defaultCheckDone)
			{
				checkDefault();

				// Check for all Attributes
				getOperatorNames();
				getPhoneNumber();
				getRoaming();
				getCountryISO();
			}
		}

		// Create sims object
		createSimObject();
		return telephonyInfo;
	}


	private static void getCountryISO() {
		boolean search = false;
		if (countryISO[0] == null || countryISO[1] == null
				|| countryISO[2] == null) {
			search = true;
		} else if (countryISO[0].trim().length() <= 0
				|| countryISO[1].trim().length() <= 0
				|| countryISO[2].trim().length() <= 0) {
			search = true;
		}
		if (search) {
			if (countryISO[0] == null || countryISO[0].trim().length() == 0) {
				countryISO[0] = telephonyManager.getNetworkCountryIso();
			}
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1 && countryISO[i] == null) {
						countryISO[i] = getDeviceIdBySlot(mContext,
								"getNetworkCountryIsoGemini", i);
					}
				}
			} catch (GeminiMethodNotFoundException e) {
				try {

					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1 && countryISO[i] == null) {
							countryISO[i] = getDeviceIdSlot(mContext,
									"getNetworkCountryIso", Long.valueOf(i));
						}
					}
				} catch (GeminiMethodNotFoundException e1) {
					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1 && countryISO[i] == null) {
								countryISO[i] = getDeviceIdBySlot(mContext,
										"getSimCountryIsoGemini", i);
							}
						}

					} catch (GeminiMethodNotFoundException e2) {
						try {
							for (int i = 0; i < simSlot.length; i++) {
								if (simSlot[i] != -1 && countryISO[i] == null) {
									countryISO[i] = getDeviceIdSlot(mContext,
											"getSimCountryIso", Long.valueOf(i));
								}
							}

						} catch (GeminiMethodNotFoundException e3) {
							try {
								for (int i = 0; i < simSlot.length; i++) {
									if (simSlot[i] != -1
											&& countryISO[i] == null) {
										countryISO[i] = getDeviceIdBySlot(
												mContext,
												"getNetworkCountryIsoForPhone",
												i);
									}
								}
							} catch (GeminiMethodNotFoundException ex) {
								try {
									for (int i = 0; i < simSlot.length; i++) {
										if (simSlot[i] != -1
												&& countryISO[i] == null) {
											countryISO[i] = getDeviceIdBySlot(
													mContext,
													"getSimCountryIso", i);
										}
									}
								} catch (GeminiMethodNotFoundException ex1) {
									try {
										for (int i = 0; i < simSlot.length; i++) {
											if (simSlot[i] != -1
													&& countryISO[i] == null) {
												countryISO[i] = getDeviceIdBySlot(
														mContext,
														"getSimCountryIsoForPhone",
														i);
											}
										}
									} catch (GeminiMethodNotFoundException ex2) {
										try {

											for (int i = 0; i < simSlot.length; i++) {
												if (simSlot[i] != -1
														&& countryISO[i] == null) {
													countryISO[i] = getDeviceIdSlot(
															mContext,
															"getNetworkCountryIsoGemini",
															Long.valueOf(i));
												}
											}
										} catch (GeminiMethodNotFoundException e4) {
											try {
												for (int i = 0; i < simSlot.length; i++) {
													if (simSlot[i] != -1
															&& countryISO[i] == null) {
														countryISO[i] = getDeviceIdBySlot(
																mContext,
																"getNetworkCountryIso",
																i);
													}
												}
											} catch (GeminiMethodNotFoundException e7) {
												try {

													for (int i = 0; i < simSlot.length; i++) {
														if (simSlot[i] != -1
																&& countryISO[i] == null) {
															countryISO[i] = getDeviceIdSlot(
																	mContext,
																	"getSimCountryIsoGemini",
																	Long.valueOf(i));
														}
													}
												} catch (GeminiMethodNotFoundException e6) {
													try {
														for (int i = 0; i < simSlot.length; i++) {
															if (simSlot[i] != -1
																	&& countryISO[i] == null) {
																countryISO[i] = getDeviceIdSlot(
																		mContext,
																		"getNetworkCountryIsoForPhone",
																		Long.valueOf(i));
															}
														}

													} catch (GeminiMethodNotFoundException e8) {
														try {
															for (int i = 0; i < simSlot.length; i++) {
																if (simSlot[i] != -1
																		&& countryISO[i] == null) {
																	countryISO[i] = getDeviceIdSlot(
																			mContext,
																			"getSimCountryIsoForPhone",
																			Long.valueOf(i));
																}
															}

														} catch (GeminiMethodNotFoundException e9) {
														}
													}
												}
											}
										}

									}
								}

							}
						}
					}
				}
			}
		}
	}

	private static void getRoaming() {

		if (roaming[0] == -1) {
			roaming[0] = telephonyManager.isNetworkRoaming() == true ? 1 : 0;
		}
		try {
			for (int i = 0; i < simSlot.length; i++) {
				if (simSlot[i] != -1 && roaming[i] == -1) {
					roaming[i] = getDeviceIdSlot(mContext,
							"getDataRoamingEnabled", Long.valueOf(i))
							.equalsIgnoreCase("true") ? 1 : 0;
				}
			}
		} catch (GeminiMethodNotFoundException e) {
			try {

				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1 && roaming[i] == -1) {
						roaming[i] = getDeviceIdSlot(mContext,
								"isNetworkRoaming", Long.valueOf(i))
								.equalsIgnoreCase("true") ? 1 : 0;
					}
				}
			} catch (GeminiMethodNotFoundException e1) {
				try {
					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1 && roaming[i] == -1) {
							roaming[i] = getDeviceIdBySlot(mContext,
									"isNetworkRoaming", i).equalsIgnoreCase(
									"true") ? 1 : 0;
						}
					}

				} catch (GeminiMethodNotFoundException e2) {
					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1 && roaming[i] == -1) {
								roaming[i] = getDeviceIdBySlot(mContext,
										"getDataRoamingEnabled", i)
										.equalsIgnoreCase("true") ? 1 : 0;
							}
						}

					} catch (GeminiMethodNotFoundException e3) {

					}
				}
			}
		}
	}

	private static void getPhoneNumber() {
		boolean search = false;
		if (number[0] == null || number[1] == null || number[2] == null) {
			search = true;
		} else if (number[0].trim().length() <= 0
				|| number[1].trim().length() <= 0
				|| number[2].trim().length() <= 0) {
			search = true;
		}
		if (search) {
			if (number[0] == null || number[0].trim().length() == 0) {
				number[0] = telephonyManager.getLine1Number();
			}
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1 && number[i] == null) {
						number[i] = getDeviceIdBySlot(mContext,
								"getLine1Number", i);
					}
				}
			} catch (GeminiMethodNotFoundException e) {
				try {

					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1 && number[i] == null) {
							number[i] = getDeviceIdSlot(mContext,
									"getLine1Number", Long.valueOf(i));
						}
					}
				} catch (GeminiMethodNotFoundException e1) {
					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1 && number[i] == null) {
								number[i] = getDeviceIdBySlot(mContext,
										"getLine1NumberForSubscriber", i);
							}
						}

					} catch (GeminiMethodNotFoundException e2) {
						try {
							for (int i = 0; i < simSlot.length; i++) {
								if (simSlot[i] != -1 && number[i] == null) {
									number[i] = getDeviceIdSlot(mContext,
											"getLine1NumberForSubscriber",
											Long.valueOf(i));
								}
							}

						} catch (GeminiMethodNotFoundException e3) {

						}
					}
				}
			}
		}
	}

	private static void getIMEI() {
		boolean search = false;
		if (imei[0] == null || imei[1] == null || imei[2] == null) {
			search = true;
		} else if (imei[0].trim().length() <= 0 || imei[1].trim().length() <= 0
				|| imei[2].trim().length() <= 0) {
			search = true;
		}
		if (search) {
			if (imei[0] == null || imei[0].trim().length() == 0) {
				imei[0] = telephonyManager.getDeviceId();
			}
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1 && imei[i] == null) {
						imei[i] = getDeviceIdBySlot(mContext,
								"getDeviceIdGemini", i);
					}
				}
			} catch (GeminiMethodNotFoundException e) {
				try {

					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1 && imei[i] == null) {
							imei[i] = getDeviceIdBySlot(mContext,
									"getDeviceId", i);
						}
					}
				} catch (GeminiMethodNotFoundException e1) {
					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1 && imei[i] == null) {
								imei[i] = getDeviceIdBySlot(mContext,
										"getDeviceIdDs", i);
							}
						}

					} catch (GeminiMethodNotFoundException e2) {

					}
				}
			}
		}

	}

	private static void checkDefault() {
		try {
			for (int i = 0; i < simSlot.length; i++) {
				if (simSlot[i] != -1) {
					getMethodCheck(mContext, "getDefault", i);
				}
			}
		} catch (GeminiMethodNotFoundException e2) {
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1) {
						getMethodCheck(mContext, "getDefault", Long.valueOf(i));
					}
				}
			} catch (GeminiMethodNotFoundException e) {
			}
		}

	}

	private static void createSimObject() {
		for (int i = 0; i < simSlot.length; i++) {
			if (simSlot[i] != -1) {
				Sim sim = new Sim();
				sim.setCountryISO(countryISO[i]);
				sim.setImei(imei[i]);
				sim.setNetworkOperator(operators[i]);
				sim.setPhoneNumber(number[i]);
				sim.setRoaming(roaming[i]);
				sim.setSlotIndex(simSlot[i]);
				sims[i] = sim;
			}
		}

	}

	private static void buildSimObjects() {
		List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(
				mContext).getActiveSubscriptionInfoList();
		if(subscriptionInfos!=null){
		for (int i = 0; i < subscriptionInfos.size(); i++) {
			Sim sim = new Sim();
			SubscriptionInfo lsuSubscriptionInfo = subscriptionInfos.get(i);
			sim.setNetworkOperator((String) lsuSubscriptionInfo
					.getDisplayName());
			sim.setCountryISO(lsuSubscriptionInfo.getCountryIso());
			sim.setRoaming(lsuSubscriptionInfo.getDataRoaming());
			sim.setPhoneNumber(lsuSubscriptionInfo.getNumber());
			sim.setSlotIndex(lsuSubscriptionInfo.getSimSlotIndex());
			sim.setImei(imei[i]);
			sims[i] = sim;
		}
		}else{
			needTocheck = true;
		}

	}

	private static int getSimReadyCount() {
		int count = 0;
		if (telephonyInfo.isSIM1Ready) {
			simSlot[0] = 0;
			count++;
		}
		if (telephonyInfo.isSIM2Ready) {
			simSlot[1] = 1;
			count++;
		}
		if (telephonyInfo.isSIM3Ready) {
			simSlot[2] = 2;
			count++;
		}
		return count;
	}

	public static void checkForMultiSim(Context context) {

		telephonyInfo.isSIM1Ready = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
		telephonyInfo.isSIM2Ready = false;
		telephonyInfo.isSIM3Ready = false;

		try {
			for (int i = 0; i < simSlot.length; i++) {

				getSIMStateBySlot(context, "getSimStateGemini", i);

			}

		} catch (GeminiMethodNotFoundException e) {

			{

				try {
					for (int i = 0; i < simSlot.length; i++) {
						getSIMStateBySlot(context, "getSimState", i);
					}
				} catch (GeminiMethodNotFoundException e1) {
					try {
						for (int i = 0; i < simSlot.length; i++) {
							getSIMStateBySlot(context, "getDefault", i);
						}
					} catch (GeminiMethodNotFoundException e2) {
					}
				}
			}
		}
	}

	public static void getOperatorNames() {

		boolean search = false;
		if (operators[0] == null || operators[1] == null
				|| operators[2] == null) {
			search = true;
		} else if (operators[0].trim().length() <= 0
				|| operators[1].trim().length() <= 0
				|| operators[2].trim().length() <= 0) {
			search = true;
		}
		if (operators[0] == null || operators[0].trim().length() == 0) {
			operators[0] = telephonyManager.getNetworkOperatorName();
		}
		if (search) {
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1 && operators[i] == null) {
						operators[i] = getDeviceIdBySlot(mContext,
								"getDefault", simSlot[i]);
					}
				}

			} catch (GeminiMethodNotFoundException e9) {

				try {
					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1) {
							operators[i] = getDeviceIdBySlot(mContext,
									"getNetworkOperatorNameGemini", simSlot[i]);
						}
					}
				} catch (GeminiMethodNotFoundException e) {

					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1) {
								operators[i] = getDeviceIdBySlot(mContext,
										"getNetworkOperatorName", simSlot[i]);
							}
						}

					} catch (GeminiMethodNotFoundException e1) {
						// Call here for next manufacturer's predicted method
						// name
						// if you wish
						try {
							for (int i = 0; i < simSlot.length; i++) {
								if (simSlot[i] != -1) {
									operators[i] = getDeviceIdBySlot(mContext,
											"getSubscriberInfo", simSlot[i]);
								}
							}

						} catch (GeminiMethodNotFoundException e2) {
							try {
								for (int i = 0; i < simSlot.length; i++) {
									if (simSlot[i] != -1) {
										operators[i] = getDeviceIdSlot(
												mContext, "getSimOperatorName",
												Long.valueOf(simSlot[i]));
									}
								}

							} catch (GeminiMethodNotFoundException e3) {
								// Call here for next manufacturer's predicted
								// method name
								// if you wish
								try {
									for (int i = 0; i < simSlot.length; i++) {
										if (simSlot[i] != -1) {
											operators[i] = getDeviceIdSlot(
													mContext,
													"getNetworkOperator",
													Long.valueOf(simSlot[i]));
										}
									}

								} catch (GeminiMethodNotFoundException e4) {
									// Call here for next manufacturer's
									// predicted
									// method name
									try {
										for (int i = 0; i < simSlot.length; i++) {
											if (simSlot[i] != -1) {
												operators[i] = getDeviceIdSlot(
														mContext,
														"getSimOperator",
														Long.valueOf(simSlot[i]));
											}
										}

									} catch (GeminiMethodNotFoundException e5) {
										try {
											for (int i = 0; i < simSlot.length; i++) {
												if (simSlot[i] != -1) {
													operators[i] = getDeviceIdSlot(
															mContext,
															"getSimOperatorName",
															Long.valueOf(simSlot[i]));
												}
											}

										} catch (GeminiMethodNotFoundException e6) {
											try {
												for (int i = 0; i < simSlot.length; i++) {
													if (simSlot[i] != -1) {
														operators[i] = getDeviceIdBySlot(
																mContext,
																"getSimOperatorNameForPhone",
																simSlot[i]);
													}
												}

											} catch (GeminiMethodNotFoundException e7) {

											}
										}
									}
								}
							}
						}
					}
				}
			}
			// telephonyManager.getAllCellInfo() ;
		}
	}

	public static boolean isLollipopMR1OrHigher() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
	}

	private static String getDeviceIdBySlot(Context context,
			String predictedMethodName, int slotID)
			throws GeminiMethodNotFoundException {

		String value = null;

		try {

			Class<?> telephonyClass = Class.forName(telephonyManager.getClass()
					.getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = int.class;
			Method getSimID = telephonyClass.getMethod(predictedMethodName,
					parameter);
			getSimID.setAccessible(true);
			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimID.invoke(telephonyManager, obParameter);
			if (ob_phone instanceof TelephonyManager) {
				TelephonyManager tManager = (TelephonyManager) ob_phone;
				String operatorNaame = tManager.getNetworkOperatorName();
				String simOperatorName = tManager.getSimOperatorName();

				if (operatorNaame == null || operatorNaame.trim().length() <= 0) {
					if (simOperatorName != null
							|| simOperatorName.trim().length() > 0) {
						operatorNaame = simOperatorName;
					}
				}

				value = operatorNaame;

			} else if (ob_phone != null) {
				value = ob_phone.toString();
				if (value.trim().length() == 0) {
					throw new GeminiMethodNotFoundException(predictedMethodName);
				}

			}
		} catch (Exception e) {
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return value;
	}

	private static String getDeviceIdSlot(Context context,
			String predictedMethodName, long slotID)
			throws GeminiMethodNotFoundException {

		String value = null;

		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		try {

			Class<?> telephonyClass = Class.forName(telephony.getClass()
					.getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = long.class;
			Method getSimID = telephonyClass.getMethod(predictedMethodName,
					parameter);
			getSimID.setAccessible(true);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimID.invoke(telephony, obParameter);

			if (ob_phone != null) {
				value = ob_phone.toString();

			}
		} catch (Exception e) {
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return value;
	}

	private static void getMethodCheck(Context context,
			String predictedMethodName, long slotID)
			throws GeminiMethodNotFoundException {

		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		try {

			Class<?> telephonyClass = Class.forName(telephony.getClass()
					.getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = long.class;
			Method getSimID = telephonyClass.getMethod(predictedMethodName,
					parameter);
			getSimID.setAccessible(true);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimID.invoke(telephony, obParameter);
			if (ob_phone instanceof TelephonyManager) {
				TelephonyManager tManager = (TelephonyManager) ob_phone;
				fetchFromTelephony((int) slotID, tManager);
			}
		} catch (Exception e) {
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

	}

	private static void getMethodCheck(Context context,
			String predictedMethodName, int slotID)
			throws GeminiMethodNotFoundException {

		String value = null;

		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		try {

			Class<?> telephonyClass = Class.forName(telephony.getClass()
					.getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = int.class;
			Method getSimID = telephonyClass.getMethod(predictedMethodName,
					parameter);
			getSimID.setAccessible(true);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimID.invoke(telephony, obParameter);
			if (ob_phone instanceof TelephonyManager) {
				TelephonyManager tManager = (TelephonyManager) ob_phone;
				fetchFromTelephony(slotID, tManager);
			}
		} catch (Exception e) {
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}
	}

	private static void fetchFromTelephony(int slotID, TelephonyManager tManager) {
		if (imei[slotID] == null || imei[slotID].trim().length() == 0) {
			imei[slotID] = tManager.getDeviceId();
		}
		if (number[slotID] == null || number[slotID].trim().length() == 0) {
			number[slotID] = tManager.getLine1Number();
		}
		if (operators[slotID] == null || operators[slotID].trim().length() == 0) {
			operators[slotID] = tManager.getNetworkOperatorName();
			if (operators[slotID] != null
					&& operators[slotID].trim().length() == 0) {
				operators[slotID] = tManager.getSimOperatorName();
			}
		}
		if (countryISO[slotID] == null
				|| countryISO[slotID].trim().length() == 0) {
			countryISO[slotID] = tManager.getNetworkCountryIso();
			if (countryISO[slotID] != null
					&& countryISO[slotID].trim().length() == 0) {
				countryISO[slotID] = tManager.getSimCountryIso();
			}
		}
		if (roaming[slotID] == -1 && tManager.isNetworkRoaming()) {
			roaming[slotID] = 1;
		}
		defaultCheckDone = true;
	}

	private static void getSIMStateBySlot(Context context,
			String predictedMethodName, int slotID)
			throws GeminiMethodNotFoundException {

		boolean isReady = false;

		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		try {

			Class<?> telephonyClass = Class.forName(telephony.getClass()
					.getName());

			Class<?>[] parameter = new Class[1];
			parameter[0] = int.class;
			Method getSimStateGemini = telephonyClass.getMethod(
					predictedMethodName, parameter);

			Object[] obParameter = new Object[1];
			obParameter[0] = slotID;
			Object ob_phone = getSimStateGemini.invoke(telephony, obParameter);
			if (ob_phone instanceof TelephonyManager) {
				TelephonyManager tManager = (TelephonyManager) ob_phone;
				int simState = tManager.getSimState();
				if (simState == TelephonyManager.SIM_STATE_READY) {
					isReady = true;
					fetchFromTelephony(slotID, tManager);
				}

				// value = operatorNaame;

			} else if (ob_phone != null) {
				int simState = Integer.parseInt(ob_phone.toString());
				if (simState == TelephonyManager.SIM_STATE_READY) {
					isReady = true;
				}
			}
		} catch (Exception e) {
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		if (slotID == 0) {
			telephonyInfo.isSIM1Ready = isReady;
		} else if (slotID == 1) {
			telephonyInfo.isSIM2Ready = isReady;
		} else if (slotID == 2) {
			telephonyInfo.isSIM3Ready = isReady;
		}
	}

	private static class GeminiMethodNotFoundException extends Exception {

		private static final long serialVersionUID = -996812356902545308L;

		public GeminiMethodNotFoundException(String info) {
			super(info);
		}
	}

	public static void printTelephonyManagerMethodNamesForThisDevice(
			Context context) {

		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		Class<?> telephonyClass;
		try {
			telephonyClass = Class.forName(telephony.getClass().getName());
			Method[] methods = telephonyClass.getDeclaredMethods();
			for (int idx = 0; idx < methods.length; idx++) {

				System.out.println("\nMethod: " + methods[idx]
						+ " declared by " + methods[idx].getDeclaringClass());
			}
		} catch (ClassNotFoundException e) {

		}
	}

}