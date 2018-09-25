//
//  AppDelegate+notification.m
//  pushtest
//
//  Created by Robert Easterday on 10/26/12.
//
//

#import "AppDelegate+notification.h"
#import "FMPush.h"
#import <objc/runtime.h>

static char launchNotificationKey;

@implementation AppDelegate (notification)

- (id) getCommandInstance:(NSString*) pluginName {
	return [self.viewController getCommandInstance:pluginName];
}

// its dangerous to override a method from within a category.
// Instead we will use method swizzling. we set this up in the load call.
+ (void)load {
	Method original, swizzled;

	original = class_getInstanceMethod(self, @selector(init));
	swizzled = class_getInstanceMethod(self, @selector(swizzled_init));
	method_exchangeImplementations(original, swizzled);
}

- (AppDelegate*)swizzled_init {
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(createNotificationChecker:)name:@"UIApplicationDidFinishLaunchingNotification" object:nil];

	// This actually calls the original init method over in AppDelegate. Equivilent to calling super
	// on an overrided method, this is not recursive, although it appears that way. neat huh?
	return [self swizzled_init];
}

// This code will be called immediately after application:didFinishLaunchingWithOptions:. We need
// to process notifications in cold-start situations
- (void)createNotificationChecker:(NSNotification*) notification {
	if (notification) {
		NSDictionary* launchOptions = [notification userInfo];
		if (launchOptions)
			self.launchNotification = [launchOptions objectForKey: @"UIApplicationLaunchOptionsRemoteNotificationKey"];
	}
}

- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken {
	FMPush* pushHandler = [self getCommandInstance:@"Push"];
	[pushHandler didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error {
	FMPush* pushHandler = [self getCommandInstance:@"Push"];
	[pushHandler didFailToRegisterForRemoteNotificationsWithError:error];
}

- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*) userInfo {
	NSLog(@"didReceiveNotification");

	// Get application state for iOS4.x+ devices, otherwise assume active
	UIApplicationState appState = UIApplicationStateActive;
	if ([application respondsToSelector:@selector(applicationState)]) {
		appState = application.applicationState;
	}

	if (appState == UIApplicationStateActive) {
		FMPush* pushHandler = [self getCommandInstance:@"Push"];
		pushHandler.notificationMessage = userInfo;
		pushHandler.isInline = YES;
		[pushHandler notificationReceived];
	} else {
		//save it for later
		self.launchNotification = userInfo;
	}
}

// didReceiveRemoteNotification with fetchCompletionHandler to schedule local notifications which will cause additional vibrations.
- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
	NSLog(@"didReceiveNotification");

	UIApplicationState state = [[UIApplication sharedApplication] applicationState];

	if (state == UIApplicationStateBackground || state == UIApplicationStateInactive) {
		NSLog(@"Received Remote Notification while being in background");

		// setup additional local notifications for the push notification
		for( int i = 1; i <= 15; i++ ) {
			UILocalNotification* notification = [UILocalNotification new];

			notification.fireDate  = [NSDate dateWithTimeIntervalSinceNow:i*2];
			notification.timeZone  = [NSTimeZone defaultTimeZone];
			notification.soundName = @"silence.aiff";
			notification.userInfo  = @{kFMAdditionalLocalNotification: @(i)};

			[application scheduleLocalNotification:notification];
		}

	} else {
		[self application:application didReceiveRemoteNotification:userInfo];
	}

	// call completion handler
	completionHandler(UIBackgroundFetchResultNoData);
}

- (void)application:(UIApplication*)application didReceiveLocalNotification:(UILocalNotification*)notification {
	// Ignore additional local notifications for received remote notifications.
	// These were only scheduled to make the phone vibrate.
	if ([notification.userInfo objectForKey:kFMAdditionalLocalNotification]) {
		NSLog(@"Brrrrzzzzzt");
		return;
	}
}

- (void)applicationDidBecomeActive:(UIApplication*)application {
	NSLog(@"active");

	//zero badge
	application.applicationIconBadgeNumber = 0;

	if (self.launchNotification) {
		FMPush* pushHandler = [self getCommandInstance:@"Push"];

		pushHandler.notificationMessage = self.launchNotification;
		self.launchNotification = nil;
		[pushHandler performSelectorOnMainThread:@selector(notificationReceived) withObject:pushHandler waitUntilDone:NO];
	}

	[[UIApplication sharedApplication] cancelAllLocalNotifications];
}

// The accessors use an Associative Reference since you can't define a iVar in a category
// http://developer.apple.com/library/ios/#documentation/cocoa/conceptual/objectivec/Chapters/ocAssociativeReferences.html
- (NSMutableArray*)launchNotification {
	return objc_getAssociatedObject(self, &launchNotificationKey);
}

- (void)setLaunchNotification:(NSDictionary*)aDictionary {
	objc_setAssociatedObject(self, &launchNotificationKey, aDictionary, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)dealloc {
	self.launchNotification = nil; // clear the association and release the object
}

@end
