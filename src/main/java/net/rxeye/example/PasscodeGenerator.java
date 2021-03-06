package net.rxeye.example;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;

/**
 * Created by khoi.nguyen on 12/26/2014.
 */
public class PasscodeGenerator {
	private static final int PASS_CODE_LENGTH = 6;

	private static final int INTERVAL = 30;

	private static final int ADJACENT_INTERVALS = 1;

	private static final int PIN_MODULO =
			(int) Math.pow(10, PASS_CODE_LENGTH);

	private final Signer signer;
	private final int codeLength;
	private final int intervalPeriod;

	interface Signer {
		byte[] sign(byte[] data) throws GeneralSecurityException;
	}

	public PasscodeGenerator(Mac mac) {
		this(mac, PASS_CODE_LENGTH, INTERVAL);
	}

	public PasscodeGenerator(final Mac mac, int passCodeLength, int interval) {
		this(new Signer() {
			public byte[] sign(byte[] data) {
				return mac.doFinal(data);
			}
		}, passCodeLength, interval);
	}

	public PasscodeGenerator(Signer signer, int passCodeLength, int interval) {
		this.signer = signer;
		this.codeLength = passCodeLength;
		this.intervalPeriod = interval;
	}

	private String padOutput(int value) {
		String result = Integer.toString(value);
		for (int i = result.length(); i < codeLength; i++) {
			result = "0" + result;
		}
		return result;
	}

	public String generateTimeoutCode() throws GeneralSecurityException {
		return generateResponseCode(clock.getCurrentInterval());
	}

	public String generateNextTimeoutCode() throws GeneralSecurityException {
		return generateResponseCode(clock.getCurrentInterval() + 1);
	}

	public String generateResponseCode(long challenge)
			throws GeneralSecurityException {
		byte[] value = ByteBuffer.allocate(8).putLong(challenge).array();
		return generateResponseCode(value);
	}

	public String generateResponseCode(byte[] challenge)
			throws GeneralSecurityException {
		byte[] hash = signer.sign(challenge);

		int offset = hash[hash.length - 1] & 0xF;
		int truncatedHash = hashToInt(hash, offset) & 0x7FFFFFFF;
		int pinValue = truncatedHash % PIN_MODULO;
		return padOutput(pinValue);
	}

	private int hashToInt(byte[] bytes, int start) {
		DataInput input = new DataInputStream(
				new ByteArrayInputStream(bytes, start, bytes.length - start));
		int val;
		try {
			val = input.readInt();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return val;
	}

	public boolean verifyResponseCode(long challenge, String response)
			throws GeneralSecurityException {
		String expectedResponse = generateResponseCode(challenge);
		return expectedResponse.equals(response);
	}

	public boolean verifyTimeoutCode(String timeoutCode)
			throws GeneralSecurityException {
		return verifyTimeoutCode(timeoutCode, ADJACENT_INTERVALS,
				ADJACENT_INTERVALS);
	}

	public boolean verifyTimeoutCode(String timeoutCode, int pastIntervals,
	                                 int futureIntervals) throws GeneralSecurityException {
		long currentInterval = clock.getCurrentInterval();
		String expectedResponse = generateResponseCode(currentInterval);
		if (expectedResponse.equals(timeoutCode)) {
			return true;
		}
		for (int i = 1; i <= pastIntervals; i++) {
			String pastResponse = generateResponseCode(currentInterval - i);
			if (pastResponse.equals(timeoutCode)) {
				return true;
			}
		}
		for (int i = 1; i <= futureIntervals; i++) {
			String futureResponse = generateResponseCode(currentInterval + i);
			if (futureResponse.equals(timeoutCode)) {
				return true;
			}
		}
		return false;
	}

	private IntervalClock clock = new IntervalClock() {
		public long getCurrentInterval() {
			long currentTimeSeconds = System.currentTimeMillis() / 1000;
			return currentTimeSeconds / getIntervalPeriod();
		}

		public int getIntervalPeriod() {
			return intervalPeriod;
		}
	};

	interface IntervalClock {
		int getIntervalPeriod();

		long getCurrentInterval();
	}
}
