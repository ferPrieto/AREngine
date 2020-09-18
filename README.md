[![Platform](https://img.shields.io/badge/platform-android-brightgreen)](https://developer.android.com/reference)

<img src="art/AR-MacOs.jpg"/>

This is an optimized repository AREngine Huawei SDK. Suitable for <b>Mac OS</b>, re-written in Kotlin and modularised, in order to abstract common AR, image rendering and architecture classes.

## Getting Started

1. Clone either this repository or the <b>Windows</b> one, in order to get the main structure and samples.
2. Click on ```Sync Project with Gradle Files``` and ```Make Project``` buttons on Android Studio, to get the dependencies and project ready.
3. Use the different examples in the fragment package (<b>app</b> module) as a guide.

## Modules

* **app** - The application module with access to **the entire application**
* **buildSrc** - Kotlin module that contains the **Dependencies** class and the **AndroidPlugin** that will simplify the build gradle files in every single module defined.
* **core** - Android module that contains the Injecting VM Factory and the needed DI basic annotations
* **navigation** - Android Jetpack navigation abstraction (it contains the navigation config file)
* **presentation** - Android module that contains the VMs used in the app.
* **arEngineCommon** - Java module that contains the AREngine common classes.
* **rendering** - Java module that contains the basic classes that manage the render mechanism.
* **arSdk** - Contains the artifact aar 'HUAWEI_AR_SDK_vX.XX.XX' with all of the AR capabilities.

## Samples

There are four examples that could be used as start point:
* World Sample
* Face Sample
* Hand Sample
* Body Sample

<p align="center">
    <img src="art/World-AR.gif"/>
    <img src="art/Face-AR.gif"/>
    <img src="art/Hand-AR.gif"/>
    <img src="art/Body-AR.gif"/>
</p>


#  License

    Copyright 2020 Fernando Prieto Moyano

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.