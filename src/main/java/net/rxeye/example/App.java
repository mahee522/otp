package net.rxeye.example;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * Created by khoi.nguyen on 12/26/2014.
 */
public class App {
	public static void main(String[] args) throws Base32String.DecodingException, GeneralSecurityException {
		String secret = "FEN7WY35IPHDZ4SL";
		byte[] keyBytes = Base32String.decode(secret);
		Mac mac = Mac.getInstance("HMACSHA1");
		mac.init(new SecretKeySpec(keyBytes, ""));
		PasscodeGenerator pc = new PasscodeGenerator(mac);
		String s = pc.generateTimeoutCode();
		System.out.println(s);
	}
}
