var PushNotification = function() {};

// Call this to register for push notifications. Content of [options] depends on whether we are working with APNS (iOS) or GCM (Android)
PushNotification.prototype.register = function( successCallback, errorCallback, options ) {
	if( errorCallback == null ) {
		errorCallback = function() {}
	}

	if( typeof errorCallback != "function" ) {
		console.log( "PushNotification.register failure: failure parameter not a function" );
		return
	}

	if( typeof successCallback != "function" ) {
		console.log( "PushNotification.register failure: success callback parameter must be a function" );
		return
	}

	require( "cordova/exec" )( successCallback, errorCallback, "Push", "register", [ options ] );
};

// Call this to unregister for push notifications
PushNotification.prototype.unregister = function( successCallback, errorCallback, options ) {
	if( errorCallback == null ) {
		errorCallback = function() {}
	}

	if( typeof errorCallback != "function" ) {
		console.log( "PushNotification.unregister failure: failure parameter not a function" );
		return
	}

	if( typeof successCallback != "function" ) {
		console.log( "PushNotification.unregister failure: success callback parameter must be a function" );
		return
	}

	require( "cordova/exec" )( successCallback, errorCallback, "Push", "unregister", [ options ] );
};

// Call this if you want to show toast notification on WP8
PushNotification.prototype.showToastNotification = function( successCallback, errorCallback, options ) {
	if( errorCallback == null ) {
		errorCallback = function() {}
	}

	if( typeof errorCallback != "function" ) {
		console.log( "PushNotification.register failure: failure parameter not a function" );
		return
	}

	require( "cordova/exec" )( successCallback, errorCallback, "Push", "showToastNotification", [ options ] );
}
// Call this to set the application icon badge
PushNotification.prototype.setApplicationIconBadgeNumber = function( successCallback, errorCallback, badge ) {
	if( errorCallback == null ) {
		errorCallback = function() {}
	}

	if( typeof errorCallback != "function" ) {
		console.log( "PushNotification.setApplicationIconBadgeNumber failure: failure parameter not a function" );
		return
	}

	if( typeof successCallback != "function" ) {
		console.log( "PushNotification.setApplicationIconBadgeNumber failure: success callback parameter must be a function" );
		return
	}

	require( "cordova/exec" )( successCallback, errorCallback, "Push", "setApplicationIconBadgeNumber", [ {
		badge : badge
	} ] );
};

//-------------------------------------------------------------------

if( typeof module != 'undefined' && module.exports ) {
	module.exports = PushNotification;
}
