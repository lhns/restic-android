# restic-android (Unofficial)

[![Test Workflow](https://github.com/LolHens/restic-android/workflows/build/badge.svg)](https://github.com/LolHens/restic-android/actions?query=workflow%3Abuild)
[![Release Notes](https://img.shields.io/github/release/LolHens/restic-android.svg?maxAge=3600)](https://github.com/LolHens/restic-android/releases/latest)
[![Restic@IoD](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/de.lolhens.resticui)](https://apt.izzysoft.de/fdroid/index/apk/de.lolhens.resticui)
[![GNU General Public License, Version 2](https://img.shields.io/github/license/LolHens/restic-android.svg?maxAge=3600)](https://www.gnu.org/licenses/gpl-2.0.html)

![Icon](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/icon.png)

This project aims to make Backups on Android easy and fast using [Restic](https://restic.net).

It also makes use of [termux/proot](https://github.com/termux/proot) to run the Restic linux binaries on android.

This project is still in a very early state and contributions are welcome!

### Disclaimer
This project is **not** an official app made by the restic team.

Please report any issues on the [restic-android issue tracker](https://github.com/LolHens/restic-android/issues).

## Features
- Manage Restic Repositories (S3, B2, Rest are currently the only supported protocols)
- Manage Restic Snapshots
- Manage Folders for Backup
- Configure Schedules for automatic Backups
- Configure Cleanup Policies for Folders
- Progress Notification

## Roadmap
- Clean up WIP Code
- Support more protocols
- More granular Backup Schedules and Cleanup Policies
- Improve Error messages
- Backup Rules (only backup when charging or only use wifi etc.)

## Screenshots
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/repos.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/repo-edit.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/repo.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/folders.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/folder-edit.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/folder.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/snapshot.png)
![](https://raw.githubusercontent.com/LolHens/restic-android/main/screenshots/about.png)

## Notice
See the file called NOTICE.

## License
This project uses the GNU General Public License, Version 2. See the file called LICENSE.
