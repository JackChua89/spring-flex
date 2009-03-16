package org.springframework.flex.messaging.config;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generic.GenericBeanFactoryAccessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.flex.messaging.remoting.FlexExclude;
import org.springframework.flex.messaging.remoting.FlexInclude;
import org.springframework.flex.messaging.remoting.FlexRemotingServiceExporter;
import org.springframework.flex.messaging.remoting.FlexService;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * {@link BeanFactoryPostProcessor} implementation that searches the {@link BeanFactory} for
 * beans annotated with {@link FlexService} and adds a corresponding {@link FlexRemotingServiceExporter}
 * bean definition according to the attributes of the FlexService annotation and any methods found to
 * be marked with either the {@link FlexInclude} or {@link FlexExclude} annotation.
 * 
 * <p>
 * This processor will be enabled automatically when using the message-broker tag of the xml config namespace.
 *
 * @author Jeremy Grelle
 */
public class FlexRemotingAnnotationPostProcessor implements
		BeanFactoryPostProcessor {

	// --------------------------- Bean Configuration Properties -------------//
	private static final String MESSAGE_BROKER_PROPERTY = "messageBroker";
	private static final String SERVICE_PROPERTY = "service";
	private static final String SERVICE_ID_PROPERTY = "destinationId";
	private static final String CHANNELS_PROPERTY = "channels";
	private static final String INCLUDE_METHODS_PROPERTY = "includeMethods";
	private static final String EXCLUDE_METHODS_PROPERTY = "excludeMethods";
	
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
		GenericBeanFactoryAccessor accessor = new GenericBeanFactoryAccessor(beanFactory);
		
		Map<String, Object> remoteBeans = accessor.getBeansWithAnnotation(FlexService.class);
		
		if (remoteBeans.size() > 0) {
			Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory, 
					"In order for services to be exported via the @FlexService annotation, the current BeanFactory must be a BeanDefinitionRegistry.");
		}
		
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
		
		for (Entry<String, Object> beanEntry : remoteBeans.entrySet()) {
			
			BeanDefinitionBuilder exporterBuilder = BeanDefinitionBuilder.rootBeanDefinition(FlexRemotingServiceExporter.class);
			exporterBuilder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			
			FlexService flexService = beanEntry.getValue().getClass().getAnnotation(FlexService.class);
			
			
			String messageBrokerId = StringUtils.hasText(flexService.messageBroker()) ? flexService.messageBroker() : BeanIds.MESSAGE_BROKER;
			String serviceId = StringUtils.hasText(flexService.value()) ? flexService.value() : beanEntry.getKey();
			
			exporterBuilder.addPropertyReference(MESSAGE_BROKER_PROPERTY, messageBrokerId);
			exporterBuilder.addPropertyReference(SERVICE_PROPERTY, beanEntry.getKey());
			exporterBuilder.addPropertyValue(SERVICE_ID_PROPERTY, serviceId);
			exporterBuilder.addPropertyValue(CHANNELS_PROPERTY, flexService.channels());
			exporterBuilder.addPropertyValue(INCLUDE_METHODS_PROPERTY, extractIncludeMethods(beanEntry.getValue().getClass()));
			exporterBuilder.addPropertyValue(EXCLUDE_METHODS_PROPERTY, extractExcludeMethods(beanEntry.getValue().getClass()));
			
			BeanDefinitionReaderUtils.registerWithGeneratedName(exporterBuilder.getBeanDefinition(), registry);
		}

	}

	private String[] extractExcludeMethods(Class<?> serviceClass) {
		final Set<String> excludes = new HashSet<String>();
		ReflectionUtils.doWithMethods(serviceClass, new MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException,
					IllegalAccessException {
				excludes.add(method.getName());
			}
		}, new FlexExcludeFilter());
		return excludes.toArray(new String[excludes.size()]);
	}

	private String[] extractIncludeMethods(Class<?> serviceClass) {
		final Set<String> includes = new HashSet<String>();
		ReflectionUtils.doWithMethods(serviceClass, new MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException,
					IllegalAccessException {
				includes.add(method.getName());
			}
		}, new FlexIncludeFilter());
		return includes.toArray(new String[includes.size()]);
	}
	
	private static class FlexExcludeFilter implements ReflectionUtils.MethodFilter {
		public boolean matches(Method method) {
			return method.getAnnotation(FlexExclude.class) != null;
		}
	}
	
	private static class FlexIncludeFilter implements ReflectionUtils.MethodFilter {
		public boolean matches(Method method) {
			return method.getAnnotation(FlexInclude.class) != null;
		}
	}
}
