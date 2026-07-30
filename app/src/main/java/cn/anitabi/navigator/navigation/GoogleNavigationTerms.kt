package cn.anitabi.navigator.navigation

import android.app.Activity
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator

fun requestGoogleNavigationTerms(
    activity: Activity,
    onReady: () -> Unit,
    onError: (String) -> Unit,
) {
    NavigationApi.getNavigator(
        activity,
        object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(navigator: Navigator) {
                onReady()
            }

            override fun onError(errorCode: Int) {
                onError(initializationErrorMessage(errorCode))
            }
        },
    )
}
