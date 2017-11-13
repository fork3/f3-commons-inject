/*
 * Copyright (c) 2010-2017 fork3
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import f3.commons.inject.ProviderContext;

/**
 * End-point provider for all injects.
 * @author n3k0nation
 *
 */
public class DefaultProvider extends AbstractProvider {

	public DefaultProvider(Object depend) {
		super(depend);
	}

	@Override
	public boolean isProvideFor(Class<?> injectClass, Field injectField) {
		if(isClassDepend()) {
			return injectField.getType().isAssignableFrom(getClassDepend());
		}
		
		if(isMethodDepend()) {
			return injectField.getType().isAssignableFrom(getMethodDepend().getReturnType());
		}
		
		if(isFieldDepend()) {
			return injectField.getType().isAssignableFrom(getFieldDepend().getType());
		}
		
		return false;
	}
	
	@Override
	public Object provide(ProviderContext context) {
		try {
			if(isClassDepend()) {
				return createClass(getClassDepend(), context.getTargetInstance());
			}
			
			if(isMethodDepend()) {
				return createFromMethod(getMethodDepend(), context.getTargetInstance());
			}
			
			if(isFieldDepend()) {
				return createFromField(getFieldDepend(), context.getTargetInstance());
			}
			
			throw new RuntimeException("Unknown inject type");
		} catch(ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Object createClass(Class<?> clazz, Object object) throws ReflectiveOperationException {
		Constructor<?> constructor = null;
		final Constructor<?>[] ctors = clazz.getConstructors();
		for (int i = 0; i < ctors.length; i++) {
			final Constructor<?> ctor = ctors[i];
			if (ctor.getParameterCount() > 1) {
				continue;
			}

			if (ctor.getParameterCount() == 0) {
				constructor = ctor;
				break;
			}

			Class<?> type = ctor.getParameterTypes()[0];
			if (type.isAssignableFrom(object.getClass())) {
				constructor = ctor;
				break;
			}
		}

		if (constructor == null) {
			throw new NoSuchMethodException("Not found inject constructor in " + clazz.getCanonicalName());
		}
		constructor.setAccessible(true);
		return constructor.newInstance(object);
	}
	
	protected Object createFromMethod(Method method, Object object) throws ReflectiveOperationException {
		method.setAccessible(true);
		
		Object invoker = null;
		if(!Modifier.isStatic(method.getModifiers())) {
			invoker = createClass(method.getDeclaringClass(), object);
		}
		
		if(method.getParameterCount() != 0) {
			return method.invoke(invoker, object);
		} else {
			return method.invoke(invoker);
		}
	}
	
	protected Object createFromField(Field field, Object object) throws ReflectiveOperationException {
		field.setAccessible(true);
		
		Object invoker = null;
		if(!Modifier.isStatic(field.getModifiers())) {
			invoker = createClass(field.getDeclaringClass(), object);
		}
		
		return field.get(invoker);
	}
	
	@Override
	public int getPriority(Class<?> clazz, Field field) {
		return Integer.MAX_VALUE; //last in chain
	}
	
	

}
