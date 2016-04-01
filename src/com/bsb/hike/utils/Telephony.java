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

		getOperatorNames();
		return telephonyInfo;
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
				operators[0] = getDeviceIdBySlot(mContext,
						"getNetworkOperatorNameGemini", 0);
				operators[1] = getDeviceIdBySlot(mContext,
						"getNetworkOperatorNameGemini", 1);
				operators[2] = getDeviceIdBySlot(mContext,
						"getNetworkOperatorNameGemini", 2);
			} catch (GeminiMethodNotFoundException e) {
				e.printStackTrace();

				try {
					operators[0] = getDeviceIdBySlot(mContext,
							"getNetworkOperatorName", 0);
					operators[1] = getDeviceIdBySlot(mContext,
							"getNetworkOperatorName", 1);
					operators[2] = getDeviceIdBySlot(mContext,
							"getNetworkOperatorName", 2);
				} catch (GeminiMethodNotFoundException e1) {
					// Call here for next manufacturer's predicted method name
					// if you wish
					try {
						operators[0] = getDeviceIdBySlot(mContext,
								"getSubscriberInfo", 0);
						operators[1] = getDeviceIdBySlot(mContext,
								"getSubscriberInfo", 1);
						operators[2] = getDeviceIdBySlot(mContext,
								"getSubscriberInfo", 2);
					} catch (GeminiMethodNotFoundException e2) {
						// Call here for next manufacturer's predicted method
						// name
						// if you wish
						e1.printStackTrace();
						try {
							operators[0] = getDeviceIdSlot(mContext,
									"getSimOperatorName", 0l);
							operators[1] = getDeviceIdSlot(mContext,
									"getSimOperatorName", 1l);
							operators[2] = getDeviceIdSlot(mContext,
									"getSimOperatorName", 2l);
						} catch (GeminiMethodNotFoundException e3) {
							// Call here for next manufacturer's predicted
							// method name
							// if you wish
							try {
								operators[0] = getDeviceIdSlot(mContext,
										"getNetworkOperator", 0l);
								operators[1] = getDeviceIdSlot(mContext,
										"getNetworkOperator", 1l);
								operators[2] = getDeviceIdSlot(mContext,
										"getNetworkOperator", 2l);
							} catch (GeminiMethodNotFoundException e4) {
								// Call here for next manufacturer's predicted
								// method name
								try {
									operators[0] = getDeviceIdSlot(mContext,
											"getSimOperator", 0l);
									operators[1] = getDeviceIdSlot(mContext,
											"getSimOperator", 1l);
									operators[2] = getDeviceIdSlot(mContext,
											"getSimOperator", 2l);
								} catch (GeminiMethodNotFoundException e5) {
									// Call here for next manufacturer's
									// predicted method name
									// if you wish
									e1.printStackTrace();
									try {
										operators[0] = getDeviceIdSlot(
												mContext, "getSimOperatorName",
												0l);
										operators[1] = getDeviceIdSlot(
												mContext, "getSimOperatorName",
												1l);
										operators[2] = getDeviceIdSlot(
												mContext, "getSimOperatorName",
												2l);
									} catch (GeminiMethodNotFoundException e6) {
										// Call here for next manufacturer's
										// predicted method name
										// if you wish
										e1.printStackTrace();
										try {
											operators[0] = getDeviceIdBySlot(
													mContext,
													"getSimOperatorNameForPhone",
													0);
											operators[1] = getDeviceIdBySlot(
													mContext,
													"getSimOperatorNameForPhone",
													1);
											operators[2] = getDeviceIdBySlot(
													mContext,
													"getSimOperatorNameForPhone",
													2);
										} catch (GeminiMethodNotFoundException e7) {
											e1.printStackTrace();
											try {
												operators[0] = getDeviceIdBySlot(
														mContext, "getDefault",
														0);
												operators[1] = getDeviceIdBySlot(
														mContext, "getDefault",
														1);
												operators[2] = getDeviceIdBySlot(
														mContext, "getDefault",
														2);
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

	private static String getDeviceIdBySlot(Context context,
			String predictedMethodName, int slotID)
			throws GeminiMethodNotFoundException {

		String imei = null;

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

				return operatorNaame;

			} else if (ob_phone != null) {
				imei = ob_phone.toString();

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return imei;
	}

	private static String getDeviceIdSlot(Context context,
			String predictedMethodName, long slotID)
			throws GeminiMethodNotFoundException {

		String imei = null;

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
				imei = ob_phone.toString();

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return imei;
	}

	private static boolean getSIMStateBySlot(Context context,
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

			if (ob_phone != null) {
				int simState = Integer.parseInt(ob_phone.toString());
				if (simState == TelephonyManager.SIM_STATE_READY) {
					isReady = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GeminiMethodNotFoundException(predictedMethodName);
		}

		return isReady;
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