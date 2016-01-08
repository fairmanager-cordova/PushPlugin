// Copyright (c) Microsoft Open Technologies, Inc.  Licensed under the MIT license.
module.exports = {
    register: function (success, fail, args) {
        try {
            var onNotificationReceived = function (e) {
                window[args[0].ecb](e);
                e.cancel = true;
            };

            Windows.Networking.PushNotifications.PushNotificationChannelManager.createPushNotificationChannelForApplicationAsync().then(
                function (channel) {
                    channel.addEventListener("pushnotificationreceived", onNotificationReceived);
                    success(channel);
            }, fail);
        } catch(ex) {
            fail(ex);
        }
    }
};
require("cordova/exec/proxy").add("PushPlugin", module.exports);
