/*
 * Copyright (c) 2010-2017 fork2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package f3.commons.inject.providers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Singleton;

import f3.commons.inject.ProviderContext;
import f3.commons.reflection.ClassUtils;

/**
 * @author n3k0nation
 *
 */
public class SingletonProvider extends DefaultProvider {
	
	private final Singleton scope;
	private Object singleton;
	
	public SingletonProvider(Object depend, Annotation scope) {
		super(depend);
		this.scope = (Singleton) scope;
	}
	
	@Override
	public Object provide(ProviderContext context) {
		if(singleton != null) {
			return singleton;
		}
		
		return super.provide(context);
	}
	
	@Override
	protected Object createClass(Class<?> clazz, Object object) throws ReflectiveOperationException {
		final Object instance = ClassUtils.singletonInstance(clazz);
		if(instance == null) {
			return singleton = super.createClass(clazz, object);
		}
		
		return singleton = instance;
	}
	
	@Override
	protected Object createFromField(Field field, Object object) throws ReflectiveOperationException {
		return singleton = super.createFromField(field, object);
	}
	
	@Override
	protected Object createFromMethod(Method method, Object object) throws ReflectiveOperationException {
		return singleton = super.createFromMethod(method, object);
	}

}
