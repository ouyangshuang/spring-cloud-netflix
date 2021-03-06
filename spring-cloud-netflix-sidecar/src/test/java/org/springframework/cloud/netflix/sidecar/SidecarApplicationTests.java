/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.sidecar;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SidecarApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=mytest", "spring.cloud.client.hostname=mhhost",
		"spring.application.instance_id=1", "eureka.instance.hostname=mhhost",
		"sidecar.port=7000" })
public class SidecarApplicationTests {

	@Autowired
	EurekaInstanceConfigBean config;

	@Test
	public void testEurekaConfigBean() {
		assertThat(this.config.getAppname(), equalTo("mytest"));
		assertThat(this.config.getHostname(), equalTo("mhhost"));
		assertThat(this.config.getInstanceId(), equalTo("mhhost:mytest:1"));
		assertThat(this.config.getNonSecurePort(), equalTo(7000));
		System.out.println();
	}

}
