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
import java.util.List;

import f3.commons.inject.SpecifiedScope;
import f3.commons.reflection.ClassUtils;

/**
 * @author n3k0nation
 *
 */
public class SpecifiedScopeProvider extends AbstractProvider {
	
	private final SpecifiedScope scope;
	
	public SpecifiedScopeProvider(Object depend, Annotation scope) {
		super(depend);
		this.scope = (SpecifiedScope) scope;
	}

	@Override
	public boolean isProvideFor(Class<?> clazz, Field field) {
		if(scope.hierarchy()) {
			if(!scope.value().isAssignableFrom(clazz)) {
				return false;
			}
		} else if(!scope.value().equals(clazz)) {
			return false;
		}
		
		if(!super.isProvideFor(clazz, field)) {
			return false;
		}
		
		if(!scope.field().isEmpty() && !scope.field().equals(field.getName())) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int getPriority(Class<?> clazz, Field field) {
		if(!scope.hierarchy() || scope.value().equals(clazz)) {
			return 0;
		}
		
		final List<Class<?>> hierarchy = ClassUtils.getAllParents(clazz);
		final int hierarchyIndex = hierarchy.indexOf(scope.value());
		if(hierarchyIndex < 0) {
			return hierarchy.size();
		}
		return hierarchyIndex;
	}

}
