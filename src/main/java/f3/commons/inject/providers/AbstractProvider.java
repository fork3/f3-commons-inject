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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import f3.commons.inject.ProviderContext;
import lombok.Getter;
import lombok.Setter;

/**
 * @author n3k0nation
 *
 */
public abstract class AbstractProvider {

	@Getter private final Object depend;
	@Setter private AbstractProvider next;
	
	public AbstractProvider(Object depend) {
		this.depend = depend;
	}
	
	public boolean isProvideFor(Class<?> clazz, Field field) {
		if(next != null) {
			return next.isProvideFor(clazz, field);
		}
		
		return false;
	}
	
	public Object provide(ProviderContext context) {
		if(next != null) {
			return next.provide(context);
		}
		return null;
	}
	
	public abstract int getPriority(Class<?> clazz, Field field);
	
	public boolean isClassDepend() {
		return depend instanceof Class;
	}
	
	public Class<?> getClassDepend() {
		return (Class) depend;
	}
	
	public boolean isMethodDepend() {
		return depend instanceof Method;
	}
	
	public Method getMethodDepend() {
		return (Method) depend;
	}
	
	public boolean isFieldDepend() {
		return depend instanceof Field;
	}
	
	public Field getFieldDepend() {
		return (Field) depend;
	}

}
