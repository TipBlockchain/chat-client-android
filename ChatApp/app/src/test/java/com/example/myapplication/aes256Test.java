package com.example.myapplication;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;


public final class aes256Test {

	
	//Example Use
	/*
	public static void main(String[] args) throws Exception {
		//KEY EXCHANGE
		
		//Bob wants to send the first message so he does as follows
		
		KeyPair RSAKP = getRSAkeyPair();						//Generate RSA Key Pair
		byte[] BobPub = RSAKP.getPublic().getEncoded();			//Get encoded RSA public key
		//Send RSA Key to Alice through socket
		
		
		//Alice has received the RSA public key and does as follows
		
		SecretKey SharedSecret = createSKey();					//Generate the AES secret key
		byte[] EKey = encryptRSA1024(BobPub, SharedSecret);		//Encrypts the AES key with Bob's public RSA key
		//Send encrypted AES key through the socket
		
		//Bob receives the encrypted AES key and does as follows
		
		byte[] DKey = decryptRSA1024(RSAKP.getPrivate(),EKey);	//Decrypt the AES key with his RSA private key
		SecretKey Secret = deriveSKey(DKey);					//Derive the AES secret key

        //MESSAGE ENCRYPTION
		
		//Bob wants to send a message to Alice and does as follows
		
		String plainText = "Hello world and Good bye!"; 		//Write message
		
        //Always generate IV before encryption
		//Generate message IV
        byte[] iv = getRIV(); 
        iv = getRIV();
        
        //Encrypt with plain text with secret key and iv
		byte[] cipherText = encryptAES256(plainText, Secret, iv);
		//Send cipherText to Alice
		//Send IV to Alice
		//REMEMBER TO SEND IV TO OTHER PARTY (does not need to be encrypted)
		
		//Alice receives cipher text and does as follows
		
		//Decrypt cipher text with secret key nad received IV
		String dcipherText = decryptAES256(cipherText, Secret, iv);
		System.out.println(dcipherText);
		//Prints out Decrypted Text to read
		
	}
*/
	
	
	//Generates random initialization vector
	//Send these bytes after a message so the receiver can decrypt
	public static byte[] getRIV() throws NoSuchAlgorithmException {
		byte[] iv = new byte[ 16 ];
		SecureRandom random = SecureRandom.getInstance( "SHA1PRNG" );
		random.nextBytes( iv );
		return iv;
	}
	
	//Takes Shared Secret from DH exchange, Plain Text, and generated initialization vector then returns encrypted text in bytes
	public static byte[] encryptAES256(String plnTxt, SecretKey k, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		
		//String Plain Text is turned into bytes for the Cipher
		byte[] pt = plnTxt.getBytes();
		
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		
		
		
			//Create AES cipher with Cipher Block Chaining and PKCS5 Padding
			Cipher eCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			
			//Sets Cipher to encryption mode with key, k, and initialization vector, ivspec
			eCipher.init(Cipher.ENCRYPT_MODE,k,ivspec);
			
			//Returns encrypted text in form of bytes
			return eCipher.doFinal(pt);
		

	}
	
	
	//Takes Shared Secret, encrypted text, and iv received from sender then returns decrypted Plain Text string
	public static String decryptAES256(byte[] eTxt, SecretKey k, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		

			//Create AES cipher with Cipher Block Chaining and PKCS5 Padding
			Cipher dCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			
			//Sets Cipher to decryption mode with key, k, and initialization vector, ivspec
			dCipher.init(Cipher.DECRYPT_MODE, k, ivspec);
			
			//Returns decrypted text as string
			String dTxt = new String(dCipher.doFinal(eTxt));
			return dTxt;
		
	}

	//Creates a AES-256 Secret Key
	public static SecretKey createSKey() throws NoSuchAlgorithmException, InvalidKeyException {
		//Creates two DH key pairs
		KeyPairGenerator KPgen = KeyPairGenerator.getInstance("DH");
        KPgen.initialize(2048);
        KeyPair kp1 = KPgen.generateKeyPair();
        KeyPair kp2 = KPgen.generateKeyPair();
        //Creates Key Agreement instance then derives secret key from key pairs
        KeyAgreement KeyAgree = KeyAgreement.getInstance("DH");
        KeyAgree.init(kp1.getPrivate());
        KeyAgree.doPhase(kp2.getPublic(), true);
        byte[] sharedSecret = KeyAgree.generateSecret();
        //Uses DH secret key as material to make a AES key
		SecretKey SK = new SecretKeySpec(sharedSecret, 0, 32, "AES");
		return SK;
	}
	
	//Derives AES secret key from encoded key
	public static SecretKey deriveSKey(byte[] SKencode) {
		SecretKey SK = new SecretKeySpec(SKencode,0, SKencode.length, "AES");
		return SK;
	}

	//Generates a RSA-1024 key pair
	public static KeyPair getRSAkeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator RSAKPG = KeyPairGenerator.getInstance("RSA");
		RSAKPG.initialize(1024);
        KeyPair RSAKP = RSAKPG.generateKeyPair();
        return RSAKP;
	}
	
	//Encrypts AES secret key with RSA public key
	public static byte[] encryptRSA1024(byte[] pkey, SecretKey k) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		//Takes encoded RSA key and creates the public key
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pkey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey RSAPubKey = keyFactory.generatePublic(keySpec);
        
        //Creates RSA Cipher in encrypt mode to encrypt secret key
		Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		eCipher.init(Cipher.ENCRYPT_MODE, RSAPubKey);
		return eCipher.doFinal(k.getEncoded());
	}
	
	//Decrypts AES secret key with RSA private key
	public static byte[] decryptRSA1024(PrivateKey PK, byte[] SK) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Cipher dCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		dCipher.init(Cipher.DECRYPT_MODE, PK);
		return dCipher.doFinal(SK);
	}
	
	
}



