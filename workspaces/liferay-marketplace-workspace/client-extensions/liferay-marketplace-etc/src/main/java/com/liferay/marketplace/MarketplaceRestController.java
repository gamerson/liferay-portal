/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.marketplace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ryan Schuhler
 */
@RestController
public class MarketplaceRestController {

	@DeleteMapping("/marketplace/trial")
	public ResponseEntity<String> deleteMarketplaceTrial(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);
		String uri = _ssaBaseURL + "/o/provisioning/trial?provisioningId=" +
				requestJSONObject.getJSONObject(
					"commerceOrder"
				).getJSONObject(
					"customFields"
				).getString(
					"provisioningId"
				);

		String response = WebClient.create(
		).delete(
		).uri(
			uri
		).accept(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trial: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	}

	@GetMapping("/marketplace/trial")
	public ResponseEntity<String> getMarketplaceTrial(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);
		String uri = _ssaBaseURL + "/o/provisioning/trial?provisioningId=" +
				requestJSONObject.getJSONObject(
					"commerceOrder"
				).getJSONObject(
					"customFields"
				).getString(
					"provisioningId"
				);

		String response = WebClient.create(
		).get(
		).uri(
			uri
		).accept(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trial: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/marketplace/trials")
	public ResponseEntity<String> getMarketplaceTrials(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);

		String uri =
			_ssaBaseURL + "/o/provisioning/trials?userId=" +
				requestJSONObject.getJSONObject(
					"commerceOrder"
				).getString(
					"accountId"
				);

		String response = WebClient.create(
		).get(
		).uri(
			uri
		).accept(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trials: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/marketplace/trials/count")
	public ResponseEntity<String> getMarketplaceTrialsCount(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);
		String uri = _ssaBaseURL + "/o/provisioning/trials/count?userId=" +
				requestJSONObject.getJSONObject(
					"commerceOrder"
				).getString(
					"accountId"
				);

		String response = WebClient.create(
		).get(
		).uri(
			uri
		).accept(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trials/count: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/marketplace/trial")
	public ResponseEntity<String> postMarketplaceTrial(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);
		JSONObject responseJSONObject = new JSONObject();

		responseJSONObject.put("duration", _ssaDuration);
		responseJSONObject.put(
			"emailAddress",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"emailAddress"
			));
		responseJSONObject.put(
			"firstName",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"firstName"
			));
		responseJSONObject.put(
			"githubUsername",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"githubUsername"
			));
		responseJSONObject.put(
			"lastName",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"lastName"
			));
		responseJSONObject.put(
			"projectId",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"projectId"
			));
		responseJSONObject.put("sendEmailForTrial", true);
		responseJSONObject.put(
			"siteInitializer",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"siteInitializer"
			));

		String response = WebClient.create(
		).post(
		).uri(
			_ssaBaseURL + "/o/provisioning/trial"
		).accept(
			MediaType.APPLICATION_JSON
		).contentType(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).bodyValue(
			responseJSONObject
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trial: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@PostMapping("/marketplace/trial/extend")
	public ResponseEntity<String> postMarketplaceTrialExtend(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String request) {

		JSONObject requestJSONObject = new JSONObject(request);
		JSONObject responseJSONObject = new JSONObject();

		responseJSONObject.put("duration", _ssaDuration);
		responseJSONObject.put(
			"provisioningId",
			requestJSONObject.getJSONObject(
				"commerceOrder"
			).getJSONObject(
				"customFields"
			).getString(
				"provisioningId"
			));

		String response = WebClient.create(
		).post(
		).uri(
			_ssaBaseURL + "/o/provisioning/trial/extend"
		).accept(
			MediaType.APPLICATION_JSON
		).contentType(
			MediaType.APPLICATION_JSON
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).bodyValue(
			responseJSONObject
		).retrieve(
		).bodyToMono(
			String.class
		).block();

		if (_log.isInfoEnabled()) {
			_log.info("/marketplace/trial/extend: " + response);
		}

		return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	}

	private static final Log _log = LogFactory.getLog(
		MarketplaceRestController.class);

	@Value("${com.liferay.marketplace.ssa.base.url}")
	private String _ssaBaseURL;

	@Value("${com.liferay.marketplace.ssa.duration}")
	private String _ssaDuration;

}