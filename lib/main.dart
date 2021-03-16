import 'dart:core';
import 'dart:developer' as developer;
import 'dart:math';

import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';

const title = "Batch Uninstaller";
const CHANNEL = 'com.saha.batchuninstaller/android';
ThemeMode _mode = ThemeMode.light;

void main() {
  SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(statusBarIconBrightness: Brightness.dark));
  runApp(BatchUninstaller());
}

class BatchUninstaller extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: title,
      debugShowCheckedModeBanner: false,
      theme: ThemeData.light().copyWith(
        primaryColor: Colors.deepOrange,
      ),
      darkTheme: ThemeData.dark().copyWith(
        primaryColor: Colors.deepOrange,
      ),
      themeMode: _mode,
      home: HomeScreen(title: title),
    );
  }
}

class HomeScreen extends StatefulWidget {
  HomeScreen({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _counter = 0;
  List listApps = [];
  List listAppsCopy = [];
  static const platform = const MethodChannel(CHANNEL);
  String _radioFilter = "radioFilter";
  String _sortFilter = "sortFilter";
  final prefs = SharedPreferences.getInstance();

  String formatBytes(int bytes, int decimals) {
    if (bytes <= 0) return "0 B";
    const suffixes = ["B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"];
    var i = (log(bytes) / log(1024)).floor();
    return ((bytes / pow(1024, i)).toStringAsFixed(decimals)) +
        ' ' +
        suffixes[i];
  }

  Future<void> _uninstallApp(String pkg) async {
    String batteryLevel;
    try {
      final int result =
          await platform.invokeMethod('uninstallApp', {"pkg": pkg});
      developer.log('RESULT: $result');
    } on PlatformException catch (e) {
      batteryLevel = "Failed to get battery level: '${e.message}'.";
      developer.log(batteryLevel);
    }
  }

  @override
  void initState() {
    super.initState();
  }

  Future getAppList() async {
    if (listApps.length != 0) return 1;
    listApps = await DeviceApps.getInstalledApplications(
        includeSystemApps: true, includeAppIcons: true);
    listAppsCopy = List.of(listApps);
    return 1;
  }

  Future filterApps(int choice) async {
    listApps = List.of(listAppsCopy);
    if (choice == 0) {
      listApps.removeWhere((element) => element.systemApp == false);
    } else if (choice == 1) {
      listApps.removeWhere((element) => element.systemApp == true);
    } else if (choice == 2) {
      //do nothing
    }
    setState(() {});
    Navigator.of(context, rootNavigator: true).pop();
  }

  Future sortApps(int choice) async {
    if (choice == 0) {
      listApps.sort(
          (a, b) => a.appName.toLowerCase().compareTo(b.appName.toLowerCase()));
    } else if (choice == 1) {
      listApps.sort(
          (a, b) => b.appName.toLowerCase().compareTo(a.appName.toLowerCase()));
    } else if (choice == 2) {
      listApps.sort((a, b) => (a.installTimeMillis - b.installTimeMillis));
    } else if (choice == 3) {
      listApps.sort((a, b) => (b.installTimeMillis - a.installTimeMillis));
    }
    setState(() {});
    Navigator.of(context, rootNavigator: true).pop();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(widget.title),
          actions: [
            PopupMenuButton(
              itemBuilder: (BuildContext bc) => [
                PopupMenuItem(
                  child: Text("Refresh"),
                  value: "/refresh",
                ),
                PopupMenuItem(
                  child: Text("Switch theme"),
                  value: "/theme_switch",
                )
              ],
              onSelected: (route) {
                if (route == "/refresh") {
                  listApps.clear();
                  getAppList();
                  setState(() {});
                } else if (route == "/theme_switch") {
                  setState(() {
                    _mode = (_mode == ThemeMode.light)
                        ? ThemeMode.dark
                        : ThemeMode.light;
                    Get.changeThemeMode(_mode);
                  });
                }
              },
            )
          ],
        ),
        body: Stack(children: [
          FutureBuilder(
              future: getAppList(),
              builder: (context, snapshot) {
                if (listApps.length == 0) {
                  return Center(child: CircularProgressIndicator());
                } else {
                  return ListView.builder(
                    itemCount: listApps.length,
                    itemBuilder: (context, int i) => Column(
                      children: [
                        new ListTile(
                            leading: Image.memory(listApps[i].icon),
                            title: new Text(listApps[i].appName),
                            subtitle: new Text(listApps[i].packageName),
                            onLongPress: () =>
                                _uninstallApp(listApps[i].packageName)
                            //onTap: (){
                            //  DeviceApps.openApp(listApps[i].package);
                            //},
                            ),
                      ],
                    ),
                  );
                }
              }),
          Align(
            alignment: Alignment.bottomRight,
            child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: FloatingActionButton.extended(
                    label: Text("Sort"),
                    icon: Icon(Icons.sort),
                    onPressed: () async {
                      showDialog<void>(
                          context: context,
                          builder: (BuildContext context) {
                            int selectedRadio = 0;
                            return AlertDialog(
                              content: StatefulBuilder(
                                builder: (BuildContext context,
                                    StateSetter setState) {
                                  return Column(
                                    mainAxisSize: MainAxisSize.min,
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: <Widget>[
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 0,
                                            groupValue: _sortFilter,
                                            onChanged: (Object value) {
                                              sortApps(value);
                                            },
                                          ),
                                          new Text(
                                            'Name (ASC)',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 1,
                                            groupValue: _sortFilter,
                                            onChanged: (Object value) {
                                              sortApps(value);
                                            },
                                          ),
                                          new Text(
                                            'Name (DESC)',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 2,
                                            groupValue: _sortFilter,
                                            onChanged: (Object value) {
                                              sortApps(value);
                                            },
                                          ),
                                          new Text(
                                            'Installed Date (ASC)',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 3,
                                            groupValue: _sortFilter,
                                            onChanged: (Object value) {
                                              sortApps(value);
                                            },
                                          ),
                                          new Text(
                                            'Installed Date (DESC)',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                    ],
                                  );
                                },
                              ),
                            );
                          });
                    })),
          ),
          Align(
            alignment: Alignment.bottomLeft,
            child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: FloatingActionButton.extended(
                    label: Text("Filter"),
                    icon: Icon(Icons.filter),
                    onPressed: () async {
                      showDialog<void>(
                          context: context,
                          builder: (BuildContext context) {
                            int selectedRadio = 0;
                            return AlertDialog(
                              content: StatefulBuilder(
                                builder: (BuildContext context,
                                    StateSetter setState) {
                                  return Column(
                                    mainAxisSize: MainAxisSize.min,
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: <Widget>[
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 0,
                                            groupValue: _radioFilter,
                                            onChanged: (Object value) {
                                              filterApps(value);
                                            },
                                          ),
                                          new Text(
                                            'System Apps',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 1,
                                            groupValue: _radioFilter,
                                            onChanged: (Object value) {
                                              filterApps(value);
                                            },
                                          ),
                                          new Text(
                                            'User Apps',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                      new Row(
                                        children: <Widget>[
                                          new Radio(
                                            value: 2,
                                            groupValue: _radioFilter,
                                            onChanged: (Object value) {
                                              filterApps(value);
                                            },
                                          ),
                                          new Text(
                                            'No Filter',
                                            style:
                                                new TextStyle(fontSize: 16.0),
                                          ),
                                        ],
                                      ),
                                    ],
                                  );
                                },
                              ),
                            );
                          });
                    })),
          )
        ]));
  }
}
