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

package com.liferay.structured.content.apio.internal.util.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeResponse;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerTracker;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMStructureConstants;
import com.liferay.dynamic.data.mapping.storage.Field;
import com.liferay.dynamic.data.mapping.storage.Fields;
import com.liferay.dynamic.data.mapping.storage.StorageType;
import com.liferay.dynamic.data.mapping.test.util.DDMStructureTestHelper;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalFolderConstants;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.journal.util.JournalConverter;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.apio.test.util.FileTestUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerTestRule;
import com.liferay.registry.Filter;
import com.liferay.registry.Registry;
import com.liferay.registry.RegistryUtil;
import com.liferay.registry.ServiceTracker;
import com.liferay.structured.content.apio.architect.model.StructuredContentLocation;
import com.liferay.structured.content.apio.architect.model.StructuredContentValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Cristina González
 */
@RunWith(Arquillian.class)
public class JournalArticleContentHelperTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerTestRule.INSTANCE,
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		Filter filter = registry.getFilter(
			"(objectClass=com.liferay.structured.content.apio.internal.util." +
				"JournalArticleContentHelper)");

		_serviceTracker = registry.trackServices(filter);

		_serviceTracker.open();

		_group = GroupTestUtil.addGroup();

		_ddmStructureTestHelper = new DDMStructureTestHelper(
			PortalUtil.getClassNameId(JournalArticle.class), _group);
	}

	@After
	public void tearDown() {
		_serviceTracker.close();
	}

	@Test
	public void testCreateJournalArticleContentWithInvalidMediaObjectStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-media-object-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		StructuredContentValue structuredContentValue =
			getDocumentStructuredContentValue(
				"MyDocumentsAndMedia", 4L, RandomTestUtil.randomString());

		AbstractThrowableAssert exception = Assertions.assertThatThrownBy(
			() -> createJournalArticleContent(
				ddmStructure,
				Collections.singletonMap(
					LocaleUtil.US,
					Collections.singletonList(structuredContentValue)))
		).isInstanceOf(
			BadRequestException.class
		);

		exception.hasMessage(
			String.format(
				"Invalid Structured Content Value No FileEntry exists with " +
					"the key {fileEntryId=4}"));
	}

	@Sync
	@Test
	public void testCreateJournalArticleContentWithLocalizedTextAndLocalizedMediaObjectStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-text-field-media-object-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		FileEntry fileEntry = _dlAppLocalService.addFileEntry(
			TestPropsValues.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			RandomTestUtil.randomString() + ".jpg", ContentTypes.IMAGE_JPEG,
			FileUtil.getBytes(
				JournalArticleContentHelperTest.class, "image.jpg"),
			ServiceContextTestUtil.getServiceContext(
				_group.getGroupId(), TestPropsValues.getUserId()));

		Map<Locale, List<? extends StructuredContentValue>>
			structuredContentValueMap = Stream.of(
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.US,
					Arrays.asList(
						getTextStructuredContentValue("MyText", "My text"),
						getDocumentStructuredContentValue(
							"MyDocumentsAndMedia", fileEntry.getFileEntryId(),
							RandomTestUtil.randomString()))),
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.SPAIN,
					Collections.singletonList(
						getTextStructuredContentValue("MyText", "Mi texto")))
			).collect(
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
			);

		String content = createJournalArticleContent(
			ddmStructure, structuredContentValueMap);

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(fields.getDefaultLocale(), LocaleUtil.US);

		Set<Locale> availableLocales = fields.getAvailableLocales();

		Assert.assertTrue(availableLocales.contains(LocaleUtil.US));
		Assert.assertTrue(availableLocales.contains(LocaleUtil.SPAIN));

		Field textField = fields.get("MyText");

		Assert.assertNotNull(textField);
		Assert.assertEquals("My text", textField.getValue());
		Assert.assertEquals("My text", textField.getValue(LocaleUtil.US));
		Assert.assertEquals("Mi texto", textField.getValue(LocaleUtil.SPAIN));

		Field mediaObjectField = fields.get("MyDocumentsAndMedia");

		JSONObject jsonObjectUS = JSONFactoryUtil.createJSONObject(
			(String)mediaObjectField.getValue(LocaleUtil.US));

		Assert.assertEquals(
			fileEntry.getFileEntryId(), jsonObjectUS.getLong("fileEntryId"));

		Assert.assertEquals(
			StringPool.BLANK, mediaObjectField.getValue(LocaleUtil.SPAIN));
	}

	@Test
	public void testCreateJournalArticleContentWithLocalizedTextAndUnlocalizedGeoLocalizationStructuredContentValueInNoDefaultLocale()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-text-field-geolocalization-field-" +
						"structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		Map<Locale, List<? extends StructuredContentValue>>
			structuredContentValueMap = Stream.of(
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.US,
					Collections.singletonList(
						getTextStructuredContentValue("MyText", "My text"))),
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.SPAIN,
					Arrays.asList(
						getTextStructuredContentValue("MyText", "Mi texto"),
						getStructuredContentLocationStructuredContentValue(
							"MyGeolocation", 12.0, -13.7)))
			).collect(
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
			);

		String content = createJournalArticleContent(
			ddmStructure, structuredContentValueMap);

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(fields.getDefaultLocale(), LocaleUtil.US);

		Set<Locale> availableLocales = fields.getAvailableLocales();

		Assert.assertTrue(availableLocales.contains(LocaleUtil.US));
		Assert.assertTrue(availableLocales.contains(LocaleUtil.SPAIN));

		Field textField = fields.get("MyText");

		Assert.assertNotNull(textField);
		Assert.assertEquals("My text", textField.getValue());
		Assert.assertEquals("My text", textField.getValue(LocaleUtil.US));
		Assert.assertEquals("Mi texto", textField.getValue(LocaleUtil.SPAIN));

		Field geolocationField = fields.get("MyGeolocation");

		Assert.assertNotNull(geolocationField);
		Assert.assertEquals(StringPool.BLANK, geolocationField.getValue());
		Assert.assertEquals(
			StringPool.BLANK, geolocationField.getValue(LocaleUtil.SPAIN));
		Assert.assertEquals(
			StringPool.BLANK, geolocationField.getValue(LocaleUtil.US));
	}

	@Test
	public void testCreateJournalArticleContentWithLocalizedTextAndUnlozalizedGeoLocalizationStructuredContentValueInDefaultLocale()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-text-field-geolocalization-field-" +
						"structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		Map<Locale, List<? extends StructuredContentValue>>
			structuredContentValueMap = Stream.of(
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.US,
					Arrays.asList(
						getTextStructuredContentValue("MyText", "My text"),
						getStructuredContentLocationStructuredContentValue(
							"MyGeolocation", 12.0, -13.7))),
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.SPAIN,
					Arrays.asList(
						getTextStructuredContentValue("MyText", "Mi texto")))
			).collect(
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
			);

		String content = createJournalArticleContent(
			ddmStructure, structuredContentValueMap);

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(fields.getDefaultLocale(), LocaleUtil.US);

		Set<Locale> availableLocales = fields.getAvailableLocales();

		Assert.assertTrue(availableLocales.contains(LocaleUtil.US));
		Assert.assertTrue(availableLocales.contains(LocaleUtil.SPAIN));

		Field textField = fields.get("MyText");

		Assert.assertNotNull(textField);
		Assert.assertEquals("My text", textField.getValue());
		Assert.assertEquals("My text", textField.getValue(LocaleUtil.US));
		Assert.assertEquals("Mi texto", textField.getValue(LocaleUtil.SPAIN));

		Field geolocationField = fields.get("MyGeolocation");

		JSONObject jsonObjectDefault = JSONFactoryUtil.createJSONObject(
			(String)geolocationField.getValue());

		Assert.assertEquals(12.0, jsonObjectDefault.getDouble("latitude"), 0);

		JSONObject jsonObjectES = JSONFactoryUtil.createJSONObject(
			(String)geolocationField.getValue(LocaleUtil.SPAIN));

		Assert.assertEquals(12.0, jsonObjectES.getDouble("latitude"), 0);

		JSONObject jsonObjectUS = JSONFactoryUtil.createJSONObject(
			(String)geolocationField.getValue(LocaleUtil.US));

		Assert.assertEquals(12.0, jsonObjectUS.getDouble("latitude"), 0);
	}

	@Test
	public void testCreateJournalArticleContentWithLocalizedTextStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-text-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		Map<Locale, List<? extends StructuredContentValue>>
			structuredContentValueMap = Stream.of(
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.US,
					Collections.singletonList(
						getTextStructuredContentValue("MyText", "My text"))),
				new AbstractMap.SimpleEntry<>(
					LocaleUtil.SPAIN,
					Collections.singletonList(
						getTextStructuredContentValue("MyText", "Mi texto")))
			).collect(
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
			);

		String content = createJournalArticleContent(
			ddmStructure, structuredContentValueMap);

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(fields.getDefaultLocale(), LocaleUtil.US);

		Set<Locale> availableLocales = fields.getAvailableLocales();

		Assert.assertTrue(availableLocales.contains(LocaleUtil.US));
		Assert.assertTrue(availableLocales.contains(LocaleUtil.SPAIN));

		Field field = fields.get("MyText");

		Assert.assertNotNull(field);
		Assert.assertEquals("My text", field.getValue());
		Assert.assertEquals("My text", field.getValue(LocaleUtil.US));
		Assert.assertEquals("Mi texto", field.getValue(LocaleUtil.SPAIN));
	}

	@Sync
	@Test
	public void testCreateJournalArticleContentWithMediaObjectStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-media-object-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		FileEntry fileEntry = _dlAppLocalService.addFileEntry(
			TestPropsValues.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			RandomTestUtil.randomString() + ".jpg", ContentTypes.IMAGE_JPEG,
			FileUtil.getBytes(
				JournalArticleContentHelperTest.class, "image.jpg"),
			ServiceContextTestUtil.getServiceContext(
				_group.getGroupId(), TestPropsValues.getUserId()));

		StructuredContentValue structuredContentValue =
			getDocumentStructuredContentValue(
				"MyDocumentsAndMedia", fileEntry.getFileEntryId(),
				RandomTestUtil.randomString());

		String content = createJournalArticleContent(
			ddmStructure,
			Collections.singletonMap(
				LocaleUtil.US,
				Collections.singletonList(structuredContentValue)));

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(LocaleUtil.US, fields.getDefaultLocale());
		Assert.assertEquals(
			fields.getAvailableLocales(), Collections.singleton(LocaleUtil.US));

		Field field = fields.get(structuredContentValue.getName());

		Assert.assertNotNull(field);
		Assert.assertEquals(structuredContentValue.getName(), field.getName());

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			(String)field.getValue());

		Assert.assertEquals(
			fileEntry.getFileEntryId(), jsonObject.getLong("fileEntryId"));
		Assert.assertEquals(
			fileEntry.getGroupId(), jsonObject.getLong("groupId"));
		Assert.assertEquals(fileEntry.getFileName(), jsonObject.get("name"));
		Assert.assertEquals(
			fileEntry.getPrimaryKey(), jsonObject.getLong("resourcePrimKey"));
		Assert.assertEquals(fileEntry.getFileName(), jsonObject.get("title"));
		Assert.assertEquals("document", jsonObject.get("type"));
		Assert.assertEquals(fileEntry.getUuid(), jsonObject.get("uuid"));
		Assert.assertEquals(
			structuredContentValue.getValue(), jsonObject.get("alt"));
	}

	@Test
	public void testCreateJournalArticleContentWithStructuredContentLocationStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-geolocalization-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		StructuredContentValue structuredContentValue =
			getStructuredContentLocationStructuredContentValue(
				"MyGeolocation", 12.0, -13.7);

		String content = createJournalArticleContent(
			ddmStructure,
			Collections.singletonMap(
				LocaleUtil.US,
				Collections.singletonList(structuredContentValue)));

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(LocaleUtil.US, fields.getDefaultLocale());
		Assert.assertEquals(
			fields.getAvailableLocales(), Collections.singleton(LocaleUtil.US));

		Field field = fields.get(structuredContentValue.getName());

		Assert.assertNotNull(field);
		Assert.assertEquals(structuredContentValue.getName(), field.getName());

		StructuredContentLocation structuredContentLocation =
			structuredContentValue.getStructuredContentLocation();

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			(String)field.getValue());

		Assert.assertEquals(
			structuredContentLocation.getLatitude(),
			jsonObject.getDouble("latitude"), 0);

		Assert.assertEquals(
			structuredContentLocation.getLongitude(),
			jsonObject.getDouble("longitude"), 0);
	}

	@Test
	public void testCreateJournalArticleContentWithTextStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-text-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		StructuredContentValue structuredContentValue =
			getTextStructuredContentValue("MyText", "My text");

		String content = createJournalArticleContent(
			ddmStructure,
			Collections.singletonMap(
				LocaleUtil.US,
				Collections.singletonList(structuredContentValue)));

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(fields.getDefaultLocale(), LocaleUtil.US);

		Assert.assertEquals(
			fields.getAvailableLocales(), Collections.singleton(LocaleUtil.US));

		Field field = fields.get(structuredContentValue.getName());

		Assert.assertNotNull(field);
		Assert.assertEquals(structuredContentValue.getName(), field.getName());
		Assert.assertEquals(
			structuredContentValue.getValue(), field.getValue());
		Assert.assertEquals(
			structuredContentValue.getValue(), field.getValue(LocaleUtil.US));
	}

	@Test
	public void testCreateJournalArticleContentWitInvalidStructuredContentStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-structured-content-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		StructuredContentValue structuredContentValue =
			getStructureStructuredContentValue("MyJournalArticle", 4L);

		AbstractThrowableAssert exception = Assertions.assertThatThrownBy(
			() -> createJournalArticleContent(
				ddmStructure,
				Collections.singletonMap(
					LocaleUtil.US,
					Collections.singletonList(structuredContentValue)))
		).isInstanceOf(
			BadRequestException.class
		);

		exception.hasMessage(
			String.format(
				"Invalid Structured Content Value No JournalArticle exists " +
					"with the primary key 4"));
	}

	@Test
	public void testCreateJournalArticleContentWitStructuredContentStructuredContentValue()
		throws Throwable {

		DDMStructure ddmStructure = _ddmStructureTestHelper.addStructure(
			PortalUtil.getClassNameId(JournalArticle.class),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			deserialize(
				_ddmFormDeserializerTracker,
				FileTestUtil.readFile(
					"test-journal-structured-content-field-structure.json",
					JournalArticleContentHelperTest.class)),
			StorageType.JSON.getValue(), DDMStructureConstants.TYPE_DEFAULT);

		JournalArticle journalArticle = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		StructuredContentValue structuredContentValue =
			getStructureStructuredContentValue(
				"MyJournalArticle", journalArticle.getPrimaryKey());

		String content = createJournalArticleContent(
			ddmStructure,
			Collections.singletonMap(
				LocaleUtil.US,
				Collections.singletonList(structuredContentValue)));

		Fields fields = _journalConverter.getDDMFields(ddmStructure, content);

		Assert.assertEquals(
			ddmStructure.getStructureId(), fields.getDDMStructureId());

		Assert.assertEquals(LocaleUtil.US, fields.getDefaultLocale());
		Assert.assertEquals(
			Collections.singleton(LocaleUtil.US), fields.getAvailableLocales());

		Field field = fields.get(structuredContentValue.getName());

		Assert.assertNotNull(field);
		Assert.assertEquals(structuredContentValue.getName(), field.getName());

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			(String)field.getValue());

		Assert.assertEquals(
			JournalArticle.class.getName(), jsonObject.getString("className"));
		Assert.assertEquals(
			journalArticle.getResourcePrimKey(), jsonObject.getLong("classPK"));
		Assert.assertEquals(
			journalArticle.getTitle(), jsonObject.getString("title"));
	}

	protected String createJournalArticleContent(
			DDMStructure ddmStructure,
			Map<Locale, List<? extends StructuredContentValue>>
				structuredContentValuesMap)
		throws Throwable {

		Object service = _serviceTracker.getService();

		Class<?> clazz = service.getClass();

		Method method = clazz.getDeclaredMethod(
			"createJournalArticleContent", DDMStructure.class, Map.class);

		method.setAccessible(true);

		try {
			return (String)method.invoke(
				service, ddmStructure, structuredContentValuesMap);
		}
		catch (InvocationTargetException ite) {
			throw ite.getTargetException();
		}
	}

	protected DDMForm deserialize(
		DDMFormDeserializerTracker ddmFormDeserializerTracker, String content) {

		DDMFormDeserializer ddmFormDeserializer =
			ddmFormDeserializerTracker.getDDMFormDeserializer("json");

		DDMFormDeserializerDeserializeRequest.Builder builder =
			DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

		DDMFormDeserializerDeserializeResponse
			ddmFormDeserializerDeserializeResponse =
				ddmFormDeserializer.deserialize(builder.build());

		return ddmFormDeserializerDeserializeResponse.getDDMForm();
	}

	protected StructuredContentValue getDocumentStructuredContentValue(
		String name, Long document, String value) {

		return new StructuredContentValue() {

			@Override
			public Long getDocument() {
				return document;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Long getStructuredContentId() {
				return null;
			}

			@Override
			public StructuredContentLocation getStructuredContentLocation() {
				return null;
			}

			@Override
			public String getValue() {
				return value;
			}

		};
	}

	protected StructuredContentValue
		getStructuredContentLocationStructuredContentValue(
			String name, Double latitude, Double longitude) {

		return new StructuredContentValue() {

			@Override
			public Long getDocument() {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Long getStructuredContentId() {
				return null;
			}

			@Override
			public StructuredContentLocation getStructuredContentLocation() {
				return new StructuredContentLocation() {

					@Override
					public Double getLatitude() {
						return latitude;
					}

					@Override
					public Double getLongitude() {
						return longitude;
					}

				};
			}

			@Override
			public String getValue() {
				return null;
			}

		};
	}

	protected StructuredContentValue getStructureStructuredContentValue(
		String name, Long structuredContentId) {

		return new StructuredContentValue() {

			@Override
			public Long getDocument() {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Long getStructuredContentId() {
				return structuredContentId;
			}

			@Override
			public StructuredContentLocation getStructuredContentLocation() {
				return null;
			}

			@Override
			public String getValue() {
				return null;
			}

		};
	}

	protected StructuredContentValue getTextStructuredContentValue(
		String name, String value) {

		return new StructuredContentValue() {

			@Override
			public Long getDocument() {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Long getStructuredContentId() {
				return null;
			}

			@Override
			public StructuredContentLocation getStructuredContentLocation() {
				return null;
			}

			@Override
			public String getValue() {
				return value;
			}

		};
	}

	private static ServiceTracker<Object, Object> _serviceTracker;

	@Inject
	private DDMFormDeserializerTracker _ddmFormDeserializerTracker;

	private DDMStructureTestHelper _ddmStructureTestHelper;

	@Inject
	private DLAppLocalService _dlAppLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private JournalConverter _journalConverter;

}