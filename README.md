# Bible First (prototype)

An Android app that covers your phone with a reading screen each morning until you mark your Bible reading as done.

## What it does
- Each morning (default 04:00) the phone locks until you tap **I've finished my reading**.
- While locked, these still work: **calls, texts, maps**, your **chosen Bible app(s)**, your home screen, keyboards and alarms. Everything else is covered by the reading screen.
- **Emergency override:** 60-second wait, then 10 minutes of normal use. Uses are counted on the main screen.
- Works with any Bible app: tick the ones you use in settings.

## Run it
1. Install Android Studio and open this folder (`BibleFirst`). Let Gradle sync.
2. Plug in your Samsung phone with USB debugging on (or use an emulator) and press Run.
3. In the app: tap **Open Accessibility settings**, switch **Bible First** on, tick your Bible app, then switch on **Morning lock**.
4. Use **Reset today (for testing)** to lock it again without waiting for tomorrow.

## Gentle reminder
- By default a notification arrives 30 minutes before the lock starts ("Your phone locks in 30 minutes..."). Tapping it opens your Bible app.
- Change it with the +/- buttons under the start time (steps of 15 minutes, 0 = off). Allow notifications when Android asks.
- It uses an inexact alarm, so it can arrive a few minutes late. It is re-armed after a reboot.

## Samsung note
On Samsung phones, if the service stops after a restart: Settings > Battery > Background usage limits > Never sleeping apps > add Bible First.
If Android says "restricted setting" when enabling accessibility: App info > Bible First > three dots > Allow restricted settings.

## Safety net (prototype only)
Android Settings is on the allow-list so you can always switch the service off. To make the lock stricter, delete the `com.android.settings` line in `AllowList.kt`.

## How it works
- `LockService` (accessibility service) notices which app opens and shows `LockActivity` over anything not on the allow-list.
- `AllowList` builds the allowed set (default phone/SMS/maps apps, Samsung apps, your Bible apps, home screen).
- `Prefs` stores settings and the lock rule: locked = enabled AND not done today AND no active override.
- A "day" rolls over at the start hour, not midnight.
