/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * Integration tests for {@link MockitoBean} that validate automatic reset
 * of stubbing.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@SpringJUnitConfig
@TestMethodOrder(OrderAnnotation.class)
public class MockitoBeanWithResetIntegrationTests {

	@MockitoBean(reset = MockReset.BEFORE)
	ExampleService service;

	@MockitoBean(reset = MockReset.BEFORE)
	FailingExampleService failingService;

	@Order(1)
	@Test
	void beanFirstEstablishingMock(ApplicationContext ctx) {
		ExampleService mock = ctx.getBean("service", ExampleService.class);
		doReturn("Mocked hello").when(mock).greeting();

		assertThat(this.service.greeting()).isEqualTo("Mocked hello");
	}

	@Order(2)
	@Test
	void beanSecondEnsuringMockReset(ApplicationContext ctx) {
		assertThat(ctx.getBean("service")).isNotNull().isSameAs(this.service);

		assertThat(this.service.greeting()).as("not stubbed").isNull();
	}

	@Order(3)
	@Test
	void factoryBeanFirstEstablishingMock(ApplicationContext ctx) {
		FailingExampleService mock = ctx.getBean(FailingExampleService.class);
		doReturn("Mocked hello").when(mock).greeting();

		assertThat(this.failingService.greeting()).isEqualTo("Mocked hello");
	}

	@Order(4)
	@Test
	void factoryBeanSecondEnsuringMockReset(ApplicationContext ctx) {
		assertThat(ctx.getBean("factory")).isNotNull().isSameAs(this.failingService);

		assertThat(this.failingService.greeting()).as("not stubbed")
				.isNull();
	}

	static class FailingExampleServiceFactory implements FactoryBean<FailingExampleService> {
		@Nullable
		@Override
		public FailingExampleService getObject() {
			return new FailingExampleService();
		}

		@Nullable
		@Override
		public Class<?> getObjectType() {
			return FailingExampleService.class;
		}
	}

	@Configuration
	static class Config {

		@Bean("service")
		ExampleService bean1() {
			return new RealExampleService("Production hello");
		}

		@Bean("factory")
		@Qualifier("factory")
		FailingExampleServiceFactory factory() {
			return new FailingExampleServiceFactory();
		}
	}

}