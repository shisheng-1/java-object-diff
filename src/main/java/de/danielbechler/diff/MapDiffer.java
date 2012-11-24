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
import de.danielbechler.diff.node.*;
import de.danielbechler.util.*;
import de.danielbechler.util.Collections;

import java.util.*;

/**
 * Used to find differences between {@link Map Maps}
 *
 * @author Daniel Bechler
 */
final class MapDiffer implements Differ<MapNode>
{
	private final DifferDelegator delegator;
	private final Configuration configuration;

	public MapDiffer(final DifferDelegator delegator, final Configuration configuration)
	{
		Assert.notNull(delegator, "delegator");
		Assert.notNull(configuration, "configuration");
		this.delegator = delegator;
		this.configuration = configuration;
	}

	public MapNode compare(final Map<?, ?> modified, final Map<?, ?> base)
	{
		return compare(Node.ROOT, Instances.of(new RootAccessor(), modified, base));
	}

	@Override
	public final MapNode compare(final Node parentNode, final Instances instances)
	{
		final MapNode node = newNode(parentNode, instances);

		if (getConfiguration().isIgnored(node))
		{
			node.setState(Node.State.IGNORED);
			return node;
		}

		indexAll(instances, node);

		if (instances.getWorking() != null && instances.getBase() == null)
		{
			handleEntries(instances, node, instances.getWorking(Map.class).keySet());
			node.setState(Node.State.ADDED);
		}
		else if (instances.getWorking() == null && instances.getBase() != null)
		{
			handleEntries(instances, node, instances.getBase(Map.class).keySet());
			node.setState(Node.State.REMOVED);
		}
		else if (instances.areSame())
		{
			node.setState(Node.State.UNTOUCHED);
		}
		else if (getConfiguration().isEqualsOnly(node))
		{
			if (instances.areEqual())
			{
				node.setState(Node.State.UNTOUCHED);
			}
			else
			{
				node.setState(Node.State.CHANGED);
			}
		}
		else
		{
			handleEntries(instances, node, findAddedKeys(instances));
			handleEntries(instances, node, findRemovedKeys(instances));
			handleEntries(instances, node, findKnownKeys(instances));
		}
		return node;
	}

	private static MapNode newNode(final Node parentNode, final Instances instances)
	{
		return new MapNode(parentNode, instances.getSourceAccessor(), instances.getType());
	}

	public Node delegate(final Node parentNode, final Instances instances)
	{
		return delegator.delegate(parentNode, instances);
	}

	protected final Configuration getConfiguration()
	{
		return configuration;
	}

	private static void indexAll(final Instances instances, final MapNode node)
	{
		node.indexKeys(instances.getWorking(Map.class),
				instances.getBase(Map.class),
				instances.getFresh(Map.class));
	}

	private void handleEntries(final Instances instances, final MapNode parent, final Iterable<?> keys)
	{
		for (final Object key : keys)
		{
			handleEntries(key, instances, parent);
		}
	}

	private void handleEntries(final Object key, final Instances instances, final MapNode parent)
	{
		final Node node = compareEntry(key, instances, parent);
		if (getConfiguration().isReturnable(node))
		{
			parent.addChild(node);
		}
	}

	private Node compareEntry(final Object key, final Instances instances, final MapNode parent)
	{
		return delegate(parent, instances.access(parent.accessorForKey(key)));
	}

	private static Collection<?> findAddedKeys(final Instances instances)
	{
		final Set<?> source = instances.getWorking(Map.class).keySet();
		final Set<?> filter = instances.getBase(Map.class).keySet();
		return Collections.filteredCopyOf(source, filter);
	}

	private static Collection<?> findRemovedKeys(final Instances instances)
	{
		final Set<?> source = instances.getBase(Map.class).keySet();
		final Set<?> filter = instances.getWorking(Map.class).keySet();
		return Collections.filteredCopyOf(source, filter);
	}

	private static Iterable<?> findKnownKeys(final Instances instances)
	{
		final Set<?> keys = instances.getWorking(Map.class).keySet();
		final Collection<?> changed = Collections.setOf(keys);
		changed.removeAll(findAddedKeys(instances));
		changed.removeAll(findRemovedKeys(instances));
		return changed;
	}
}
