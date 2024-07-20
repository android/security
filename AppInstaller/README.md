# Package Installer sample

This sample demonstrates how to use the Package Installer API to install, upgrade and delete apps on
Android S+ devices.

## What the sample does

The sample app addresses the following critical user journeys:
- How to install apps
- How to manually or automatically upgrade apps
- How to uninstall apps

## What this sample does not do

- This sample does **not** download APKs from internet (which is the common way of fetching apps)
- This sample does **not** verify version code when upgrading apps, it's only overwriting the
currently installed APK
- This sample does **not** handle gracefully failure at any stage. Action will be cancelled but no
warning messages are shown to the user

## How does it work
Before being able to install applications, we need to request the ability to install app from
unknown sources as your installer isn't pre-trusted on the device. Keep in mind that this settings
can be changed by the user only from the settings (and be updated at any time), and not via a
runtime permission (check the `AndroidManifest.xml` of this sample to see the requested permissions).

Following the ability to install applications, we can create [PackageInstaller.Session][1] using
[PackageInstaller.createSession][2] where we will copy the APK to be installed. Once we have a
session ID, we open the session using [PackageInstaller.openSession][3], we write the APK bytes to
it. We finish by calling [Session.commit][4] with a [PendingIntent][5] to be called if the install
action requires user confirmation.

To handle install status (including [PackageInstaller.STATUS_PENDING_USER_ACTION][6]), we need to
register a [BroadcastReceiver][7] that will listen to intents we've initialized during our session
creation. Your **BroadcastReceiver** should be registered statically in your manifest as install
status could be sent to your app while it's closed.

Upgrading an app follows the same process, we're just overwriting the current installed app's APK by
another one. It's your responsibility to make sure you don't overwrite the installed app by an older
version.

On Android 12+, installers that have been granted the `REQUEST_INSTALL_PACKAGES` permission, user
action will not be required when all of the following conditions are met:

* [setRequireUserAction][8] is set to false
* The app being installed targets **API 29** or higher
* The installer is the installer of record of an existing version of the app (in other words, this
install session is an app update) or the installer is updating itself
* The installer declares the `UPDATE_PACKAGES_WITHOUT_USER_ACTION` permission

Note: The target API level requirement will advance in future Android versions. Session owners
should always be prepared to handle `STATUS_PENDING_USER_ACTION`.

Uninstalling app is done by calling [PackageInstaller.uninstall][9] with the package ID of the app
and a **PendingIntent**. A **BroadcastReceiver** should be able to receive intents from the system
similar to the install process.

[1]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller.Session
[2]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller#createsession
[3]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller#openSession(kotlin.Int)
[4]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller.Session#commit
[5]: https://developer.android.com/reference/kotlin/android/app/PendingIntent
[6]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller#status_pending_user_action
[7]: https://developer.android.com/reference/kotlin/android/content/BroadcastReceiver
[8]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller.SessionParams#setrequireuseraction
[9]: https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller#uninstall

## Support

If you've found an error in this sample, please file an issue [here](https://github.com/android/security-samples/issues).
https://github.com/android/security-samples

Patches are encouraged, and may be submitted by forking this project and submitting a pull request
through GitHub but the scope of the sample is on purpose limited to not complexify it. Create an
issue explaining your upcoming changes before committing to them. Please see CONTRIBUTING.md for
more details.

## Suggested Reading

- [Package Installer API](https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller)
- [setRequireUserAction method](https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller.SessionParams#setinstallreason)