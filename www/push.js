"use strict";

class Push {
	// Call this to register for push notifications. Content of [options] depends on whether we are working with APNS (iOS) or GCM (Android)
	register( successCallback, errorCallback, options ) {
		if( errorCallback === null ) {
			errorCallback = () => null;
		}

		if( typeof errorCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.register failure: failure parameter not a function" );
			return;
		}
		if( typeof successCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.register failure: success callback parameter must be a function" );
			return;
		}
		require( "cordova/exec" )( successCallback, errorCallback, "Push", "register", [ options ] );
	}

	// Call this to unregister for push notifications
	unregister( successCallback, errorCallback, options ) {
		if( errorCallback === null ) {
			errorCallback = () => null;
		}
		if( typeof errorCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.unregister failure: failure parameter not a function" );
			return;
		}
		if( typeof successCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.unregister failure: success callback parameter must be a function" );
			return;
		}
		require( "cordova/exec" )( successCallback, errorCallback, "Push", "unregister", [ options ] );
	}

	// Call this if you want to show toast notification on WP8
	showToastNotification( successCallback, errorCallback, options ) {
		if( errorCallback === null ) {
			errorCallback = () => null;
		}
		if( typeof errorCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.register failure: failure parameter not a function" );
			return;
		}
		require( "cordova/exec" )( successCallback, errorCallback, "Push", "showToastNotification", [ options ] );
	}

	// Call this to set the application icon badge
	setApplicationIconBadgeNumber( successCallback, errorCallback, badge ) {
		if( errorCallback === null ) {
			errorCallback = () => null;
		}
		if( typeof errorCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.setApplicationIconBadgeNumber failure: failure parameter not a function" );
			return;
		}
		if( typeof successCallback !== "function" ) {
			// eslint-disable-next-line no-console
			console.log( "Push.setApplicationIconBadgeNumber failure: success callback parameter must be a function" );
			return;
		}
		require( "cordova/exec" )( successCallback, errorCallback, "Push", "setApplicationIconBadgeNumber", [ {
			badge : badge
		} ] );
	}
}

if( typeof module !== "undefined" && module.exports ) {
	module.exports = new Push();
}
