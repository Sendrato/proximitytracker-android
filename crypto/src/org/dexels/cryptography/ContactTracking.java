package org.dexels.cryptography;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ContactTracking {

	private static final String HMAC_SHA512 = "HmacSHA256";

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static byte [] truncate(byte [] input, int length) {

		byte [] result = new byte[length];

		System.arraycopy(input, 0, result, 0, length);

		return result;
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte [] calculateHMAC(String data, byte [] key) throws Exception
	{
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_SHA512);
		Mac mac = Mac.getInstance(HMAC_SHA512);
		mac.init(secretKeySpec);
		return mac.doFinal(data.getBytes());
	}

	public static boolean validateHMAC(byte [] hmac, byte [] key) throws Exception {
		String cs = bytesToHex(hmac);
		for ( int i = 0; i < 120; i++) {
			byte [] c = truncate(calculateHMAC(i+"", key),16);
			if ( bytesToHex(c).equals(cs) ) {
				return true;
			}
		}
		return false;
	}
	
	public static byte [] generateCRNG() {

		SecureRandom rand = new SecureRandom(); 

		byte [] tracingKey = new byte[32];
		rand.nextBytes(tracingKey);

		return tracingKey;

	}

	public static byte [] getDailyTracingKey(byte [] tracingKey) {

		byte [] dailyTracingKey = null;

		long dayNumber = System.currentTimeMillis() / ( 60 * 60 * 24 * 1000);

		Hkdf hkdf = Hkdf.usingDefaults();

		SecretKey originalKey = new SecretKeySpec(tracingKey, 0, tracingKey.length, "AES");

		byte [] info = BigInteger.valueOf(dayNumber).toByteArray();

		dailyTracingKey = hkdf.expand(originalKey, info, 16);

		return dailyTracingKey;

	}

	public static byte [] getRollingProximityIdentifier(byte [] dailyTracingKey) throws Exception {

		byte [] rpiPre = null;

		long timeIntervalNumber = ( ( System.currentTimeMillis() / 1000 ) % ( 60*60*24 ) ) / (60 * 10);

		System.err.println("timeIntervalNumber: " + timeIntervalNumber);

		rpiPre = calculateHMAC(timeIntervalNumber + "", dailyTracingKey);

		System.err.println("rollingProximityIdentifier (pre truncate): " + bytesToHex(rpiPre));

		return truncate(rpiPre, 16);

	}

	public static void main(String [] args) throws Exception {

		byte [] tracingKey = ContactTracking.generateCRNG();
		System.err.println("tracingKey: " + bytesToHex(tracingKey));

		System.err.println("CHANGING KEYS:");
		
		for ( int i =0; i < 10; i++ ) {
			byte [] dailyTracingKey = getDailyTracingKey(tracingKey);
			System.err.println("dailyTracingKey: " + bytesToHex(dailyTracingKey));
			byte [] rollingProximityIdentifier = getRollingProximityIdentifier(dailyTracingKey);
			System.err.println("rollingProximityIdentifier: " + bytesToHex(rollingProximityIdentifier));
			boolean b = validateHMAC(rollingProximityIdentifier, dailyTracingKey);
			System.err.println("valid key: " + b);
			System.err.println("===============================================================");
		}


	}
}
