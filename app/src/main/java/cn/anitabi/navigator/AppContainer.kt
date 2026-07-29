package cn.anitabi.navigator

import android.content.Context
import cn.anitabi.navigator.data.local.AnitabiDatabase
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.network.UserAgentInterceptor
import cn.anitabi.navigator.data.network.anitabi.AnitabiApi
import cn.anitabi.navigator.data.network.bangumi.BangumiApi
import cn.anitabi.navigator.data.network.ors.OrsApi
import cn.anitabi.navigator.data.network.transit.TransitousApi
import cn.anitabi.navigator.data.repository.PilgrimageRepository
import cn.anitabi.navigator.data.repository.TourRepository
import cn.anitabi.navigator.core.routing.OrsRoadRoutingProvider
import cn.anitabi.navigator.core.routing.TourPlanner
import cn.anitabi.navigator.core.routing.TransitousJourneyProvider
import cn.anitabi.navigator.security.OrsKeyStore
import cn.anitabi.navigator.navigation.AndroidLocationProvider

class AppContainer(context: Context) {
    private val json = ApiHttpClient.defaultJson
    private val database = AnitabiDatabase.create(context)
    val orsKeyStore = OrsKeyStore(context)
    val locationProvider = AndroidLocationProvider(context)
    private val httpClient = ApiHttpClient(
        userAgentInterceptor = UserAgentInterceptor(
            appName = "AnitabiNavigator",
            appVersion = BuildConfig.VERSION_NAME,
            contact = PROJECT_CONTACT,
        ),
        json = json,
    )

    val bangumiApi = BangumiApi(httpClient, json)
    val pilgrimageRepository = PilgrimageRepository(
        api = AnitabiApi(httpClient),
        cacheDao = database.pilgrimageCacheDao(),
        json = json,
    )
    val tourRepository = TourRepository(database.tourPlanDao(), json)
    val tourPlanner = TourPlanner(
        roadProvider = OrsRoadRoutingProvider(OrsApi(httpClient, orsKeyStore::get, json)),
        transitProvider = TransitousJourneyProvider(
            TransitousApi(httpClient) { BuildConfig.TRANSITOUS_APPROVED },
        ),
    )

    companion object {
        const val PROJECT_CONTACT = "https://github.com/realMisakaMikoto"
    }
}
