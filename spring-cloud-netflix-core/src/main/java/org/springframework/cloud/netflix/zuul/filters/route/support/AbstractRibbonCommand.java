/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.support;

import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 */
public abstract class AbstractRibbonCommand<LBC extends AbstractLoadBalancerAwareClient<RQ, RS>, RQ extends ClientRequest, RS extends HttpResponse> extends HystrixCommand<ClientHttpResponse> implements
		RibbonCommand {

	protected final LBC client;
	protected RibbonCommandContext context;

	public AbstractRibbonCommand(LBC client, RibbonCommandContext context) {
		this("default", client, context);
	}

	public AbstractRibbonCommand(String commandKey, LBC client, RibbonCommandContext context) {
		super(getSetter(commandKey));
		this.client = client;
		this.context = context;
	}

	protected static Setter getSetter(final String commandKey) {

		// we want to default to semaphore-isolation since this wraps
		// 2 others commands that are already thread isolated
		// @formatter:off
		final String name = ZuulConstants.ZUUL_EUREKA + commandKey + ".semaphore.maxSemaphores";
		final DynamicIntProperty value = DynamicPropertyFactory.getInstance()
				.getIntProperty(name, 100);
		final HystrixCommandProperties.Setter setter = HystrixCommandProperties .Setter()
				.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
				.withExecutionIsolationSemaphoreMaxConcurrentRequests(value.get());
		return Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey + "RibbonCommand"))
				.andCommandPropertiesDefaults(setter);
		// @formatter:on
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		final RequestContext context = RequestContext.getCurrentContext();
		long contentLength = context.getRequest().getContentLengthLong();
		if (contentLength != -1) {
			this.context.setContentLength(contentLength);
		}

		RQ request = createRequest();
		RS response = this.client.executeWithLoadBalancer(request);

		context.set("ribbonResponse", response);

		// Explicitly close the HttpResponse if the Hystrix command timed out to
		// release the underlying HTTP connection held by the response.
		//
		if (this.isResponseTimedOut()) {
			if (response != null) {
				response.close();
			}
		}

		return new RibbonHttpResponse(response);
	}

	public LBC getClient() {
		return client;
	}

	public RibbonCommandContext getContext() {
		return context;
	}

	protected abstract RQ createRequest() throws Exception;
}
