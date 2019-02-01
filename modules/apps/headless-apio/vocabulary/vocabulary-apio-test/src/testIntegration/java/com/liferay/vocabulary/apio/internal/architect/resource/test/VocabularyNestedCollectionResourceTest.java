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

package com.liferay.vocabulary.apio.internal.architect.resource.test;

import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.resource.NestedCollectionResource;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portal.apio.test.util.PaginationRequest;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.context.ContextUserReplace;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerTestRule;
import com.liferay.registry.Registry;
import com.liferay.registry.RegistryUtil;
import com.liferay.vocabulary.apio.architect.model.Vocabulary;
import com.liferay.vocabulary.apio.internal.architect.resource.test.model.VocabularyImpl;

import java.lang.reflect.Method;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Rubén Pulido
 */
@RunWith(Arquillian.class)
public class VocabularyNestedCollectionResourceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@Test
	public void testAddAssetVocabulary() throws Exception {
		Vocabulary vocabulary = new VocabularyImpl(
			"testAddAssetVocabulary vocabulary name",
			"testAddAssetVocabulary vocabulary description");

		AssetVocabulary assetVocabulary = _addAssetVocabulary(
			_group.getGroupId(), vocabulary);

		Assert.assertNotNull(assetVocabulary.getCreateDate());

		Map<Locale, String> descriptionMap = vocabulary.getDescriptionMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			descriptionMap.get(LocaleUtil.getDefault()),
			assetVocabulary.getDescription(LocaleUtil.getDefault()));

		Assert.assertNotNull(assetVocabulary.getModifiedDate());

		Map<Locale, String> nameMap = vocabulary.getNameMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			nameMap.get(LocaleUtil.getDefault()), assetVocabulary.getName());
	}

	@Test
	public void testGetAssetVocabulary() throws Exception {
		Vocabulary vocabulary = new VocabularyImpl(
			"testGetAssetVocabulary vocabulary name",
			"testGetAssetVocabulary vocabulary description");

		AssetVocabulary assetVocabulary = _addAssetVocabulary(
			_group.getGroupId(), vocabulary);

		AssetVocabulary assetVocabularyRetrieved = _getAssetVocabulary(
			assetVocabulary.getVocabularyId());

		Map<Locale, String> descriptionMap = vocabulary.getDescriptionMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			descriptionMap.get(LocaleUtil.getDefault()),
			assetVocabularyRetrieved.getDescription(LocaleUtil.getDefault()));

		Map<Locale, String> nameMap = vocabulary.getNameMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			nameMap.get(LocaleUtil.getDefault()),
			assetVocabularyRetrieved.getName());
	}

	@Test
	public void testGetPageItems() throws Exception {
		Vocabulary vocabulary = new VocabularyImpl(
			"testGetPageItems vocabulary name",
			"testGetPageItems vocabulary description");

		_addAssetVocabulary(_group.getGroupId(), vocabulary);

		PageItems<AssetVocabulary> pageItems = _getPageItems(
			PaginationRequest.of(10, 1), _group.getGroupId());

		Assert.assertEquals(1, pageItems.getTotalCount());

		List<AssetVocabulary> assetVocabularies =
			(List<AssetVocabulary>)pageItems.getItems();

		Assert.assertEquals(
			assetVocabularies.toString(), 1, assetVocabularies.size());

		AssetVocabulary assetVocabulary = assetVocabularies.get(0);

		Map<Locale, String> descriptionMap = vocabulary.getDescriptionMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			descriptionMap.get(LocaleUtil.getDefault()),
			assetVocabulary.getDescription(LocaleUtil.getDefault()));

		Map<Locale, String> nameMap = vocabulary.getNameMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			nameMap.get(LocaleUtil.getDefault()), assetVocabulary.getName());
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndGroupPermissionAndAdminUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addGroupAdminUser(_group);

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndGroupPermissionAndGuestUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = _userLocalService.getDefaultUser(
			TestPropsValues.getCompanyId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndGroupPermissionAndNoSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser();

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndGroupPermissionAndSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser(_group.getGroupId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndNoGroupPermissionAndAdminUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addGroupAdminUser(_group);

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndNoGroupPermissionAndGuestUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = _userLocalService.getDefaultUser(
			TestPropsValues.getCompanyId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndNoGroupPermissionAndNoSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser();

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithGuestPermissionAndNoGroupPermissionAndSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(true);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser(_group.getGroupId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndGroupPermissionAndAdminUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addGroupAdminUser(_group);

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndGroupPermissionAndGuestUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = _userLocalService.getDefaultUser(
			TestPropsValues.getCompanyId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(0, pageItems.getTotalCount());
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndGroupPermissionAndNoSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser();

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(0, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndGroupPermissionAndSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(true);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser(_group.getGroupId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndNoGroupPermissionAndAdminUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addGroupAdminUser(_group);

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(1, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndNoGroupPermissionAndGuestUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = _userLocalService.getDefaultUser(
			TestPropsValues.getCompanyId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(0, pageItems.getTotalCount());
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndNoGroupPermissionAndNoSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser();

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(0, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testGetPageItemsWithNoGuestPermissionAndNoGroupPermissionAndSiteUser()
		throws Exception {

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		serviceContext.setAddGuestPermissions(false);
		serviceContext.setAddGroupPermissions(false);

		_addAssetVocabulary(_group.getGroupId(), serviceContext);

		User user = UserTestUtil.addUser(_group.getGroupId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		try (ContextUserReplace contextUserReplace = new ContextUserReplace(
				user, permissionChecker)) {

			PageItems<AssetVocabulary> pageItems = _getPageItems(
				PaginationRequest.of(10, 1), _group.getGroupId());

			Assert.assertEquals(0, pageItems.getTotalCount());
		}
		finally {
			_userLocalService.deleteUser(user);
		}
	}

	@Test
	public void testUpdateAssetVocabulary() throws Exception {
		Vocabulary vocabulary = new VocabularyImpl(
			RandomTestUtil.randomString(10), RandomTestUtil.randomString(10));

		AssetVocabulary assetVocabulary = _addAssetVocabulary(
			_group.getGroupId(), vocabulary);

		Vocabulary updatedVocabulary = new VocabularyImpl(
			"testUpdateAssetVocabulary vocabulary name",
			"testUpdateAssetVocabulary vocabulary description");

		AssetVocabulary updatedAssetVocabulary = _updateAssetVocabulary(
			assetVocabulary.getVocabularyId(), updatedVocabulary);

		Map<Locale, String> descriptionMap =
			updatedVocabulary.getDescriptionMap(LocaleUtil.getDefault());

		Assert.assertEquals(
			descriptionMap.get(LocaleUtil.getDefault()),
			updatedAssetVocabulary.getDescription(LocaleUtil.getDefault()));

		Map<Locale, String> nameMap = updatedVocabulary.getNameMap(
			LocaleUtil.getDefault());

		Assert.assertEquals(
			nameMap.get(LocaleUtil.getDefault()),
			updatedAssetVocabulary.getName());
	}

	private AssetVocabulary _addAssetVocabulary(
			long groupId, ServiceContext serviceContext)
		throws Exception {

		User adminUser = UserTestUtil.getAdminUser(
			TestPropsValues.getCompanyId());

		return AssetVocabularyLocalServiceUtil.addVocabulary(
			adminUser.getUserId(), groupId, StringUtil.randomString(10),
			serviceContext);
	}

	private AssetVocabulary _addAssetVocabulary(
			long groupId, Vocabulary vocabulary)
		throws Exception {

		NestedCollectionResource nestedCollectionResource =
			_getNestedCollectionResource();

		Class<?> clazz = nestedCollectionResource.getClass();

		Method method = clazz.getDeclaredMethod(
			"_addAssetVocabulary", long.class, Vocabulary.class);

		method.setAccessible(true);

		return (AssetVocabulary)method.invoke(
			_getNestedCollectionResource(), groupId, vocabulary);
	}

	private AssetVocabulary _getAssetVocabulary(long assetVocabularyId)
		throws Exception {

		NestedCollectionResource nestedCollectionResource =
			_getNestedCollectionResource();

		Class<?> clazz = nestedCollectionResource.getClass();

		Method method = clazz.getDeclaredMethod(
			"_getAssetVocabulary", long.class);

		method.setAccessible(true);

		return (AssetVocabulary)method.invoke(
			_getNestedCollectionResource(), assetVocabularyId);
	}

	private NestedCollectionResource _getNestedCollectionResource()
		throws Exception {

		Registry registry = RegistryUtil.getRegistry();

		Collection<NestedCollectionResource> collection = registry.getServices(
			NestedCollectionResource.class,
			"(component.name=com.liferay.vocabulary.apio.internal.architect." +
				"resource.VocabularyNestedCollectionResource)");

		Iterator<NestedCollectionResource> iterator = collection.iterator();

		return iterator.next();
	}

	private PageItems<AssetVocabulary> _getPageItems(
			Pagination pagination, long groupId)
		throws Exception {

		NestedCollectionResource nestedCollectionResource =
			_getNestedCollectionResource();

		Class<?> clazz = nestedCollectionResource.getClass();

		Method method = clazz.getDeclaredMethod(
			"_getPageItems", Pagination.class, long.class);

		method.setAccessible(true);

		return (PageItems)method.invoke(
			_getNestedCollectionResource(), pagination, groupId);
	}

	private AssetVocabulary _updateAssetVocabulary(
			long assetVocabularyId, Vocabulary vocabulary)
		throws Exception {

		NestedCollectionResource nestedCollectionResource =
			_getNestedCollectionResource();

		Class<?> clazz = nestedCollectionResource.getClass();

		Method method = clazz.getDeclaredMethod(
			"_updateAssetVocabulary", long.class, Vocabulary.class);

		method.setAccessible(true);

		return (AssetVocabulary)method.invoke(
			_getNestedCollectionResource(), assetVocabularyId, vocabulary);
	}

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private UserLocalService _userLocalService;

}