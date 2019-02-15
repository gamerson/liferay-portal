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

package com.liferay.headless.workflow.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "WorkflowTask")
public class WorkflowTask {

	public Boolean getCompleted() {
		return _completed;
	}

	public Date getDateCompleted() {
		return _dateCompleted;
	}

	public Date getDateCreated() {
		return _dateCreated;
	}

	public String getDefinitionName() {
		return _definitionName;
	}

	public String getDescription() {
		return _description;
	}

	public Date getDueDate() {
		return _dueDate;
	}

	public Long getId() {
		return _id;
	}

	public WorkflowLog[] getLogs() {
		return _logs;
	}

	public Long[] getLogsIds() {
		return _logsIds;
	}

	public String getName() {
		return _name;
	}

	public ObjectReviewed getObjectReviewed() {
		return _objectReviewed;
	}

	public String[] getTransitions() {
		return _transitions;
	}

	public void setCompleted(Boolean completed) {
		_completed = completed;
	}

	public void setCompleted(
		UnsafeSupplier<Boolean, Throwable> completedUnsafeSupplier) {

		try {
			_completed = completedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDateCompleted(Date dateCompleted) {
		_dateCompleted = dateCompleted;
	}

	public void setDateCompleted(
		UnsafeSupplier<Date, Throwable> dateCompletedUnsafeSupplier) {

		try {
			_dateCompleted = dateCompletedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDateCreated(Date dateCreated) {
		_dateCreated = dateCreated;
	}

	public void setDateCreated(
		UnsafeSupplier<Date, Throwable> dateCreatedUnsafeSupplier) {

		try {
			_dateCreated = dateCreatedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDefinitionName(String definitionName) {
		_definitionName = definitionName;
	}

	public void setDefinitionName(
		UnsafeSupplier<String, Throwable> definitionNameUnsafeSupplier) {

		try {
			_definitionName = definitionNameUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDescription(String description) {
		_description = description;
	}

	public void setDescription(
		UnsafeSupplier<String, Throwable> descriptionUnsafeSupplier) {

		try {
			_description = descriptionUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setDueDate(Date dueDate) {
		_dueDate = dueDate;
	}

	public void setDueDate(
		UnsafeSupplier<Date, Throwable> dueDateUnsafeSupplier) {

		try {
			_dueDate = dueDateUnsafeSupplier.get();
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

	public void setLogs(
		UnsafeSupplier<WorkflowLog[], Throwable> logsUnsafeSupplier) {

		try {
			_logs = logsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setLogs(WorkflowLog[] logs) {
		_logs = logs;
	}

	public void setLogsIds(Long[] logsIds) {
		_logsIds = logsIds;
	}

	public void setLogsIds(
		UnsafeSupplier<Long[], Throwable> logsIdsUnsafeSupplier) {

		try {
			_logsIds = logsIdsUnsafeSupplier.get();
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

	public void setObjectReviewed(ObjectReviewed objectReviewed) {
		_objectReviewed = objectReviewed;
	}

	public void setObjectReviewed(
		UnsafeSupplier<ObjectReviewed, Throwable>
			objectReviewedUnsafeSupplier) {

		try {
			_objectReviewed = objectReviewedUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setTransitions(String[] transitions) {
		_transitions = transitions;
	}

	public void setTransitions(
		UnsafeSupplier<String[], Throwable> transitionsUnsafeSupplier) {

		try {
			_transitions = transitionsUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private Boolean _completed;
	private Date _dateCompleted;
	private Date _dateCreated;
	private String _definitionName;
	private String _description;
	private Date _dueDate;
	private Long _id;
	private WorkflowLog[] _logs;
	private Long[] _logsIds;
	private String _name;
	private ObjectReviewed _objectReviewed;
	private String[] _transitions;

}