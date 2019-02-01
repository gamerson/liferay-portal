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

package com.liferay.apio.architect.routes;

import aQute.bnd.annotation.ProviderType;

import com.liferay.apio.architect.alias.form.FormBuilderFunction;
import com.liferay.apio.architect.alias.routes.CustomItemFunction;
import com.liferay.apio.architect.alias.routes.DeleteItemConsumer;
import com.liferay.apio.architect.alias.routes.GetItemFunction;
import com.liferay.apio.architect.alias.routes.UpdateItemFunction;
import com.liferay.apio.architect.alias.routes.permission.HasRemovePermissionFunction;
import com.liferay.apio.architect.alias.routes.permission.HasUpdatePermissionFunction;
import com.liferay.apio.architect.consumer.throwable.ThrowableBiConsumer;
import com.liferay.apio.architect.consumer.throwable.ThrowableConsumer;
import com.liferay.apio.architect.consumer.throwable.ThrowablePentaConsumer;
import com.liferay.apio.architect.consumer.throwable.ThrowableTetraConsumer;
import com.liferay.apio.architect.consumer.throwable.ThrowableTriConsumer;
import com.liferay.apio.architect.credentials.Credentials;
import com.liferay.apio.architect.custom.actions.CustomRoute;
import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.function.throwable.ThrowableBiFunction;
import com.liferay.apio.architect.function.throwable.ThrowableFunction;
import com.liferay.apio.architect.function.throwable.ThrowableHexaFunction;
import com.liferay.apio.architect.function.throwable.ThrowablePentaFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTetraFunction;
import com.liferay.apio.architect.function.throwable.ThrowableTriFunction;
import com.liferay.apio.architect.identifier.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Holds information about the routes supported for an {@link
 * com.liferay.apio.architect.router.ItemRouter}.
 *
 * <p>
 * This interface's methods return functions that get the item resource's
 * different endpoints. You should always use a {@link Builder} to create
 * instances of this interface.
 * </p>
 *
 * @author     Alejandro Hernández
 * @param      <T> the model's type
 * @param      <S> the type of the model's identifier (e.g., {@code Long},
 *             {@code String}, etc.)
 * @see        Builder
 * @deprecated As of 1.9.0, use {@link
 *             com.liferay.apio.architect.annotation.Actions} annotations
 *             instead
 */
@Deprecated
@ProviderType
@SuppressWarnings("DeprecatedIsStillUsed")
public interface ItemRoutes<T, S> {

	/**
	 * Returns the function that creates custom operations, if the endpoint was
	 * added through {@link CollectionRoutes.Builder} and the function therefore
	 * exists. Returns {@code Optional#empty()} otherwise.
	 *
	 * @return     the function used to create custom operations, if the
	 *             function exists; {@code Optional#empty()} otherwise
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	public Optional<Map<String, CustomItemFunction<?, S>>>
		getCustomItemFunctionsOptional();

	/**
	 * Returns the custom routes configured based on their paths.
	 *
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	public Map<String, CustomRoute> getCustomRoutes();

	/**
	 * Returns the function that deletes the item, if the endpoint was added
	 * through {@link ItemRoutes.Builder} and the function therefore exists.
	 * Returns {@code Optional#empty()} otherwise.
	 *
	 * @return     the function that deletes the item, if the function exists;
	 *             {@code Optional#empty()} otherwise
	 * @deprecated use annotation builder instead
	 */
	@Deprecated
	public Optional<DeleteItemConsumer<S>> getDeleteConsumerOptional();

	/**
	 * Returns the form that is used to update a collection item, if it was
	 * added through the {@link ItemRoutes.Builder}. Returns {@code
	 * Optional#empty()} otherwise.
	 *
	 * @return     the form used to update a collection item; {@code
	 *             Optional#empty()} otherwise
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	public Optional<Form> getFormOptional();

	/**
	 * Returns the function used to obtain the item, if the endpoint was added
	 * through the {@link ItemRoutes.Builder} and the function therefore exists.
	 * Returns {@code Optional#empty()} otherwise.
	 *
	 * @return     the function used to obtain the item, if the function exists;
	 *             {@code Optional#empty()} otherwise
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	public Optional<GetItemFunction<T, S>> getItemFunctionOptional();

	/**
	 * Returns the function used to update the item, if the endpoint was added
	 * through the {@link ItemRoutes.Builder} and the function therefore exists.
	 * Returns {@code Optional#empty()} otherwise.
	 *
	 * @return     the function used to update the item, if the function exists;
	 *             {@code Optional#empty()} otherwise
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	public Optional<UpdateItemFunction<T, S>> getUpdateItemFunctionOptional();

	/**
	 * Creates the {@code ItemRoutes} of an {@link
	 * com.liferay.apio.architect.router.ItemRouter}.
	 *
	 * @param      <T> the model's type
	 * @param      <S> the type of the model's identifier (e.g., {@code Long},
	 *             {@code String}, etc.)
	 * @deprecated As of 1.9.0, use {@link
	 *             com.liferay.apio.architect.annotation.Actions} annotations
	 *             instead
	 */
	@Deprecated
	@ProviderType
	public interface Builder<T, S> {

		/**
		 * Adds a {@link CustomRoute} via the {@code CustomRoute} object (that
		 * sets the HTTP method to use) and the custom route function {@code
		 * throwableBiFunction}.
		 *
		 * @param      customRoute the custom route's name and method
		 * @param      throwableBiFunction the custom route function
		 * @param      identifierClass the identifier class of type {@code R}
		 * @param      permissionBiFunction the permission function for this
		 *             route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Action}
		 *             annotation instead
		 */
		@Deprecated
		public default <R, U, I extends Identifier<?>> Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableBiFunction<S, R, U> throwableBiFunction,
				Class<I> identifierClass,
				BiFunction<Credentials, S, Boolean> permissionBiFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			return addCustomRoute(
				customRoute, (s, r, ignore) -> throwableBiFunction.apply(s, r),
				Void.class, identifierClass, permissionBiFunction,
				formBuilderFunction);
		}

		/**
		 * Adds a {@link CustomRoute} via the {@code CustomRoute} object (that
		 * sets the HTTP method to use) and the custom route function {@code
		 * throwableHexaFunction}.
		 *
		 * @param      customRoute the custom route's name and method
		 * @param      throwableHexaFunction the custom route function
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @param      cClass the class of the item function's fourth parameter
		 * @param      dClass the class of the item function's fifth parameter
		 * @param      identifierClass the identifier class of type {@code R}
		 * @param      permissionBiFunction the permission function for this
		 *             route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Action}
		 *             annotation instead
		 */
		@Deprecated
		public <A, B, C, D, R, U, I extends Identifier<?>> Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableHexaFunction<S, R, A, B, C, D, U>
					throwableHexaFunction,
				Class<A> aClass, Class<B> bClass, Class<C> cClass,
				Class<D> dClass, Class<I> identifierClass,
				BiFunction<Credentials, S, Boolean> permissionBiFunction,
				FormBuilderFunction<R> formBuilderFunction);

		/**
		 * Adds a {@link CustomRoute} via the {@code CustomRoute} object (that
		 * sets the HTTP method to use) and the custom route function {@code
		 * throwablePentaFunction}.
		 *
		 * @param      customRoute the custom route's name and method
		 * @param      throwablePentaFunction the custom route function
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @param      cClass the class of the item function's fourth parameter
		 * @param      identifierClass the identifier class of type {@code R}
		 * @param      permissionBiFunction the permission function for this
		 *             route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Action}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, C, R, U, I extends Identifier<?>> Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowablePentaFunction<S, R, A, B, C, U> throwablePentaFunction,
				Class<A> aClass, Class<B> bClass, Class<C> cClass,
				Class<I> identifierClass,
				BiFunction<Credentials, S, Boolean> permissionBiFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			return addCustomRoute(
				customRoute,
				(s, r, a, b, c, ignore) -> throwablePentaFunction.apply(
					s, r, a, b, c),
				aClass, bClass, cClass, Void.class, identifierClass,
				permissionBiFunction, formBuilderFunction);
		}

		/**
		 * Adds a {@link CustomRoute} via the {@code CustomRoute} object (that
		 * sets the HTTP method to use) and the custom route function {@code
		 * throwableTetraFunction}.
		 *
		 * @param      customRoute the custom route's name and method
		 * @param      throwableTetraFunction the custom route function
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @param      identifierClass the identifier class of type {@code R}
		 * @param      permissionBiFunction the permission function for this
		 *             route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Action}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, R, U, I extends Identifier<?>> Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableTetraFunction<S, R, A, B, U> throwableTetraFunction,
				Class<A> aClass, Class<B> bClass, Class<I> identifierClass,
				BiFunction<Credentials, S, Boolean> permissionBiFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			return addCustomRoute(
				customRoute,
				(s, r, a, b, ignore) -> throwableTetraFunction.apply(
					s, r, a, b),
				aClass, bClass, Void.class, identifierClass,
				permissionBiFunction, formBuilderFunction);
		}

		/**
		 * Adds a {@link CustomRoute} via the {@code CustomRoute} object (that
		 * sets the HTTP method to use) and the custom route function {@code
		 * throwableTriFunction}.
		 *
		 * @param      customRoute the custom route's name and method
		 * @param      throwableTriFunction the custom route function
		 * @param      aClass the class of the item function's second parameter
		 * @param      identifierClass the identifier class of type {@code R}
		 * @param      permissionBiFunction the permission function for this
		 *             route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Action}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, R, U, I extends Identifier<?>> Builder<T, S>
			addCustomRoute(
				CustomRoute customRoute,
				ThrowableTriFunction<S, R, A, U> throwableTriFunction,
				Class<A> aClass, Class<I> identifierClass,
				BiFunction<Credentials, S, Boolean> permissionBiFunction,
				FormBuilderFunction<R> formBuilderFunction) {

			return addCustomRoute(
				customRoute,
				(s, r, a, ignore) -> throwableTriFunction.apply(s, r, a),
				aClass, Void.class, identifierClass, permissionBiFunction,
				formBuilderFunction);
		}

		/**
		 * Adds a route to an item function with one extra parameter.
		 *
		 * @param      getterThrowableBiFunction the function that calculates
		 *             the item
		 * @param      aClass the class of the item function's second parameter
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Retrieve}
		 *             annotation instead
		 */
		@Deprecated
		public default <A> Builder<T, S> addGetter(
			ThrowableBiFunction<S, A, T> getterThrowableBiFunction,
			Class<A> aClass) {

			return addGetter(
				(s, a, ignore) -> getterThrowableBiFunction.apply(s, a), aClass,
				Void.class);
		}

		/**
		 * Adds a route to an item function with no extra parameters.
		 *
		 * @param      getterThrowableFunction the function that calculates the
		 *             item
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Retrieve}
		 *             annotation instead
		 */
		@Deprecated
		public default Builder<T, S> addGetter(
			ThrowableFunction<S, T> getterThrowableFunction) {

			return addGetter(
				(s, ignore) -> getterThrowableFunction.apply(s), Void.class);
		}

		/**
		 * Adds a route to an item function with four extra parameters.
		 *
		 * @param      getterThrowablePentaFunction the function that calculates
		 *             the item
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @param      cClass the class of the item function's fourth parameter
		 * @param      dClass the class of the item function's fifth parameter
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Retrieve}
		 *             annotation instead
		 */
		@Deprecated
		public <A, B, C, D> Builder<T, S> addGetter(
			ThrowablePentaFunction<S, A, B, C, D, T>
				getterThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass);

		/**
		 * Adds a route to an item function with three extra parameters.
		 *
		 * @param      getterThrowableTetraFunction the function that calculates
		 *             the item
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @param      cClass the class of the item function's fourth parameter
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Retrieve}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, C> Builder<T, S> addGetter(
			ThrowableTetraFunction<S, A, B, C, T> getterThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass) {

			return addGetter(
				(s, a, b, c, ignore) -> getterThrowableTetraFunction.apply(
					s, a, b, c),
				aClass, bClass, cClass, Void.class);
		}

		/**
		 * Adds a route to an item function with two extra parameters.
		 *
		 * @param      getterThrowableTriFunction the function that calculates
		 *             the item
		 * @param      aClass the class of the item function's second parameter
		 * @param      bClass the class of the item function's third parameter
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Retrieve}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B> Builder<T, S> addGetter(
			ThrowableTriFunction<S, A, B, T> getterThrowableTriFunction,
			Class<A> aClass, Class<B> bClass) {

			return addGetter(
				(s, a, b, ignore) -> getterThrowableTriFunction.apply(s, a, b),
				aClass, bClass, Void.class);
		}

		/**
		 * Adds a route to a remover function with one extra parameter.
		 *
		 * @param      removerThrowableBiConsumer the remover function
		 * @param      aClass the class of the item remover function's second
		 *             parameter
		 * @param      hasRemovePermissionFunction the permission function for
		 *             this route
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Remove}
		 *             annotation instead
		 */
		@Deprecated
		public default <A> Builder<T, S> addRemover(
			ThrowableBiConsumer<S, A> removerThrowableBiConsumer,
			Class<A> aClass,
			HasRemovePermissionFunction<S> hasRemovePermissionFunction) {

			return addRemover(
				(s, a, ignore) -> removerThrowableBiConsumer.accept(s, a),
				aClass, Void.class, hasRemovePermissionFunction);
		}

		/**
		 * Adds a route to a remover function with no extra parameters.
		 *
		 * @param      removerThrowableConsumer the remover function
		 * @param      hasRemovePermissionFunction the permission function for
		 *             this route
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Remove}
		 *             annotation instead
		 */
		@Deprecated
		public default Builder<T, S> addRemover(
			ThrowableConsumer<S> removerThrowableConsumer,
			HasRemovePermissionFunction<S> hasRemovePermissionFunction) {

			return addRemover(
				(s, ignore) -> removerThrowableConsumer.accept(s), Void.class,
				hasRemovePermissionFunction);
		}

		/**
		 * Adds a route to a remover function with four extra parameters.
		 *
		 * @param      removerThrowablePentaConsumer the remover function
		 * @param      aClass the class of the item remover function's second
		 *             parameter
		 * @param      bClass the class of the item remover function's third
		 *             parameter
		 * @param      cClass the class of the item remover function's fourth
		 *             parameter
		 * @param      dClass the class of the item remover function's fifth
		 *             parameter
		 * @param      hasRemovePermissionFunction the permission function for
		 *             this route
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Remove}
		 *             annotation instead
		 */
		@Deprecated
		public <A, B, C, D> Builder<T, S> addRemover(
			ThrowablePentaConsumer<S, A, B, C, D> removerThrowablePentaConsumer,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasRemovePermissionFunction<S> hasRemovePermissionFunction);

		/**
		 * Adds a route to a remover function with three extra parameters.
		 *
		 * @param      removerThrowableTetraConsumer the remover function
		 * @param      aClass the class of the item remover function's second
		 *             parameter
		 * @param      bClass the class of the item remover function's third
		 *             parameter
		 * @param      cClass the class of the item remover function's fourth
		 *             parameter
		 * @param      hasRemovePermissionFunction the permission function for
		 *             this route
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Remove}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, C> Builder<T, S> addRemover(
			ThrowableTetraConsumer<S, A, B, C> removerThrowableTetraConsumer,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasRemovePermissionFunction<S> hasRemovePermissionFunction) {

			return addRemover(
				(s, a, b, c, ignore) -> removerThrowableTetraConsumer.accept(
					s, a, b, c),
				aClass, bClass, cClass, Void.class,
				hasRemovePermissionFunction);
		}

		/**
		 * Adds a route to a remover function with two extra parameters.
		 *
		 * @param      removerThrowableTriConsumer the remover function
		 * @param      aClass the class of the item remover function's second
		 *             parameter
		 * @param      bClass the class of the item remover function's third
		 *             parameter
		 * @param      hasRemovePermissionFunction the permission function for
		 *             this route
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Remove}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B> Builder<T, S> addRemover(
			ThrowableTriConsumer<S, A, B> removerThrowableTriConsumer,
			Class<A> aClass, Class<B> bClass,
			HasRemovePermissionFunction<S> hasRemovePermissionFunction) {

			return addRemover(
				(s, a, b, ignore) -> removerThrowableTriConsumer.accept(
					s, a, b),
				aClass, bClass, Void.class, hasRemovePermissionFunction);
		}

		/**
		 * Adds a route to an updater function with no extra parameters.
		 *
		 * @param      updaterThrowableBiFunction the updater function
		 * @param      hasUpdatePermissionFunction the permission function for
		 *             this route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Replace}
		 *             annotation instead
		 */
		@Deprecated
		public default <R> Builder<T, S> addUpdater(
			ThrowableBiFunction<S, R, T> updaterThrowableBiFunction,
			HasUpdatePermissionFunction<S> hasUpdatePermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			return addUpdater(
				(s, r, ignore) -> updaterThrowableBiFunction.apply(s, r),
				Void.class, hasUpdatePermissionFunction, formBuilderFunction);
		}

		/**
		 * Adds a route to an updater function with four extra parameters.
		 *
		 * @param      updaterThrowableHexaFunction the updater function
		 * @param      aClass the class of the updater function's third
		 *             parameter
		 * @param      bClass the class of the updater function's fourth
		 *             parameter
		 * @param      cClass the class of the updater function's fifth
		 *             parameter
		 * @param      dClass the class of the updater function's sixth
		 *             parameter
		 * @param      hasUpdatePermissionFunction the permission function for
		 *             this route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Replace}
		 *             annotation instead
		 */
		@Deprecated
		public <A, B, C, D, R> Builder<T, S> addUpdater(
			ThrowableHexaFunction<S, R, A, B, C, D, T>
				updaterThrowableHexaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass, Class<D> dClass,
			HasUpdatePermissionFunction<S> hasUpdatePermissionFunction,
			FormBuilderFunction<R> formBuilderFunction);

		/**
		 * Adds a route to an updater function with three extra parameters.
		 *
		 * @param      updaterThrowablePentaFunction the updater function that
		 *             removes the item
		 * @param      aClass the class of the updater function's third
		 *             parameter
		 * @param      bClass the class of the updater function's fourth
		 *             parameter
		 * @param      cClass the class of the updater function's fifth
		 *             parameter
		 * @param      hasUpdatePermissionFunction the permission function for
		 *             this route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Replace}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, C, R> Builder<T, S> addUpdater(
			ThrowablePentaFunction<S, R, A, B, C, T>
				updaterThrowablePentaFunction,
			Class<A> aClass, Class<B> bClass, Class<C> cClass,
			HasUpdatePermissionFunction<S> hasUpdatePermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			return addUpdater(
				(s, r, a, b, c, ignore) -> updaterThrowablePentaFunction.apply(
					s, r, a, b, c),
				aClass, bClass, cClass, Void.class, hasUpdatePermissionFunction,
				formBuilderFunction);
		}

		/**
		 * Adds a route to an updater function with two extra parameters.
		 *
		 * @param      updaterThrowableTetraFunction the updater function
		 * @param      aClass the class of the updater function's third
		 *             parameter
		 * @param      bClass the class of the updater function's fourth
		 *             parameter
		 * @param      hasUpdatePermissionFunction the permission function for
		 *             this route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Replace}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, B, R> Builder<T, S> addUpdater(
			ThrowableTetraFunction<S, R, A, B, T> updaterThrowableTetraFunction,
			Class<A> aClass, Class<B> bClass,
			HasUpdatePermissionFunction<S> hasUpdatePermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			return addUpdater(
				(s, r, a, b, ignore) -> updaterThrowableTetraFunction.apply(
					s, r, a, b),
				aClass, bClass, Void.class, hasUpdatePermissionFunction,
				formBuilderFunction);
		}

		/**
		 * Adds a route to an updater function with one extra parameter.
		 *
		 * @param      updaterThrowableTriFunction the updater function that
		 *             removes the item
		 * @param      aClass the class of the updater function's third
		 *             parameter
		 * @param      hasUpdatePermissionFunction the permission function for
		 *             this route
		 * @param      formBuilderFunction the function that creates the form
		 *             for this operation
		 * @return     the updated builder
		 * @deprecated As of 1.9.0, use the {@link
		 *             com.liferay.apio.architect.annotation.Actions.Replace}
		 *             annotation instead
		 */
		@Deprecated
		public default <A, R> Builder<T, S> addUpdater(
			ThrowableTriFunction<S, R, A, T> updaterThrowableTriFunction,
			Class<A> aClass,
			HasUpdatePermissionFunction<S> hasUpdatePermissionFunction,
			FormBuilderFunction<R> formBuilderFunction) {

			return addUpdater(
				(s, r, a, ignore) -> updaterThrowableTriFunction.apply(s, r, a),
				aClass, Void.class, hasUpdatePermissionFunction,
				formBuilderFunction);
		}

		/**
		 * Constructs the {@link ItemRoutes} instance with the information
		 * provided to the builder.
		 *
		 * @return     the {@code Routes} instance
		 * @deprecated As of 1.9.0, use {@link
		 *             com.liferay.apio.architect.annotation.Actions}
		 *             annotations instead
		 */
		@Deprecated
		public ItemRoutes<T, S> build();

	}

}