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

package com.liferay.apio.architect.resource;

import aQute.bnd.annotation.ProviderType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an API resource. Only three implementations are allowed: {@link
 * Item}, {@link Paged}, and {@link Nested}.
 *
 * <p>
 * This class should never be directly instantiated. Use one of its descendants'
 * static methods ({@link Paged#of}, {@link Item#of} and {@link Nested#of})
 * instead.
 * </p>
 *
 * @author Alejandro Hernández
 * @see    Item
 * @see    Paged
 * @see    Nested
 */
@ProviderType
public class Resource {

	/**
	 * Returns the resource's name.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Represents a generic parent resource. This class should never be directly
	 * instantiated. Always use the {@link #of} method to create a new instance.
	 *
	 * <p>
	 * This class is intended for resources that must be scoped, but that scope
	 * (e.g., parent) can't be another resource. For example:
	 * </p>
	 *
	 * <ul>
	 * <li>
	 * Blog post comments ({@link Nested}): {@code /blog-post/42/comment}
	 * </li>
	 * <li>
	 * Comments by a generic parent ({@link GenericParent}: {@code
	 * /comment/by-generic-parent/blog-post:42}
	 * </li>
	 * </ul>
	 */
	public static class GenericParent extends Resource {

		/**
		 * Creates a new {@code GenericParent} with the parent name, parent ID,
		 * and name.
		 *
		 * @param  parentName the parent name
		 * @param  parentId the parent ID
		 * @param  name the name
		 * @review
		 */
		public static GenericParent of(
			String parentName, Id parentId, String name) {

			return new GenericParent(parentName, parentId, name);
		}

		/**
		 * Creates a new {@code GenericParent} with the parent name and name.
		 *
		 * @param  parentName the parent name
		 * @param  name the name
		 * @review
		 */
		public static GenericParent of(String parentName, String name) {
			return new GenericParent(parentName, null, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof GenericParent)) {
				return false;
			}

			GenericParent genericParent = (GenericParent)obj;

			if (Objects.equals(getName(), genericParent.getName()) &&
				Objects.equals(_parentName, genericParent._parentName)) {

				return true;
			}

			return false;
		}

		/**
		 * Returns the resource's generic parent ID.
		 */
		public Optional<Id> getParentIdOptional() {
			return Optional.ofNullable(_parentId);
		}

		/**
		 * Returns the resource's generic parent name.
		 */
		public String getParentName() {
			return _parentName;
		}

		@Override
		public int hashCode() {
			int h = 5381;

			String name = getName();

			h += (h << 5) + name.hashCode();

			h += (h << 5) + _parentName.hashCode();

			return h;
		}

		@Override
		public String toString() {
			return "{name=" + getName() + ", parentId=" + _parentId +
				", parentName=" + _parentName + "}";
		}

		/**
		 * Copies the current {@code GenericParent} by setting a new value for
		 * the generic parent's ID attribute. If the same ID is supplied, a
		 * shallow reference equality check prevents the copy and the method
		 * returns {@code this}.
		 *
		 * @param  id the new ID
		 * @return the generic parent copy, if the new ID is different from the
		 *         current generic parent's ID; {@code this} otherwise
		 */
		public GenericParent withParentId(Id id) {
			if ((_parentId != null) && _parentId.equals(id)) {
				return this;
			}

			return new GenericParent(_parentName, id, getName());
		}

		private GenericParent(String parentName, Id parentId, String name) {
			super(name);

			_parentName = parentName;
			_parentId = parentId;
		}

		private final Id _parentId;
		private final String _parentName;

	}

	/**
	 * Represents an item's ID. You should never instantiate this class
	 * directly. Always use the {@link Id#of} method to create a new instance.
	 */
	public static class Id {

		/**
		 * Creates a new {@code ID} with the provided object-string pair.
		 *
		 * @param  objectVersion the object
		 * @return the new {@code ID}
		 */
		public static Id of(Object objectVersion, String stringVersion) {
			return new Id(objectVersion, stringVersion);
		}

		/**
		 * Returns the {@code ID} as an {@code Object} instance. This can be any
		 * class supported by a {@link
		 * com.liferay.apio.architect.uri.mapper.PathIdentifierMapper}.
		 *
		 * @return the {@code ID} as an {@code Object} instance
		 */
		public Object asObject() {
			return _objectVersion;
		}

		/**
		 * Returns the {@code ID} as a {@code String} instance.
		 *
		 * @return the {@code ID} as a {@code String} instance
		 */
		public String asString() {
			return _stringVersion;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Id)) {
				return false;
			}

			Id id = (Id)obj;

			if (Objects.equals(_objectVersion, id._objectVersion) &&
				Objects.equals(_stringVersion, id._stringVersion)) {

				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int h = 5381;

			h += (h << 5) + _objectVersion.hashCode();
			h += (h << 5) + _stringVersion.hashCode();

			return h;
		}

		@Override
		public String toString() {
			return "{" + _stringVersion + "}";
		}

		private Id(Object objectVersion, String stringVersion) {
			_objectVersion = objectVersion;
			_stringVersion = stringVersion;
		}

		private final Object _objectVersion;
		private final String _stringVersion;

	}

	/**
	 * Represents an item resource. You should never instantiate this class
	 * directly. Always use an {@link Item#of} method to create a new instance.
	 */
	public static class Item extends Resource {

		/**
		 * Creates a new {@code Item} with the provided name.
		 *
		 * @param  name the name
		 * @return the new {@code Item}
		 */
		public static Item of(String name) {
			return new Item(name, null);
		}

		/**
		 * Creates a new {@code Item} with the provided {@code name} and {@code
		 * ID}.
		 *
		 * @param  name the name
		 * @return the new {@code Item}
		 */
		public static Item of(String name, Id id) {
			return new Item(name, id);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Item)) {
				return false;
			}

			Item item = (Item)obj;

			if (Objects.equals(getName(), item.getName())) {
				return true;
			}

			return false;
		}

		/**
		 * Returns the resource's ID if it exists, or an empty {@code Optional}
		 * otherwise. This component is not taken into account when performing
		 * an {@link #equals)} check.
		 *
		 * @return the resource's ID if it exists; an empty {@code Optional}
		 *         otherwise
		 */
		public Optional<Id> getIdOptional() {
			return Optional.ofNullable(_id);
		}

		@Override
		public int hashCode() {
			int h = 5381;

			String name = getName();

			h += (h << 5) + name.hashCode();

			return h;
		}

		@Override
		public String toString() {
			if (_id != null) {
				return "{id=" + _id + ", name=" + getName() + "}";
			}

			return "{name=" + getName() + "}";
		}

		/**
		 * Copies the current {@code Item} by setting a new value for the item's
		 * ID. If the same ID is supplied, a shallow reference equality check
		 * prevents the copy and the method returns {@code this}.
		 *
		 * @param  id the new ID
		 * @return the {@code Item} copy, if the new ID is different from the
		 *         current item's ID; {@code this} otherwise
		 */
		public Item withId(Id id) {
			if ((_id != null) && _id.equals(id)) {
				return this;
			}

			return new Item(getName(), id);
		}

		private Item(String name, Id id) {
			super(name);

			_id = id;
		}

		private final Id _id;

	}

	/**
	 * Represents a nested resource. You should never instantiate this class
	 * directly. Always use the {@link Nested#of} method to create a new
	 * instance.
	 */
	public static class Nested extends Resource {

		/**
		 * Creates a new {@code Nested} with the provided parent item and name.
		 *
		 * @param  parentItem the parent item
		 * @param  name the name
		 * @return the new {@code Nested}
		 * @review
		 */
		public static Nested of(Item parentItem, String name) {
			return new Nested(parentItem, name);
		}

		public Nested(Item parentItem, String name) {
			super(name);

			_parentItem = parentItem;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Nested)) {
				return false;
			}

			Nested nested = (Nested)obj;

			if (Objects.equals(getName(), nested.getName()) &&
				Objects.equals(_parentItem, nested._parentItem)) {

				return true;
			}

			return false;
		}

		/**
		 * The resource's parent item.
		 *
		 * @return the parent item
		 */
		public Item getParentItem() {
			return _parentItem;
		}

		@Override
		public int hashCode() {
			int h = 5381;

			String name = getName();

			h += (h << 5) + name.hashCode();

			h += (h << 5) + _parentItem.hashCode();

			return h;
		}

		@Override
		public String toString() {
			return "{name=" + getName() + ", parentItem=" + _parentItem + "}";
		}

		/**
		 * Copies the current {@code Nested} by setting a new value for its ID.
		 * If the same ID is supplied, a shallow reference equality check
		 * prevents the copy and the method returns {@code this}.
		 *
		 * @param  id the new ID
		 * @return the {@code Nested} copy, if the new ID is different from the
		 *         current {@code Nested} object's ID; {@code this} otherwise
		 */
		public Nested withParentId(Id id) {
			if ((_parentItem._id != null) && _parentItem._id.equals(id)) {
				return this;
			}

			return new Nested(_parentItem.withId(id), getName());
		}

		private final Item _parentItem;

	}

	/**
	 * Represents a paged resource. You should never instantiate this class
	 * directly. Always use the {@link Paged#of} method to create a new
	 * instance.
	 */
	public static class Paged extends Resource {

		/**
		 * Creates a new {@code Paged} with the provided name.
		 *
		 * @param  name the name
		 * @return the new {@code Paged}
		 * @review
		 */
		public static Paged of(String name) {
			return new Paged(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Paged)) {
				return false;
			}

			Paged paged = (Paged)obj;

			if (Objects.equals(getName(), paged.getName())) {
				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int h = 5381;

			String name = getName();

			h += (h << 5) + name.hashCode();

			return h;
		}

		@Override
		public String toString() {
			return "{name=" + getName() + "}";
		}

		private Paged(String name) {
			super(name);
		}

	}

	private Resource(String name) {
		_name = name;
	}

	private final String _name;

}