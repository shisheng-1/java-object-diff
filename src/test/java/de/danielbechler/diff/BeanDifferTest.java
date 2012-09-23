/*
 * Copyright 2012 Daniel Bechler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.danielbechler.diff;

import de.danielbechler.diff.accessor.*;
import de.danielbechler.diff.annotation.*;
import de.danielbechler.diff.introspect.*;
import de.danielbechler.diff.mock.*;
import de.danielbechler.diff.node.*;
import de.danielbechler.diff.visitor.*;
import org.hamcrest.core.*;
import org.mockito.*;
import org.testng.annotations.*;

import java.util.*;

import static de.danielbechler.diff.node.NodeAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsSame.*;
import static org.mockito.Mockito.*;

/** @author Daniel Bechler */
public class BeanDifferTest
{
	@Mock
	private DelegatingObjectDiffer delegate;
	@Mock
	private Introspector introspector;
	@Mock
	private Accessor accessor;
	@Mock
	private Node node;
	private BeanDiffer differ;

	@BeforeMethod
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		differ = new BeanDiffer();
		differ.setDelegate(delegate);
		differ.setIntrospector(introspector);
	}

	@Test
	public void testCompareWithDifferentStrings() throws Exception
	{
		when(delegate.isEqualsOnly(any(Node.class))).thenReturn(true);
		final Node node = differ.compare("foo", "bar");
		assertThat(node.hasChanges(), is(true));
		assertThat(node.hasChildren(), is(false));
		assertThat(node.getState(), is(Node.State.CHANGED));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testCompareWithDifferentTypes()
	{
		differ.compare("foo", 1337);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testCompareWithoutWorkingInstance()
	{
		when(delegate.isEqualsOnly(any(Node.class))).thenReturn(true);
		differ.compare(null, "foo");
	}

	@Test
	public void testCompareWithoutAnyInstances()
	{
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), null, null));
		assertThat(node.getState(), is(Node.State.UNTOUCHED));
	}

	@Test
	public void testCompareWithIgnoredProperty()
	{
		when(delegate.isIgnored(any(Node.class))).thenReturn(true);
		assertThat(differ.compare("foo", "bar").getState(), is(Node.State.IGNORED));
	}

	@Test
	public void testCompareWithAddedInstance()
	{
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), "foo", null));
		assertThat(node.getState(), is(Node.State.ADDED));
	}

	@Test
	public void testCompareWithRemovedInstance()
	{
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), null, "foo"));
		assertThat(node.getState(), is(Node.State.REMOVED));
	}

	@Test
	public void testCompareWithSameInstance()
	{
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), "foo", "foo"));
		assertThat(node.getState(), is(Node.State.UNTOUCHED));
	}

	@Test
	public void testCompareWithEqualsOnlyTypes()
	{
		when(delegate.isEqualsOnly(any(Node.class))).thenReturn(true);

		final ObjectWithHashCodeAndEquals working = new ObjectWithHashCodeAndEquals("foo");
		final ObjectWithHashCodeAndEquals base = new ObjectWithHashCodeAndEquals("foo");
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), working, base));

		assertThat(node.getState(), is(Node.State.UNTOUCHED));
	}

	@Test
	public void testCompareWithUnequalEqualsOnlyTypes()
	{
		when(delegate.isEqualsOnly(any(Node.class))).thenReturn(true);

		final ObjectWithHashCodeAndEquals working = new ObjectWithHashCodeAndEquals("foo");
		final ObjectWithHashCodeAndEquals base = new ObjectWithHashCodeAndEquals("bar");
		final Node node = differ.compare(Node.ROOT, Instances.of(new RootAccessor(), working, base));

		assertThat(node.getState(), is(Node.State.CHANGED));
	}

//	@Test
//	public void testCompareWithEqualsOnlyPath()
//	{
//		when(delegate.isEqualsOnlyPath(any(PropertyPath.class))).thenReturn(true);
//
//		final ObjectWithHashCodeAndEquals working = new ObjectWithHashCodeAndEquals("foo", "1");
//		final ObjectWithHashCodeAndEquals base = new ObjectWithHashCodeAndEquals("bar", "1");
//		final Node node = differ.compare(working, base);
//
//		verify(delegate, atLeastOnce()).isEqualsOnlyPath(any(PropertyPath.class));
//		assertThat(node.getState(), is(Node.State.CHANGED));
//	}

	@Test
	public void testCompareWithComplexType()
	{
		when(introspector.introspect(any(Class.class))).thenReturn(Arrays.<Accessor>asList(accessor));
		when(delegate.delegate(any(Node.class), any(Instances.class))).thenReturn(node);
		when(delegate.isIntrospectible(any(Node.class))).thenReturn(true);
		when(delegate.isReturnable(any(Node.class))).thenReturn(true);
		when(node.hasChanges()).thenReturn(true);

		final Node node = differ.compare(new Object(), new Object());
		assertThat(node.getState(), is(Node.State.CHANGED));
	}

	@Test
	public void testStupidStuffToGetOneHundredPercentCodeCoverage1()
	{
		final Configuration configuration = new Configuration();
		when(delegate.getConfiguration()).thenReturn(configuration);
		assertThat(differ.getConfiguration(), sameInstance(configuration));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testConstructionWithoutObjectDiffer()
	{
		new BeanDiffer(null);
	}

	/** Just for the sake of 100% code coverage */
	@Test
	public void testConstructionWithObjectDiffer()
	{
		new BeanDiffer(delegate);
	}

	@Test
	public void testThatIgnoredPropertiesAreNeverAccessed()
	{
		final ObjectWithAccessTrackingIgnoredProperty working = new ObjectWithAccessTrackingIgnoredProperty();
		final ObjectWithAccessTrackingIgnoredProperty base = new ObjectWithAccessTrackingIgnoredProperty();
		new BeanDiffer().compare(working, base);
		assertThat(working.accessed, Is.is(false));
		assertThat(base.accessed, Is.is(false));
	}

	@Test
	public void testThatObjectGraphForAddedObjectsGetsReturned()
	{
		final ObjectWithNestedObject base = new ObjectWithNestedObject("1");
		final ObjectWithNestedObject working = new ObjectWithNestedObject("1", new ObjectWithNestedObject("2", new ObjectWithNestedObject("foo")));
		differ = new BeanDiffer();
		differ.getConfiguration().withChildrenOfAddedNodes();
		node = differ.compare(working, base);
		node.visit(new NodeHierarchyVisitor());
		assertThat(node).node().hasState(Node.State.CHANGED);
		assertThat(node).child("object").hasState(Node.State.ADDED);
		assertThat(node).child("object", "object").hasState(Node.State.ADDED);
	}

	@SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
	private static class ObjectWithAccessTrackingIgnoredProperty
	{
		private boolean accessed = false;

		@ObjectDiffProperty(ignore = true)
		public void getValue()
		{
			accessed = true;
		}

		public void setValue()
		{
			accessed = true;
		}
	}
}