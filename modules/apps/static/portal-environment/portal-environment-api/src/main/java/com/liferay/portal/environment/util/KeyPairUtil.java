/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.environment.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import java.util.Base64;

/**
 * Converts the identity key pair to and from the forms the DXP operator stores
 * it in, so that a key pair either side created stays usable by the other.
 *
 * @author Gregory Amerson
 */
public class KeyPairUtil {

	public static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
			_ALGORITHM);

		keyPairGenerator.initialize(2048);

		return keyPairGenerator.generateKeyPair();
	}

	public static String getPrivateKeyPEM(PrivateKey privateKey) {
		Base64.Encoder encoder = Base64.getMimeEncoder(64, new byte[] {'\n'});

		return StringBundler.concat(
			_PEM_HEADER, "\n", encoder.encodeToString(privateKey.getEncoded()),
			"\n", _PEM_FOOTER, "\n");
	}

	public static String getPublicKeyBase64(PublicKey publicKey) {
		Base64.Encoder encoder = Base64.getEncoder();

		return encoder.encodeToString(publicKey.getEncoded());
	}

	public static KeyPair toKeyPair(String privateKeyPEM) throws Exception {
		Base64.Decoder decoder = Base64.getMimeDecoder();

		KeyFactory keyFactory = KeyFactory.getInstance(_ALGORITHM);

		RSAPrivateCrtKey rsaPrivateCrtKey =
			(RSAPrivateCrtKey)keyFactory.generatePrivate(
				new PKCS8EncodedKeySpec(
					decoder.decode(
						StringUtil.removeSubstrings(
							privateKeyPEM, _PEM_FOOTER, _PEM_HEADER))));

		return new KeyPair(
			keyFactory.generatePublic(
				new RSAPublicKeySpec(
					rsaPrivateCrtKey.getModulus(),
					rsaPrivateCrtKey.getPublicExponent())),
			rsaPrivateCrtKey);
	}

	private static final String _ALGORITHM = "RSA";

	private static final String _PEM_FOOTER = "-----END PRIVATE KEY-----";

	private static final String _PEM_HEADER = "-----BEGIN PRIVATE KEY-----";

}