[![Platform](https://img.shields.io/badge/platform-android-brightgreen)](https://developer.android.com/reference)

# AREngine using Huawei SDK

This is the version that fits into <b>Windows</b>.

## Modules :department_store:

* **app** - The application module with access to **all the application**
* **buildSrc** - Kotlin module that contains the **Dependencies** class and the **AndroidPlugin** that will simplify the build gradle files in every single module defined.
* **core** - Android module that contains the Injecting VM Factory and the needed DI basic annotations **(it can't access any other module)**
* **navigation** - Android Jetpack navigation abstraction (it contains the navigation config file) **cannot access any other module**
* **presentation** - Android module that contains the UI model and VMs used in the app


#  License :oncoming_police_car:

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