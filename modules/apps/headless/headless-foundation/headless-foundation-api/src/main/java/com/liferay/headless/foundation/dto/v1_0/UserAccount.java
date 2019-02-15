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

package com.liferay.headless.foundation.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "UserAccount")
public class UserAccount {

	public String getAdditionalName() {
		return _additionalName;
	}

	public String getAlternateName() {
		return _alternateName;
	}

	public Date getBirthDate() {
		return _birthDate;
	}

	public ContactInformation getContactInformation() {
		return _contactInformation;
	}

	public String getDashboardURL() {
		return _dashboardURL;
	}

	public String getEmail() {
		return _email;
	}

	public String getFamilyName() {
		return _familyName;
	}

	public String getGivenName() {
		return _givenName;
	}

	public String getHonorificPrefix() {
		return _honorificPrefix;
	}

	public String getHonorificSuffix() {
		return _honorificSuffix;
	}

	public Long getId() {
		return _id;
	}

	public String getImage() {
		return _image;
	}

	public String getJobTitle() {
		return _jobTitle;
	}

	public Organization[] getMyOrganizations() {
		return _myOrganizations;
	}

	public Long[] getMyOrganizationsIds() {
		return _myOrganizationsIds;
	}

	public String getName() {
		return _name;
	}

	public String getProfileURL() {
		return _profileURL;
	}

	public Role[] getRoles() {
		return _roles;
	}

	public Long[] getRolesIds() {
		return _rolesIds;
	}

	public String[] getTasksAssignedToMe() {
		return _tasksAssignedToMe;
	}

	public String[] getTasksAssignedToMyRoles() {
		return _tasksAssignedToMyRoles;
	}

	public void setAdditionalName(String additionalName) {
		_additionalName = additionalName;
	}

	public void setAdditionalName(
		UnsafeSupplier<String, Throwable> additionalNameUnsafeSupplier) {

		try {
			_additionalName = additionalNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAlternateName(String alternateName) {
		_alternateName = alternateName;
	}

	public void setAlternateName(
		UnsafeSupplier<String, Throwable> alternateNameUnsafeSupplier) {

		try {
			_alternateName = alternateNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setBirthDate(Date birthDate) {
		_birthDate = birthDate;
	}

	public void setBirthDate(
		UnsafeSupplier<Date, Throwable> birthDateUnsafeSupplier) {

		try {
			_birthDate = birthDateUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setContactInformation(ContactInformation contactInformation) {
		_contactInformation = contactInformation;
	}

	public void setContactInformation(
		UnsafeSupplier<ContactInformation, Throwable>
			contactInformationUnsafeSupplier) {

		try {
			_contactInformation = contactInformationUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDashboardURL(String dashboardURL) {
		_dashboardURL = dashboardURL;
	}

	public void setDashboardURL(
		UnsafeSupplier<String, Throwable> dashboardURLUnsafeSupplier) {

		try {
			_dashboardURL = dashboardURLUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setEmail(String email) {
		_email = email;
	}

	public void setEmail(
		UnsafeSupplier<String, Throwable> emailUnsafeSupplier) {

		try {
			_email = emailUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setFamilyName(String familyName) {
		_familyName = familyName;
	}

	public void setFamilyName(
		UnsafeSupplier<String, Throwable> familyNameUnsafeSupplier) {

		try {
			_familyName = familyNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setGivenName(String givenName) {
		_givenName = givenName;
	}

	public void setGivenName(
		UnsafeSupplier<String, Throwable> givenNameUnsafeSupplier) {

		try {
			_givenName = givenNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setHonorificPrefix(String honorificPrefix) {
		_honorificPrefix = honorificPrefix;
	}

	public void setHonorificPrefix(
		UnsafeSupplier<String, Throwable> honorificPrefixUnsafeSupplier) {

		try {
			_honorificPrefix = honorificPrefixUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setHonorificSuffix(String honorificSuffix) {
		_honorificSuffix = honorificSuffix;
	}

	public void setHonorificSuffix(
		UnsafeSupplier<String, Throwable> honorificSuffixUnsafeSupplier) {

		try {
			_honorificSuffix = honorificSuffixUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setId(Long id) {
		_id = id;
	}

	public void setId(UnsafeSupplier<Long, Throwable> idUnsafeSupplier) {
		try {
			_id = idUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setImage(String image) {
		_image = image;
	}

	public void setImage(
		UnsafeSupplier<String, Throwable> imageUnsafeSupplier) {

		try {
			_image = imageUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setJobTitle(String jobTitle) {
		_jobTitle = jobTitle;
	}

	public void setJobTitle(
		UnsafeSupplier<String, Throwable> jobTitleUnsafeSupplier) {

		try {
			_jobTitle = jobTitleUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setMyOrganizations(Organization[] myOrganizations) {
		_myOrganizations = myOrganizations;
	}

	public void setMyOrganizations(
		UnsafeSupplier<Organization[], Throwable>
			myOrganizationsUnsafeSupplier) {

		try {
			_myOrganizations = myOrganizationsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setMyOrganizationsIds(Long[] myOrganizationsIds) {
		_myOrganizationsIds = myOrganizationsIds;
	}

	public void setMyOrganizationsIds(
		UnsafeSupplier<Long[], Throwable> myOrganizationsIdsUnsafeSupplier) {

		try {
			_myOrganizationsIds = myOrganizationsIdsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setName(String name) {
		_name = name;
	}

	public void setName(UnsafeSupplier<String, Throwable> nameUnsafeSupplier) {
		try {
			_name = nameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setProfileURL(String profileURL) {
		_profileURL = profileURL;
	}

	public void setProfileURL(
		UnsafeSupplier<String, Throwable> profileURLUnsafeSupplier) {

		try {
			_profileURL = profileURLUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRoles(Role[] roles) {
		_roles = roles;
	}

	public void setRoles(
		UnsafeSupplier<Role[], Throwable> rolesUnsafeSupplier) {

		try {
			_roles = rolesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setRolesIds(Long[] rolesIds) {
		_rolesIds = rolesIds;
	}

	public void setRolesIds(
		UnsafeSupplier<Long[], Throwable> rolesIdsUnsafeSupplier) {

		try {
			_rolesIds = rolesIdsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setTasksAssignedToMe(String[] tasksAssignedToMe) {
		_tasksAssignedToMe = tasksAssignedToMe;
	}

	public void setTasksAssignedToMe(
		UnsafeSupplier<String[], Throwable> tasksAssignedToMeUnsafeSupplier) {

		try {
			_tasksAssignedToMe = tasksAssignedToMeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setTasksAssignedToMyRoles(String[] tasksAssignedToMyRoles) {
		_tasksAssignedToMyRoles = tasksAssignedToMyRoles;
	}

	public void setTasksAssignedToMyRoles(
		UnsafeSupplier<String[], Throwable>
			tasksAssignedToMyRolesUnsafeSupplier) {

		try {
			_tasksAssignedToMyRoles =
				tasksAssignedToMyRolesUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private String _additionalName;
	private String _alternateName;
	private Date _birthDate;
	private ContactInformation _contactInformation;
	private String _dashboardURL;
	private String _email;
	private String _familyName;
	private String _givenName;
	private String _honorificPrefix;
	private String _honorificSuffix;
	private Long _id;
	private String _image;
	private String _jobTitle;
	private Organization[] _myOrganizations;
	private Long[] _myOrganizationsIds;
	private String _name;
	private String _profileURL;
	private Role[] _roles;
	private Long[] _rolesIds;
	private String[] _tasksAssignedToMe;
	private String[] _tasksAssignedToMyRoles;

}