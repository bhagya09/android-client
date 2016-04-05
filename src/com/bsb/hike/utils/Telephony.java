package com.bsb.hike.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.telephony.TelephonyManager;

public final class Telephony {

	private static Telephony telephonyInfo;
	private static Context mContext;
	private static TelephonyManager telephonyManager;
	private String imeiSIM1;
	private String imeiSIM2;

	static String[] operators = new String[3];
	static int[] simSlot = {-1,-1,-1};
	private static int activeSimCount;

	public static int getActiveSimCount() {
		return activeSimCount;
	}

	public static void setActiveSimCount(int activeSimCount) {
		Telephony.activeSimCount = activeSimCount;
	}

	public String[] getOperators() {
		return operators;
	}

	public String getOperator1() {
		return operator1;
	}

	public void setOperator1(String operator1) {
		this.operator1 = operator1;
	}

	public String getOperator2() {
		return operator2;
	}

	public void setOperator2(String operator2) {
		this.operator2 = operator2;
	}

	private String operator1;
	private String operator2;

	private boolean isSIM1Ready;
	private boolean isSIM2Ready;
	private String imeiSIM3;
	private boolean isMultiSimEnabled;
	private boolean isSIM3Ready;

	public String getImeiSIM1() {
		return imeiSIM1;
	}

	public String getImeiSIM2() {
		return imeiSIM2;
	}

	public boolean isSIM1Ready() {
		return isSIM1Ready;
	}

	public boolean isSIM2Ready() {
		return isSIM2Ready;
	}

	public boolean isDualSIM() {
		return imeiSIM2 != null;
	}

	private Telephony() {
	}

	public static Telephony getInstance(Context context) {

		mContext = context;
		printTelephonyManagerMethodNamesForThisDevice(context);
		telephonyInfo = new Telephony();

		telephonyManager = ((TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE));

		checkForMultiSim(context);
		activeSimCount = getSimReadyCount();
		getOperatorNames();
		return telephonyInfo;
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

			e.printStackTrace();
			 {
				
					try {
						for (int i = 0; i < simSlot.length; i++){
						   getSIMStateBySlot(context, "getSimState",i);
						}
					} catch (GeminiMethodNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						try {
							for (int i = 0; i < simSlot.length; i++){
							   getSIMStateBySlot(context, "getDefault",i);
							}
						} catch (GeminiMethodNotFoundException e2) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
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
		if (search) {
			try {
				for (int i = 0; i < simSlot.length; i++) {
					if (simSlot[i] != -1) {
						getDeviceIdBySlot(mContext,
								"getNetworkOperatorNameGemini", simSlot[i]);
					}
				}
			} catch (GeminiMethodNotFoundException e) {
				e.printStackTrace();

				try {
					for (int i = 0; i < simSlot.length; i++) {
						if (simSlot[i] != -1) {
							getDeviceIdBySlot(mContext,
									"getNetworkOperatorName", simSlot[i]);
						}
					}

				} catch (GeminiMethodNotFoundException e1) {
					// Call here for next manufacturer's predicted method name
					// if you wish
					try {
						for (int i = 0; i < simSlot.length; i++) {
							if (simSlot[i] != -1) {
								getDeviceIdBySlot(mContext,
										"getSubscriberInfo", simSlot[i]);
							}
						}

					} catch (GeminiMethodNotFoundException e2) {
						// Call here for next manufacturer's predicted method
						// name
						// if you wish
						e1.printStackTrace();
						try {
							for (int i = 0; i < simSlot.length; i++) {
								if (simSlot[i] != -1) {
									getDeviceIdSlot(mContext,
											"getSimOperatorName",
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
										getDeviceIdSlot(mContext,
												"getNetworkOperator",
												Long.valueOf(simSlot[i]));
									}
								}

							} catch (GeminiMethodNotFoundException e4) {
								// Call here for next manufacturer's predicted
								// method name
								try {
									for (int i = 0; i < simSlot.length; i++) {
										if (simSlot[i] != -1) {
											getDeviceIdSlot(mContext,
													"getSimOperator",
													Long.valueOf(simSlot[i]));
										}
									}

								} catch (GeminiMethodNotFoundException e5) {
									// Call here for next manufacturer's
									// predicted method name
									// if you wish
									e1.printStackTrace();
									try {
										for (int i = 0; i < simSlot.length; i++) {
											if (simSlot[i] != -1) {
												getDeviceIdSlot(
														mContext,
														"getSimOperatorName",
														Long.valueOf(simSlot[i]));
											}
										}

									} catch (GeminiMethodNotFoundException e6) {
										// Call here for next manufacturer's
										// predicted method name
										// if you wish
										e1.printStackTrace();
										try {
											for (int i = 0; i < simSlot.length; i++) {
												if (simSlot[i] != -1) {
													getDeviceIdBySlot(
															mContext,
															"getSimOperatorNameForPhone",
															simSlot[i]);
												}
											}

										} catch (GeminiMethodNotFoundException e7) {
											e1.printStackTrace();
											try {
												for (int i = 0; i < simSlot.length; i++) {
													if (simSlot[i] != -1) {
														getDeviceIdBySlot(
																mContext,
																"getDefault",
																simSlot[i]);
													}
												}

											} catch (GeminiMethodNotFoundException e9) {
												// Call here for next
												// manufacturer's predicted
												// method name
												// if you wish
												e1.printStackTrace();

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

	private static void getDeviceIdBySlot(Context context,
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
				if( value.trim().length()==0){
					throw new GeminiMethodNotFoundException(predictedMethodName);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		operators[slotID] = value;
	}

	private static void getDeviceIdSlot(Context context,
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
			e.printStackTrace();
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		operators[(int)slotID] = value;
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
				}

				// value = operatorNaame;

			} else if(ob_phone != null) {
				int simState = Integer.parseInt(ob_phone.toString());
				if (simState == TelephonyManager.SIM_STATE_READY) {
					isReady = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
			// e.printStackTrace();
		}
	}

}