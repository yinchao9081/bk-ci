package com.tencent.devops.common.kubernetes.config

import com.tencent.devops.common.kubernetes.client.BkKubernetesDiscoveryClient
import io.kubernetes.client.informer.SharedInformer
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.models.V1Endpoints
import io.kubernetes.client.openapi.models.V1Service
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.kubernetes.client.discovery.ConditionalOnKubernetesDiscoveryEnabled
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnKubernetesDiscoveryEnabled
@EnableConfigurationProperties(KubernetesDiscoveryProperties::class)
class BkKubernetesConfiguration {
    @Bean
    @Primary
    @SuppressWarnings("LongParameterList")
    fun kubernetesInformerDiscoveryClient(
        kubernetesNamespaceProvider: KubernetesNamespaceProvider,
        sharedInformerFactory: SharedInformerFactory,
        serviceLister: Lister<V1Service>,
        endpointsLister: Lister<V1Endpoints>,
        serviceInformer: SharedInformer<V1Service>,
        endpointsInformer: SharedInformer<V1Endpoints>,
        kubernetesDiscoveryProperties: KubernetesDiscoveryProperties
    ): BkKubernetesDiscoveryClient {
        logger.debug("properties allNamespaces : ${kubernetesDiscoveryProperties.isAllNamespaces}")
        logger.debug(
            "properties cacheLoadingTimeoutSeconds : " +
                    "${kubernetesDiscoveryProperties.cacheLoadingTimeoutSeconds}"
        )
        logger.info("kubernetesInformerDiscoveryClient init success")
        return BkKubernetesDiscoveryClient(
            kubernetesNamespaceProvider.namespace,
            sharedInformerFactory, serviceLister, endpointsLister, serviceInformer, endpointsInformer,
            kubernetesDiscoveryProperties
        )
    }

    companion object {
        @Bean
        fun bkKubernetesPostProcessor(
            properties: KubernetesDiscoveryProperties
        ): BkKubernetesPostProcessor {
            return BkKubernetesPostProcessor(properties)
        }

        private val logger = LoggerFactory.getLogger(BkKubernetesConfiguration::class.java)
    }
}
