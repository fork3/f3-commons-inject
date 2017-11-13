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
package f3.commons.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Scope;

import f3.commons.inject.providers.AbstractProvider;
import f3.commons.inject.providers.DefaultProvider;
import f3.commons.inject.providers.NamedScopeProvider;
import f3.commons.inject.providers.SingletonProvider;
import f3.commons.inject.providers.SpecifiedScopeProvider;
import f3.commons.inject.rules.DefaultProviderRule;
import f3.commons.inject.rules.IProviderRule;
import f3.commons.inject.rules.SingletonProviderRule;
import f3.commons.reflection.ClassUtils;
import f3.commons.reflection.FieldUtils;
import f3.commons.reflection.exception.ClassNotFoundUncheckedException;
import lombok.NonNull;

/**
 * @author n3k0nation
 *
 */
public class Injector {
	private final Map<Class<?>, Map<Field, AbstractProvider>> binds = new HashMap<>();
	private final List<IProviderRule> rules = new ArrayList<>();
	private IInjectorListener listener = new InjectorListenerStub();
	
	public Injector() {
		rules.add(new DefaultProviderRule(NamedScope.class, NamedScopeProvider::new));
		rules.add(new SingletonProviderRule());
		rules.add(new DefaultProviderRule(SpecifiedScope.class, SpecifiedScopeProvider::new));
	}
	
	public void clearBinds() {
		binds.clear();
	}
	
	public void addRule(IProviderRule rule) {
		rules.add(rule);
	}
	
	public boolean removeRule(IProviderRule rule) {
		return rules.remove(rule);
	}
	
	public void setListener(@NonNull IInjectorListener listener) {
		this.listener = listener;
	}
	
	public void addBind(Field inject, Class<?> depend) {
		addBind(inject, depend, false);
	}
	
	public void addBind(Field inject, Method depend) {
		addBind(inject, depend, false);
	}
	
	public void addBind(Field inject, Field depend) {
		addBind(inject, depend, false);
	}
	
	public void addSingletonBind(Field inject, Class<?> depend) {
		addBind(inject, depend, true);
	}
	
	public void addSingletonBind(Field inject, Method depend) {
		addBind(inject, depend, true);
	}
	
	public void addSingletonBind(Field inject, Field depend) {
		addBind(inject, depend, true);
	}
	
	private void addBind(Field inject, Object depend, boolean isSingleton) {
		final Class<?> clazz = inject.getDeclaringClass();
		Map<Field, AbstractProvider> submap = binds.get(clazz);
		if(submap == null) {
			binds.put(clazz, submap = new HashMap<>());
		}
		
		submap.put(inject, isSingleton ? new SingletonProvider(depend, null) : new DefaultProvider(depend));
	}
	
	public void autoBind(List<Class<?>> classes) {
		final List<AbstractProvider> providers = getProviders(classes);
		
		for(int i = 0; i < classes.size(); i++) {
			final Class<?> clazz = classes.get(i);
			final int modifiers = clazz.getModifiers();
			if(ClassUtils.isAbstractClass(clazz) || !Modifier.isPublic(modifiers) || clazz.isSynthetic()) {
				continue;
			}
			
			final List<Field> fields = FieldUtils.getAnnotatedField(clazz, Inject.class);
			for(int j = 0; j < fields.size(); j++) {
				final Field field = fields.get(j);
				field.setAccessible(true);
				
				Map<Field, AbstractProvider> submap = binds.get(clazz);
				if(submap == null) {
					binds.put(clazz, submap = new HashMap<>());
				}
				
				final AbstractProvider hittedProvider = providers.stream()
						.filter(provider -> provider.isProvideFor(clazz, field))
						//.sorted((p1, p2) -> p1 instanceof DefaultProvider ? p2 instanceof DefaultProvider ? 0 : 1 : -1)
						.sorted((p1, p2) -> p1.getPriority(clazz, field) - p2.getPriority(clazz, field))
						.findFirst()
						.orElseGet(() -> tryCreateDefaultProvider(clazz, field, classes));
				
				if(hittedProvider == null) {
					listener.onBindNotFound(clazz, field);
					continue;
				}
				
				submap.put(field, hittedProvider);
			}
		}
	}
	
	private AbstractProvider tryCreateDefaultProvider(Class<?> clazz, Field field, List<Class<?>> classes) {
//		final Annotation[] annotations = field.getAnnotations();
//		for(int i = 0; i < annotations.length; i++) { //check field to any qualifier
//			final Annotation annotation = annotations[i];
//			if(annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
//				return null;
//			}
//		}
		
		final Class<?> type = field.getType();
		Class<?> impl;
		try {
			impl = ClassUtils.getChildOf(type, classes, false);
		} catch(ClassNotFoundUncheckedException e) {
			return null;
		}
		
		final DefaultProvider defaultProvider = new DefaultProvider(impl);
		if(defaultProvider.isProvideFor(clazz, field)) {
			return defaultProvider;
		}
		return null;
	}
	
	public void inject(Object instance) {
		final Class<?> clazz = instance.getClass();
		
		final Map<Field, AbstractProvider> classInjects = binds.get(clazz);
		if(classInjects == null) {
			return;
		}
		
		for(Map.Entry<Field, AbstractProvider> entry : classInjects.entrySet()) {
			final Field injectField = entry.getKey();
			final AbstractProvider provider = entry.getValue();
			final ProviderContext context = new ProviderContext(injectField, instance);
			
			Object dependInstance;
			injectField.setAccessible(true);
			try {
				dependInstance = provider.provide(context);
			} catch(RuntimeException e) {
				listener.onFailedProvideDepend(injectField, provider.getDepend(), e);
				continue;
			}
			
			inject(dependInstance);
			
			try {
				injectField.set(instance, dependInstance);
			} catch(ReflectiveOperationException e) {
				listener.onFailedInject(injectField, dependInstance);
			}
		}
	}
	
	private List<AbstractProvider> getProviders(List<Class<?>> classes) {
		final ArrayList<AbstractProvider> providerChains = new ArrayList<>();
		for(int i = 0; i < classes.size(); i++) {
			final Class<?> clazz = classes.get(i);
			final int modifiers = clazz.getModifiers();
			if(ClassUtils.isAbstractClass(clazz) || !Modifier.isPublic(modifiers) || clazz.isSynthetic()) {
				continue;
			}
			
			providerChains.addAll(createChains(getPointcut(clazz, clazz, clazz.getAnnotations())));
			
			final Method[] methods = clazz.getDeclaredMethods();
			for(int j = 0; j < methods.length; j++) {
				final Method method = methods[j];
				providerChains.addAll(createChains(getPointcut(clazz, method, method.getAnnotations())));
			}
			
			final Field[] fields = clazz.getDeclaredFields();
			for(int j = 0; j < fields.length; j++) {
				final Field field = fields[j];
				providerChains.addAll(createChains(getPointcut(clazz, field, field.getAnnotations())));
			}
		}
		return providerChains;
	}
	
	private List<AbstractProvider> getPointcut(Class<?> owner, Object depend, Annotation[] annotations) {
		final ArrayList<AbstractProvider> pointcut = new ArrayList<>();
		for(int i = 0; i < annotations.length; i++) {
			final Annotation annotation = annotations[i];
			
			if(annotation.annotationType().isAnnotationPresent(RepeatableScope.class)) {
				try {
					final Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
					final Object value = valueMethod.invoke(annotation);
					if(value instanceof Annotation[]) {
						pointcut.addAll(getPointcut(owner, depend, (Annotation[]) value));
					}
				} catch (ReflectiveOperationException e) {
				}
				
				continue;
			}
			
			if(!annotation.annotationType().isAnnotationPresent(Scope.class)) {
				continue;
			}
			
			for(int j = 0; j < rules.size(); j++) {
				final IProviderRule rule = rules.get(j);
				if(!rule.isSupportScope(owner, annotation)) {
					continue;
				}
				
				pointcut.add(rule.createProvider(depend, annotation));
				break;
			}
		}
		return pointcut;
	}
	
	private List<AbstractProvider> createChains(List<AbstractProvider> pointcut) {
		if(pointcut.isEmpty()) {
			return Collections.emptyList();
		}
		
		AbstractProvider defaultProvider = null;
		for(int i = 0; i < pointcut.size(); i++) {
			final AbstractProvider provider = pointcut.get(i);
			if(provider instanceof DefaultProvider) {
				defaultProvider = provider;
				break;
			}
		}
		
		if(defaultProvider != null && pointcut.size() == 1) {
			return pointcut;
		}
		
		if(defaultProvider == null) {
			defaultProvider = new DefaultProvider(pointcut.get(0).getDepend());
		}
		
		final ArrayList<AbstractProvider> chains = new ArrayList<>();
		for(int i = 0; i < pointcut.size(); i++) {
			final AbstractProvider provider = pointcut.get(i);
			
			if(provider == defaultProvider) { //default provider is end-point of pointcut
				continue;
			}
			
			provider.setNext(defaultProvider);
			chains.add(provider);
		}
		
		return chains;
	}
}
