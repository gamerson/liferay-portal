/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Supplies crypto.subtle.digest when the browser withholds it.
 *
 * The Web Crypto specification marks Crypto.subtle [SecureContext], so the
 * property is absent entirely unless the page is served over HTTPS or from a
 * loopback host. The OAuth2 client needs it for one thing: the SHA-256 of the
 * PKCE code verifier. Without it the client throws before it builds a request.
 *
 * This is a development convenience for a portal served over plain HTTP on a
 * named host. Nothing here belongs in production: reach a secure context and
 * the browser's own implementation takes over, because the shim only installs
 * itself when the real one is missing.
 */

const BLOCK_SIZE = 64;

const K = new Uint32Array([
	0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1,
	0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
	0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786,
	0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
	0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
	0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
	0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
	0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
	0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
	0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
	0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
]);

function rightRotate(value, amount) {
	return ((value >>> amount) | (value << (32 - amount))) >>> 0;
}

function sha256(bytes) {
	const hash = new Uint32Array([
		0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c,
		0x1f83d9ab, 0x5be0cd19,
	]);

	// Append the 0x80 marker, pad to 56 bytes of the final block, then the
	// message length in bits as a big endian 64 bit value.

	const length = bytes.length;
	const paddedLength =
		((length + 9 + BLOCK_SIZE - 1) / BLOCK_SIZE) | 0;
	const padded = new Uint8Array(paddedLength * BLOCK_SIZE);

	padded.set(bytes);

	padded[length] = 0x80;

	const bitLength = length * 8;

	const view = new DataView(padded.buffer);

	view.setUint32(padded.length - 8, Math.floor(bitLength / 0x100000000));
	view.setUint32(padded.length - 4, bitLength >>> 0);

	const w = new Uint32Array(64);

	for (let offset = 0; offset < padded.length; offset += BLOCK_SIZE) {
		for (let i = 0; i < 16; i++) {
			w[i] = view.getUint32(offset + i * 4);
		}

		for (let i = 16; i < 64; i++) {
			const s0 =
				rightRotate(w[i - 15], 7) ^
				rightRotate(w[i - 15], 18) ^
				(w[i - 15] >>> 3);
			const s1 =
				rightRotate(w[i - 2], 17) ^
				rightRotate(w[i - 2], 19) ^
				(w[i - 2] >>> 10);

			w[i] = (w[i - 16] + s0 + w[i - 7] + s1) >>> 0;
		}

		let [a, b, c, d, e, f, g, h] = hash;

		for (let i = 0; i < 64; i++) {
			const s1 = rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25);
			const ch = (e & f) ^ (~e & g);
			const temp1 = (h + s1 + ch + K[i] + w[i]) >>> 0;
			const s0 = rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22);
			const maj = (a & b) ^ (a & c) ^ (b & c);
			const temp2 = (s0 + maj) >>> 0;

			h = g;
			g = f;
			f = e;
			e = (d + temp1) >>> 0;
			d = c;
			c = b;
			b = a;
			a = (temp1 + temp2) >>> 0;
		}

		hash[0] = (hash[0] + a) >>> 0;
		hash[1] = (hash[1] + b) >>> 0;
		hash[2] = (hash[2] + c) >>> 0;
		hash[3] = (hash[3] + d) >>> 0;
		hash[4] = (hash[4] + e) >>> 0;
		hash[5] = (hash[5] + f) >>> 0;
		hash[6] = (hash[6] + g) >>> 0;
		hash[7] = (hash[7] + h) >>> 0;
	}

	const digest = new Uint8Array(32);
	const digestView = new DataView(digest.buffer);

	for (let i = 0; i < 8; i++) {
		digestView.setUint32(i * 4, hash[i]);
	}

	return digest;
}

function toBytes(data) {
	if (data instanceof Uint8Array) {
		return data;
	}

	if (ArrayBuffer.isView(data)) {
		return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
	}

	return new Uint8Array(data);
}

export function installWebCryptoShim() {
	if (typeof crypto === 'undefined' || crypto.subtle) {
		return false;
	}

	Object.defineProperty(crypto, 'subtle', {
		configurable: true,
		value: {
			digest(algorithm, data) {
				const name =
					typeof algorithm === 'string' ? algorithm : algorithm && algorithm.name;

				if (String(name).toUpperCase() !== 'SHA-256') {
					return Promise.reject(
						new Error(
							`This page is not a secure context, so only SHA-256 is available. Asked for ${name}.`
						)
					);
				}

				return Promise.resolve(sha256(toBytes(data)).buffer);
			},
		},
		writable: true,
	});

	// eslint-disable-next-line no-console
	console.warn(
		'crypto.subtle is unavailable because this page is not a secure context. ' +
			'A SHA-256 shim has been installed so the OAuth2 PKCE exchange can run. ' +
			'Serve the portal over HTTPS or from localhost and the browser implementation is used instead.'
	);

	return true;
}
