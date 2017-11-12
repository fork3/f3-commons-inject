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
package f3.commons.inject;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author n3k0nation
 *
 */
public class InjectorTest {
	class InjectorListener implements IInjectorListener {
		boolean isFailOnBindNotFound = true;
		public InjectorListener() {
		}
		
		public InjectorListener(boolean isFailOnBindNotFound) {
			this.isFailOnBindNotFound = isFailOnBindNotFound;
		}
		
		@Override
		public void onFailedProvideDepend(Field injectField, Object depend, RuntimeException e) {
			Assert.fail("Class: " + injectField.getDeclaringClass().getCanonicalName() + ", Field: " + injectField.getName()
					+ ", depend: " + depend.toString());
		}
		
		@Override
		public void onFailedInject(Field injectField, Object dependInstance) {
			Assert.fail("Class: " + injectField.getDeclaringClass().getCanonicalName() + ", Field: " + injectField.getName()
					+ ", depend: " + dependInstance.toString());
		}
		
		@Override
		public void onBindNotFound(Class<?> injectClass, Field injectField) {
			if(isFailOnBindNotFound) {
				Assert.fail("Class: " + injectClass.getCanonicalName() + ", Field: " + injectField.getName());
			}
		}
	}
	
	@Test
	public void testSimpleInject() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		classes.add(ISharedComponent.class);
		classes.add(TestSimpleInject.class);
		classes.add(ComponentA.class);
		
		Injector injector = new Injector();
		injector.setListener(new InjectorListener());
		injector.autoBind(classes);
		
		TestSimpleInject tsi1 = new TestSimpleInject();
		injector.inject(tsi1);
		Assert.assertEquals(tsi1.component.doSmth(), "ComponentA::doSmth");
		
		TestSimpleInject tsi2 = new TestSimpleInject();
		injector.inject(tsi2);
		Assert.assertEquals(tsi2.component.doSmth(), "ComponentA::doSmth");
		Assert.assertTrue(tsi1.component == tsi2.component);
	}
	
	@Test
	public void testInject() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		classes.add(ISharedComponent.class);
		classes.add(ComponentA.class);
		classes.add(NamedInject.class);
		classes.add(ComponentB.class);
		
		Injector injector = new Injector();
		injector.setListener(new InjectorListener());
		injector.autoBind(classes);
		
		NamedInject ni = new NamedInject();
		injector.inject(ni);
		Assert.assertEquals(ni.component.doSmth(), "ComponentB::doSmth");
		
		classes.add(SpecifiedInject.class);
		classes.add(ComponentWrap.class);
		injector.autoBind(classes);
		
		SpecifiedInject si = new SpecifiedInject();
		injector.inject(si);
		Assert.assertEquals(si.component.doSmth(), "ComponentB::doSmth && ComponentC::doSmth");
		
		classes.add(IMethodInject.class);
		classes.add(MethodInject1.class);
		classes.add(MethodInject2.class);
		classes.add(ComponentDFactory.class);
		classes.add(ComponentD.class);
		classes.add(ComponentEField.class);
		classes.add(ComponentE.class);
		injector.autoBind(classes);
		
		MethodInject1 mi1 = new MethodInject1();
		injector.inject(mi1);
		Assert.assertEquals(mi1.component.doSmth(), "ComponentD::doSmth && MethodInject1::getData");
		
		MethodInject2 mi2 = new MethodInject2();
		injector.inject(mi2);
		Assert.assertEquals(mi2.component.doSmth(), "ComponentD::doSmth && MethodInject2::getData");
		Assert.assertEquals(mi2.fieldComponent.doSmth(), "ComponentE::doSmth");
	}
	
	public static interface ISharedComponent {
		String doSmth();
	}
	
	public static class TestSimpleInject {
		@Inject ISharedComponent component; 
	}
	
	@Singleton
	public static class ComponentA implements ISharedComponent {
		@Override
		public String doSmth() {
			return "ComponentA::doSmth";
		}
	}
	
	public static class NamedInject {
		@Named("TestInject") @Inject ISharedComponent component;
	}
	
	@NamedScope("TestInject")
	public static class ComponentB implements ISharedComponent {
		@Override
		public String doSmth() {
			return "ComponentB::doSmth";
		}
	}
	
	public static class SpecifiedInject {
		@Inject ISharedComponent component;
	}
	
	@SpecifiedScope(SpecifiedInject.class)
	public static class ComponentWrap implements ISharedComponent {
		
		@Named("TestInject") @Inject ISharedComponent component;
		
		@Override
		public String doSmth() {
			return component.doSmth() + " && ComponentC::doSmth";
		}
		
	}
	
	public static interface IMethodInject {
		String getData();
	}
	
	public static class MethodInject1 implements IMethodInject {
		@Named("factory") @Inject ISharedComponent component;
		
		@Override
		public String getData() {
			return "MethodInject1::getData";
		}
	}
	
	public static class MethodInject2 implements IMethodInject {
		@Named("factory") @Inject ISharedComponent component;
		@Named("field") @Inject ISharedComponent fieldComponent;
		
		@Override
		public String getData() {
			return "MethodInject2::getData";
		}
	}
	
	public static class ComponentDFactory {
		@NamedScope("factory")
		public ComponentD createComponentD(IMethodInject methodInject) {
			return new ComponentD(methodInject.getData());
		}
	}
	
	@Singleton
	public static class ComponentEField {
		@NamedScope("field")
		ISharedComponent theComponent = new ComponentE();
	}
	
	public static class ComponentD implements ISharedComponent {
		final String data;
		public ComponentD(String data) {
			this.data = data;
		}
		
		@Override
		public String doSmth() {
			return "ComponentD::doSmth && " + data;
		}
	}
	
	public static class ComponentE implements ISharedComponent {
		@Override
		public String doSmth() {
			return "ComponentE::doSmth";
		}
	}
	
	
	
	@Test
	public void testHierarchyInject() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		classes.add(ISharedComponent.class);
		classes.add(AbstractClass.class);
		classes.add(AbstractChild.class);
		classes.add(ChildImpl1.class);
		classes.add(ChildImpl2.class);
		classes.add(OverridedChildImpl1.class);
		classes.add(OverridedChildImpl2.class);
		classes.add(ComponentChildImpl1.class);
		classes.add(ComponentOverridedChildImpl1.class);
		
		Injector injector = new Injector();
		injector.setListener(new InjectorListener());
		injector.autoBind(classes);
		
		ChildImpl1 child1 = new ChildImpl1();
		injector.inject(child1);
		Assert.assertEquals(child1.component.doSmth(), "ComponentChildImpl1::doSmth");
		
		ChildImpl2 child2 = new ChildImpl2();
		injector.inject(child2);
		Assert.assertEquals(child2.component.doSmth(), "ComponentChildImpl1::doSmth");
		
		OverridedChildImpl1 override = new OverridedChildImpl1();
		injector.inject(override);
		Assert.assertEquals(override.component.doSmth(), "ComponentOverridedChildImpl1::doSmth");
		
		OverridedChildImpl2 override2 = new OverridedChildImpl2();
		injector.inject(override2);
		Assert.assertEquals(override2.component.doSmth(), "ComponentChildImpl1::doSmth");
	}
	
	public static abstract class AbstractClass {
		@Inject ISharedComponent component;
	}
	
	public static abstract class AbstractChild extends AbstractClass {
		
	}
	
	public static class ChildImpl1 extends AbstractChild {
		
	}
	
	public static class ChildImpl2 extends AbstractChild {
		
	}
	
	public static class OverridedChildImpl1 extends ChildImpl1 {
		
	}
	
	public static class OverridedChildImpl2 extends ChildImpl2 {
		
	}
	
	@SpecifiedScope(value=ChildImpl1.class, hierarchy=true)
	@SpecifiedScope(value=ChildImpl2.class, hierarchy=true)
	public static class ComponentChildImpl1 implements ISharedComponent {
		@Override
		public String doSmth() {
			return "ComponentChildImpl1::doSmth";
		}
	}
	
	@SpecifiedScope(OverridedChildImpl1.class)
	public static class ComponentOverridedChildImpl1 implements ISharedComponent {
		@Override
		public String doSmth() {
			return "ComponentOverridedChildImpl1::doSmth";
		}
	}
}
