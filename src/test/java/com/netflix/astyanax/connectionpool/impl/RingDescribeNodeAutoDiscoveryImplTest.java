/*******************************************************************************
 * Copyright 2011 Netflix
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.netflix.astyanax.connectionpool.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.mock.MockConnectionPool;
import com.netflix.astyanax.mock.MockKeyspace;
import com.netflix.astyanax.mock.MockTokenRange;
import com.netflix.astyanax.model.TokenRange;

public class RingDescribeNodeAutoDiscoveryImplTest {
	@Test
	public void testRingDescribe() {
		String keyspaceName = "KEYSPACE";
		String clusterName = "CLUSTER";
		
		MockKeyspace keyspace 
			= new MockKeyspace(keyspaceName);
		keyspace.start();
		
		ConnectionPoolConfigurationImpl config 
			= new ConnectionPoolConfigurationImpl(clusterName, keyspaceName);
		config.setAutoDiscoveryDelay(30);
		config.setRingIpFilter("10.");
		
		MockConnectionPool pool
		 	= new MockConnectionPool();
		
		RingDescribeNodeAutoDiscoveryImpl discovery 
			= new RingDescribeNodeAutoDiscoveryImpl(config, keyspace, pool);
		
		List<TokenRange> tokens = new ArrayList<TokenRange>();
		TokenRange range1 = new MockTokenRange("0", "1", Arrays.asList("127.0.0.1", "10.0.0.2"));
		TokenRange range2 = new MockTokenRange("2", "3", Arrays.asList("10.0.0.2", "127.0.0.3"));
		tokens.addAll(Arrays.asList(range1, range2));
		keyspace.setTokenRange(tokens);
		
		Assert.assertNull(pool.getHosts());
		discovery.start();
		
		Map<String, List<Host>> ring = pool.getHosts();
		
		Assert.assertEquals(ring.size(), 2);
		Assert.assertNotNull(ring.get("0"));
		Assert.assertNotNull(ring.get("2"));
		
		discovery.shutdown();
			
	}
}
