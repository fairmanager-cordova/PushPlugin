//
//  AppDelegate+notification.h
//  pushtest
//
//  Created by Robert Easterday on 10/26/12.
//
//

#import "AppDelegate.h"

#define kFMAdditionalLocalNotification @"__FAIR_MANAGER__LOCAL_NOTIFICATION_FOR_PUSH__"

@interface AppDelegate (notification)
- (void)application:(UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken;
- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error;
- (void)application:(UIApplication*)application didReceiveRemoteNotification:(NSDictionary*)userInfo;
- (void)applicationDidBecomeActive:(UIApplication*)application;
- (id) getCommandInstance:(NSString*)pluginName;

@property (nonatomic, retain) NSDictionary* launchNotification;

@end
