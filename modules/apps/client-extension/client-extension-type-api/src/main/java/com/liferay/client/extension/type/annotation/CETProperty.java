package com.liferay.client.extension.type.annotation;

public @interface CETProperty {
	String name() default "";
	String description() default "";
	String defaultValue() default "";
	String type() default "";
}
