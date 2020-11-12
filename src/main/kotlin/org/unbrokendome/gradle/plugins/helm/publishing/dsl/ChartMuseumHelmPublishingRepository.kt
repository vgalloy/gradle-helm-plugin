package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.SerializableCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.toSerializable
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.PublisherParams
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import java.net.URI
import javax.inject.Inject


interface ChartMuseumHelmPublishingRepository : HelmPublishingRepository {

    /**
     * The tenant IDs for a [multitenancy](https://chartmuseum.com/docs/#multitenancy)-enabled Chartmuseum server.
     *
     * The number of elements should correspond to the `--depth` level that the Chartmuseum server is configured
     * with. For a single-tenant server (`--depth=0`), this list should be empty (which is the default).
     */
    val tenantIds: ListProperty<String>
}


private open class DefaultChartMuseumHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), ChartMuseumHelmPublishingRepository {

    override val tenantIds: ListProperty<String> =
        objects.listProperty<String>().empty()


    override val publisherParams: PublisherParams
        get() = ChartMuseumPublisherParams(
            url = url.get(),
            tenantIds = tenantIds.getOrElse(emptyList()),
            credentials = configuredCredentials.orNull?.toSerializable()
        )


    private class ChartMuseumPublisherParams(
        private val url: URI,
        private val tenantIds: List<String>,
        private val credentials: SerializableCredentials?
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            ChartMuseumPublisher(url, tenantIds, credentials)
    }


    private class ChartMuseumPublisher(
        url: URI,
        private val tenantIds: List<String>,
        credentials: SerializableCredentials?
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "POST"

        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/api" + tenantIds.joinToString(separator = "/", prefix = "/") + "/charts"
    }
}


internal fun ObjectFactory.newChartMuseumHelmPublishingRepository(name: String): ChartMuseumHelmPublishingRepository =
    newInstance(DefaultChartMuseumHelmPublishingRepository::class.java, name)
