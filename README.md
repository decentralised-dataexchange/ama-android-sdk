# AMA-android-sdk

This contains the SDK for Aries mobileagent android, an open source mobile agent for achieving self sovereign identity (SSI), created as part NGI-Trust eSSIF Lab, with efforts from iGrant.io, unikk.me, MyData etc. This SDK can be packaged into any mobile app enabling them with a decentralised datawallet App features. 


Download
--------

```gradle
repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
    maven { url 'https://repo.sovrin.org/repository/maven-public'}
}

dependencies {
    implementation 'com.github.decentralised-dataexchange:ama-android-sdk:1.12.3'

    implementation platform('com.google.firebase:firebase-bom:28.0.1')
    implementation 'com.google.firebase:firebase-dynamic-links-ktx'
}
```

Intializing
--------

````kotlin
DataWallet.initializeSdk(
            this,
            object : InitializeWalletCallback {
                override fun progressUpdate(progress: Int) {
                    when (progress) {
                        InitializeWalletState.INITIALIZE_WALLET_STARTED -> {
                            
                        }
                        InitializeWalletState.INITIALIZE_WALLET_EXTERNAL_FILES_LOADED -> {
                            
                        }
                        InitializeWalletState.POOL_CREATED -> {
                            
                        }
                        InitializeWalletState.WALLET_OPENED -> {
                           
                        }
                    }
                }
            },LedgerNetworkType.getSelectedNetwork(this)
        )
````

After Wallet has opened, we can start accessing the Wallet, Connection, Notification etc.

### Wallet
````
    DataWallet.showWallet(this)
````

### Connections
````
    DataWallet.showConnections(this)
````

### Notifications
````
    DataWallet.showNotifications(this)
````

### Deeplink
To process deeplink for connection, issue credential and verify credential

Add the below intent filter to the activity where we initialize SDK
````
    <intent-filter>
         <action android:name="android.intent.action.VIEW" />

         <category android:name="android.intent.category.DEFAULT" />
         <category android:name="android.intent.category.BROWSABLE" />

         <data android:scheme="didcomm" />
    </intent-filter>
````

Then add the below code to Wallet SDK initialize callback -> InitializeWalletState.WALLET_OPENED
````
    if (intent.scheme == "didcomm") {
        DataWallet.processDeepLink(this, intent.data.toString())
    }
````
## Licensing
Copyright (c) 2021 LCubed AB (iGrant.io), Sweden

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the LICENSE for the specific language governing permissions and limitations under the License.
